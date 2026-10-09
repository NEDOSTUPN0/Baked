package io.github.nedostupn0.baked.client.mixin.renderer.compat.sodium;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.BlockEntityModelsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker.MeshableEntityData;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker.Piece;
import io.github.nedostupn0.baked.client.model.TintPalette;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import io.github.nedostupn0.baked.client.util.meshing.SectionMeshingUtil;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Pseudo
@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public class ChunkBuilderMeshingTaskMixin {

    @Unique private final BlockEntityModelsManager blockEntityModelsManager = new BlockEntityModelsManager();
    @Unique private final SectionPos sectionPos = ((ChunkBuilderMeshingTask)(Object) this).getRenderSection().getPosition();

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState baked$getBlockState(LevelSlice slice, int x, int y, int z, Operation<BlockState> original, @Share("be") LocalRef<BlockEntity> beRef){
        beRef.set(slice.getBlockEntity(x, y, z));
        return original.call(slice, x, y, z);
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
    private RenderShape baked$getRenderShape(BlockState state, Operation<RenderShape> original, @Share("be") LocalRef<BlockEntity> beRef){
        return SectionMeshingUtil.getCorrectedRenderShape(state, beRef.get(), sectionPos, original.call(state));
    }

    @WrapOperation(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    public void baked$wrapRenderModel(BlockRenderer instance, BlockStateModel originalModel, BlockState state, BlockPos pos, BlockPos origin, Operation<Void> original, @Share("be") LocalRef<BlockEntity> beRef) {
        BlockStateModel model = SectionMeshingUtil.getCorrectedModel(state, beRef.get(), originalModel, pos);
        
        original.call(instance, model, state, pos, origin);
    }

    @WrapOperation(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ExtendedBlockEntityType;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Z"
        )
    )
    private boolean baked$wrapShouldRender(BlockEntityType<?> type, BlockGetter slice, BlockPos pos, BlockEntity be, Operation<Boolean> original) {
        BlockEntityExt ext = (BlockEntityExt) be;
        if(ext != null && ext.baked$isEnabled() && (!(ext.baked$forceEntity() || !ext.baked$isSupported() || ext.baked$renderModeDelayed() == RenderMode.ENTITY || ext.baked$renderBoth()) || ext.baked$shouldSkipRendering())) {
            return false;
        }
        return original.call(type, slice, pos, be);
    }

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;release()V"))
    private void baked$appendMeshData(CallbackInfoReturnable<ChunkBuildOutput> ci, @Local BlockRenderer blockRenderer, @Local LevelSlice slice){
        if(!SettingsManager.MOD_TOGGLE.getValue()) return;

        Collection<MeshableEntityData> entitiesData = MeshableEntityTracker.getMeshableEntities(sectionPos);
        if(entitiesData == null) return;

        MutableBlockPos pos = new MutableBlockPos();
        MutableBlockPos modelOffset = new MutableBlockPos();

        for(MeshableEntityData data : entitiesData){
            if(!data.isEnabled() || data.level() != Minecraft.getInstance().level){
                MeshableEntityTracker.onCompiled(sectionPos, data, false);
                continue;
            }
            for(Piece piece : data.snapshot().pieces()){
                if(!SectionPos.of(piece.pos()).equals(sectionPos)) continue;
                pos.set(piece.pos());
                modelOffset.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                blockRenderer.renderModel(piece.model(), TintPalette.CARRIER, pos, modelOffset);
            }
            MeshableEntityTracker.onCompiled(sectionPos, data, true);
        }
    }

}