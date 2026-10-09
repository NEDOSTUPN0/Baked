package io.github.nedostupn0.baked.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * Bakes entity-side quads (block display and item models) into a terrain model. Sprites from other atlases are remapped
 * to their block atlas copy; a missing copy fails the bake.
 */
public class QuadBaker {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float CELL_BIAS = 1.0E-3F;

    private final List<BakedQuad> quads = new ArrayList<>();
    /** Experimental: carry tints through {@link TintPalette} instead of failing on them. */
    private final boolean allowTints;
    private final TextureAtlas blockAtlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
    private boolean failed;

    public QuadBaker() {
        this(false);
    }

    public QuadBaker(boolean allowTints) {
        this.allowTints = allowTints;
    }

    public boolean allowsTints() {
        return allowTints;
    }

    public boolean failed() {
        return failed;
    }

    public boolean isEmpty() {
        return quads.isEmpty();
    }

    public int size() {
        return quads.size();
    }

    /** @param layer forced section layer, or null to keep the quads' own */
    public QuadBaker addParts(List<BlockStateModelPart> parts, PoseStack.Pose pose, int lightEmission, @Nullable ChunkSectionLayer layer) {
        return addParts(parts, pose, lightEmission, layer, null);
    }

    public QuadBaker addParts(List<BlockStateModelPart> parts, PoseStack.Pose pose, int lightEmission, @Nullable ChunkSectionLayer layer, int @Nullable [] tints) {
        for (BlockStateModelPart part : parts) {
            for (Direction direction : DIRECTIONS) {
                addQuads(part.getQuads(direction), pose, lightEmission, layer, tints);
            }
            addQuads(part.getQuads(null), pose, lightEmission, layer, tints);
        }
        return this;
    }

    public QuadBaker addQuads(List<BakedQuad> source, PoseStack.Pose pose, int lightEmission) {
        return addQuads(source, pose, lightEmission, null);
    }

    public QuadBaker addQuads(List<BakedQuad> source, PoseStack.Pose pose, int lightEmission, @Nullable ChunkSectionLayer layer) {
        return addQuads(source, pose, lightEmission, layer, null);
    }

    /** @param tints ARGB by tintIndex (-1 untinted), or null; non-white tints need {@link #allowsTints()} */
    public QuadBaker addQuads(List<BakedQuad> source, PoseStack.Pose pose, int lightEmission, @Nullable ChunkSectionLayer layer, int @Nullable [] tints) {
        for (BakedQuad quad : source) {
            if (failed) return this;
            int tintIndex = resolveTint(quad.materialInfo().tintIndex(), tints);
            if (tintIndex == FAILED_TINT) {
                failed = true;
                return this;
            }
            BakedQuad baked = transform(quad, pose, lightEmission, layer, tintIndex);
            if (baked == null) failed = true;
            else quads.add(baked);
        }
        return this;
    }

    public QuadBaker addAll(QuadBaker other) {
        quads.addAll(other.quads);
        failed |= other.failed;
        return this;
    }

    private static final int FAILED_TINT = Integer.MIN_VALUE;

    /** Maps a source tintIndex onto the palette carried by {@link TintPalette#CARRIER}. */
    private int resolveTint(int tintIndex, int @Nullable [] tints) {
        if (tintIndex < 0 || tints == null || tintIndex >= tints.length || tints[tintIndex] == -1) return -1;
        return tintFor(tints[tintIndex]);
    }

    /** Palette tintIndex for a whole-model tint color, or {@link #FAILED_TINT}. */
    public int tintFor(int argb) {
        if (argb == -1) return -1;
        if (!allowTints) return FAILED_TINT;
        int index = TintPalette.indexOf(argb);
        return index < 0 ? FAILED_TINT : index;
    }

    public static boolean isFailedTint(int tintIndex) {
        return tintIndex == FAILED_TINT;
    }

    private @Nullable BakedQuad transform(BakedQuad quad, PoseStack.Pose pose, int lightEmission, @Nullable ChunkSectionLayer layer, int tintIndex) {
        BakedQuad.MaterialInfo info = quad.materialInfo();
        TextureAtlasSprite sprite = info.sprite();
        TextureAtlasSprite target = toBlockAtlas(sprite);
        if (target == null) return null;

        Vector3f[] positions = new Vector3f[4];
        long[] uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            positions[i] = pose.pose().transformPosition(quad.position(i), new Vector3f());
            long uv = quad.packedUV(i);
            uvs[i] = target == sprite ? uv : remapUV(uv, sprite, target);
        }

