package io.github.nedostupn0.baked.client.renderer.skull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTexture;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.util.blockentity.SkullBlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

/**
 * Packs the heads of downloaded skins into a block atlas sprite, so custom player heads can be baked. Slots are
 * refcounted by loaded heads; when full, the one released longest ago is reused.
 */
public final class SkinPool {
    public static final Identifier SPRITE = Identifier.fromNamespaceAndPath("baked", "block/skin_pool");
    private static final String SLOT_PREFIX = "skin_pool/";
    private static final int SKIN_SIZE = 64;
    private static final int HEAD_HEIGHT = 16;
    /** The 16 rows of a slot shrink to one texel at this level. */
    private static final int MAX_MIP_LEVEL = 4;

    private static final Map<ResolvableProfile, Request> requests = new ConcurrentHashMap<>();
    private static final Map<Identifier, Slot> slotsBySkin = new HashMap<>();
    private static final Map<Identifier, Slot> slotsById = new ConcurrentHashMap<>();
    private static final List<Slot> slots = new ArrayList<>();
    /** Slots no loaded head uses, released longest ago first. */
    private static final LinkedHashSet<Slot> unused = new LinkedHashSet<>();
    private static boolean warnedFull;

    private SkinPool() {}

    private static final class Request {
        volatile @Nullable Identifier slotId;
        boolean done;
        final List<SkullBlockEntity> waiters = new ArrayList<>();
    }

    /** One skin head in the pool. Maps the UVs of a 64x64 skin onto the slot. */
    public static final class Slot implements UvMapping {
        final int index;
        final Identifier id;
        Identifier skin;
        NativeImage head;
        int references;

        Slot(int index, Identifier id, Identifier skin, NativeImage head) {
            this.index = index;
            this.id = id;
            this.skin = skin;
            this.head = head;
        }

        @Override
        public float getU(float offset) {
            TextureAtlasSprite pool = pool();
            int width = pool.contents().width();
            return pool.getU(((index % columns(pool)) * SKIN_SIZE + offset * SKIN_SIZE) / width);
        }

        @Override
        public float getV(float offset) {
            TextureAtlasSprite pool = pool();
            int height = pool.contents().height();
            return pool.getV(((index / columns(pool)) * HEAD_HEIGHT + offset * SKIN_SIZE) / height);
        }
    }

    public static boolean isEnabled() {
        return SettingsManager.OPTIMISED_SKULLS.getValue() && SettingsManager.CUSTOM_SKULLS.getValue();
    }

    /** True for the virtual sprite ids handed out by {@link #find}. */
    public static boolean isSlot(Identifier id) {
        return id.getNamespace().equals(SPRITE.getNamespace()) && id.getPath().startsWith(SLOT_PREFIX);
    }

    public static @Nullable Slot slot(Identifier id) {
        return slotsById.get(id);
    }

    /** Virtual sprite id of the profile's head in the pool, or null while it is not there. Any thread. */
    public static @Nullable Identifier find(ResolvableProfile profile) {
        if (!isEnabled()) return null;
        Request request = requests.get(profile);
        return request == null ? null : request.slotId;
    }

    /** Starts fetching the profile's skin if needed; the waiter is refreshed once it lands in the pool. Main thread. */
    public static void request(ResolvableProfile profile, SkullBlockEntity waiter) {
        if (!isEnabled()) return;
        Request request = requests.get(profile);
        if (request == null) {
            request = new Request();
            requests.put(profile, request);
            fetch(profile, request);
        }
        if (!request.done) request.waiters.add(waiter);
    }

