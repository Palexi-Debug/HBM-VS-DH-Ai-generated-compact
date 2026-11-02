package com.hbm.platform.forge.drop;

import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A bomb released from a {@code nuke_drop_rack}. It carries the bomb block's state and its component inventory
 * (the saved block entity), falls like a falling block with the speed it had when it left the rack and, when it hits
 * something, puts the block back with its parts and fires the bomb through its normal {@link IBomb#explode} path.
 *
 * <p>Not saved with the world: a bomb in the air when the chunk unloads is lost.
 */
public class DroppedBomb extends Entity {

    private static final EntityDataAccessor<Integer> BLOCK_ID =
            SynchedEntityData.defineId(DroppedBomb.class, EntityDataSerializers.INT);

    /** Server only: the block entity data of the bomb (its parts). */
    private CompoundTag beData;

    public DroppedBomb(EntityType<? extends DroppedBomb> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public void setBomb(BlockState state, CompoundTag blockEntityData) {
        this.entityData.set(BLOCK_ID, Block.getId(state));
        this.beData = blockEntityData;
    }

    public BlockState getBombState() {
        return Block.stateById(this.entityData.get(BLOCK_ID));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK_ID, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.998D));
        if (level().isClientSide()) return;

        if (this.isOnGround() || this.horizontalCollision || this.verticalCollision) {
            impact();
        } else if (this.tickCount > 20 * 60 * 10 || getY() < level().getMinBuildHeight() - 64) {
            discard();
        }
    }

    /** Places the bomb back with its parts and detonates it; if it cannot be placed it simply disappears. */
    private void impact() {
        Level level = level();
        BlockState state = getBombState();
        BlockPos pos = BlockPos.containing(getX(), getBoundingBox().minY + 0.1D, getZ());
        for (int i = 0; i < 4 && !level.getBlockState(pos).canBeReplaced(); i++) pos = pos.above();
        discard();
        if (!(state.getBlock() instanceof IBomb bomb) || !level.getBlockState(pos).canBeReplaced()) return;

        level.setBlock(pos, state, 3);
        if (beData != null && level.getBlockEntity(pos) instanceof TileEntityNukeBomb be) {
            be.load(beData);
        }
        bomb.explode(level, pos, null);
    }

    /** Sets the starting velocity in blocks per tick. */
    public void launch(Vec3 velocity) {
        setDeltaMovement(velocity);
    }
}
