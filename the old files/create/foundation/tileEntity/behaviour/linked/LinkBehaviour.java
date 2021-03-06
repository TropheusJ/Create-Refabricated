package com.simibubi.create.foundation.tileEntity.behaviour.linked;

import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.linked.LinkBehaviour.Mode;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Vec3d;

public class LinkBehaviour extends TileEntityBehaviour {

	public static BehaviourType<LinkBehaviour> TYPE = new BehaviourType<>();

	enum Mode {
		TRANSMIT, RECEIVE
	}

	Frequency frequencyFirst;
	Frequency frequencyLast;
	ValueBoxTransform firstSlot;
	ValueBoxTransform secondSlot;
	Vec3d textShift;

	public boolean newPosition;
	private Mode mode;
	private IntSupplier transmission;
	private IntConsumer signalCallback;

	protected LinkBehaviour(SmartTileEntity te, Pair<ValueBoxTransform, ValueBoxTransform> slots) {
		super(te);
		frequencyFirst = Frequency.EMPTY;
		frequencyLast = Frequency.EMPTY;
		firstSlot = slots.getLeft();
		secondSlot = slots.getRight();
		textShift = Vec3d.ZERO;
		newPosition = true;
	}

	public static LinkBehaviour receiver(SmartTileEntity te, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		IntConsumer signalCallback) {
		LinkBehaviour behaviour = new LinkBehaviour(te, slots);
		behaviour.signalCallback = signalCallback;
		behaviour.mode = Mode.RECEIVE;
		return behaviour;
	}

	public static LinkBehaviour transmitter(SmartTileEntity te, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		IntSupplier transmission) {
		LinkBehaviour behaviour = new LinkBehaviour(te, slots);
		behaviour.transmission = transmission;
		behaviour.mode = Mode.TRANSMIT;
		return behaviour;
	}

	public LinkBehaviour moveText(Vec3d shift) {
		textShift = shift;
		return this;
	}

	public void copyItemsFrom(LinkBehaviour behaviour) {
		if (behaviour == null)
			return;
		frequencyFirst = behaviour.frequencyFirst;
		frequencyLast = behaviour.frequencyLast;
	}

	public boolean isListening() {
		return mode == Mode.RECEIVE;
	}

	public int getTransmittedStrength() {
		return mode == Mode.TRANSMIT ? transmission.getAsInt() : 0;
	}

	public void updateReceiver(int networkPower) {
		if (!newPosition)
			return;
		signalCallback.accept(networkPower);
	}

	public void notifySignalChange() {
		Create.redstoneLinkNetworkHandler.updateNetworkOf(this);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (tileEntity.getWorld().isClient)
			return;
		getHandler().addToNetwork(this);
		newPosition = true;
	}

	public Pair<Frequency, Frequency> getNetworkKey() {
		return Pair.of(frequencyFirst, frequencyLast);
	}

	@Override
	public void remove() {
		super.remove();
		if (tileEntity.getWorld().isClient)
			return;
		getHandler().removeFromNetwork(this);
	}

	@Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		nbt.put("FrequencyFirst", frequencyFirst.getStack()
			.toTag(new CompoundTag()));
		nbt.put("FrequencyLast", frequencyLast.getStack()
			.toTag(new CompoundTag()));
		nbt.putLong("LastKnownPosition", tileEntity.getPos()
			.asLong());
	}

	@Override
	public void read(CompoundTag nbt, boolean clientPacket) {
		long positionInTag = tileEntity.getPos()
			.asLong();
		long positionKey = nbt.getLong("LastKnownPosition");
		newPosition = positionInTag != positionKey;

		super.read(nbt, clientPacket);
		frequencyFirst = Frequency.of(ItemStack.fromTag(nbt.getCompound("FrequencyFirst")));
		frequencyLast = Frequency.of(ItemStack.fromTag(nbt.getCompound("FrequencyLast")));
	}

	public void setFrequency(boolean first, ItemStack stack) {
		stack = stack.copy();
		stack.setCount(1);
		ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
		boolean changed =
			!ItemStack.areItemsEqualIgnoreDamage(stack, toCompare) || !ItemStack.areTagsEqual(stack, toCompare);

		if (changed)
			getHandler().removeFromNetwork(this);

		if (first)
			frequencyFirst = Frequency.of(stack);
		else
			frequencyLast = Frequency.of(stack);

		if (!changed)
			return;

		tileEntity.sendData();
		getHandler().addToNetwork(this);
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	private RedstoneLinkNetworkHandler getHandler() {
		return Create.redstoneLinkNetworkHandler;
	}

	public static class SlotPositioning {
		Function<BlockState, Pair<Vec3d, Vec3d>> offsets;
		Function<BlockState, Vec3d> rotation;
		float scale;

		public SlotPositioning(Function<BlockState, Pair<Vec3d, Vec3d>> offsetsForState,
			Function<BlockState, Vec3d> rotationForState) {
			offsets = offsetsForState;
			rotation = rotationForState;
			scale = 1;
		}

		public SlotPositioning scale(float scale) {
			this.scale = scale;
			return this;
		}

	}

	public boolean testHit(Boolean first, Vec3d hit) {
		BlockState state = tileEntity.getCachedState();
		Vec3d localHit = hit.subtract(Vec3d.of(tileEntity.getPos()));
		return (first ? firstSlot : secondSlot).testHit(state, localHit);
	}

}