    private static void fetch(ResolvableProfile profile, Request request) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.playerSkinRenderCache().lookup(profile).handleAsync((info, error) -> {
            complete(profile, request, info == null ? Optional.empty() : info);
            return null;
        }, minecraft);
    }

    private static void complete(ResolvableProfile profile, Request request, Optional<PlayerSkinRenderCache.RenderInfo> info) {
        request.done = true;
        Slot slot = info.map(i -> acquire(i.playerSkin().body().texturePath())).orElse(null);
        List<SkullBlockEntity> waiters = List.copyOf(request.waiters);
        request.waiters.clear();
        if (slot == null) return;
        request.slotId = slot.id;
        for (SkullBlockEntity be : waiters) {
            if (!be.isRemoved() && profile.equals(be.getOwnerProfile())) SkullBlockUtil.refresh(be);
        }
    }

    private static @Nullable Slot acquire(Identifier skin) {
        Slot slot = slotsBySkin.get(skin);
        if (slot != null) return slot;

        // Downloaded skins are dynamic textures that keep their pixels; default and patched skins are not.
        if (!(Minecraft.getInstance().getTextureManager().getTexture(skin) instanceof DynamicTexture dynamic)) return null;
        NativeImage pixels = dynamic.getPixels();
        if (pixels.isClosed() || pixels.getWidth() != SKIN_SIZE || pixels.getHeight() < HEAD_HEIGHT) return null;

        TextureAtlas atlas = blocks();
        TextureAtlasSprite pool = atlas.getSprite(SPRITE);
        if (!pool.contents().name().equals(SPRITE)) return null;
        boolean full = slots.size() >= capacity(pool);
        if (full && unused.isEmpty()) {
            if (!warnedFull) Baked.LOGGER.warn("Player head skin pool is full ({} skins in use), the rest stay on the vanilla renderer", slots.size());
            warnedFull = true;
            return null;
        }

        NativeImage head = new NativeImage(SKIN_SIZE, HEAD_HEIGHT, false);
        for (int y = 0; y < HEAD_HEIGHT; y++) {
            for (int x = 0; x < SKIN_SIZE; x++) head.setPixel(x, y, pixels.getPixel(x, y));
        }
        // Gives empty hat texels the color of their neighbours, so mip levels do not fade to black.
        TextureUtil.solidify(head);

        if (full) {
            slot = evictOldestUnused();
            slot.head.close();
            slot.head = head;
            slot.skin = skin;
        } else {
            int index = slots.size();
            slot = new Slot(index, Identifier.fromNamespaceAndPath(SPRITE.getNamespace(), SLOT_PREFIX + index), skin, head);
            slots.add(slot);
            slotsById.put(slot.id, slot);
        }
        slotsBySkin.put(skin, slot);
        // Evictable until a head holds it, in case every waiting head is gone by now.
        unused.add(slot);
        upload(atlas, pool, slot);
        return slot;
    }

    /**
     * Frees the slot released longest ago. No loaded head uses it, so only the profiles resolved to it must forget it.
     */
    private static Slot evictOldestUnused() {
        Iterator<Slot> iterator = unused.iterator();
        Slot slot = iterator.next();
        iterator.remove();
        slotsBySkin.remove(slot.skin);
        requests.values().removeIf(request -> slot.id.equals(request.slotId));
        return slot;
    }

    /** Points a head at its slot (null for none), keeping slot reference counts. Main thread. */
    public static void hold(SkinHolder holder, @Nullable Identifier slotId) {
        Slot slot = slotId == null ? null : slotsById.get(slotId);
        Slot previous = holder.baked$skinSlot();
        if (previous == slot) return;
        if (slot != null && slot.references++ == 0) unused.remove(slot);
        if (previous != null && --previous.references == 0) unused.add(previous);
        holder.baked$skinSlot(slot);
    }

    /** The atlas was rebuilt blank: write every slot again. */
    public static void onAtlasUploaded(TextureAtlas atlas) {
        if (slots.isEmpty()) return;
        TextureAtlasSprite pool = atlas.getSprite(SPRITE);
        if (!pool.contents().name().equals(SPRITE)) return;
        for (Slot slot : slots) upload(atlas, pool, slot);
    }

    private static void upload(TextureAtlas atlas, TextureAtlasSprite pool, Slot slot) {
        int columns = columns(pool);
        if (slot.index >= capacity(pool)) return;
        GpuTexture texture = atlas.getTexture();
        // Sprites sit on multiples of 2^mipLevels, slot corners on multiples of 16: every level stays texel-aligned.
        int x = Math.round(pool.getU0() * texture.getWidth(0)) + (slot.index % columns) * SKIN_SIZE;
        int y = Math.round(pool.getV0() * texture.getHeight(0)) + (slot.index / columns) * HEAD_HEIGHT;
        int maxLevel = Math.min(texture.getMipLevels() - 1, MAX_MIP_LEVEL);

        NativeImage[] levels = MipmapGenerator.generateMipLevels(SPRITE, new NativeImage[] {slot.head}, maxLevel, MipmapStrategy.MEAN, 0.0F, Transparency.TRANSPARENT);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (int level = 0; level < levels.length; level++) {
            encoder.writeToTexture(texture, levels[level], level, 0, x >> level, y >> level);
            if (level > 0) levels[level].close();
        }
    }

    private static TextureAtlas blocks() {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
    }

    private static TextureAtlasSprite pool() {
        return blocks().getSprite(SPRITE);
    }

    private static int columns(TextureAtlasSprite pool) {
        return Math.max(1, pool.contents().width() / SKIN_SIZE);
    }

    private static int capacity(TextureAtlasSprite pool) {
        return (pool.contents().width() / SKIN_SIZE) * (pool.contents().height() / HEAD_HEIGHT);
    }

    public static int size() {
        return slots.size();
    }

    public static String stats() {
        int heads = 0;
        for (Slot slot : slots) heads += slot.references;
        long pending = requests.values().stream().filter(request -> !request.done).count();
        long failed = requests.values().stream().filter(request -> request.done && request.slotId == null).count();
        return "skin pool: skins=" + slots.size() + " unused=" + unused.size() + " baked heads=" + heads
            + " profiles pending=" + pending + " failed=" + failed + " enabled=" + isEnabled();
    }
}
