package com.hbm.blocks.bomb;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The bomb drop rack: a bomb placed directly underneath hangs from it, and a redstone signal releases it into free
 * fall ({@link BombDropHook}). On a Valkyrien Skies ship the bomb leaves with the ship's speed.
 */
public class BlockBombRack extends CompatBlock {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public BlockBombRack(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, boolean movedByPiston) {
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), 3);
        if (powered) BombDropHook.release(level, pos);
    }
}
