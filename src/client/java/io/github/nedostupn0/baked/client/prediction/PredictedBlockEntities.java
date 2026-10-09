package io.github.nedostupn0.baked.client.prediction;

import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Restores block entity data after a rejected block prediction. The server resends only the block state, so heads lose
 * skins and signs text (Hypixel, spawn protection).
 */
public final class PredictedBlockEntities {
    /** Predictions are acknowledged within a few ticks; anything older is stale. */
    private static final long MAX_AGE_TICKS = 200;
    /** Never acknowledged snapshots (world change, lost packets) are dropped past this size. */
    private static final int PRUNE_THRESHOLD = 64;

    private static final Long2ObjectMap<Snapshot> snapshots = new Long2ObjectOpenHashMap<>();

    private PredictedBlockEntities() {}

    private record Snapshot(ClientLevel level, BlockState state, BlockEntityType<?> type, CompoundTag data, long time) {}

    /** The client is about to replace a block on its own. Main thread. */
    public static void beforePredictedChange(ClientLevel level, BlockPos pos, BlockState newState) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.getBlockState() == newState) return;
        long key = pos.asLong();
        Snapshot existing = snapshots.get(key);
        // Several predictions on one block: the first snapshot holds the server's data.
        if (existing != null && existing.level == level && level.getGameTime() - existing.time <= MAX_AGE_TICKS) return;
        if (snapshots.size() >= PRUNE_THRESHOLD) {
            snapshots.values().removeIf(s -> s.level != level || level.getGameTime() - s.time > MAX_AGE_TICKS);
        }
        snapshots.put(key, new Snapshot(level, be.getBlockState(), be.getType(), be.saveWithoutMetadata(level.registryAccess()), level.getGameTime()));
    }

    /** The server's state of the block. During a prediction it is applied with the acknowledgement. Main thread. */
    public static void afterServerState(ClientLevel level, BlockPos pos, BlockState serverState) {
        if (snapshots.isEmpty()) return;
        long key = pos.asLong();
        Snapshot snapshot = snapshots.get(key);
        if (snapshot == null) return;
        if (snapshot.level != level || level.getGameTime() - snapshot.time > MAX_AGE_TICKS || serverState != snapshot.state) {
            // The change went through (or the snapshot is from another time or world).
            snapshots.remove(key);
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.getType() != snapshot.type || be.getBlockState() != snapshot.state) return;
        snapshots.remove(key);

        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(be.problemPath(), LogUtils.getLogger())) {
            be.loadWithComponents(TagValueInput.create(reporter, level.registryAccess(), snapshot.data));
        }
        // Baked block entities live in the section mesh.
        level.sendBlockUpdated(pos, snapshot.state, snapshot.state, 8);
    }
}
