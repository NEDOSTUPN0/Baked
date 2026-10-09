package io.github.nedostupn0.baked.client.mixin.texture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.skull.SkinPool;
import net.minecraft.client.renderer.texture.TextureAtlas;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {

    @Inject(method = "upload", at = @At("TAIL"))
    private void baked$refillSkinPool(CallbackInfo ci) {
        TextureAtlas atlas = (TextureAtlas)(Object)this;
        if (atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) SkinPool.onAtlasUploaded(atlas);
    }
}
