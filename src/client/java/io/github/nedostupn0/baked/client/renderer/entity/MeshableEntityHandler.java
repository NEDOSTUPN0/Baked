package io.github.nedostupn0.baked.client.renderer.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.Entity;

/** How a block-like entity is baked. Snapshots are taken on the main thread. */
public interface MeshableEntityHandler<T extends Entity> {

    boolean isEnabled();

    /** Baked state, or null to use the vanilla renderer. */
    MeshableEntityTracker.@Nullable Snapshot snapshot(T entity);

    /**
     * Placement of entities that move without synced data; once it changes, vanilla renders the entity until it
     * settles. Null if it never moves on its own.
     */
    default @Nullable Object placementKey(T entity) {
        return null;
    }

    /** Called once the snapshot replaced the previous one (main thread). */
    default void onApplied(T entity, MeshableEntityTracker.Snapshot snapshot) {}

    /** Called when the entity's snapshot is dropped (replaced, removed, suspended). */
    default void onRemoved(int entityId) {}
}
