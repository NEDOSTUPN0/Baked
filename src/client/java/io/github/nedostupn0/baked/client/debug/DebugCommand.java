package io.github.nedostupn0.baked.client.debug;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityHandler;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker;
import io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker.MeshableEntityData;
import io.github.nedostupn0.baked.client.renderer.entity.ext.EntityExt;
import io.github.nedostupn0.baked.client.renderer.misc.RenderModeManager;
import io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch;
import io.github.nedostupn0.baked.client.renderer.skull.SkinPool;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import io.github.nedostupn0.baked.client.util.entity.ArmorStandUtil;

public class DebugCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("baked").then(literal("debug").executes(context -> {
                for (String line : describe(Minecraft.getInstance().crosshairPickEntity)) {
                    context.getSource().sendFeedback(Component.literal(line));
                    Baked.LOGGER.info("[debug] {}", line);
                }
                return 1;
            }))
        ));
    }

    private static List<String> describe(Entity entity) {
        List<String> out = new ArrayList<>();
        out.add(SkinPool.stats());
        out.add(SignTextBatch.stats());
        out.addAll(SignTextBatch.breakdown());
        if (entity == null) {
            out.add("no entity under crosshair");
            return out;
        }

        EntityExt ext = (EntityExt) entity;
        out.add(entity.getType().builtInRegistryHolder().key().identifier() + " #" + entity.getId() + " at " + entity.blockPosition().toShortString());
        out.add("supported=" + ext.baked$isSupported() + " enabled=" + MeshableEntityTracker.isEnabled(entity)
            + " pending=" + MeshableEntityTracker.isPending(entity));
        out.add("renderMode=" + ext.baked$renderMode() + " meshedParts=" + ext.baked$meshedParts()
            + " fullyMeshed=" + ext.baked$fullyMeshed() + " skipExtraction=" + RenderModeManager.canSkipMeshedEntity(entity));

        MeshableEntityData data = MeshableEntityTracker.getData(entity.getId());
        if (data == null) {
            out.add("tracker: no data");
        } else {
            out.add("tracker: sections=" + data.sections().size() + " uploaded=" + data.uploaded().size()
                + " parts=" + data.snapshot().parts() + " pieces=" + describePieces(data.snapshot()));
        }

        MeshableEntityHandler<Entity> handler = MeshableEntityTracker.getHandler(entity);
        if (handler != null) {
            try {
                MeshableEntityTracker.Snapshot fresh = handler.snapshot(entity);
                out.add("fresh snapshot: " + (fresh == null ? "null (vanilla renderer)" : "parts=" + fresh.parts() + " pieces=" + describePieces(fresh)));
                if (fresh == null && entity instanceof ArmorStand && ArmorStandUtil.lastFailure != null) {
                    out.add("fallback reason: " + ArmorStandUtil.lastFailure);
                }
            } catch (RuntimeException e) {
                out.add("fresh snapshot threw " + e);
                Baked.LOGGER.error("[debug] snapshot failed", e);
            }
        }
        return out;
    }

    private static String describePieces(MeshableEntityTracker.Snapshot snapshot) {
        StringBuilder sb = new StringBuilder().append(snapshot.pieces().size());
        for (MeshableEntityTracker.Piece piece : snapshot.pieces()) {
            sb.append(" [").append(piece.pos().toShortString()).append(": ").append(describeQuads(piece.model())).append(']');
        }
        return sb.toString();
    }

    private static String describeQuads(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42), parts);
        Map<ChunkSectionLayer, Integer> byLayer = new EnumMap<>(ChunkSectionLayer.class);
        int total = 0;
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) total += count(part.getQuads(direction), byLayer);
            total += count(part.getQuads(null), byLayer);
        }
        return total + " " + byLayer;
    }

    private static int count(List<BakedQuad> quads, Map<ChunkSectionLayer, Integer> byLayer) {
        for (BakedQuad quad : quads) byLayer.merge(quad.materialInfo().layer(), 1, Integer::sum);
        return quads.size();
    }
}
