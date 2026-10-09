package io.github.nedostupn0.baked.client.mixin.texture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.nedostupn0.baked.client.model.TrimSprites;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.resources.Identifier;

@Mixin(PalettedTextureManager.class)
public abstract class PalettedTextureManagerMixin {

    @Inject(method = "getOrPrepare", at = @At("RETURN"))
    private void baked$recordHandle(Identifier baseTexture, Identifier paletteId, CallbackInfoReturnable<PalettedTextureManager.Handle> cir) {
        TrimSprites.record(cir.getReturnValue(), baseTexture, paletteId);
    }
}
