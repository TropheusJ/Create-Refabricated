package com.tropheus_jay.kinetic_api.content.contraptions.relays.elementary;

import com.tropheus_jay.kinetic_api.content.contraptions.base.RotatedPillarKineticBlock;
import com.tropheus_jay.kinetic_api.content.contraptions.wrench.IWrenchableWithBracket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.Optional;

public abstract class AbstractShaftBlock extends RotatedPillarKineticBlock implements Waterloggable, IWrenchableWithBracket {

	public AbstractShaftBlock(Settings settings) {
		super(settings);
		setDefaultState(super.getDefaultState().with(Properties.WATERLOGGED, false));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return IWrenchableWithBracket.super.onWrenched(state, context);
	}

	@Override
	public PistonBehavior getPistonBehavior(BlockState state) {
		return PistonBehavior.NORMAL;
	}

	/*@Override todo: KILL ME ALREADY
	public BlockEntity createTileEntity(BlockState state, BlockView world) {
		return AllTileEntities.SIMPLE_KINETIC.kinetic_api();
	}*/

	@Override
	@SuppressWarnings("deprecation")
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state != newState && !isMoving) {
			//removeBracket(world, pos, true).ifPresent(stack -> Block.dropStack(world, pos, stack)); todo: brackets
			super.onStateReplaced(state, world, pos, newState, isMoving);
		}
	}

	// IRotate:

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.get(AXIS);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(AXIS);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false)
				: Fluids.EMPTY.getDefaultState();
	}

/* todo: why is builder the wrong builder */

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(Properties.WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState,
		WorldAccess world, BlockPos pos, BlockPos neighbourPos) {
		if (state.get(Properties.WATERLOGGED)) {
			world.getFluidTickScheduler()
				.schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
		return state;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		FluidState ifluidstate = context.getWorld()
			.getFluidState(context.getBlockPos());
		return super.getPlacementState(context).with(Properties.WATERLOGGED,
			Boolean.valueOf(ifluidstate.getFluid() == Fluids.WATER));
	}
//todo: brackets
	@Override
	public Optional<ItemStack> removeBracket(BlockView world, BlockPos pos, boolean inOnReplacedContext) {
		/*BracketedTileEntityBehaviour behaviour = TileEntityBehaviour.get(world, pos, BracketedTileEntityBehaviour.TYPE);
		if (behaviour == null)
			return Optional.empty();
		BlockState bracket = behaviour.getBracket();
		behaviour.removeBracket(inOnReplacedContext);
		if (bracket == BellBlock.FACING.n())
			return Optional.empty();
		return Optional.of(new ItemCooldownManager(bracket.b()));
		*/
		return null; //this is such a horrible idea
	}
}
