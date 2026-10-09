package io.github.nedostupn0.baked.client.model;

import java.util.List;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Forwards only the breaking overlay: a baked block entity's model is already in the mesh.
 * Cracks go straight into the breaking overlay phase, so collectors wrapped by other mods get none.
 */
public class CrumblingOnlyCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector delegate;
    private final int order;

    public CrumblingOnlyCollector(SubmitNodeCollector delegate) {
        this(delegate, 0);
    }

    private CrumblingOnlyCollector(SubmitNodeCollector delegate, int order) {
        this.delegate = delegate;
        this.order = order;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new CrumblingOnlyCollector(delegate, order);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        if (crumblingOverlay == null || !renderType.affectsCrumbling()) return;
        if (!(delegate.order(order) instanceof SubmitNodeCollection collection)) return;
        RenderType crumblingRenderType = ModelBakery.DESTROY_TYPES.get(crumblingOverlay.progress());
        collection.breakingOverlay.submit(new ModelFeatureRenderer.Submit<>(crumblingRenderType, poseStack.last().copy(), model, state, lightCoords, overlayCoords, tintedColor, null, crumblingOverlay.cameraPose()));
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
        delegate.order(order).submitBreakingBlockModel(poseStack, parts, progress);
    }

    @Override public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}
    @Override public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {}
    @Override public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {}
    @Override public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {}
    @Override public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {}
    @Override public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {}
    @Override public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {}
    @Override public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {}
    @Override public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {}
    @Override public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {}
    @Override public void submitQuadParticleGroup(QuadParticleRenderState particles) {}
    @Override public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {}
}
