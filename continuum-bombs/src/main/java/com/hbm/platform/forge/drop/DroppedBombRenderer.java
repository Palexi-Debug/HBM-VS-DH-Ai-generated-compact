package com.hbm.platform.forge.drop;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the falling bomb with the bomb block's own block entity renderer (the bombs are not plain block models). */
public class DroppedBombRenderer extends EntityRenderer<DroppedBomb> {

    private BlockEntity cached;
    private int cachedId = -1;
    private boolean failed;

    public DroppedBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(DroppedBomb entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(DroppedBomb entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        super.render(entity, yaw, partialTicks, pose, buffers, light);
        if (failed) return;
        try {
            BlockState state = entity.getBombState();
            int id = net.minecraft.world.level.block.Block.getId(state);
            if (cached == null || cachedId != id) {
                cached = state.getBlock() instanceof EntityBlock eb ? eb.newBlockEntity(BlockPos.ZERO, state) : null;
                if (cached != null) cached.setLevel(entity.level());
                cachedId = id;
            }
            if (cached == null) return;
            pose.pushPose();
            pose.translate(-0.5D, 0.0D, -0.5D);
            Minecraft.getInstance().getBlockEntityRenderDispatcher()
                    .renderItem(cached, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        } catch (Throwable t) {
            failed = true;
            com.hbm.Constants.LOG.warn("Falling bomb renderer disabled: {}", t.toString());
        }
    }
}
