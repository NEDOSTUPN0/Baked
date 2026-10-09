package io.github.nedostupn0.baked.client.model;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.nedostupn0.baked.client.renderer.batch.DynamicPartBatch;
import io.github.nedostupn0.baked.client.util.entity.ItemFrameUtil;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Captures what a vanilla entity renderer submits as terrain quads. Anything that can't be meshed (blending, glint,
 * tints, dynamic textures) fails the capture and the caller keeps vanilla; overlays like name tags and shadows are
 * ignored.
 */
public class CapturingSubmitCollector implements SubmitNodeCollector {
    private static final String TEXTURE_SAMPLER = "Sampler0";

    private final QuadBaker baker;
    /** Experimental: receives glinted geometry; null fails on glint. */
    private final DynamicPartBatch.@Nullable Builder dynamic;
    private @Nullable String failure;

    public CapturingSubmitCollector(QuadBaker baker, DynamicPartBatch.@Nullable Builder dynamic) {
        this.baker = baker;
        this.dynamic = dynamic;
    }

    public boolean failed() {
        return failure != null || baker.failed();
    }

    public @Nullable String failure() {
        return failure != null ? failure : baker.failed() ? "sprite missing from block atlas" : null;
    }

    private void fail(String reason) {
        if (failure == null) failure = reason;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable UvMapping uvMapping, int outlineColor) {
        if (failed()) return;
        if (renderType.toString().contains("trimmed_armor_glint")) {
            // A depth-equal overlay over the trim; our trim is in the mesh, so depths would not match.
            fail("trimmed armor glint");
            return;
        }
        if (renderType.toString().contains("glint")) {
            submitGlintModel(model, state, poseStack, renderType, tintedColor, uvMapping);
            return;
        }
        int tintIndex = baker.tintFor(tintedColor);
        if (QuadBaker.isFailedTint(tintIndex)) {
            fail("tinted model " + renderType);
            return;
        }
        TextureAtlasSprite sprite = resolveSprite(renderType, uvMapping);
        if (sprite == null) return;

        ((Model<S>) model).setupAnim(state);
        boolean doubleSided = !renderType.pipeline().isCull();
        BakedQuad.MaterialInfo material = BakedQuad.MaterialInfo.of(new Material.Baked(sprite, false), Transparency.TRANSPARENT, tintIndex, null, 0);
        bakePart(model.root(), poseStack, sprite, material, doubleSided);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        if (failed()) return;
        if (renderType.hasBlending()) {
            fail("translucent block model");
            return;
        }
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                if (!checkQuads(part.getQuads(direction))) return;
            }
            if (!checkQuads(part.getQuads(null))) return;
        }
        baker.addParts(parts, poseStack.last(), 0, null, tintLayers);
        if (baker.failed()) fail("tinted block model");
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType) {
        if (failed()) return;
        if (foilType == ItemStackRenderState.FoilType.STANDARD && dynamic != null) {
            ItemFrameUtil.addGlintQuads(dynamic, quads.all(), poseStack.last(), tintLayers);
            return;
        }
        if (foilType != ItemStackRenderState.FoilType.NONE) {
            fail("item glint");
            return;
        }
        if (!checkQuads(quads.all())) return;
        baker.addQuads(quads.all(), poseStack.last(), 0, null, tintLayers);
        if (baker.failed()) fail("tinted item");
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, ItemQuads quads, MeshView mesh, ItemStackRenderState.FoilType foilType) {
        if (mesh.size() > 0) {
            fail("fabric item mesh");
            return;
        }
        submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, java.util.function.Function<ChunkSectionLayer, RenderType> renderTypes, boolean translucent, List<BlockStateModelPart> parts, Mesh mesh, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        if (mesh.size() > 0) {
            fail("fabric block mesh");
            return;
        }
        if (translucent) {
            fail("translucent block model");
            return;
        }
        submitBlockModel(poseStack, renderTypes.apply(ChunkSectionLayer.CUTOUT), parts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        fail("custom geometry " + renderType);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        fail("moving block");
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {}

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {}

    @Override
    public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {}

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {}

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {}

    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {}

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {}

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {}

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {}

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {}

    private boolean checkQuads(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (quad.materialInfo().layer().translucent()) {
                fail("translucent quads");
                return false;
            }
        }
        return true;
    }

    private @Nullable TextureAtlasSprite resolveSprite(RenderType renderType, @Nullable UvMapping uvMapping) {
        if (renderType.hasBlending()) {
            fail("blending render type " + renderType);
            return null;
        }
        TextureAtlasSprite sprite;
        if (uvMapping != null) {
            if (uvMapping instanceof TextureAtlasSprite atlasSprite) {
                sprite = baker.toBlockAtlas(atlasSprite);
            } else {
                // Experimental: runtime-coloured trims have a pre-coloured copy in the block atlas.
                Identifier trim = baker.allowsTints() ? TrimSprites.spriteName(uvMapping) : null;
                if (trim == null) {
                    fail("dynamic uv mapping " + uvMapping.getClass().getSimpleName());
                    return null;
                }
                sprite = baker.spriteByName(trim);
            }
        } else {
            RenderSetup.TextureBinding binding = renderType.state.textures.get(TEXTURE_SAMPLER);
            if (binding == null) {
                fail("render type without texture " + renderType);
                return null;
            }
            sprite = baker.spriteForTexture(binding.location());
        }
        if (sprite == null) fail("texture not in block atlas: " + (uvMapping != null ? uvMapping : renderType));
        return sprite;
    }

    /** Glinted models go to the dynamic batch with their own render type and uvs. */
    @SuppressWarnings("unchecked")
    private <S> void submitGlintModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int tintedColor, @Nullable UvMapping uvMapping) {
        if (dynamic == null) {
            fail("glint " + renderType);
            return;
        }
        ((Model<S>) model).setupAnim(state);
        Vector3f[] positions = new Vector3f[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        visitPolygons(model.root(), poseStack, (pose, polygon) -> {
            ModelPart.Vertex[] vertices = polygon.vertices();
            if (vertices.length != 4) return;
            for (int i = 0; i < 4; i++) {
                ModelPart.Vertex vertex = vertices[i];
                positions[i] = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                us[i] = uvMapping != null ? uvMapping.getU(vertex.u()) : vertex.u();
                vs[i] = uvMapping != null ? uvMapping.getV(vertex.v()) : vertex.v();
            }
            Vector3f normal = polygon.normal().mul(pose.normal(), new Vector3f()).normalize();
            dynamic.addQuad(renderType, positions, us, vs, normal, tintedColor);
        });
    }

    private interface PolygonVisitor {
        void visit(PoseStack.Pose pose, ModelPart.Polygon polygon);
    }

    /** Mirrors {@link ModelPart#render}: honours visibility and skipDraw, unlike {@link ModelPart#visit}. */
    private static void visitPolygons(ModelPart part, PoseStack poseStack, PolygonVisitor visitor) {
        if (!part.visible || (part.cubes.isEmpty() && part.children.isEmpty())) return;
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        if (!part.skipDraw) {
            PoseStack.Pose pose = poseStack.last();
            for (ModelPart.Cube cube : part.cubes) {
                for (ModelPart.Polygon polygon : cube.polygons) visitor.visit(pose, polygon);
            }
        }
        for (ModelPart child : part.children.values()) {
            visitPolygons(child, poseStack, visitor);
        }
        poseStack.popPose();
    }

    private void bakePart(ModelPart part, PoseStack poseStack, TextureAtlasSprite sprite, BakedQuad.MaterialInfo material, boolean doubleSided) {
        visitPolygons(part, poseStack, (pose, polygon) -> addPolygon(polygon, pose, sprite, material, doubleSided));
    }

    private void addPolygon(ModelPart.Polygon polygon, PoseStack.Pose pose, TextureAtlasSprite sprite, BakedQuad.MaterialInfo material, boolean doubleSided) {
        ModelPart.Vertex[] vertices = polygon.vertices();
        if (vertices.length != 4) return;

        Vector3f[] positions = new Vector3f[4];
        long[] uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            ModelPart.Vertex vertex = vertices[i];
            positions[i] = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
            uvs[i] = UVPair.pack(sprite.getU(vertex.u()), sprite.getV(vertex.v()));
        }
        Vector3f normal = polygon.normal().mul(pose.normal(), new Vector3f());
        Direction direction = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());

        baker.addBaked(new BakedQuad(positions[0], positions[1], positions[2], positions[3], uvs[0], uvs[1], uvs[2], uvs[3], direction, material));
        if (doubleSided) {
            // Terrain always culls back faces; no-cull entity layers (armor) must stay visible from inside.
            baker.addBaked(new BakedQuad(positions[0], positions[3], positions[2], positions[1], uvs[0], uvs[3], uvs[2], uvs[1], direction.getOpposite(), material));
        }
    }
}
