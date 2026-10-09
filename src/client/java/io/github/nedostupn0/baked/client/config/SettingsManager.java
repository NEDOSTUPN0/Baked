package io.github.nedostupn0.baked.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.compat.ModCompat;
import io.github.nedostupn0.baked.client.config.SettingsManager;
import io.github.nedostupn0.baked.client.platform.PlatformHelper;
import io.github.nedostupn0.baked.client.resources.ResourceUtil;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class SettingsManager {

    public static Map<String, Option<?>> ALL_OPTIONS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = PlatformHelper.getConfigDir().resolve(Baked.MOD_ID + ".json");
    private static Map<String, String> loadedSettings = loadSettings();

    private static Runnable emptyAction = () -> {};
    private static Runnable reloadResourcesAction = () -> {
        ResourceUtil.clearCache();
        Minecraft.getInstance().levelExtractor.allChanged();
    };
    private static Runnable reloadChunksAction = () -> Minecraft.getInstance().levelExtractor.allChanged();

    private static final boolean isIncompatible = ModCompat.isIncompatibilityDetected();

    public static Option<Boolean> MOD_TOGGLE = loadOptionWithDefaults("mod_toggle",
        "baked.config.option.mod_toggle",
        true,
        reloadResourcesAction
    );
    
    public static Option<Boolean> EMF_COMPAT = loadOptionWithDefaults("emf_compat",
        "baked.config.option.emf_compat",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> MODEL_OPTIMIZATION = loadOptionWithDefaults("optimized_models",
        "baked.config.option.optimised_models",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_CHESTS = loadOptionWithDefaults("optimized_chest",
        "baked.config.option.optimised_chests",
        !isIncompatible,
        reloadChunksAction
    );

    public static Option<Boolean> CHEST_AMBIENT_OCCLUSION = loadOptionWithDefaults("chest_ambient_occlusion",
        "baked.config.option.chest_ao",
        false,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_BANNERS = loadOptionWithDefaults("optimized_banner",
        "baked.config.option.optimised_banners",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> BANNER_AMBIENT_OCCLUSION = loadOptionWithDefaults("banner_ambient_occlusion",
        "baked.config.option.banner_ao",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_SIGNS = loadOptionWithDefaults("optimized_sign",
        "baked.config.option.optimised_signs",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> ATLAS_SIGN_TEXT = loadOptionWithDefaults("atlas_sign_text",
        "baked.config.option.atlas_sign_text",
        "baked.config.option.atlas_sign_text.description",
        true,
        emptyAction
    );

    public static Option<Boolean> SIGN_TEXT_RECHECK = loadOptionWithDefaults("sign_text_recheck",
        "baked.config.option.sign_text_recheck",
        "baked.config.option.sign_text_recheck.description",
        true,
        emptyAction
    );

    public static Option<Boolean> SIGN_TEXT_ALWAYS_OUTLINE = loadOptionWithDefaults("sign_text_always_outline",
        "baked.config.option.sign_text_always_outline",
        "baked.config.option.sign_text_always_outline.description",
        true,
        () -> io.github.nedostupn0.baked.client.renderer.sign.SignTextBatch.markAllDirty()
    );

    public static Option<Boolean> SIGN_AMBIENT_OCCLUSION = loadOptionWithDefaults("sign_ambient_occlusion",
        "baked.config.option.sign_ao",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> SIGN_TEXT_CULLING = loadOptionWithDefaults("sign_text_culling",
        "baked.config.option.sign_text_culling",
        true,
        emptyAction
    );

    public static Option<Boolean> OPTIMISED_SHULKER_BOXES = loadOptionWithDefaults("optimized_shulker_box",
        "baked.config.option.optimised_shulker_boxes",
        !isIncompatible,
        reloadChunksAction
    );

    public static Option<Boolean> SHULKER_BOX_AMBIENT_OCCLUSION = loadOptionWithDefaults("shulker_box_ambient_occlusion",
        "baked.config.option.shulker_box_ao",
        false,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_SKULLS = loadOptionWithDefaults("optimized_skull",
        "baked.config.option.optimised_skulls",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> SKULL_AMBIENT_OCCLUSION = loadOptionWithDefaults("skull_ambient_occlusion",
        "baked.config.option.skull_ao",
        false,
        reloadResourcesAction
    );

    public static Option<Boolean> CUSTOM_SKULLS = loadOptionWithDefaults("built_in_custom_skull",
        "baked.config.option.custom_skulls",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_BEDS = loadOptionWithDefaults("optimized_bed",
        "baked.config.option.optimised_beds",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> BED_AMBIENT_OCCLUSION = loadOptionWithDefaults("bed_ambient_occlusion",
        "baked.config.option.bed_ao",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_BELLS = loadOptionWithDefaults("optimized_bell",
        "baked.config.option.optimised_bells",
        !isIncompatible,
       reloadChunksAction
    );

    public static Option<Boolean> BELL_AMBIENT_OCCLUSION = loadOptionWithDefaults("bell_ambient_occlusion",
        "baked.config.option.bell_ao",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_DECORATED_POTS = loadOptionWithDefaults("optimized_decorated_pot",
        "baked.config.option.optimised_decorated_pots",
        !isIncompatible,
        reloadChunksAction
    );

    public static Option<Boolean> DECORATED_POT_AMBIENT_OCCLUSION = loadOptionWithDefaults("decorated_pot_ambient_occlusion",
        "baked.config.option.decorated_pot_ao",
        false,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_COPPER_GOLEMS = loadOptionWithDefaults("optimized_copper_golem",
        "baked.config.option.optimised_copper_golems",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> COPPER_GOLEM_AMBIENT_OCCLUSION = loadOptionWithDefaults("copper_golem_ambient_occlusion",
        "baked.config.option.copper_golem_ao",
        true,
        reloadResourcesAction
    );

    public static Option<Boolean> OPTIMISED_SHELVES = loadOptionWithDefaults("optimized_shelf",
        "baked.config.option.optimised_shelves",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> OPTIMISED_LECTERNS = loadOptionWithDefaults("optimized_lectern",
        "baked.config.option.optimised_lecterns",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> OPTIMISED_CAMPFIRES = loadOptionWithDefaults("optimized_campfire",
        "baked.config.option.optimised_campfires",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> OPTIMISED_BEACONS = loadOptionWithDefaults("optimized_beacon",
        "baked.config.option.optimised_beacons",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> OPTIMISED_ITEM_FRAMES = loadOptionWithDefaults("optimized_item_frame",
        "baked.config.option.optimised_item_frames",
        "baked.config.option.optimised_item_frames.description",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> EXPERIMENTAL_FRAME_EFFECTS = loadOptionWithDefaults("experimental_frame_effects",
        "baked.config.option.experimental_frame_effects",
        "baked.config.option.experimental_effects.description",
        false,
        SettingsManager::rebakeEntities
    );

    public static Option<Boolean> EXPERIMENTAL_STAND_EFFECTS = loadOptionWithDefaults("experimental_stand_effects",
        "baked.config.option.experimental_stand_effects",
        "baked.config.option.experimental_effects.description",
        false,
        SettingsManager::rebakeEntities
    );

    /** Entity options need fresh snapshots, not just rebuilt sections. */
    private static void rebakeEntities() {
        reloadChunksAction.run();
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) io.github.nedostupn0.baked.client.renderer.entity.MeshableEntityTracker.markAllDirty(level);
    }

    public static Option<Boolean> BATCHED_MAPS = loadOptionWithDefaults("batched_maps",
        "baked.config.option.batched_maps",
        "baked.config.option.batched_maps.description",
        true,
        SettingsManager::rebakeEntities
    );

    public static Option<Boolean> OPTIMISED_PAINTINGS = loadOptionWithDefaults("optimized_painting",
        "baked.config.option.optimised_paintings",
        "baked.config.option.optimised_paintings.description",
        true,
        reloadChunksAction
    );

    public static Option<Boolean> OPTIMISED_ARMOR_STANDS = loadOptionWithDefaults("optimized_armor_stand",
        "baked.config.option.optimised_armor_stands",
        "baked.config.option.optimised_armor_stands.description",
        true,
        reloadChunksAction
    );

    public static final Map<String, Option<Boolean>> GROUP_TOGGLE_SETTINGS = Map.of(
        "chest", SettingsManager.OPTIMISED_CHESTS,
        "banner", SettingsManager.OPTIMISED_BANNERS,
        "shulker_box", SettingsManager.OPTIMISED_SHULKER_BOXES,
        "skull", SettingsManager.OPTIMISED_SKULLS,
        "bell", SettingsManager.OPTIMISED_BELLS,
        "decorated_pot", SettingsManager.OPTIMISED_DECORATED_POTS,
        "copper_golem_statue", SettingsManager.OPTIMISED_COPPER_GOLEMS,
        "sign", SettingsManager.OPTIMISED_SKULLS,
        "bed", SettingsManager.OPTIMISED_BEDS
    );

    public static void saveSettings() {
        Map<String, String> map = toMap(ALL_OPTIONS.values());
        Set<Runnable> actions = new HashSet<Runnable>();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(map, writer);
            }
            Set<String> allEntries = new HashSet<>();

            allEntries.addAll(map.keySet());
            allEntries.addAll(loadedSettings.keySet());
            for(String key : allEntries){
                if(!Objects.equals(map.get(key), loadedSettings.get(key))){
                    if(SettingsManager.ALL_OPTIONS.containsKey(key)){
                        Runnable action = SettingsManager.ALL_OPTIONS.get(key).getRunnable();
                        if(actions.add(action)) action.run();
                    }
                }
            }
            if(map != null) loadedSettings = map;
        } catch (IOException e) {
            Baked.LOGGER.error("Failed to save config to {}", CONFIG_PATH, e);
        }
    }

    private static Map<String, String> toMap(Collection<Option<?>> options) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Option<?> option : options) {
            if (option.getValue() != option.getDefaultValue()) map.put(option.getId(), option.getValue().toString());
        }
        return map;
    }

    private static Map<String, String> loadSettings() {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> map = GSON.fromJson(reader, type);
            return map;
        } catch (Exception e) {
            Baked.LOGGER.info("Config file not found or invalid, using defaults");
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getOptionValue(String key, T defaultValue) {
        if (loadedSettings == null || !loadedSettings.containsKey(key)) return null;
        else if (defaultValue instanceof Enum<?> e){
            return (T) Enum.valueOf(e.getDeclaringClass(), loadedSettings.get(key));
        }
        else if (defaultValue instanceof Float){
            return (T) Float.valueOf(loadedSettings.get(key));
        }
        else if (defaultValue instanceof Double){
            return (T) Double.valueOf(loadedSettings.get(key));
        }
        else if (defaultValue instanceof Integer){
            return (T) Integer.valueOf(loadedSettings.get(key));
        }
        else if (defaultValue instanceof Boolean){
            return (T) (Boolean) Boolean.parseBoolean(loadedSettings.get(key));
        }
        else return null;
    }

    private static <T> Option<T> loadOptionWithDefaults(String id, String name, T defaultValue) {
        return loadOptionWithDefaults(id, name, name, defaultValue, emptyAction);
    }

    private static <T> Option<T> loadOptionWithDefaults(String id, String name, T defaultValue, Runnable action) {
        return loadOptionWithDefaults(id, name, name, defaultValue, action);
    }

    private static <T> Option<T> loadOptionWithDefaults(String id, String name, String description, T defaultValue) {
        return loadOptionWithDefaults(id, name, description, defaultValue, emptyAction);
    }

    private static <T> Option<T> loadOptionWithDefaults(String id, String name, String description, T defaultValue, Runnable action) {
        T optionValue= getOptionValue(id, defaultValue);
        if (optionValue == null) optionValue = defaultValue;
        Option<T> option = new Option<T>(
                id,
                name,
                description,
                optionValue,
                defaultValue,
                action
        );
        return option;
    }
}