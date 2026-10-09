package io.github.nedostupn0.baked.client.util.entity;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.model.QuadBaker;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityHandler;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.phys.Vec3;

/**
 * Bakes paintings, mirroring {@link net.minecraft.client.renderer.entity.PaintingRenderer}. The block atlas copies
 * {@code textures/painting}, so pack overrides and modded variants work.
 */
public class PaintingUtil {
    private static final float EDGE = 0.03125F;
    private static final String ATLAS_PREFIX = "painting/";

    public static final MeshableEntityHandler<Painting> HANDLER = new MeshableEntityHandler<>() {
        @Override
        public boolean isEnabled() {
            return SettingsManager.OPTIMISED_PAINTINGS.getValue();
        }

        @Override
        public MeshableEntityTracker.@Nullable Snapshot snapshot(Painting painting) {
            return PaintingUtil.snapshot(painting);
        }
    };

    private static MeshableEntityTracker.@Nullable Snapshot snapshot(Painting painting) {
        PaintingVariant variant = painting.getVariant().value();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite front = findSprite(atlas, variant.assetId());
        TextureAtlasSprite back = findSprite(atlas, Identifier.withDefaultNamespace("back"));
        if (front == null || back == null) return null;

        Direction direction = painting.getDirection();
        BlockPos blockPos = painting.getPos();
        Vec3 origin = painting.position().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        PoseStack poseStack = new PoseStack();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - direction.get2DDataValue() * 90));
        PoseStack.Pose pose = poseStack.last();

        BakedQuad.MaterialInfo frontMaterial = material(front);
        BakedQuad.MaterialInfo backMaterial = material(back);
        QuadBaker baker = new QuadBaker();

        int width = variant.width();
        int height = variant.height();
        float offsetX = -width / 2.0F;
        float offsetY = -height / 2.0F;

        float backU0 = back.getU0(), backU1 = back.getU1(), backV0 = back.getV0(), backV1 = back.getV1();
        float tbV1 = back.getV(0.0625F);
        float lrU1 = back.getU(0.0625F);
        double deltaU = 1.0 / width;
        double deltaV = 1.0 / height;

        for (int segmentX = 0; segmentX < width; segmentX++) {
            for (int segmentY = 0; segmentY < height; segmentY++) {
                float x0 = offsetX + (segmentX + 1);
                float x1 = offsetX + segmentX;
                float y0 = offsetY + (segmentY + 1);
                float y1 = offsetY + segmentY;
                float frontU0 = front.getU((float) (deltaU * (width - segmentX)));
                float frontU1 = front.getU((float) (deltaU * (width - (segmentX + 1))));
                float frontV0 = front.getV((float) (deltaV * (height - segmentY)));
                float frontV1 = front.getV((float) (deltaV * (height - (segmentY + 1))));

                baker.addBaked(quad(pose, frontMaterial, 0, 0, -1,
                    x0, y1, -EDGE, frontU1, frontV0,
                    x1, y1, -EDGE, frontU0, frontV0,
                    x1, y0, -EDGE, frontU0, frontV1,
                    x0, y0, -EDGE, frontU1, frontV1));
                baker.addBaked(quad(pose, backMaterial, 0, 0, 1,
                    x0, y0, EDGE, backU1, backV0,
                    x1, y0, EDGE, backU0, backV0,
                    x1, y1, EDGE, backU0, backV1,
                    x0, y1, EDGE, backU1, backV1));
                if (segmentY == height - 1) {
                    baker.addBaked(quad(pose, backMaterial, 0, 1, 0,
                        x0, y0, -EDGE, backU0, backV0,
                        x1, y0, -EDGE, backU1, backV0,
                        x1, y0, EDGE, backU1, tbV1,
                        x0, y0, EDGE, backU0, tbV1));
                }
                if (segmentY == 0) {
                    baker.addBaked(quad(pose, backMaterial, 0, -1, 0,
                        x0, y1, EDGE, backU0, backV0,
                        x1, y1, EDGE, backU1, backV0,
                        x1, y1, -EDGE, backU1, tbV1,
                        x0, y1, -EDGE, backU0, tbV1));
                }
                if (segmentX == width - 1) {
                    baker.addBaked(quad(pose, backMaterial, -1, 0, 0,
                        x0, y0, EDGE, lrU1, backV0,
                        x0, y1, EDGE, lrU1, backV1,
                        x0, y1, -EDGE, backU0, backV1,
                        x0, y0, -EDGE, backU0, backV0));
                }
                if (segmentX == 0) {
                    baker.addBaked(quad(pose, backMaterial, 1, 0, 0,
                        x1, y0, -EDGE, lrU1, backV0,
                        x1, y1, -EDGE, lrU1, backV1,
                        x1, y1, EDGE, backU0, backV1,
                        x1, y0, EDGE, backU0, backV0));
                }
            }
        }

        return new MeshableEntityTracker.Snapshot(baker.buildPieces(blockPos, null, false), MeshableEntityTracker.ALL_PARTS);
    }

    private static @Nullable TextureAtlasSprite findSprite(TextureAtlas atlas, Identifier assetId) {
        Identifier name = assetId.withPrefix(ATLAS_PREFIX);
        TextureAtlasSprite sprite = atlas.getSprite(name);
        return sprite.contents().name().equals(name) ? sprite : null;
    }

    private static BakedQuad.MaterialInfo material(TextureAtlasSprite sprite) {
        return BakedQuad.MaterialInfo.of(new Material.Baked(sprite, false), Transparency.NONE, -1, true, 0);
    }

    private static BakedQuad quad(PoseStack.Pose pose, BakedQuad.MaterialInfo material, int nx, int ny, int nz,
                                  float x0, float y0, float z0, float u0, float v0,
                                  float x1, float y1, float z1, float u1, float v1,
                                  float x2, float y2, float z2, float u2, float v2,
                                  float x3, float y3, float z3, float u3, float v3) {
        Vector3f normal = pose.normal().transform(new Vector3f(nx, ny, nz));
        return new BakedQuad(
            pose.pose().transformPosition(x0, y0, z0, new Vector3f()),
            pose.pose().transformPosition(x1, y1, z1, new Vector3f()),
            pose.pose().transformPosition(x2, y2, z2, new Vector3f()),
            pose.pose().transformPosition(x3, y3, z3, new Vector3f()),
            UVPair.pack(u0, v0), UVPair.pack(u1, v1), UVPair.pack(u2, v2), UVPair.pack(u3, v3),
            Direction.getApproximateNearest(normal.x(), normal.y(), normal.z()),
            material
        );
    }
}
