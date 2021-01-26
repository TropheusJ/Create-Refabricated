package com.simibubi.create.content.contraptions.components.structureMovement.chassis;

import static net.minecraft.block.enums.BambooLeaves.F;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.components.structureMovement.BlockMovementTraits;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.BulkScrollValueBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ChassisTileEntity extends SmartTileEntity {

	ScrollValueBehaviour range;

	public ChassisTileEntity(BellBlockEntity<? extends ChassisTileEntity> type) {
		super(type);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		int max = AllConfigs.SERVER.kinetics.maxChassisRange.get();
		range = new BulkScrollValueBehaviour(Lang.translate("generic.range"), this, new CenteredSideValueBoxTransform(),
				te -> ((ChassisTileEntity) te).collectChassisGroup());
		range.requiresWrench();
		range.between(1, max);
		range
				.withClientCallback(
						i -> DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ChassisRangeDisplay.display(this)));
		range.value = max / 2;
		behaviours.add(range);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (p().b() instanceof RadialChassisBlock)
			range.setLabel(Lang.translate("generic.radius"));
	}

	public int getRange() {
		return range.getValue();
	}

	public List<BlockPos> getIncludedBlockPositions(Direction forcedMovement, boolean visualize) {
		if (!(p().b() instanceof AbstractChassisBlock))
			return Collections.emptyList();
		return isRadial() ? getIncludedBlockPositionsRadial(forcedMovement, visualize)
				: getIncludedBlockPositionsLinear(forcedMovement, visualize);
	}

	protected boolean isRadial() {
		return d.d_(e).b() instanceof RadialChassisBlock;
	}

	public List<ChassisTileEntity> collectChassisGroup() {
		List<BlockPos> frontier = new ArrayList<>();
		List<ChassisTileEntity> collected = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		frontier.add(e);
		while (!frontier.isEmpty()) {
			BlockPos current = frontier.remove(0);
			if (visited.contains(current))
				continue;
			visited.add(current);
			BeehiveBlockEntity tileEntity = d.c(current);
			if (tileEntity instanceof ChassisTileEntity) {
				ChassisTileEntity chassis = (ChassisTileEntity) tileEntity;
				collected.add(chassis);
				visited.add(current);
				chassis.addAttachedChasses(frontier, visited);
			}
		}
		return collected;
	}

	public boolean addAttachedChasses(List<BlockPos> frontier, Set<BlockPos> visited) {
		PistonHandler state = p();
		if (!(state.b() instanceof AbstractChassisBlock))
			return false;
		Axis axis = state.c(AbstractChassisBlock.e);
		if (isRadial()) {

			// Collect chain of radial chassis
			for (int offset : new int[] { -1, 1 }) {
				Direction direction = Direction.get(AxisDirection.POSITIVE, axis);
				BlockPos currentPos = e.offset(direction, offset);
				if (!d.p(currentPos))
					return false;

				PistonHandler neighbourState = d.d_(currentPos);
				if (!AllBlocks.RADIAL_CHASSIS.has(neighbourState))
					continue;
				if (axis != neighbourState.c(BambooLeaves.F))
					continue;
				if (!visited.contains(currentPos))
					frontier.add(currentPos);
			}

			return true;
		}

		// Collect group of connected linear chassis
		for (Direction offset : Iterate.directions) {
			if (offset.getAxis() == axis)
				continue;
			BlockPos current = e.offset(offset);
			if (visited.contains(current))
				continue;
			if (!d.p(current))
				return false;

			PistonHandler neighbourState = d.d_(current);
			if (!LinearChassisBlock.isChassis(neighbourState))
				continue;
			if (!LinearChassisBlock.sameKind(state, neighbourState))
				continue;
			if (neighbourState.c(F) != axis)
				continue;

			frontier.add(current);
		}

		return true;
	}

	private List<BlockPos> getIncludedBlockPositionsLinear(Direction forcedMovement, boolean visualize) {
		List<BlockPos> positions = new ArrayList<>();
		PistonHandler state = p();
		AbstractChassisBlock block = (AbstractChassisBlock) state.b();
		Axis axis = state.c(AbstractChassisBlock.e);
		Direction facing = Direction.get(AxisDirection.POSITIVE, axis);
		int chassisRange = visualize ? range.scrollableValue : getRange();

		for (int offset : new int[] { 1, -1 }) {
			if (offset == -1)
				facing = facing.getOpposite();
			boolean sticky = state.c(block.getGlueableSide(state, facing));
			for (int i = 1; i <= chassisRange; i++) {
				BlockPos current = e.offset(facing, i);
				PistonHandler currentState = d.d_(current);

				if (forcedMovement != facing && !sticky)
					break;

				// Ignore replaceable Blocks and Air-like
				if (!BlockMovementTraits.movementNecessary(d, current))
					break;
				if (BlockMovementTraits.isBrittle(currentState))
					break;

				positions.add(current);

				if (BlockMovementTraits.notSupportive(currentState, facing))
					break;
			}
		}

		return positions;
	}

	private List<BlockPos> getIncludedBlockPositionsRadial(Direction forcedMovement, boolean visualize) {
		List<BlockPos> positions = new ArrayList<>();
		PistonHandler state = d.d_(e);
		Axis axis = state.c(AbstractChassisBlock.e);
		AbstractChassisBlock block = (AbstractChassisBlock) state.b();
		int chassisRange = visualize ? range.scrollableValue : getRange();

		for (Direction facing : Iterate.directions) {
			if (facing.getAxis() == axis)
				continue;
			if (!state.c(block.getGlueableSide(state, facing)))
				continue;

			BlockPos startPos = e.offset(facing);
			List<BlockPos> localFrontier = new LinkedList<>();
			Set<BlockPos> localVisited = new HashSet<>();
			localFrontier.add(startPos);

			while (!localFrontier.isEmpty()) {
				BlockPos searchPos = localFrontier.remove(0);
				PistonHandler searchedState = d.d_(searchPos);

				if (localVisited.contains(searchPos))
					continue;
				if (!searchPos.isWithinDistance(e, chassisRange + .5f))
					continue;
				if (!BlockMovementTraits.movementNecessary(d, searchPos))
					continue;
				if (BlockMovementTraits.isBrittle(searchedState))
					continue;

				localVisited.add(searchPos);
				if (!searchPos.equals(e))
					positions.add(searchPos);

				for (Direction offset : Iterate.directions) {
					if (offset.getAxis() == axis)
						continue;
					if (searchPos.equals(e) && offset != facing)
						continue;
					if (BlockMovementTraits.notSupportive(searchedState, offset))
						continue;

					localFrontier.add(searchPos.offset(offset));
				}
			}
		}

		return positions;
	}

}
