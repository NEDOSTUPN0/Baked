package io.github.nedostupn0.baked.client.util.meshing;

import io.github.nedostupn0.baked.client.renderer.blockentity.BlockEntityModelsManager;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import io.github.nedostupn0.baked.client.resources.ResourceUtil;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SectionMeshingUtil {

    private static final BlockEntityModelsManager blockEntityModelsManager = new BlockEntityModelsManager();

    public static RenderShape getCorrectedRenderShape(BlockState state, BlockEntity be, SectionPos sectionPos, RenderShape originalRenderShape){
        if(state.hasBlockEntity()){
            BlockEntityExt ext = (BlockEntityExt) be;
            if(ext != null && ext.baked$isSupported()) {
                RenderModeManager.updateBlockEntityOnChunkRemesh(ext, sectionPos);
                if(ext.baked$isEnabled() && ext.baked$renderModeDelayed() == RenderMode.TERRAIN && !ext.baked$forceEntity()){
                    return RenderShape.MODEL;
                }
            }
        }
        return originalRenderShape;
    }

    public static BlockStateModel getCorrectedModel(BlockState state, BlockEntity be, BlockStateModel originalModel, BlockPos pos){
        if(state.hasBlockEntity()){

            BlockStateModel model = originalModel;
            BlockEntityExt ext = (BlockEntityExt) be;

            if(ext != null){
                if(ext.baked$renderModeDelayed() != RenderMode.TERRAIN || !ext.baked$isSupported() || !ext.baked$isEnabled() || ext.baked$forceEntity()){
                    model = ResourceUtil.getDefaultModel(be.getBlockState());
                }
                else if(ext.baked$hasSpecialRenderer()) model = blockEntityModelsManager.getModel(state, originalModel, be);
            }

            if(model == null) model = ResourceUtil.getDefaultModel(be.getBlockState());

            return model;
        }
        else{
            return originalModel;
        }
    }
}
