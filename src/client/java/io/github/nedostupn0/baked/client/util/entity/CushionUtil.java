package io.github.nedostupn0.baked.client.util.entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.model.BlockEntityStateModel;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityHandler;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.resources.ResourceUtil;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CushionRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

public class CushionUtil{
    private static Map<CacheKey, BlockEntityStateModel> MODEL_CACHE = new ConcurrentHashMap<>();

    public static final MeshableEntityHandler<Cushion> HANDLER = new MeshableEntityHandler<>() {
        @Override
        public boolean isEnabled() {
            return SettingsManager.OPTIMISED_CUSHIONS.getValue();
        }

        @Override
        public MeshableEntityTracker.Snapshot snapshot(Cushion cushion) {
            return new MeshableEntityTracker.Snapshot(List.of(new MeshableEntityTracker.Piece(cushion.getPos(), getModel(cushion))), MeshableEntityTracker.ALL_PARTS);
        }
    };

    public static ModelLayerLocation getModelLayerLocation(){
        return ModelLayers.CUSHION;
    }

    public static Identifier getMaterial(DyeColor color){
        return ResourceUtil.entityTextureFormatter(CushionRenderer.TEXTURES_BY_COLOR.get(color));
    }

    public static BlockEntityStateModel getModel(Cushion cushion){
        BlockPos blockPos = cushion.getPos();
        Vec3 pos = cushion.position();
        DyeColor color = cushion.getColor();

        double relativeX = pos.x - blockPos.getX();
        double relativeY = pos.y - blockPos.getY();
        double relativeZ = pos.z - blockPos.getZ();

        return MODEL_CACHE.computeIfAbsent(new CacheKey(relativeX, relativeY, relativeZ, cushion.getRotationVector().y, color.getId()), (k) -> {
            PoseStack poseStack = new PoseStack();
            poseStack.translate(k.relativeX, k.relativeY, k.relativeZ);
            poseStack.rotate(Axis.YP.rotationDegrees(k.yRot));
            poseStack.rotate(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0, -0.25, 0.0);
            return new BlockEntityStateModel(getModelLayerLocation(), getMaterial(color), poseStack, SettingsManager.CUSHION_AMBIENT_OCCLUSION.getValue(), null, null);
        });
    }

    public static void clearCache(){
        MODEL_CACHE.clear();
    }

    private static record CacheKey(double relativeX, double relativeY, double relativeZ, float yRot, int colorId){}
}
