package io.github.nedostupn0.baked.client.mixin.blockentity.skull;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import io.github.nedostupn0.baked.client.renderer.skull.SkinHolder;
import io.github.nedostupn0.baked.client.renderer.skull.SkinPool;
import io.github.nedostupn0.baked.client.util.blockentity.SkullBlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin implements SkinHolder {
    @Unique private SkinPool.@Nullable Slot baked$skinSlot;

    @Override public SkinPool.@Nullable Slot baked$skinSlot() { return baked$skinSlot; }
    @Override public void baked$skinSlot(SkinPool.@Nullable Slot slot) { baked$skinSlot = slot; }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void baked$init(CallbackInfo ci) {
        
        SkullBlockEntity be = (SkullBlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        
        if(SkullBlockUtil.isDynamicTexture(be)) ext.baked$renderMode(RenderMode.ENTITY);
        ext.baked$isSupported(Registry.isSupported("skull", be.getType()));
        ext.baked$hasSpecialRenderer(SkullBlockUtil.hasBuiltInTexture(be));
    }

    @Inject(method = "animation", at = @At("TAIL"))
    private static void baked$updateAnimation(final Level level, final BlockPos pos, final BlockState state, final SkullBlockEntity entity, CallbackInfo ci){
        SkullBlockEntity be = (SkullBlockEntity) entity;
        if(be.isAnimating || SkullBlockUtil.isDynamicTexture(be)){
            RenderModeManager.setRenderModeDelayed(be, RenderMode.ENTITY, be.getBlockPos());
        }
        else{
            RenderModeManager.setRenderModeDelayed(be, RenderMode.TERRAIN, be.getBlockPos());
        }
    }

    @Inject(method = "applyImplicitComponents", at = @At("RETURN"))
    private void baked$checkProfile(CallbackInfo ci){
        SkullBlockUtil.refresh((SkullBlockEntity)(Object)this);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void baked$checkProfileBis(CallbackInfo ci){
        SkullBlockUtil.refresh((SkullBlockEntity)(Object)this);
    }
}
