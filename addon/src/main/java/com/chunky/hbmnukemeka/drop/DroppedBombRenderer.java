package com.chunky.hbmnukemeka.drop;

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
            String name = String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()));
            // Long, horizontal bombs fall nose down. Per model (measured from the OBJ at yaw 0): the facing that gives
            // yaw 0, the model centre relative to the block centre, and which end is the nose (+1 = +X).
            //                       facing  centreX  centreY  nose
            float[] pose0 = name.endsWith(":nuke_boy") ? new float[] {2, -0.75F, 0.425F, 1}
                    : name.endsWith(":nuke_man") ? new float[] {4, 0.405F, 0.75F, -1}
                    : name.endsWith(":nuke_tsar") ? new float[] {2, -1.23F, 1.27F, 1}
                    : null;
            if (pose0 != null) {
                for (net.minecraft.world.level.block.state.properties.Property<?> p : state.getProperties()) {
                    if (p.getName().equals("facing") && p instanceof net.minecraft.world.level.block.state.properties.IntegerProperty ip) {
                        state = state.setValue(ip, (int) pose0[0]);
                    }
                }
            }
            int id = net.minecraft.world.level.block.Block.getId(state);
            if (cached == null || cachedId != id) {
                cached = state.getBlock() instanceof EntityBlock eb ? eb.newBlockEntity(BlockPos.ZERO, state) : null;
                if (cached != null) cached.setLevel(entity.level());
                cachedId = id;
            }
            if (cached == null) return;
            pose.pushPose();
            if (pose0 != null) {
                // about the hit box centre: nose to straight down, then move the model centre onto that point
                pose.translate(0.0D, 0.49D, 0.0D);
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90F * pose0[3]));
                pose.translate(-(0.5D + pose0[1]), -pose0[2], -0.5D);
            } else {
                pose.translate(-0.5D, 0.0D, -0.5D); // upright bombs (gadget, N2, ...) just fall as they stand
            }
            Minecraft.getInstance().getBlockEntityRenderDispatcher()
                    .renderItem(cached, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        } catch (Throwable t) {
            failed = true;
            com.chunky.hbmnukemeka.HbmNukeMeka.LOGGER.warn("Falling bomb renderer disabled: {}", t.toString());
        }
    }
}
