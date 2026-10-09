package io.github.nedostupn0.baked.client.renderer.skull;

import org.jspecify.annotations.Nullable;

/** A player head block entity: the {@link SkinPool} slot its baked model samples, if any. */
public interface SkinHolder {
    SkinPool.@Nullable Slot baked$skinSlot();

    void baked$skinSlot(SkinPool.@Nullable Slot slot);
}
