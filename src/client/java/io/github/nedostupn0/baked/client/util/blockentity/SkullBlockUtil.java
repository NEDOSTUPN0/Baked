package io.github.nedostupn0.baked.client.util.blockentity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.resources.ResourceUtil;
import io.github.nedostupn0.baked.client.renderer.blockentity.ext.BlockEntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager.RenderMode;
import io.github.nedostupn0.baked.client.renderer.skull.SkinHolder;
import io.github.nedostupn0.baked.client.renderer.skull.SkinPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SkullBlockUtil {

    public static Map<String, Identifier> BUILT_IN_TEXTURES = new HashMap<>();

    public static Identifier getMaterial(BlockState state) {
        SkullBlock.Type type = ((AbstractSkullBlock)state.getBlock()).getType();
        Identifier id = SkullBlockRenderer.SKIN_BY_TYPE.get(type);
        return ResourceUtil.entityTextureFormatter(id);
    }

    public static ModelLayerLocation getModelLayerLocation(BlockState state){
        if(state.getBlock() instanceof AbstractSkullBlock block){
            SkullBlock.Type type = block.getType();
            if (type instanceof SkullBlock.Types vanillaType) {
                return switch (vanillaType) {
                    case SKELETON -> ModelLayers.SKELETON_SKULL;
                    case WITHER_SKELETON -> ModelLayers.WITHER_SKELETON_SKULL;
                    case PLAYER -> ModelLayers.PLAYER_HEAD;
                    case ZOMBIE -> ModelLayers.ZOMBIE_HEAD;
                    case CREEPER -> ModelLayers.CREEPER_HEAD;
                    case DRAGON -> ModelLayers.DRAGON_SKULL;
                    case PIGLIN -> ModelLayers.PIGLIN_HEAD;
                };
            }
            else return null;
        }
        else return null;
    }

    public static void transform(BlockState state, PoseStack poseStack){
        if (state.getBlock() instanceof WallSkullBlock) {
            Direction facing = state.getValue(WallSkullBlock.FACING);
            poseStack.mulPose(SkullBlockRenderer.TRANSFORMATIONS.wallTransformation(facing));
        } else {
            poseStack.mulPose(SkullBlockRenderer.TRANSFORMATIONS.freeTransformations(state.getValue(SkullBlock.ROTATION)));
        }
    }

    public static void transform(BlockState state, BlockEntity be, PoseStack poseStack){
        transform(state, poseStack);
    }

    public static ModelLayerLocation getModelLayerLocation(BlockState state, BlockEntity be){
        return getModelLayerLocation(state);
    }

    public static Identifier getMaterial(BlockState state, BlockEntity be) {
        if (be instanceof SkullBlockEntity skullBe) {
            return getBuiltInTexture(skullBe);
        }
        return getMaterial(state);
    }

    public static boolean hasBuiltInTexture(BlockEntity be) {
        if(!SettingsManager.CUSTOM_SKULLS.getValue()) return false;
        // Profiles given by name only carry no texture value but still resolve to a skin in the pool.
        return getBuiltInTexture(be) != null;
    }

    /** Texture of a custom head: mapped by a resource pack, or the head of its skin in the {@link SkinPool}. */
    public static @Nullable Identifier getBuiltInTexture(BlockEntity be) {
        String textureValue = getBuiltInTextureValue(be);
        Identifier builtIn = textureValue == null ? null : SkullBlockUtil.BUILT_IN_TEXTURES.get(textureValue);
        if(builtIn != null) return builtIn;
        if(be instanceof SkullBlockEntity skullBe && isPlayerHead(skullBe) && skullBe.getOwnerProfile() != null){
            return SkinPool.find(skullBe.getOwnerProfile());
        }
        return null;
    }

    /** Heads whose skin is not (yet) bakeable stay on the vanilla renderer. */
    public static boolean isDynamicTexture(SkullBlockEntity be) {
        return be.getOwnerProfile() != null && !hasBuiltInTexture(be);
    }

    /** Re-evaluates how a head is drawn after its profile or pooled skin changed. Main thread. */
    public static void refresh(SkullBlockEntity be) {
        // Also runs for the integrated server's block entities.
        if(!Minecraft.getInstance().isSameThread() || (be.hasLevel() && !be.getLevel().isClientSide())) return;
        BlockEntityExt ext = (BlockEntityExt) be;
        ResolvableProfile profile = be.getOwnerProfile();
        if(profile != null && isPlayerHead(be) && !hasBuiltInTexture(be)) SkinPool.request(profile, be);

        ext.baked$hasSpecialRenderer(hasBuiltInTexture(be));
        Identifier texture = getBuiltInTexture(be);
        SkinPool.hold((SkinHolder) be, texture != null && SkinPool.isSlot(texture) ? texture : null);
        boolean dynamic = isDynamicTexture(be);
        if(!be.hasLevel()){
            if(dynamic) ext.baked$renderMode(RenderMode.ENTITY);
            return;
        }
        BlockPos pos = be.getBlockPos();
        if(dynamic) RenderModeManager.setRenderMode(be, RenderMode.ENTITY, pos);
        else{
            RenderModeManager.setRenderModeDelayed(be, RenderMode.TERRAIN, pos);
            // The baked model depends on the texture, which may have changed without a mode change.
            RenderModeManager.setDirty(pos);
        }
    }

    private static boolean isPlayerHead(SkullBlockEntity be) {
        return be.getBlockState().getBlock() instanceof AbstractSkullBlock block && block.getType() == SkullBlock.Types.PLAYER;
    }

    public static String getBuiltInTextureValue(BlockEntity be) {
        if(be instanceof SkullBlockEntity skullBe) {
            if(skullBe.getOwnerProfile() == null || !SettingsManager.CUSTOM_SKULLS.getValue()) return null;
            Collection<Property> properties = skullBe.getOwnerProfile().partialProfile().properties().get("textures");
            String textureValue = properties.stream()
            .map(Property::value)
            .findFirst()
            .orElse(null);
            return textureValue;
        }
        else return null;
    }
}
