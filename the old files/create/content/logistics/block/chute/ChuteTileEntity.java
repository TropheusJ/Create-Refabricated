package com.simibubi.create.content.logistics.block.chute;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.components.fan.AirCurrent;
import com.simibubi.create.content.contraptions.components.fan.EncasedFanBlock;
import com.simibubi.create.content.contraptions.components.fan.EncasedFanTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.particle.AirParticleData;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.gui.widgets.InterpolatedValue;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
/*
 * Commented Code: Chutes create air streams and act similarly to encased fans
 * (Unfinished)
 */
public class ChuteTileEntity extends SmartTileEntity implements IHaveGoggleInformation { // , IAirCurrentSource {

//	public AirCurrent airCurrent;

	float pull;
	float push;

	ItemStack item;
	InterpolatedValue itemPosition;
	ChuteItemHandler itemHandler;
	LazyOptional<IItemHandler> lazyHandler;
	boolean canPickUpItems;

	float bottomPullDistance;
	float beltBelowOffset;
	TransportedItemStackHandlerBehaviour beltBelow;
	boolean updateAirFlow;
	int airCurrentUpdateCooldown;
	int entitySearchCooldown;

	LazyOptional<IItemHandler> capAbove;
	LazyOptional<IItemHandler> capBelow;

	public ChuteTileEntity(BlockEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
		item = ItemStack.EMPTY;
		itemPosition = new InterpolatedValue();
		itemHandler = new ChuteItemHandler(this);
		lazyHandler = LazyOptional.of(() -> itemHandler);
		canPickUpItems = false;
		capAbove = LazyOptional.empty();
		capBelow = LazyOptional.empty();
		bottomPullDistance = 0;
//		airCurrent = new AirCurrent(this);
		updateAirFlow = true;
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen((d) -> canDirectlyInsertCached()));
	}

	// Cached per-tick, useful when a lot of items are waiting on top of it
	public boolean canDirectlyInsertCached() {
		return canPickUpItems;
	}

	private boolean canDirectlyInsert() {
		BlockState blockState = getCachedState();
		BlockState blockStateAbove = world.getBlockState(pos.up());
		if (!AbstractChuteBlock.isChute(blockState))
			return false;
		if (AbstractChuteBlock.getChuteFacing(blockStateAbove) == Direction.DOWN)
			return false;
		if (getItemMotion() > 0 && getInputChutes().isEmpty())
			return false;
		return AbstractChuteBlock.isOpenChute(blockState);
	}

	@Override
	public void initialize() {
		super.initialize();
		onAdded();
	}

	@Override
	public Box getRenderBoundingBox() {
		return new Box(pos).stretch(0, -3, 0);
	}

	@Override
	public void tick() {
		super.tick();

		if (!world.isClient)
			canPickUpItems = canDirectlyInsert();

		float itemMotion = getItemMotion();
		if (itemMotion != 0 && world != null && world.isClient)
			spawnParticles(itemMotion);
		tickAirStreams(itemMotion);

		if (item.isEmpty()) {
			if (itemMotion < 0)
				handleInputFromAbove();
			if (itemMotion > 0)
				handleInputFromBelow();
			return;
		}

		float nextOffset = itemPosition.value + itemMotion;

		if (itemMotion < 0) {
			if (nextOffset < .5f) {
				if (!handleDownwardOutput(true))
					nextOffset = .5f;
				else if (nextOffset < 0) {
					handleDownwardOutput(world.isClient);
					return;
				}
			}
		}

		if (itemMotion > 0) {
			if (nextOffset > .5f) {
				if (!handleUpwardOutput(true))
					nextOffset = .5f;
				else if (nextOffset > 1) {
					handleUpwardOutput(world.isClient);
					return;
				}
			}
		}

		itemPosition.set(nextOffset);
	}

	private void updateAirFlow(float itemSpeed) {
		updateAirFlow = false;
//		airCurrent.rebuild();
		if (itemSpeed > 0 && world != null && !world.isClient) {
			float speed = pull - push;
			beltBelow = null;

			float maxPullDistance;
			if (speed >= 128)
				maxPullDistance = 3;
			else if (speed >= 64)
				maxPullDistance = 2;
			else if (speed >= 32)
				maxPullDistance = 1;
			else
				maxPullDistance = MathHelper.lerp(speed / 32, 0, 1);

			if (AbstractChuteBlock.isChute(world.getBlockState(pos.down())))
				maxPullDistance = 0;
			float flowLimit = maxPullDistance;
			if (flowLimit > 0)
				flowLimit = AirCurrent.getFlowLimit(world, pos, maxPullDistance, Direction.DOWN);

			for (int i = 1; i <= flowLimit + 1; i++) {
				TransportedItemStackHandlerBehaviour behaviour =
					TileEntityBehaviour.get(world, pos.down(i), TransportedItemStackHandlerBehaviour.TYPE);
				if (behaviour == null)
					continue;
				beltBelow = behaviour;
				beltBelowOffset = i - 1;
				break;
			}
			this.bottomPullDistance = flowLimit;
		}
		sendData();
	}

	private void findEntities(float itemSpeed) {
//		if (getSpeed() != 0)
//			airCurrent.findEntities();
		if (bottomPullDistance <= 0 && !getItem().isEmpty() || itemSpeed <= 0 || world == null || world.isClient)
			return;
		if (!canCollectItemsFromBelow())
			return;
		Vec3d center = VecHelper.getCenterOf(pos);
		Box searchArea =
			new Box(center.add(0, -bottomPullDistance - 0.5, 0), center.add(0, -0.5, 0)).expand(.45f);
		for (ItemEntity itemEntity : world.getNonSpectatingEntities(ItemEntity.class, searchArea)) {
			if (!itemEntity.isAlive())
				continue;
			ItemStack entityItem = itemEntity.getStack();
			if (!canAcceptItem(entityItem))
				continue;
			setItem(entityItem.copy(), (float) (itemEntity.getBoundingBox()
				.getCenter().y - pos.getY()));
			itemEntity.remove();
			AllTriggers.triggerForNearbyPlayers(AllTriggers.UPWARD_CHUTE, world, pos, 5);
			break;
		}
	}

	private void extractFromBelt(float itemSpeed) {
		if (itemSpeed <= 0 || world == null || world.isClient)
			return;
		if (getItem().isEmpty() && beltBelow != null) {
			beltBelow.handleCenteredProcessingOnAllItems(.5f, ts -> {
				if (canAcceptItem(ts.stack)) {
					setItem(ts.stack.copy(), -beltBelowOffset);
					return TransportedResult.removeItem();
				}
				return TransportedResult.doNothing();
			});
		}
	}

	private void tickAirStreams(float itemSpeed) {
		if (!world.isClient && airCurrentUpdateCooldown-- <= 0) {
			airCurrentUpdateCooldown = AllConfigs.SERVER.kinetics.fanBlockCheckRate.get();
			updateAirFlow = true;
		}

		if (updateAirFlow) {
			updateAirFlow(itemSpeed);
		}

		if (entitySearchCooldown-- <= 0 && item.isEmpty()) {
			entitySearchCooldown = 5;
			findEntities(itemSpeed);
		}

		extractFromBelt(itemSpeed);
//		if (getSpeed() != 0)
//			airCurrent.tick();
	}

	public void blockBelowChanged() {
		updateAirFlow = true;
	}

	private void spawnParticles(float itemMotion) {
		// todo: reduce the amount of particles
		if (world == null)
			return;
		BlockState blockState = getCachedState();
		boolean up = itemMotion > 0;
		float absMotion = up ? itemMotion : -itemMotion;
		if (blockState == null || !AbstractChuteBlock.isChute(blockState))
			return;
		if (push == 0 && pull == 0)
			return;

		if (up && AbstractChuteBlock.isOpenChute(blockState) && BlockHelper.noCollisionInSpace(world, pos.up()))
			spawnAirFlow(1, 2, absMotion, .5f);

		if (AbstractChuteBlock.getChuteFacing(blockState) != Direction.DOWN)
			return;

		if (AbstractChuteBlock.isTransparentChute(blockState))
			spawnAirFlow(up ? 0 : 1, up ? 1 : 0, absMotion, 1);

		if (!up && BlockHelper.noCollisionInSpace(world, pos.down()))
			spawnAirFlow(0, -1, absMotion, .5f);

		if (up && canCollectItemsFromBelow() && bottomPullDistance > 0) {
			spawnAirFlow(-bottomPullDistance, 0, absMotion, 2);
			spawnAirFlow(-bottomPullDistance, 0, absMotion, 2);
		}
	}

	private void spawnAirFlow(float verticalStart, float verticalEnd, float motion, float drag) {
		if (world == null)
			return;
		AirParticleData airParticleData = new AirParticleData(drag, motion);
		Vec3d origin = Vec3d.of(pos);
		float xOff = Create.random.nextFloat() * .5f + .25f;
		float zOff = Create.random.nextFloat() * .5f + .25f;
		Vec3d v = origin.add(xOff, verticalStart, zOff);
		Vec3d d = origin.add(xOff, verticalEnd, zOff)
			.subtract(v);
		if (Create.random.nextFloat() < 2 * motion)
			world.addImportantParticle(airParticleData, v.x, v.y, v.z, d.x, d.y, d.z);
	}

	private void handleInputFromAbove() {
		if (!capAbove.isPresent())
			capAbove = grabCapability(Direction.UP);
		if (capAbove.isPresent()) {
			int count = getExtractionAmount();
			if (count == 0)
				item =
					ItemHelper.extract(capAbove.orElse(null), this::canAcceptItem, ExtractionCountMode.UPTO, 16, false);
			else
				item = ItemHelper.extract(capAbove.orElse(null), this::canAcceptItem, ExtractionCountMode.EXACTLY,
					count, false);
		}
	}

	private void handleInputFromBelow() {
		if (!capBelow.isPresent())
			capBelow = grabCapability(Direction.DOWN);
		if (capBelow.isPresent()) {
			int count = getExtractionAmount();
			if (count == 0)
				item =
					ItemHelper.extract(capBelow.orElse(null), this::canAcceptItem, ExtractionCountMode.UPTO, 16, false);
			else
				item = ItemHelper.extract(capBelow.orElse(null), this::canAcceptItem, ExtractionCountMode.EXACTLY,
					count, false);
		}
	}

	private boolean handleDownwardOutput(boolean simulate) {
		BlockState blockState = getCachedState();
		ChuteTileEntity targetChute = getTargetChute(blockState);
		Direction direction = AbstractChuteBlock.getChuteFacing(blockState);

		if (world == null)
			return false;
		if (!capBelow.isPresent())
			capBelow = grabCapability(Direction.DOWN);
		if (capBelow.isPresent()) {
			ItemStack remainder = ItemHandlerHelper.insertItemStacked(capBelow.orElse(null), item, simulate);
			if (!simulate)
				setItem(remainder);
			if (remainder.isEmpty())
				return true;
			if (direction == Direction.DOWN)
				return false;
		}

		if (targetChute != null) {
			boolean canInsert = targetChute.canAcceptItem(item);
			if (!simulate && canInsert) {
				targetChute.setItem(item, direction == Direction.DOWN ? 1 : .51f);
				setItem(ItemStack.EMPTY);
			}
			return canInsert;
		}

		// Diagonal chutes cannot drop items
		if (direction.getAxis()
			.isHorizontal())
			return false;

		if (Block.hasTopRim(world, pos.down()))
			return false;

		if (!simulate) {
			Vec3d dropVec = VecHelper.getCenterOf(pos)
				.add(0, -12 / 16f, 0);
			ItemEntity dropped = new ItemEntity(world, dropVec.x, dropVec.y, dropVec.z, item.copy());
			dropped.setToDefaultPickupDelay();
			dropped.setVelocity(0, -.25f, 0);
			world.spawnEntity(dropped);
			setItem(ItemStack.EMPTY);
		}

		return true;
	}

	private boolean handleUpwardOutput(boolean simulate) {
		BlockState stateAbove = world.getBlockState(pos.up());

		if (world == null)
			return false;

		if (AbstractChuteBlock.isOpenChute(getCachedState())) {
			if (!capAbove.isPresent())
				capAbove = grabCapability(Direction.UP);
			if (capAbove.isPresent()) {
				ItemStack remainder = ItemHandlerHelper.insertItemStacked(capAbove.orElse(null), item, simulate);
				if (!simulate)
					setItem(ItemStack.EMPTY);
				return remainder.isEmpty();
			}
		}

		ChuteTileEntity bestOutput = null;
		List<ChuteTileEntity> inputChutes = getInputChutes();
		for (ChuteTileEntity targetChute : inputChutes) {
			if (!targetChute.canAcceptItem(item))
				continue;
			float itemMotion = targetChute.getItemMotion();
			if (itemMotion < 0)
				continue;
			if (bestOutput == null || bestOutput.getItemMotion() < itemMotion) {
				bestOutput = targetChute;
			}
		}

		if (bestOutput != null) {
			if (!simulate) {
				bestOutput.setItem(item, 0);
				setItem(ItemStack.EMPTY);
			}
			return true;
		}

		if (BlockHelper.hasBlockSolidSide(stateAbove, world, pos.up(), Direction.DOWN))
			return false;
		if (!inputChutes.isEmpty())
			return false;

		if (!simulate) {
			Vec3d dropVec = VecHelper.getCenterOf(pos)
				.add(0, 8 / 16f, 0);
			ItemEntity dropped = new ItemEntity(world, dropVec.x, dropVec.y, dropVec.z, item.copy());
			dropped.setToDefaultPickupDelay();
			dropped.setVelocity(0, getItemMotion() * 2, 0);
			world.spawnEntity(dropped);
			setItem(ItemStack.EMPTY);
		}
		return true;
	}

	protected boolean canAcceptItem(ItemStack stack) {
		return item.isEmpty();
	}

	protected int getExtractionAmount() {
		return 0;
	}

	protected boolean canCollectItemsFromBelow() {
		return true;
	}

	private LazyOptional<IItemHandler> grabCapability(Direction side) {
		BlockPos pos = this.pos.offset(side);
		if (world == null)
			return LazyOptional.empty();
		BlockEntity te = world.getBlockEntity(pos);
		if (te == null)
			return LazyOptional.empty();
		if (te instanceof ChuteTileEntity) {
			if (side != Direction.DOWN || !(te instanceof SmartChuteTileEntity) || getItemMotion() > 0)
				return LazyOptional.empty();
		}
		return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
	}

	public void setItem(ItemStack stack) {
		setItem(stack, getItemMotion() < 0 ? 1 : 0);
	}

	public void setItem(ItemStack stack, float insertionPos) {
		item = stack;
		itemPosition.lastValue = itemPosition.value = insertionPos;
		markDirty();
		sendData();
	}

	@Override
	public void markRemoved() {
		super.markRemoved();
		if (lazyHandler != null)
			lazyHandler.invalidate();
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		compound.put("Item", item.serializeNBT());
		compound.putFloat("ItemPosition", itemPosition.value);
		compound.putFloat("Pull", pull);
		compound.putFloat("Push", push);
		compound.putFloat("BottomAirFlowDistance", bottomPullDistance);
		super.write(compound, clientPacket);
	}

	@Override
	protected void fromTag(BlockState state, CompoundTag compound, boolean clientPacket) {
		ItemStack previousItem = item;
		item = ItemStack.fromTag(compound.getCompound("Item"));
		itemPosition.lastValue = itemPosition.value = compound.getFloat("ItemPosition");
		pull = compound.getFloat("Pull");
		push = compound.getFloat("Push");
		bottomPullDistance = compound.getFloat("BottomAirFlowDistance");
		super.fromTag(state, compound, clientPacket);
//		if (clientPacket)
//			airCurrent.rebuild();

		if (hasWorld() && world != null && world.isClient && !previousItem.equals(item, false) && !item.isEmpty()) {
			if (world.random.nextInt(3) != 0)
				return;
			Vec3d p = VecHelper.getCenterOf(pos);
			p = VecHelper.offsetRandomly(p, world.random, .5f);
			Vec3d m = Vec3d.ZERO;
			world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, item), p.x, p.y, p.z, m.x, m.y, m.z);
		}
	}

	public float getItemMotion() {
		// Chutes per second
		final float fanSpeedModifier = 1 / 64f;
		final float maxItemSpeed = 20f;
		final float gravity = 4f;

		float motion = (push + pull) * fanSpeedModifier;
		return (MathHelper.clamp(motion, -maxItemSpeed, maxItemSpeed) + (motion <= 0 ? -gravity : 0)) / 20f;
	}

	public void onRemoved(BlockState chuteState) {
		ChuteTileEntity targetChute = getTargetChute(chuteState);
		List<ChuteTileEntity> inputChutes = getInputChutes();
		if (!item.isEmpty() && world != null)
			ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), item);
		markRemoved();
		if (targetChute != null) {
			targetChute.updatePull();
			targetChute.propagatePush();
		}
		inputChutes.forEach(c -> c.updatePush(inputChutes.size()));
	}

	public void onAdded() {
		resetBlock();
		updatePull();
		ChuteTileEntity targetChute = getTargetChute(getCachedState());
		if (targetChute != null)
			targetChute.propagatePush();
		else
			updatePush(1);
	}

	public void updatePull() {
		float totalPull = calculatePull();
		if (pull == totalPull)
			return;
		pull = totalPull;
		updateAirFlow = true;
		sendData();
		ChuteTileEntity targetChute = getTargetChute(getCachedState());
		if (targetChute != null)
			targetChute.updatePull();
	}

	public void updatePush(int branchCount) {
		float totalPush = calculatePush(branchCount);
		if (push == totalPush)
			return;
		updateAirFlow = true;
		push = totalPush;
		sendData();
		propagatePush();
	}

	public void propagatePush() {
		List<ChuteTileEntity> inputs = getInputChutes();
		inputs.forEach(c -> c.updatePush(inputs.size()));
	}

	protected float calculatePull() {
		BlockState blockStateAbove = world.getBlockState(pos.up());
		if (AllBlocks.ENCASED_FAN.has(blockStateAbove)
			&& blockStateAbove.get(EncasedFanBlock.FACING) == Direction.DOWN) {
			BlockEntity te = world.getBlockEntity(pos.up());
			if (te instanceof EncasedFanTileEntity && !te.isRemoved()) {
				EncasedFanTileEntity fan = (EncasedFanTileEntity) te;
				return fan.getSpeed();
			}
		}

		float totalPull = 0;
		for (Direction d : Iterate.directions) {
			ChuteTileEntity inputChute = getInputChute(d);
			if (inputChute == null)
				continue;
			totalPull += inputChute.pull;
		}
		return totalPull;
	}

	protected float calculatePush(int branchCount) {
		if (world == null)
			return 0;
		BlockState blockStateBelow = world.getBlockState(pos.down());
		if (AllBlocks.ENCASED_FAN.has(blockStateBelow) && blockStateBelow.get(EncasedFanBlock.FACING) == Direction.UP) {
			BlockEntity te = world.getBlockEntity(pos.down());
			if (te instanceof EncasedFanTileEntity && !te.isRemoved()) {
				EncasedFanTileEntity fan = (EncasedFanTileEntity) te;
				return fan.getSpeed();
			}
		}

		ChuteTileEntity targetChute = getTargetChute(getCachedState());
		if (targetChute == null)
			return 0;
		return targetChute.push / branchCount;
	}

	@Nullable
	private ChuteTileEntity getTargetChute(BlockState state) {
		if (world == null)
			return null;
		Direction targetDirection = AbstractChuteBlock.getChuteFacing(state);
		if (targetDirection == null)
			return null;
		BlockPos chutePos = pos.down();
		if (targetDirection.getAxis()
			.isHorizontal())
			chutePos = chutePos.offset(targetDirection.getOpposite());
		BlockState chuteState = world.getBlockState(chutePos);
		if (!AbstractChuteBlock.isChute(chuteState))
			return null;
		BlockEntity te = world.getBlockEntity(chutePos);
		if (te instanceof ChuteTileEntity)
			return (ChuteTileEntity) te;
		return null;
	}

	private List<ChuteTileEntity> getInputChutes() {
		List<ChuteTileEntity> inputs = new LinkedList<>();
		for (Direction d : Iterate.directions) {
			ChuteTileEntity inputChute = getInputChute(d);
			if (inputChute == null)
				continue;
			inputs.add(inputChute);
		}
		return inputs;
	}

	@Nullable
	private ChuteTileEntity getInputChute(Direction direction) {
		if (world == null || direction == Direction.DOWN)
			return null;
		direction = direction.getOpposite();
		BlockPos chutePos = pos.up();
		if (direction.getAxis()
			.isHorizontal())
			chutePos = chutePos.offset(direction);
		BlockState chuteState = world.getBlockState(chutePos);
		Direction chuteFacing = AbstractChuteBlock.getChuteFacing(chuteState);
		if (chuteFacing != direction)
			return null;
		BlockEntity te = world.getBlockEntity(chutePos);
		if (te instanceof ChuteTileEntity && !te.isRemoved())
			return (ChuteTileEntity) te;
		return null;
	}

	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		boolean downward = getItemMotion() < 0;
		tooltip.add(componentSpacing.copy()
			.append(Lang.translate("tooltip.chute.header")));
		if (pull == 0 && push == 0)
			tooltip.add(componentSpacing.copy()
				.append(Lang.translate("tooltip.chute.no_fans_attached"))
				.formatted(Formatting.GRAY));
		if (pull != 0)
			tooltip.add(componentSpacing.copy()
				.append(Lang.translate("tooltip.chute.fans_" + (pull > 0 ? "pull_up" : "push_down"))
					.formatted(Formatting.GRAY)));
		if (push != 0)
			tooltip.add(componentSpacing.copy()
				.append(Lang.translate("tooltip.chute.fans_" + (push > 0 ? "push_up" : "pull_down"))
					.formatted(Formatting.GRAY)));
		tooltip.add(componentSpacing.copy()
			.append("-> ")
			.append(Lang.translate("tooltip.chute.items_move_" + (downward ? "down" : "up"))
				.formatted(Formatting.YELLOW)));
		if (!item.isEmpty()) {
			tooltip.add(componentSpacing.copy()
				.append(Lang.translate("tooltip.chute.contains", new TranslatableText(item.getItem()
					.getTranslationKey(item)).getString(), item.getCount()))
				.formatted(Formatting.GREEN));
		}
		return true;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return lazyHandler.cast();
		return super.getCapability(cap, side);
	}

	public ItemStack getItem() {
		return item;
	}

//	@Override
//	@Nullable
//	public AirCurrent getAirCurrent() {
//		return airCurrent;
//	}
//
//	@Nullable
//	@Override
//	public World getAirCurrentWorld() {
//		return world;
//	}
//
//	@Override
//	public BlockPos getAirCurrentPos() {
//		return pos;
//	}
//
//	@Override
//	public float getSpeed() {
//		if (getBlockState().get(ChuteBlock.SHAPE) == Shape.NORMAL && getBlockState().get(ChuteBlock.FACING) != Direction.DOWN)
//			return 0;
//		return pull + push;
//	}
//
//	@Override
//	@Nullable
//	public Direction getAirFlowDirection() {
//		float speed = getSpeed();
//		if (speed == 0)
//			return null;
//		return speed > 0 ? Direction.UP : Direction.DOWN;
//	}
//
//	@Override
//	public boolean isSourceRemoved() {
//		return removed;
//	}
//
//	@Override
//	public Direction getAirflowOriginSide() {
//		return world != null && !(world.getTileEntity(pos.down()) instanceof IAirCurrentSource)
//			&& getBlockState().get(ChuteBlock.FACING) == Direction.DOWN ? Direction.DOWN : Direction.UP;
//	}
}
