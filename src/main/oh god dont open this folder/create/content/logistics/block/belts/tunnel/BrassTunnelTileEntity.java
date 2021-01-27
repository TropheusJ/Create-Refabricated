package com.simibubi.create.content.logistics.block.belts.tunnel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.relays.belt.BeltHelper;
import com.simibubi.create.content.contraptions.relays.belt.BeltTileEntity;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.SidedFilteringBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class BrassTunnelTileEntity extends BeltTunnelTileEntity {

	SidedFilteringBehaviour filtering;

	boolean connectedLeft;
	boolean connectedRight;

	ItemCooldownManager stackToDistribute;
	float distributionProgress;
	List<Pair<BlockPos, Direction>> distributionTargets;
	int distributionDistanceLeft;
	int distributionDistanceRight;
	int previousOutputIndex;

	private boolean syncedOutputActive;
	private Set<BrassTunnelTileEntity> syncSet;

	protected ScrollOptionBehaviour<SelectionMode> selectionMode;
	private LazyOptional<IItemHandler> beltCapability;
	private LazyOptional<IItemHandler> tunnelCapability;

	public BrassTunnelTileEntity(BellBlockEntity<? extends BeltTunnelTileEntity> type) {
		super(type);
		distributionTargets = new ArrayList<>();
		syncSet = new HashSet<>();
		stackToDistribute = ItemCooldownManager.tick;
		beltCapability = LazyOptional.empty();
		tunnelCapability = LazyOptional.of(() -> new BrassTunnelItemHandler(this));
		previousOutputIndex = 0;
		syncedOutputActive = false;
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(selectionMode = new ScrollOptionBehaviour<>(SelectionMode.class,
			Lang.translate("logistics.when_multiple_outputs_available"), this,
			new CenteredSideValueBoxTransform((state, d) -> d == Direction.UP)));
		selectionMode.requiresWrench();

		// Propagate settings across connected tunnels
		selectionMode.withCallback(setting -> {
			for (boolean side : Iterate.trueAndFalse) {
				if (!isConnected(side))
					continue;
				BrassTunnelTileEntity adjacent = getAdjacent(side);
				if (adjacent != null)
					adjacent.selectionMode.setValue(setting);
			}
		});
	}

	@Override
	public void aj_() {
		super.aj_();
		BeltTileEntity beltBelow = BeltHelper.getSegmentTE(d, e.down());

		if (distributionProgress > 0)
			distributionProgress--;
		if (beltBelow == null || beltBelow.getSpeed() == 0)
			return;
		if (stackToDistribute.a() && !syncedOutputActive)
			return;
		if (d.v)
			return;

		if (distributionProgress == -1) {
			distributionTargets.clear();
			distributionDistanceLeft = 0;
			distributionDistanceRight = 0;

			syncSet.clear();
			List<Pair<BrassTunnelTileEntity, Direction>> validOutputs = gatherValidOutputs();
			if (selectionMode.get() == SelectionMode.SYNCHRONIZE) {
				boolean allEmpty = true;
				boolean allFull = true;
				for (BrassTunnelTileEntity te : syncSet) {
					boolean hasStack = !te.stackToDistribute.a();
					allEmpty &= !hasStack;
					allFull &= hasStack;
				}
				final boolean notifySyncedOut = !allEmpty;
				if (allFull || allEmpty)
					syncSet.forEach(te -> te.syncedOutputActive = notifySyncedOut);
			}

			if (validOutputs == null)
				return;
			if (stackToDistribute.a())
				return;

			for (boolean filterPass : Iterate.trueAndFalse) {
				for (Pair<BrassTunnelTileEntity, Direction> pair : validOutputs) {
					BrassTunnelTileEntity tunnel = pair.getKey();
					Direction output = pair.getValue();
					if (filterPass && tunnel.flapFilterEmpty(output))
						continue;
					if (insertIntoTunnel(tunnel, output, stackToDistribute, true) == null)
						continue;
					distributionTargets.add(Pair.of(tunnel.e, output));
					int distance = tunnel.e.getX() + tunnel.e.getZ() - e.getX() - e.getZ();
					if (distance < 0)
						distributionDistanceLeft = Math.max(distributionDistanceLeft, -distance);
					else
						distributionDistanceRight = Math.max(distributionDistanceRight, distance);
				}

				if (!distributionTargets.isEmpty() && filterPass)
					break;
			}

			if (distributionTargets.isEmpty())
				return;

			if (selectionMode.get() != SelectionMode.SYNCHRONIZE || syncedOutputActive) {
				distributionProgress = 10;
				sendData();
			}
			return;
		}

		if (distributionProgress == 0) {
			List<Pair<BrassTunnelTileEntity, Direction>> validTargets = new ArrayList<>();
			for (Pair<BlockPos, Direction> pair : distributionTargets) {
				BlockPos tunnelPos = pair.getKey();
				Direction output = pair.getValue();
				BeehiveBlockEntity te = d.c(tunnelPos);
				if (!(te instanceof BrassTunnelTileEntity))
					continue;
				validTargets.add(Pair.of((BrassTunnelTileEntity) te, output));
			}

			distribute(validTargets);
			distributionProgress = -1;
			return;
		}

	}

	private static Random rand = new Random();

	private void distribute(List<Pair<BrassTunnelTileEntity, Direction>> validTargets) {
		final int amountTargets = validTargets.size();
		if (amountTargets == 0)
			return;

		int indexStart = previousOutputIndex % amountTargets;
		SelectionMode mode = selectionMode.get();
		boolean force = mode == SelectionMode.FORCED_ROUND_ROBIN || mode == SelectionMode.FORCED_SPLIT;
		boolean split = mode == SelectionMode.FORCED_SPLIT || mode == SelectionMode.SPLIT;

		if (mode == SelectionMode.RANDOMIZE)
			indexStart = rand.nextInt(amountTargets);
		if (mode == SelectionMode.PREFER_NEAREST || mode == SelectionMode.SYNCHRONIZE)
			indexStart = 0;

		ItemCooldownManager toDistribute = null;
		int leftovers = 0;

		for (boolean simulate : Iterate.trueAndFalse) {
			leftovers = 0;
			int index = indexStart;
			int stackSize = stackToDistribute.E();
			int splitStackSize = stackSize / amountTargets;
			int splitRemainder = stackSize % amountTargets;
			int visited = 0;

			toDistribute = stackToDistribute.i();
			if (!force && simulate)
				continue;
			while (visited < amountTargets) {
				Pair<BrassTunnelTileEntity, Direction> pair = validTargets.get(index);
				BrassTunnelTileEntity tunnel = pair.getKey();
				Direction side = pair.getValue();
				index = (index + 1) % amountTargets;
				visited++;

				int count = split ? splitStackSize + (splitRemainder > 0 ? 1 : 0) : stackSize;
				ItemCooldownManager toOutput = ItemHandlerHelper.copyStackWithSize(toDistribute, count);
				ItemCooldownManager remainder = insertIntoTunnel(tunnel, side, toOutput, simulate);

				if (remainder == null || remainder.E() == count) {
					if (force)
						return;
					continue;
				}

				leftovers += remainder.E();
				toDistribute.g(count);
				if (toDistribute.a())
					break;
				splitRemainder--;
				if (!split)
					break;
			}
		}

		stackToDistribute = ItemHandlerHelper.copyStackWithSize(stackToDistribute, toDistribute.E() + leftovers);
		previousOutputIndex++;
		previousOutputIndex %= amountTargets;
		notifyUpdate();
	}

	public void setStackToDistribute(ItemCooldownManager stack) {
		stackToDistribute = stack;
		distributionProgress = -1;
		sendData();
		X_();
	}

	public ItemCooldownManager getStackToDistribute() {
		return stackToDistribute;
	}

	@Nullable
	protected ItemCooldownManager insertIntoTunnel(BrassTunnelTileEntity tunnel, Direction side, ItemCooldownManager stack,
		boolean simulate) {
		if (stack.a())
			return stack;
		if (!tunnel.testFlapFilter(side, stack))
			return null;

		BeltTileEntity below = BeltHelper.getSegmentTE(d, tunnel.e.down());
		if (below == null)
			return null;
		BlockPos offset = tunnel.o()
			.down()
			.offset(side);
		DirectBeltInputBehaviour sideOutput = TileEntityBehaviour.get(d, offset, DirectBeltInputBehaviour.TYPE);
		if (sideOutput != null) {
			if (!sideOutput.canInsertFromSide(side))
				return null;
			ItemCooldownManager result = sideOutput.handleInsertion(stack, side, simulate);
			if (result.a() && !simulate)
				tunnel.flap(side, true);
			return result;
		}

		Direction movementFacing = below.getMovementFacing();
		if (side == movementFacing)
			if (!BlockHelper.hasBlockSolidSide(d.d_(offset), d, offset, side.getOpposite())) {
				BeltTileEntity controllerTE = below.getControllerTE();
				if (controllerTE == null)
					return null;

				if (!simulate) {
					tunnel.flap(side, true);
					ItemCooldownManager ejected = stack;
					float beltMovementSpeed = below.getDirectionAwareBeltMovementSpeed();
					float movementSpeed = Math.max(Math.abs(beltMovementSpeed), 1 / 8f);
					int additionalOffset = beltMovementSpeed > 0 ? 1 : 0;
					EntityHitResult outPos = BeltHelper.getVectorForOffset(controllerTE, below.index + additionalOffset);
					EntityHitResult outMotion = EntityHitResult.b(side.getVector()).a(movementSpeed)
						.b(0, 1 / 8f, 0);
					outPos.e(outMotion.d());
					PaintingEntity entity = new PaintingEntity(d, outPos.entity, outPos.c + 6 / 16f, outPos.d, ejected);
					entity.f(outMotion);
					entity.m();
					entity.w = true;
					d.c(entity);
				}

				return ItemCooldownManager.tick;
			}

		return null;
	}

	public boolean testFlapFilter(Direction side, ItemCooldownManager stack) {
		if (filtering == null)
			return false;
		if (filtering.get(side) == null) {
			FilteringBehaviour adjacentFilter =
				TileEntityBehaviour.get(d, e.offset(side), FilteringBehaviour.TYPE);
			if (adjacentFilter == null)
				return true;
			return adjacentFilter.test(stack);
		}
		return filtering.test(side, stack);
	}

	public boolean flapFilterEmpty(Direction side) {
		if (filtering == null)
			return false;
		if (filtering.get(side) == null) {
			FilteringBehaviour adjacentFilter =
				TileEntityBehaviour.get(d, e.offset(side), FilteringBehaviour.TYPE);
			if (adjacentFilter == null)
				return true;
			return adjacentFilter.getFilter()
				.a();
		}
		return filtering.getFilter(side)
			.a();
	}

	@Override
	public void initialize() {
		if (filtering == null) {
			filtering = createSidedFilter();
			attachBehaviourLate(filtering);
		}
		super.initialize();
	}

	public boolean canInsert(Direction side, ItemCooldownManager stack) {
		if (filtering != null && !filtering.test(side, stack))
			return false;
		if (!hasDistributionBehaviour())
			return true;
		if (!stackToDistribute.a())
			return false;
		return true;
	}

	public boolean hasDistributionBehaviour() {
		if (flaps.isEmpty())
			return false;
		if (connectedLeft || connectedRight)
			return true;
		PistonHandler blockState = p();
		if (!AllBlocks.BRASS_TUNNEL.has(blockState))
			return false;
		Axis axis = blockState.c(BrassTunnelBlock.HORIZONTAL_AXIS);
		for (Direction direction : flaps.keySet())
			if (direction.getAxis() != axis)
				return true;
		return false;
	}

	private List<Pair<BrassTunnelTileEntity, Direction>> gatherValidOutputs() {
		List<Pair<BrassTunnelTileEntity, Direction>> validOutputs = new ArrayList<>();
		boolean synchronize = selectionMode.get() == SelectionMode.SYNCHRONIZE;
		addValidOutputsOf(this, validOutputs);

		for (boolean left : Iterate.trueAndFalse) {
			BrassTunnelTileEntity adjacent = this;
			while (adjacent != null) {
				if (!d.isAreaLoaded(adjacent.o(), 1))
					return null;
				adjacent = adjacent.getAdjacent(left);
				if (adjacent == null)
					continue;
				addValidOutputsOf(adjacent, validOutputs);
			}
		}

		if (!syncedOutputActive && synchronize)
			return null;
		return validOutputs;
	}

	private void addValidOutputsOf(BrassTunnelTileEntity tunnelTE,
		List<Pair<BrassTunnelTileEntity, Direction>> validOutputs) {
		syncSet.add(tunnelTE);
		BeltTileEntity below = BeltHelper.getSegmentTE(d, tunnelTE.e.down());
		if (below == null)
			return;
		Direction movementFacing = below.getMovementFacing();
		PistonHandler blockState = p();
		if (!AllBlocks.BRASS_TUNNEL.has(blockState))
			return;

		for (Direction direction : Iterate.horizontalDirections) {
			if (direction == movementFacing && below.getSpeed() == 0)
				continue;
			if (direction == movementFacing.getOpposite())
				continue;
			if (tunnelTE.sides.contains(direction)) {
				BlockPos offset = tunnelTE.e.down()
					.offset(direction);
				DirectBeltInputBehaviour inputBehaviour =
					TileEntityBehaviour.get(d, offset, DirectBeltInputBehaviour.TYPE);
				if (inputBehaviour == null) {
					if (direction == movementFacing)
						if (!BlockHelper.hasBlockSolidSide(d.d_(offset), d, offset, direction.getOpposite()))
							validOutputs.add(Pair.of(tunnelTE, direction));
					continue;
				}
				if (inputBehaviour.canInsertFromSide(direction))
					validOutputs.add(Pair.of(tunnelTE, direction));
				continue;
			}
		}
	}

	@Override
	public void addBehavioursDeferred(List<TileEntityBehaviour> behaviours) {
		super.addBehavioursDeferred(behaviours);
		filtering = createSidedFilter();
		behaviours.add(filtering);
	}

	protected SidedFilteringBehaviour createSidedFilter() {
		return new SidedFilteringBehaviour(this, new BrassTunnelFilterSlot(), this::makeFilter,
			this::isValidFaceForFilter);
	}

	private FilteringBehaviour makeFilter(Direction side, FilteringBehaviour filter) {
		return filter;
	}

	private boolean isValidFaceForFilter(Direction side) {
		return sides.contains(side);
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		compound.putBoolean("SyncedOutput", syncedOutputActive);
		compound.putBoolean("ConnectedLeft", connectedLeft);
		compound.putBoolean("ConnectedRight", connectedRight);

		compound.put("StackToDistribute", stackToDistribute.serializeNBT());
		compound.putFloat("DistributionProgress", distributionProgress);
		compound.putInt("PreviousIndex", previousOutputIndex);
		compound.putInt("DistanceLeft", distributionDistanceLeft);
		compound.putInt("DistanceRight", distributionDistanceRight);
		compound.put("Targets", NBTHelper.writeCompoundList(distributionTargets, pair -> {
			CompoundTag nbt = new CompoundTag();
			nbt.put("Pos", NbtHelper.fromBlockPos(pair.getKey()));
			nbt.putInt("Face", pair.getValue()
				.getId());
			return nbt;
		}));

		super.write(compound, clientPacket);
	}

	@Override
	protected void fromTag(PistonHandler state, CompoundTag compound, boolean clientPacket) {
		boolean wasConnectedLeft = connectedLeft;
		boolean wasConnectedRight = connectedRight;

		syncedOutputActive = compound.getBoolean("SyncedOutput");
		connectedLeft = compound.getBoolean("ConnectedLeft");
		connectedRight = compound.getBoolean("ConnectedRight");
		stackToDistribute = ItemCooldownManager.a(compound.getCompound("StackToDistribute"));
		distributionProgress = compound.getFloat("DistributionProgress");
		previousOutputIndex = compound.getInt("PreviousIndex");
		distributionDistanceLeft = compound.getInt("DistanceLeft");
		distributionDistanceRight = compound.getInt("DistanceRight");
		distributionTargets = NBTHelper.readCompoundList(compound.getList("Targets", NBT.TAG_COMPOUND), nbt -> {
			BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("Pos"));
			Direction face = Direction.byId(nbt.getInt("Face"));
			return Pair.of(pos, face);
		});

		super.fromTag(state, compound, clientPacket);

		if (!clientPacket)
			return;
		if (wasConnectedLeft != connectedLeft || wasConnectedRight != connectedRight) {
			requestModelDataUpdate();
			if (n())
				d.a(o(), p(), p(), 16);
		}
		filtering.updateFilterPresence();
	}

	public boolean isConnected(boolean leftSide) {
		return leftSide ? connectedLeft : connectedRight;
	}

	@Override
	public void updateTunnelConnections() {
		super.updateTunnelConnections();
		boolean connectivityChanged = false;
		boolean nowConnectedLeft = determineIfConnected(true);
		boolean nowConnectedRight = determineIfConnected(false);

		if (connectedLeft != nowConnectedLeft) {
			connectedLeft = nowConnectedLeft;
			connectivityChanged = true;
			BrassTunnelTileEntity adjacent = getAdjacent(true);
			if (adjacent != null && !d.v) {
				adjacent.updateTunnelConnections();
				adjacent.selectionMode.setValue(selectionMode.getValue());
				AllTriggers.triggerForNearbyPlayers(AllTriggers.CONNECT_TUNNEL, d, e, 4);
			}
		}

		if (connectedRight != nowConnectedRight) {
			connectedRight = nowConnectedRight;
			connectivityChanged = true;
			BrassTunnelTileEntity adjacent = getAdjacent(false);
			if (adjacent != null && !d.v) {
				adjacent.updateTunnelConnections();
				adjacent.selectionMode.setValue(selectionMode.getValue());
			}
		}

		if (filtering != null)
			filtering.updateFilterPresence();
		if (connectivityChanged)
			sendData();
	}

	protected boolean determineIfConnected(boolean leftSide) {
		if (flaps.isEmpty())
			return false;
		BrassTunnelTileEntity adjacentTunnelTE = getAdjacent(leftSide);
		return adjacentTunnelTE != null && !adjacentTunnelTE.flaps.isEmpty();
	}

	@Nullable
	protected BrassTunnelTileEntity getAdjacent(boolean leftSide) {
		if (!n())
			return null;

		PistonHandler blockState = p();
		if (!AllBlocks.BRASS_TUNNEL.has(blockState))
			return null;

		Axis axis = blockState.c(BrassTunnelBlock.HORIZONTAL_AXIS);
		Direction baseDirection = Direction.get(AxisDirection.POSITIVE, axis);
		Direction direction = leftSide ? baseDirection.rotateYCounterclockwise() : baseDirection.rotateYClockwise();
		BlockPos adjacentPos = e.offset(direction);
		PistonHandler adjacentBlockState = d.d_(adjacentPos);

		if (!AllBlocks.BRASS_TUNNEL.has(adjacentBlockState))
			return null;
		if (adjacentBlockState.c(BrassTunnelBlock.HORIZONTAL_AXIS) != axis)
			return null;
		BeehiveBlockEntity adjacentTE = d.c(adjacentPos);
		if (adjacentTE.q())
			return null;
		if (!(adjacentTE instanceof BrassTunnelTileEntity))
			return null;
		return (BrassTunnelTileEntity) adjacentTE;
	}

	@Override
	public void al_() {
		tunnelCapability.invalidate();
		super.al_();
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return tunnelCapability.cast();
		return super.getCapability(capability, side);
	}

	public LazyOptional<IItemHandler> getBeltCapability() {
		if (!beltCapability.isPresent()) {
			BeehiveBlockEntity tileEntity = d.c(e.down());
			if (tileEntity != null)
				beltCapability = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		}
		return beltCapability;
	}

	public enum SelectionMode implements INamedIconOptions {
		SPLIT(AllIcons.I_TUNNEL_SPLIT),
		FORCED_SPLIT(AllIcons.I_TUNNEL_FORCED_SPLIT),
		ROUND_ROBIN(AllIcons.I_TUNNEL_ROUND_ROBIN),
		FORCED_ROUND_ROBIN(AllIcons.I_TUNNEL_FORCED_ROUND_ROBIN),
		PREFER_NEAREST(AllIcons.I_TUNNEL_PREFER_NEAREST),
		RANDOMIZE(AllIcons.I_TUNNEL_RANDOMIZE),
		SYNCHRONIZE(AllIcons.I_TUNNEL_SYNCHRONIZE),

		;

		private final String translationKey;
		private final AllIcons icon;

		SelectionMode(AllIcons icon) {
			this.icon = icon;
			this.translationKey = "tunnel.selection_mode." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}
	}

	public boolean canTakeItems() {
		return stackToDistribute.a() && !syncedOutputActive;
	}

}
