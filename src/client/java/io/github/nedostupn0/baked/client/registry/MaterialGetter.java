package io.github.nedostupn0.baked.client.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.nedostupn0.baked.Baked;
import io.github.nedostupn0.baked.client.util.blockentity.BannerUtil;
import io.github.nedostupn0.baked.client.util.blockentity.BellUtil;
import io.github.nedostupn0.baked.client.util.blockentity.ChestUtil;
import io.github.nedostupn0.baked.client.util.blockentity.CopperGolemStatueUtil;
import io.github.nedostupn0.baked.client.util.blockentity.DecoratedPotUtil;
import io.github.nedostupn0.baked.client.util.blockentity.ShulkerBoxUtil;
import io.github.nedostupn0.baked.client.util.blockentity.SkullBlockUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MaterialGetter {
    private static Map<BlockEntityType<?>, Function<BlockState, Identifier>> materialsGetterProvider = new ConcurrentHashMap<>();
    private static Map<Block, Function<BlockState, Identifier>> perBlockMaterialsGetterProvider = new ConcurrentHashMap<>();
    private static Map<String, Function<BlockState, Identifier>> defaultMaterialsGetterProvider = new ConcurrentHashMap<>();

    public static void init(){
        registerDefault("chest", ChestUtil::getMaterial);
        registerDefault("skull", SkullBlockUtil::getMaterial);
        registerDefault("bell", BellUtil::getMaterial);
        registerDefault("banner", BannerUtil::getMaterial);
        registerDefault("shulker_box", ShulkerBoxUtil::getMaterial);
        registerDefault("decorated_pot", DecoratedPotUtil::getMaterial);
        registerDefault("copper_golem_statue", CopperGolemStatueUtil::getMaterial);
    }

    public static void registerDefault(String group, Function<BlockState, Identifier> getter){
        if(!Registry.hasGroup(group)){
            Baked.LOGGER.error("An external mod tried registering a default material getter in a non existing group: " + group);
        }
        else{
            defaultMaterialsGetterProvider.put(group, getter);
        }
    }

    public static void register(BlockEntityType<?> beType, Function<BlockState, Identifier> getter){
        materialsGetterProvider.put(beType, getter);
    }

    public static void register(Block block, Function<BlockState, Identifier> getter){
        perBlockMaterialsGetterProvider.put(block, getter);
    }

    public static Identifier getMaterial(BlockState state){
        return getMaterial(state, null);
    }

    public static Identifier getMaterial(BlockState state, String group){
        if(!state.hasBlockEntity()) return null;

        Block block = state.getBlock();
        Function<BlockState, Identifier> provider = perBlockMaterialsGetterProvider.get(block);
        if (provider != null) return provider.apply(state);

        BlockEntityType<?> beType = Registry.getBlockEntityType(state);
        if (beType == null) return null;
        provider = materialsGetterProvider.get(beType);
        if (provider != null) return provider.apply(state);

        if (group == null) group = Registry.getGroup(beType);
        if (group != null) provider = defaultMaterialsGetterProvider.get(group);
        if (provider != null) return provider.apply(state);
        
        return null;
    }
}
