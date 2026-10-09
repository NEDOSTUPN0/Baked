package io.github.nedostupn0.baked.client.mixin.blockentity.chest;

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
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(EnderChestBlockEntity.class)
public abstract class EnderChestBlockEntityMixin{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void baked$init(CallbackInfo ci) {
        
        BlockEntity be = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        
        ext.baked$isSupported(Registry.isSupported("chest", be.getType()));
    }

    @Inject(method = "lidAnimateTick", at = @At("RETURN"))
    private static void baked$lidAnimateTick(final Level level, final BlockPos pos, final BlockState state, final EnderChestBlockEntity entity, CallbackInfo ci) {
        BlockEntityExt ext = (BlockEntityExt)entity;
        if(entity.getOpenNess(0.5f) > 0){
            RenderModeManager.setRenderModeDelayed(ext, RenderMode.ENTITY, pos);
        }
        else{
            RenderModeManager.setRenderModeDelayed(ext, RenderMode.TERRAIN, pos);
        }
    }
}
