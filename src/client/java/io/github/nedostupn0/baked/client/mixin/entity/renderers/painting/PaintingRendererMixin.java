package io.github.nedostupn0.baked.client.mixin.entity.renderers.painting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.world.entity.decoration.painting.Painting;

@Mixin(PaintingRenderer.class)
public abstract class PaintingRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/painting/Painting;Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;F)V", at = @At("HEAD"))
    private void baked$linkEntity(Painting entity, PaintingRenderState state, float partialTicks, CallbackInfo ci) {
        ((EntityRenderStateExt) state).entity(entity);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/painting/Painting;Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;F)V", at = @At("TAIL"))
    private void baked$dropMeshed(Painting entity, PaintingRenderState state, float partialTicks, CallbackInfo ci) {
        if (RenderModeManager.isPartMeshed(state, MeshableEntityTracker.ALL_PARTS)) state.variant = null;
    }
}
