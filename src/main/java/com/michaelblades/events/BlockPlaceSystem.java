package com.michaelblades.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.michaelblades.SprinklersPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class BlockPlaceSystem  extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private final SprinklersPlugin plugin;

    public BlockPlaceSystem(SprinklersPlugin plugin) {
        super(PlaceBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl PlaceBlockEvent event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = store.getComponent(ref, Player.getComponentType());
        var itemId = event.getItemInHand().getItemId();
//        player.sendMessage(Message.raw(itemId));
//        NotificationUtil.sendNotificationToUniverse("Testing");

        if (itemId.contains("Michael_Sprinkler_SprinklerBlock")) {
           // player.sendMessage(Message.raw("Adding Soil!"));
            var worldName = store.getExternalData().getWorld().getName();
            var pos = event.getTargetBlock();

            plugin.addIrrigator(worldName, pos);

        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
