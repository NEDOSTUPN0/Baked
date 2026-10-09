package io.github.nedostupn0.baked.client.mixin.renderer.compat.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.SpecialBlockEntityRenderingManager;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

@Pseudo
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class SodiumWorldRendererMixin {

    @Inject(method = "extractBlockEntity", at = @At("HEAD"), cancellable = true)
    public void baked$preventUselessExtraction(CallbackInfo ci, @Local(argsOnly = true) BlockEntity be){
        if(SpecialBlockEntityRenderingManager.shouldSkipRendering(be)){
            ci.cancel();
        }
    }

    @Inject(
        method = "isEntityVisible",
        at = @At("HEAD"),
        cancellable = true
    )
    private <T extends Entity> void baked$shouldRenderEntity(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) T entity) {
        if(RenderModeManager.canSkipMeshedEntity(entity)){
            cir.setReturnValue(false);
        }
    }
}
