package io.github.nedostupn0.baked.client.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Maps model UVs (0..1 over the texture) into the atlas: a whole sprite, or a part of one for virtual sprites. */
public interface UvMapping {
    float getU(float offset);

    float getV(float offset);

    static UvMapping of(TextureAtlasSprite sprite) {
        return new UvMapping() {
            @Override
            public float getU(float offset) {
                return sprite.getU(offset);
            }

            @Override
            public float getV(float offset) {
                return sprite.getV(offset);
            }
        };
    }
}
