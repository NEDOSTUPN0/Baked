package io.github.nedostupn0.baked.client.mixin.entity.renderers.itemframe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.util.entity.ItemFrameUtil;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;

/** Strips baked parts from the render state, so mods wrapping the submits don't draw them twice. */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("HEAD"))
    private void baked$linkEntity(ItemFrame entity, ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        ((EntityRenderStateExt) state).entity(entity);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V", at = @At("TAIL"))
    private void baked$dropMeshedParts(ItemFrame entity, ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        if (RenderModeManager.isPartMeshed(state, ItemFrameUtil.PART_FRAME)) state.frameModel.clear();
        if (RenderModeManager.isPartMeshed(state, ItemFrameUtil.PART_ITEM)) state.item.clear();
        if (RenderModeManager.isPartMeshed(state, ItemFrameUtil.PART_MAP)) state.mapId = null;
    }
}
