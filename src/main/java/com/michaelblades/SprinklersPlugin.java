package com.michaelblades;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.michaelblades.events.BlockBreakSystem;
import com.michaelblades.events.BlockPlaceSystem;

import javax.annotation.Nonnull;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SprinklersPlugin extends JavaPlugin {

    public SprinklersPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    private ScheduledFuture<?> loopTask;

    // Farmland block id(s) - you may have separate "dry" and "wet" IDs or a "data value" state.

    private static final int RADIUS = 2;
    private static final long PERIOD_MS = 10000; // run every 10 seconds

    private SprinklerConfig config;

    private final ConcurrentHashMap<String, Set<Vector3i>> irrigatorsByWorld = new ConcurrentHashMap<>();


    @Override
    protected void setup() {
        super.setup();
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceSystem(this));
        this.getEntityStoreRegistry().registerSystem(new BlockBreakSystem(this));
        loopTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::tickAllIrrigatorsSafe,
                PERIOD_MS,
                PERIOD_MS,
                TimeUnit.MILLISECONDS
        );
        this.config = new SprinklerConfig(Paths.get("config", "Michael_Sprinkler"));
    }

    @Override
    protected void start() {
        loadIrrigators();
    }

    public void loadIrrigators() {
        config.load();
        var irrigators = config.getSprinklers();
        if (!irrigators.isEmpty()) {
            // Create a copy of the list to avoid ConcurrentModificationException
            var irrigatorsCopy = new java.util.ArrayList<>(irrigators);
            irrigatorsCopy.forEach(sprinklerEntry -> {
                // Don't use addIrrigator here - it saves to config
                // Just add to memory since we're loading from config
                Vector3i pos = new Vector3i(sprinklerEntry.getX(), sprinklerEntry.getY(), sprinklerEntry.getZ());
                irrigatorsByWorld.computeIfAbsent(sprinklerEntry.getWorldId(), _id -> ConcurrentHashMap.newKeySet()).add(pos);
            });
        }
    }

    public void addIrrigator(String world, Vector3i pos) {
        SprinklerConfig.SprinklerEntry entry =
                new SprinklerConfig.SprinklerEntry(world, pos.x, pos.y, pos.z);
        config.addSprinkler(entry);
        irrigatorsByWorld.computeIfAbsent(world, _id -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    public void removeIrrigator(String world, Vector3i pos) {
        // Remove from config
        boolean removed = config.removeSprinkler(world, pos.x, pos.y, pos.z);

        if (removed) {
            // Remove from the in-memory map
            Set<Vector3i> positions = irrigatorsByWorld.get(world);
            if (positions != null) {
                positions.remove(pos);

                // If this was the last irrigator in this world, remove the world entry
                if (positions.isEmpty()) {
                    irrigatorsByWorld.remove(world);
                }
            }
        }
    }

    private void tickAllIrrigatorsSafe() {

        irrigatorsByWorld.forEach((worldId, positions) -> {
            var world = Universe.get().getWorld(worldId);
            if (world == null) return;

            // Queue hydration work onto that world's thread
            world.execute(() -> {
                //NotificationUtil.sendNotificationToUniverse("Starting");
                for (Vector3i irrigatorPos : positions) {
                    hydrateAround(irrigatorPos, world);
                }
            });
        });
    }


    private void hydrateAround(Vector3i origin, World world) {
        var blockType = world.getBlockType(origin).getId();
        IndexedLookupTableAssetMap<String, Fluid> fluidMap = Fluid.getAssetMap();
        var lowerBlockType = world.getFluidId(origin.x, origin.y - 1, origin.z);
        Fluid fluidBelow = fluidMap.getAsset(lowerBlockType);
        if (!fluidBelow.getId().contains("Water_Source")) {
            return;
        }
        var sprinklerRadius = 1;

        if (blockType.contains("Thorium")) {
            sprinklerRadius = 2;
        } else if (blockType.contains("Cobalt")) {
            sprinklerRadius = 3;
        }

        for (int dx = -sprinklerRadius; dx <= sprinklerRadius; dx++) {
            for (int dz = -sprinklerRadius; dz <= sprinklerRadius; dz++) {

                int x = origin.x + dx;
                int y = origin.y - 1;
                int z = origin.z + dz;

                WorldChunk worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                Ref<ChunkStore> blockRef = worldChunk.getBlockComponentEntity(x, y, z);
                if (blockRef == null) {
                    blockRef = BlockModule.ensureBlockEntity(worldChunk, x, y, z);
                }

                if (blockRef == null) {
                    // Do Nothing
                } else {
                    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                    WorldTimeResource worldTimeResource = world.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
                    TilledSoilBlock soil = chunkStore.getComponent(blockRef, TilledSoilBlock.getComponentType());
                    Instant wateredUntil = worldTimeResource.getGameTime().plus(86400, ChronoUnit.SECONDS);
                    soil.setWateredUntil(wateredUntil);
                    worldChunk.setTicking(x, y, z, true);
                    worldChunk.getBlockChunk().getSectionAtBlockY(y).scheduleTick(ChunkUtil.indexBlock(x, y, z), wateredUntil);
                    worldChunk.setTicking(x, y + 1, z, true);
                }
            }
        }
    }
}