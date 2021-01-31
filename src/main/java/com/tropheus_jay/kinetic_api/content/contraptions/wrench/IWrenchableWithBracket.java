package com.tropheus_jay.kinetic_api.content.contraptions.wrench;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Optional;

public interface IWrenchableWithBracket extends IWrenchable {

	public Optional<ItemStack> removeBracket(BlockView world, BlockPos pos, boolean inOnReplacedContext);

	@Override
	default ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (tryRemoveBracket(context))
			return ActionResult.SUCCESS;
		return IWrenchable.super.onWrenched(state, context);
	}

	default boolean tryRemoveBracket(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		Optional<ItemStack> bracket = removeBracket(world, pos, false);
		BlockState blockState = world.getBlockState(pos);
		if (bracket.isPresent()) {
			PlayerEntity player = context.getPlayer();
			if (!world.isClient && !player.isCreative())
				player.inventory.offerOrDrop(world, bracket.get());
			/*if (!world.isClient && AllBlocks.FLUID_PIPE.has(blockState)) { todo: fluids / pipes
				Axis preferred = FluidPropagator.getStraightPipeAxis(blockState);
				Direction preferredDirection =
					preferred == null ? Direction.UP : Direction.get(AxisDirection.POSITIVE, preferred);
				PistonHandler updated = AllBlocks.FLUID_PIPE.get()
					.updateBlockState(blockState, preferredDirection, null, world, pos);
				if (updated != blockState)
					world.a(pos, updated);
			} */
			return true;
		}
		return false;
	}

}
