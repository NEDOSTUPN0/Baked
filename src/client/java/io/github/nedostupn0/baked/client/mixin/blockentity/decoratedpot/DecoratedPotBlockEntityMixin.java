package io.github.nedostupn0.baked.client.mixin.blockentity.decoratedpot;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;

@Mixin(DecoratedPotBlockEntity.class)
public abstract class DecoratedPotBlockEntityMixin{

    @Unique PotDecorations defaultPotDecorations = new PotDecorations(Items.BRICK, Items.BRICK, Items.BRICK, Items.BRICK);

    @Shadow
    private PotDecorations decorations;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {

        BlockEntity be = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;

        ext.baked$isSupported(Registry.isSupported("decorated_pot", be.getType()));
        baked$updatePot();
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void baked$load(CallbackInfo ci) {
        baked$updatePot();
    }

    @Inject(method = "applyImplicitComponents", at = @At("RETURN"))
    public void baked$applyComponents(CallbackInfo ci) {
        baked$updatePot();
    }

    @Inject(method = "triggerEvent", at = @At("RETURN"))
    public void baked$triggerEvent(final int event, final int data, CallbackInfoReturnable<Boolean> ci) {
        DecoratedPotBlockEntity be = (DecoratedPotBlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        RenderModeManager.setRenderModeDelayed(ext, RenderMode.ENTITY, be.getBlockPos());
        ext.baked$setTimer(be.wobbleStartedAtTick, be.lastWobbleStyle.duration);
    }

    @Inject(method = "getDecorations", at = @At("RETURN"))
    public void baked$updateTimer(CallbackInfoReturnable<PotDecorations> cir) {
        DecoratedPotBlockEntity be = (DecoratedPotBlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        if(ext.baked$isTimerFinished()) RenderModeManager.setRenderModeDelayed(ext, RenderMode.TERRAIN, be.getBlockPos());
    }

    @Unique
    private void baked$updatePot(){
        DecoratedPotBlockEntity be = (DecoratedPotBlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)be;
        if(!decorations.equals(PotDecorations.EMPTY) && !decorations.equals(defaultPotDecorations)){
            ext.baked$forceEntity(true);
        }
        else{
            ext.baked$forceEntity(false);
        }
    }
}
