package io.github.nedostupn0.baked.client.mixin.blockentity.bell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;

@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void baked$init(CallbackInfo ci) {
        BlockEntity be = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;

        ext.baked$isSupported(Registry.isSupported("bell", be.getType()));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void tick(final Level level, final BlockPos pos, final BlockState state, final BellBlockEntity entity, final @Coerce Object onResonationEnd, CallbackInfo ci) {
        if (level != null && !level.isClientSide()) return;
        BlockEntityExt ext = (BlockEntityExt)entity;
        if(entity.shaking){
            RenderModeManager.setRenderModeDelayed(ext, RenderMode.ENTITY, pos);
        }
        else{
            RenderModeManager.setRenderModeDelayed(ext, RenderMode.TERRAIN, pos);
        }
    }
}
