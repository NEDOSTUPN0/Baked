package io.github.nedostupn0.baked.client.renderer.map;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Packs framed map textures into pages drawn in one call each; a changed map re-uploads only its tile. */
public final class MapAtlas {
    public static final int MAP_SIZE = 128;
    private static final int TILES_PER_ROW = 16;
    private static final int PAGE_SIZE = MAP_SIZE * TILES_PER_ROW;
    private static final int TILES_PER_PAGE = TILES_PER_ROW * TILES_PER_ROW;

    private static final List<Page> pages = new ArrayList<>();
    private static final Int2ObjectMap<Slot> slots = new Int2ObjectOpenHashMap<>();
    private static final ByteBuffer scratch = ByteBuffer.allocateDirect(MAP_SIZE * MAP_SIZE * 4).order(ByteOrder.LITTLE_ENDIAN);

    private MapAtlas() {}

    /** Where a map lives in the atlas. UVs are in 0..1 page space. */
    public static final class Slot {
        final int mapId;
        final Page page;
        final int tile;
        int references;
        boolean dirty = true;
        // Dirty rectangle in map pixels, inclusive min / exclusive max.
        int dirtyMinX, dirtyMinY, dirtyMaxX = MAP_SIZE, dirtyMaxY = MAP_SIZE;

        void markDirty(int x, int y, int width, int height) {
            if (!dirty) {
                dirtyMinX = x;
                dirtyMinY = y;
                dirtyMaxX = x + width;
                dirtyMaxY = y + height;
                dirty = true;
            } else {
                dirtyMinX = Math.min(dirtyMinX, x);
                dirtyMinY = Math.min(dirtyMinY, y);
                dirtyMaxX = Math.max(dirtyMaxX, x + width);
                dirtyMaxY = Math.max(dirtyMaxY, y + height);
            }
        }

        Slot(int mapId, Page page, int tile) {
            this.mapId = mapId;
            this.page = page;
            this.tile = tile;
        }

        public Identifier texture() {
            return page.location;
        }

        public int pageIndex() {
            return page.index;
        }

        public float u0() {
            return (float) (tile % TILES_PER_ROW) / TILES_PER_ROW;
        }

        public float v0() {
            return (float) (tile / TILES_PER_ROW) / TILES_PER_ROW;
        }

        public float size() {
            return 1.0F / TILES_PER_ROW;
        }
    }

    private static final class Page {
        final int index;
        final Identifier location;
        final DynamicTexture texture;
        final BitSet used = new BitSet(TILES_PER_PAGE);

        Page(int index) {
            this.index = index;
            this.location = Identifier.fromNamespaceAndPath("baked", "map_atlas/" + index);
            this.texture = new DynamicTexture(() -> "baked map atlas " + index, PAGE_SIZE, PAGE_SIZE, true);
            Minecraft.getInstance().getTextureManager().register(location, texture);
        }
    }

    public static Slot acquire(int mapId) {
        Slot slot = slots.get(mapId);
        if (slot == null) {
            slot = allocate(mapId);
            slots.put(mapId, slot);
        }
        slot.references++;
        return slot;
    }

    public static void release(int mapId) {
        Slot slot = slots.get(mapId);
        if (slot == null || --slot.references > 0) return;
        slots.remove(mapId);
        slot.page.used.clear(slot.tile);
    }

    public static boolean contains(int mapId) {
        return slots.containsKey(mapId);
    }

    /** Part of the map's pixels changed (map data packet with a color patch). */
    public static void markDirty(int mapId, int x, int y, int width, int height) {
        Slot slot = slots.get(mapId);
        if (slot != null) slot.markDirty(x, y, width, height);
    }

    private static Slot allocate(int mapId) {
        for (Page page : pages) {
            int tile = page.used.nextClearBit(0);
            if (tile < TILES_PER_PAGE) {
                page.used.set(tile);
                return new Slot(mapId, page, tile);
            }
        }
        Page page = new Page(pages.size());
        pages.add(page);
        page.used.set(0);
        return new Slot(mapId, page, 0);
    }

    /** Uploads the tiles of all changed maps. Render thread. */
    public static void uploadDirty(ClientLevel level) {
        for (Slot slot : slots.values()) {
            if (!slot.dirty) continue;
            MapItemSavedData data = level.getMapData(new MapId(slot.mapId));
            if (data == null) continue;
            upload(slot, data);
            slot.dirty = false;
        }
    }

    /** Only the dirty rectangle is converted and uploaded: live maps send small patches. */
    private static void upload(Slot slot, MapItemSavedData data) {
        int[] palette = palette();
        int minX = Math.max(0, slot.dirtyMinX), minY = Math.max(0, slot.dirtyMinY);
        int maxX = Math.min(MAP_SIZE, slot.dirtyMaxX), maxY = Math.min(MAP_SIZE, slot.dirtyMaxY);
        if (minX >= maxX || minY >= maxY) return;

        scratch.clear();
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                scratch.putInt(palette[data.colors[x + y * MAP_SIZE] & 0xFF]);
            }
        }
        scratch.flip();
        int tileX = (slot.tile % TILES_PER_ROW) * MAP_SIZE;
        int tileY = (slot.tile / TILES_PER_ROW) * MAP_SIZE;
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(slot.page.texture.getTexture(), scratch, 0, 0, tileX + minX, tileY + minY, maxX - minX, maxY - minY);
    }

    /** ABGR pixel for every packed map color: MapColor#getColorFromPackedId goes through map lookups per call. */
    private static int @Nullable [] palette;

    private static int[] palette() {
        if (palette == null) {
            int[] table = new int[256];
            for (int i = 0; i < 256; i++) table[i] = ARGB.toABGR(MapColor.getColorFromPackedId(i));
            palette = table;
        }
        return palette;
    }

    public static @Nullable Slot get(int mapId) {
        return slots.get(mapId);
    }

    public static void reset() {
        slots.clear();
        for (Page page : pages) {
            Minecraft.getInstance().getTextureManager().release(page.location);
        }
        pages.clear();
    }
}
