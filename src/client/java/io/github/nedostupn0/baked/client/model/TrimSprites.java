package io.github.nedostupn0.baked.client.model;

import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;

/** Terrain can't sample runtime trim palette slots: maps a slot to its block atlas copy, {@code <base>_<palette>}. */
public final class TrimSprites {
    private static final Map<UvMapping, Identifier> spriteByHandle = new WeakHashMap<>();

    private TrimSprites() {}

    public static synchronized void record(UvMapping handle, Identifier baseTexture, Identifier paletteId) {
        String path = paletteId.getPath();
        String palette = path.substring(path.lastIndexOf('/') + 1);
        spriteByHandle.put(handle, baseTexture.withSuffix("_" + palette));
    }

    public static synchronized @Nullable Identifier spriteName(UvMapping handle) {
        return spriteByHandle.get(handle);
    }
}
