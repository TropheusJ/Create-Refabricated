package com.simibubi.create.content.contraptions.components.crafter;

import static com.simibubi.create.content.contraptions.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicates;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraftforge.common.util.Constants.NBT;

public class RecipeGridHandler {

	public static List<MechanicalCrafterTileEntity> getAllCraftersOfChain(MechanicalCrafterTileEntity root) {
		return getAllCraftersOfChainIf(root, Predicates.alwaysTrue());
	}

	public static List<MechanicalCrafterTileEntity> getAllCraftersOfChainIf(MechanicalCrafterTileEntity root,
		Predicate<MechanicalCrafterTileEntity> test) {
		return getAllCraftersOfChainIf(root, test, false);
	}

	public static List<MechanicalCrafterTileEntity> getAllCraftersOfChainIf(MechanicalCrafterTileEntity root,
		Predicate<MechanicalCrafterTileEntity> test, boolean poweredStart) {
		List<MechanicalCrafterTileEntity> crafters = new ArrayList<>();
		List<Pair<MechanicalCrafterTileEntity, MechanicalCrafterTileEntity>> frontier = new ArrayList<>();
		Set<MechanicalCrafterTileEntity> visited = new HashSet<>();
		frontier.add(Pair.of(root, null));

		boolean powered = false;
		boolean empty = false;
		boolean allEmpty = true;

		while (!frontier.isEmpty()) {
			Pair<MechanicalCrafterTileEntity, MechanicalCrafterTileEntity> pair = frontier.remove(0);
			MechanicalCrafterTileEntity current = pair.getKey();
			MechanicalCrafterTileEntity last = pair.getValue();

			if (visited.contains(current))
				return null;
			if (!(test.test(current)))
				empty = true;
			else
				allEmpty = false;
			if (poweredStart && current.v()
				.r(current.o()))
				powered = true;

			crafters.add(current);
			visited.add(current);

			MechanicalCrafterTileEntity target = getTargetingCrafter(current);
			if (target != last && target != null)
				frontier.add(Pair.of(target, current));
			for (MechanicalCrafterTileEntity preceding : getPrecedingCrafters(current))
				if (preceding != last)
					frontier.add(Pair.of(preceding, current));
		}

		return empty && !powered || allEmpty ? null : crafters;
	}

	public static MechanicalCrafterTileEntity getTargetingCrafter(MechanicalCrafterTileEntity crafter) {
		PistonHandler state = crafter.p();
		if (!isCrafter(state))
			return null;

		BlockPos targetPos = crafter.o()
			.offset(MechanicalCrafterBlock.getTargetDirection(state));
		MechanicalCrafterTileEntity targetTE = CrafterHelper.getCrafter(crafter.v(), targetPos);
		if (targetTE == null)
			return null;

		PistonHandler targetState = targetTE.p();
		if (!isCrafter(targetState))
			return null;
		if (state.c(HORIZONTAL_FACING) != targetState.c(HORIZONTAL_FACING))
			return null;
		return targetTE;
	}

	public static List<MechanicalCrafterTileEntity> getPrecedingCrafters(MechanicalCrafterTileEntity crafter) {
		BlockPos pos = crafter.o();
		GameMode world = crafter.v();
		List<MechanicalCrafterTileEntity> crafters = new ArrayList<>();
		PistonHandler blockState = crafter.p();
		if (!isCrafter(blockState))
			return crafters;

		Direction blockFacing = blockState.c(HORIZONTAL_FACING);
		Direction blockPointing = MechanicalCrafterBlock.getTargetDirection(blockState);
		for (Direction facing : Iterate.directions) {
			if (blockFacing.getAxis() == facing.getAxis())
				continue;
			if (blockPointing == facing)
				continue;

			BlockPos neighbourPos = pos.offset(facing);
			PistonHandler neighbourState = world.d_(neighbourPos);
			if (!isCrafter(neighbourState))
				continue;
			if (MechanicalCrafterBlock.getTargetDirection(neighbourState) != facing.getOpposite())
				continue;
			if (blockFacing != neighbourState.c(HORIZONTAL_FACING))
				continue;
			MechanicalCrafterTileEntity te = CrafterHelper.getCrafter(world, neighbourPos);
			if (te == null)
				continue;

			crafters.add(te);
		}

		return crafters;
	}

	private static boolean isCrafter(PistonHandler state) {
		return AllBlocks.MECHANICAL_CRAFTER.has(state);
	}

	public static ItemCooldownManager tryToApplyRecipe(GameMode world, GroupedItems items) {
		items.calcStats();
		PropertyDelegate craftinginventory = new MechanicalCraftingInventory(items);
		ItemCooldownManager result = null;
		if (AllConfigs.SERVER.recipes.allowRegularCraftingInCrafter.get())
			result = world.o()
				.a(Recipe.a, craftinginventory, world)
				.map(r -> r.a(craftinginventory))
				.orElse(null);
		if (result == null)
			result = AllRecipeTypes.MECHANICAL_CRAFTING.find(craftinginventory, world)
				.map(r -> r.a(craftinginventory))
				.orElse(null);
		return result;
	}

	public static class GroupedItems {
		Map<Pair<Integer, Integer>, ItemCooldownManager> grid = new HashMap<>();
		int minX, minY, maxX, maxY, width, height;
		boolean statsReady;

		public GroupedItems() {}

		public GroupedItems(ItemCooldownManager stack) {
			grid.put(Pair.of(0, 0), stack);
		}

		public void mergeOnto(GroupedItems other, Pointing pointing) {
			int xOffset = pointing == Pointing.LEFT ? 1 : pointing == Pointing.RIGHT ? -1 : 0;
			int yOffset = pointing == Pointing.DOWN ? 1 : pointing == Pointing.UP ? -1 : 0;
			grid.forEach(
				(pair, stack) -> other.grid.put(Pair.of(pair.getKey() + xOffset, pair.getValue() + yOffset), stack));
			other.statsReady = false;
		}

		public void write(CompoundTag nbt) {
			ListTag gridNBT = new ListTag();
			grid.forEach((pair, stack) -> {
				CompoundTag entry = new CompoundTag();
				entry.putInt("x", pair.getKey());
				entry.putInt("y", pair.getValue());
				entry.put("item", stack.serializeNBT());
				gridNBT.add(entry);
			});
			nbt.put("Grid", gridNBT);
		}

		public static GroupedItems read(CompoundTag nbt) {
			GroupedItems items = new GroupedItems();
			ListTag gridNBT = nbt.getList("Grid", NBT.TAG_COMPOUND);
			gridNBT.forEach(inbt -> {
				CompoundTag entry = (CompoundTag) inbt;
				int x = entry.getInt("x");
				int y = entry.getInt("y");
				ItemCooldownManager stack = ItemCooldownManager.a(entry.getCompound("item"));
				items.grid.put(Pair.of(x, y), stack);
			});
			return items;
		}

		public void calcStats() {
			if (statsReady)
				return;
			statsReady = true;

			minX = 0;
			minY = 0;
			maxX = 0;
			maxY = 0;

			for (Pair<Integer, Integer> pair : grid.keySet()) {
				int x = pair.getKey();
				int y = pair.getValue();
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}

			width = maxX - minX + 1;
			height = maxY - minY + 1;
		}

	}

}