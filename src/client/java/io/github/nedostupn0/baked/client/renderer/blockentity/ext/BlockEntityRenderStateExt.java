package io.github.nedostupn0.baked.client.renderer.blockentity.ext;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityRenderStateExt {
    void blockEntity(BlockEntity blockEntity);
    BlockEntity blockEntity();

    /** The block entity is baked into the terrain: this state only carries its breaking overlay. */
    boolean baked$crumblingOnly();
    void baked$crumblingOnly(boolean crumblingOnly);
}
