package io.github.nedostupn0.baked.client.model;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries arbitrary colors (dyed leather, potions) through the terrain tint path: quads are tesselated with {@link
 * #CARRIER}, a never rendered block whose tint layer i is palette color i.
 */
public final class TintPalette {
    public static final BlockState CARRIER = Blocks.VOID_AIR.defaultBlockState();
    /** Each tinted piece copies the whole layer list in the vanilla compiler, so keep it modest. */
    private static final int CAPACITY = 1024;

    private static final int[] colors = new int[CAPACITY];
    private static final Int2IntOpenHashMap indexByColor = new Int2IntOpenHashMap();
    private static int size;

    private static final List<BlockTintSource> LAYERS = new Layers();

    static {
        java.util.Arrays.fill(colors, -1);
        indexByColor.defaultReturnValue(-1);
    }

    private TintPalette() {}

    public static void register() {
        BlockColorRegistry.register(LAYERS, Blocks.VOID_AIR);
    }

    /** Palette index (tintIndex) for an ARGB color, or -1 when the palette is full. Main thread. */
    public static int indexOf(int argb) {
        int index = indexByColor.get(argb);
        if (index >= 0 || size >= CAPACITY) return index;
        colors[size] = argb;
        indexByColor.put(argb, size);
        return size++;
    }

    private static final class Layers extends AbstractList<BlockTintSource> implements RandomAccess {
        private final BlockTintSource[] sources = new BlockTintSource[CAPACITY];

        Layers() {
            for (int i = 0; i < CAPACITY; i++) {
                int index = i;
                sources[i] = state -> colors[index];
            }
        }

        @Override
        public BlockTintSource get(int index) {
            return sources[index];
        }

        @Override
        public int size() {
            return CAPACITY;
        }
    }
}
