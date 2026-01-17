package com.michaelblades.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.michaelblades.SprinklersPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class BlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final SprinklersPlugin plugin;

    public BlockBreakSystem(SprinklersPlugin plugin) {
        super(BreakBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent breakBlockEvent) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = store.getComponent(ref, Player.getComponentType());
        var block = breakBlockEvent.getBlockType().getId();
        if (block.equals("Empty")) return;
        if (block.contains("Michael_Sprinkler_SprinklerBlock")) {
            var worldName = store.getExternalData().getWorld().getName();
            var pos = breakBlockEvent.getTargetBlock();

            plugin.removeIrrigator(worldName, pos);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
