package io.github.nedostupn0.baked.client.renderer.misc;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.chunk.ChunkTaskHolder;
import io.github.nedostupn0.baked.client.config.Option;
import io.github.nedostupn0.baked.client.registry.MaterialGetter;
import io.github.nedostupn0.baked.client.registry.ModelLayerLocationGetter;
import io.github.nedostupn0.baked.client.registry.Registry;
import io.github.nedostupn0.baked.client.registry.SpecialModelGetter;
import io.github.nedostupn0.baked.client.registry.SpecialModelGetter.SpecialModelProvider;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityRenderStateExt;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityRenderStateExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RenderModeManager {

    public static void setRenderMode(BlockEntity be, RenderMode mode, BlockPos pos){
        setRenderMode((BlockEntityExt)be, mode, pos);
    }

    public static void setRenderMode(BlockEntityExt ext, RenderMode mode, BlockPos pos){
        if(ext.baked$renderMode() != mode) {
            setDirty(pos);
            ext.baked$renderMode(mode);
            ext.baked$renderModeDelayed(mode);
        }
    }

    /**
     * Fully baked entity that nothing forces back to vanilla (name tag, outline, crosshair target): no render state is
     * needed.
     */
    public static boolean canSkipMeshedEntity(Entity entity){
        if(!(entity instanceof EntityExt ext) || !ext.baked$isSupported()) return false;
        if(ext.baked$renderMode() != RenderMode.TERRAIN || !ext.baked$fullyMeshed()) return false;
        if(!MeshableEntityTracker.isEnabled(entity)) return false;
        if(entity.shouldShowName() || entity.isCurrentlyGlowing()) return false;
        return Minecraft.getInstance().crosshairPickEntity != entity;
    }

    /** Set while a vanilla renderer runs into a capturing collector, so our own skips stay out of the way. */
    public static boolean capturing = false;

    /** True when the given handler-defined part of the entity is drawn by the section mesh. */
    public static boolean isPartMeshed(EntityRenderState state, int part){
        if(capturing) return false;
        return isPartMeshed(((EntityRenderStateExt)state).entity(), part);
    }

    public static boolean isPartMeshed(Entity entity, int part){
        if(!(entity instanceof EntityExt ext) || !ext.baked$isSupported()) return false;
        if(ext.baked$renderMode() != RenderMode.TERRAIN || (ext.baked$meshedParts() & part) == 0) return false;
        return !entity.isCurrentlyGlowing();
    }

    public static boolean shouldRenderEntity(EntityRenderState state){
        return shouldRenderEntity(((EntityRenderStateExt)state).entity());
    }

    public static <T extends Entity> boolean shouldRenderEntity(T entity){
        return shouldRenderEntity((EntityExt)entity);
    }

    public static <T extends Entity> boolean shouldRenderEntity(EntityExt ext){
        return ext.baked$renderMode() == RenderMode.ENTITY;
    }

    /** Set while baked block entities render only their breaking overlay. */
    public static boolean crumblingPass = false;

    public static boolean shouldRenderEntity(BlockEntityRenderState state){
        return shouldRenderEntity(((BlockEntityRenderStateExt)state).blockEntity());
    }

    public static <T extends BlockEntity> boolean shouldRenderEntity(boolean setting, BlockEntityRenderState state){
        return shouldRenderEntity(setting, ((BlockEntityRenderStateExt)state).blockEntity());
    }

    public static <T extends BlockEntity> boolean shouldRenderEntity(boolean setting, T be){
        return shouldRenderEntity(setting, (BlockEntityExt) be, be);
    }

    public static <T extends BlockEntity> boolean shouldRenderEntity(boolean setting, BlockEntityExt ext, BlockEntity be){
        if(crumblingPass) return true;
        return ext == null || !be.hasLevel() || !ext.baked$isSupported() || setting || !SettingsManager.MOD_TOGGLE.getValue();
    }

    public static <T extends BlockEntity> boolean shouldRenderEntity(T be){
        return shouldRenderEntity((BlockEntityExt) be, be);
    }

    public static <T extends BlockEntity> boolean shouldRenderEntity(BlockEntityExt ext, T be){
        if(crumblingPass) return true;
        return ext == null || !be.hasLevel() || ext.baked$forceEntity() || !ext.baked$isSupported() || ext.baked$renderMode() == RenderMode.ENTITY || ext.baked$renderModeDelayed() == RenderMode.ENTITY || ext.baked$renderBoth();
    }

    public static <T extends BlockEntity> boolean shouldRenderEntityFast(BlockEntityExt ext){
        return ext.baked$forceEntity() || !ext.baked$isSupported() || ext.baked$renderMode() == RenderMode.ENTITY || ext.baked$renderModeDelayed() == RenderMode.ENTITY || ext.baked$renderBoth();
    }

    public static <T extends BlockEntity> void setRenderModeDelayed(T be, RenderMode mode, BlockPos pos){
        setRenderModeDelayed((BlockEntityExt)be, mode, pos);
    }

    public static void setRenderModeDelayed(BlockEntityExt ext, RenderMode mode, BlockPos pos){
        if(ext.baked$renderModeDelayed() != mode && (mode != RenderMode.TERRAIN || canBeTerrain(ext))){
            ext.baked$renderModeDelayed(mode);
            setDirty(pos);
        }
    }

    public static void setDirty(BlockPos pos){
        Minecraft client = Minecraft.getInstance();
        if(client.level == null) return;
        if (!client.isSameThread()) {
            client.execute(() -> setDirty(pos));
            return;
        }
        client.levelExtractor.blockChanged(pos, 8);
    }

    /**
     * Rebuilds exactly one section: {@link #setDirty} goes through blockChanged, which also dirties up to 8 neighbours.
     */
    public static void setSectionDirty(SectionPos section){
        Minecraft client = Minecraft.getInstance();
        if(client.level == null) return;
        if (!client.isSameThread()) {
            client.execute(() -> setSectionDirty(section));
            return;
        }
        client.levelExtractor.setSectionDirty(section.x(), section.y(), section.z(), true);
    }

    public static void updateBlockEntityOnChunkRemesh(BlockEntityExt ext, SectionPos pos){
        if(!ext.baked$isSupported()) return;
        else if(!SettingsManager.MOD_TOGGLE.getValue()) ext.baked$isEnabled(false);
        else{
            String group = Registry.getGroup(((BlockEntity)ext).getType());
            if(group == null) return;
            Option<Boolean> option = SettingsManager.GROUP_TOGGLE_SETTINGS.get(group);
            if(option != null) ext.baked$isEnabled(option.getValue());
            if(ext.baked$isEnabled()){
                if(ext.baked$isTimerFinished()){
                    ChunkTaskHolder.addTask(pos, () -> ext.baked$renderMode(RenderMode.TERRAIN));
                }
                if(ext.baked$renderMode() != ext.baked$renderModeDelayed()){
                    ChunkTaskHolder.addTask(pos, () -> ext.baked$renderMode(ext.baked$renderModeDelayed()));
                }
            }
        }
    }

    public static boolean canBeTerrain(BlockEntityExt ext){

        if(Minecraft.getInstance().level == null) return false;
        
        BlockEntity be = (BlockEntity)ext;
        BlockState state = be.getBlockState();
        String group = Registry.getGroup(state);

        SpecialModelProvider customModelProvider = SpecialModelGetter.getSpecialModelProvider(state, group);

        if(ext.baked$hasSpecialRenderer() && customModelProvider != null){
            if(customModelProvider.getModelLayerLocationProvider().apply(state, be) == null) return false;
            if(customModelProvider.getMaterialProvider().apply(state, be) == null) return false;
        }
        else{
            if(ModelLayerLocationGetter.getModelLayerLocation(state, group) == null) return false;
            if(MaterialGetter.getMaterial(state, group) == null) return false;
        }
        return true;
    }

    public static enum RenderMode {
        TERRAIN,
        ENTITY
    }
}
