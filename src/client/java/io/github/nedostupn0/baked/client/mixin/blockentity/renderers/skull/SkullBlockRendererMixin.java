package io.github.nedostupn0.baked.client.mixin.blockentity.renderers.skull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    public void baked$cancelSubmit(CallbackInfo ci, @Local SkullBlockRenderState state){
        if(!RenderModeManager.shouldRenderEntity(state) && SettingsManager.OPTIMISED_SKULLS.getValue()) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void baked$cancelExtract(CallbackInfo ci, @Local SkullBlockRenderState state, @Local SkullBlockEntity be){
        ((BlockEntityRenderStateExt)state).blockEntity(be);
        if(!RenderModeManager.shouldRenderEntity(be) && SettingsManager.OPTIMISED_SKULLS.getValue()) ci.cancel();
    }
}
