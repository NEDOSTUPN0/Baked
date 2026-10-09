package io.github.nedostupn0.baked.client.mixin.blockentity.shulkerbox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.AnimationStatus;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void baked$init(CallbackInfo ci) {
        
        BlockEntity be = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        
        ext.baked$isSupported(Registry.isSupported("shulker_box", be.getType()));
    }

    @Inject(method = "updateAnimation", at = @At("RETURN"))
    public void baked$updateAnimation(final Level level, final BlockPos pos, final BlockState blockState, CallbackInfo ci){
        ShulkerBoxBlockEntity be = (ShulkerBoxBlockEntity) (Object) this;
        if(be.getAnimationStatus() != AnimationStatus.CLOSED){
            RenderModeManager.setRenderModeDelayed(be, RenderMode.ENTITY, be.getBlockPos());
        }
        else{
            RenderModeManager.setRenderModeDelayed(be, RenderMode.TERRAIN, be.getBlockPos());
        }
    }
}
