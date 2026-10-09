package io.github.nedostupn0.baked.client.mixin.level;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.nedostupn0.baked.client.prediction.PredictedBlockEntities;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow @Final private BlockStatePredictionHandler blockStatePredictionHandler;

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void baked$snapshotPredictedBlockEntity(BlockPos pos, BlockState state, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        if (blockStatePredictionHandler.isPredicting()) PredictedBlockEntities.beforePredictedChange((ClientLevel)(Object)this, pos, state);
    }

    @Inject(method = "syncBlockState", at = @At("TAIL"))
    private void baked$restoreAfterAck(BlockPos pos, BlockState state, Vec3 playerPos, CallbackInfo ci) {
        PredictedBlockEntities.afterServerState((ClientLevel)(Object)this, pos, state);
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    private void baked$restoreAfterUpdate(BlockPos pos, BlockState state, int updateFlags, CallbackInfo ci) {
        PredictedBlockEntities.afterServerState((ClientLevel)(Object)this, pos, state);
    }
}