        Vector3f normal = pose.normal().transform(new Vector3f(quad.direction().getUnitVec3f()));
        Direction direction = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());

        ChunkSectionLayer targetLayer = layer != null ? layer : info.layer();
        BakedQuad.MaterialInfo material = target == sprite && info.lightEmission() >= lightEmission && targetLayer == info.layer() && tintIndex == info.tintIndex()
            ? info
            : new BakedQuad.MaterialInfo(
                target,
                targetLayer,
                info.itemRenderType(),
                tintIndex,
                info.shade(),
                Math.max(info.lightEmission(), lightEmission)
            );

        return new BakedQuad(positions[0], positions[1], positions[2], positions[3], uvs[0], uvs[1], uvs[2], uvs[3], direction, material);
    }

    /** Looks up the block atlas sprite for a standalone texture path such as {@code textures/entity/x.png}. */
    public @Nullable TextureAtlasSprite spriteForTexture(Identifier texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/")) path = path.substring("textures/".length());
        if (path.endsWith(".png")) path = path.substring(0, path.length() - ".png".length());
        Identifier name = texture.withPath(path);
        TextureAtlasSprite sprite = blockAtlas.getSprite(name);
        return sprite.contents().name().equals(name) ? sprite : null;
    }

    /** Block atlas sprite with exactly this name, or null if it is not there. */
    public @Nullable TextureAtlasSprite spriteByName(Identifier name) {
        TextureAtlasSprite sprite = blockAtlas.getSprite(name);
        return sprite.contents().name().equals(name) ? sprite : null;
    }

    public @Nullable TextureAtlasSprite toBlockAtlas(TextureAtlasSprite sprite) {
        if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) return sprite;
        TextureAtlasSprite target = blockAtlas.getSprite(sprite.contents().name());
        return target.contents().name().equals(sprite.contents().name()) ? target : null;
    }

    private static long remapUV(long packed, TextureAtlasSprite from, TextureAtlasSprite to) {
        float u = (UVPair.unpackU(packed) - from.getU0()) / (from.getU1() - from.getU0());
        float v = (UVPair.unpackV(packed) - from.getV0()) / (from.getV1() - from.getV0());
        return UVPair.pack(to.getU(u), to.getV(v));
    }

    public BlockStateModel build(Material.@Nullable Baked particleMaterial, boolean useAo) {
        return build(quads, particleMaterial, useAo);
    }

    /**
     * Splits the quads into one model per block (by quad center), so each piece gets that block's lighting and section.
     */
    public List<MeshableEntityTracker.Piece> buildPieces(BlockPos origin, Material.@Nullable Baked particleMaterial, boolean useAo) {
        Map<BlockPos, List<BakedQuad>> byBlock = new LinkedHashMap<>();
        for (BakedQuad quad : quads) {
            float cx = 0, cy = 0, cz = 0;
            for (int i = 0; i < 4; i++) {
                Vector3fc p = quad.position(i);
                cx += p.x();
                cy += p.y();
                cz += p.z();
            }
            // Faces lying exactly on a block boundary belong to the block behind them (the one they bound).
            Vec3i n = quad.direction().getUnitVec3i();
            BlockPos cell = new BlockPos(Mth.floor(cx / 4 - n.getX() * CELL_BIAS), Mth.floor(cy / 4 - n.getY() * CELL_BIAS), Mth.floor(cz / 4 - n.getZ() * CELL_BIAS));
            byBlock.computeIfAbsent(cell, k -> new ArrayList<>()).add(cell.equals(BlockPos.ZERO) ? quad : translate(quad, -cell.getX(), -cell.getY(), -cell.getZ()));
        }

        List<MeshableEntityTracker.Piece> pieces = new ArrayList<>(byBlock.size());
        byBlock.forEach((cell, cellQuads) -> pieces.add(new MeshableEntityTracker.Piece(origin.offset(cell), build(cellQuads, particleMaterial, useAo))));
        return pieces;
    }

    private static BakedQuad translate(BakedQuad quad, float dx, float dy, float dz) {
        return new BakedQuad(
            quad.position0().add(dx, dy, dz, new Vector3f()),
            quad.position1().add(dx, dy, dz, new Vector3f()),
            quad.position2().add(dx, dy, dz, new Vector3f()),
            quad.position3().add(dx, dy, dz, new Vector3f()),
            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
            quad.direction(), quad.materialInfo()
        );
    }

    /** The quad's sprite must be in the block atlas. */
    public QuadBaker addBaked(BakedQuad quad) {
        quads.add(quad);
        return this;
    }

    private static BlockStateModel build(List<BakedQuad> quads, Material.@Nullable Baked particleMaterial, boolean useAo) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : quads) {
            builder.addUnculledFace(quad);
        }
        SimpleModelWrapper part = new SimpleModelWrapper(builder.build(), useAo, particleMaterial);
        return new SinglePartModel(part);
    }

    private record SinglePartModel(SimpleModelWrapper part) implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            output.add(part);
        }

        @Override
        public Material.@Nullable Baked particleMaterial() {
            return part.particleMaterial();
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return part.materialFlags();
        }
    }
}
