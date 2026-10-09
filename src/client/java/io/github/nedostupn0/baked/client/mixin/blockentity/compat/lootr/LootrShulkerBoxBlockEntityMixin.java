package io.github.nedostupn0.baked.client.mixin.blockentity.compat.lootr;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
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
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.AnimationStatus;
import net.minecraft.world.level.block.state.BlockState;
import noobanidus.mods.lootr.common.block.entity.LootrShulkerBoxBlockEntity;

@Pseudo
@Mixin(value = LootrShulkerBoxBlockEntity.class, remap = false)
public class LootrShulkerBoxBlockEntityMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void baked$register(CallbackInfo ci){
        
        BlockEntity be = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        
        ext.baked$isSupported(Registry.isSupported("shulker_box", be.getType()));
        ext.baked$hasSpecialRenderer(true);
    }

    @Inject(method = "updateAnimation", at = @At("HEAD"))
    private void baked$lidAnimateTick(final Level level, final BlockPos pos, final BlockState state, CallbackInfo ci) {
        LootrShulkerBoxBlockEntity be = (LootrShulkerBoxBlockEntity)(Object)this;
        if(be.getAnimationStatus() != AnimationStatus.CLOSED){
            RenderModeManager.setRenderModeDelayed(be, RenderMode.ENTITY, be.getBlockPos());
        }
        else{
            RenderModeManager.setRenderModeDelayed(be, RenderMode.TERRAIN, be.getBlockPos());
        }
    }
}
