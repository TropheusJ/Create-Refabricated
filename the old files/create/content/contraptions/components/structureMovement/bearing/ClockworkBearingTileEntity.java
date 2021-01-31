package com.simibubi.kinetic_api.content.contraptions.components.structureMovement.bearing;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import afj;
import com.simibubi.kinetic_api.content.contraptions.base.KineticTileEntity;
import com.simibubi.kinetic_api.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.kinetic_api.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.kinetic_api.content.contraptions.components.structureMovement.bearing.ClockworkBearingTileEntity.ClockHands;
import com.simibubi.kinetic_api.content.contraptions.components.structureMovement.bearing.ClockworkContraption.HandType;
import com.simibubi.kinetic_api.foundation.advancement.AllTriggers;
import com.simibubi.kinetic_api.foundation.gui.AllIcons;
import com.simibubi.kinetic_api.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.kinetic_api.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.kinetic_api.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.kinetic_api.foundation.utility.AngleHelper;
import com.simibubi.kinetic_api.foundation.utility.Lang;
import com.simibubi.kinetic_api.foundation.utility.ServerSpeedProvider;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class ClockworkBearingTileEntity extends KineticTileEntity implements IBearingTileEntity {

	protected ControlledContraptionEntity hourHand;
	protected ControlledContraptionEntity minuteHand;
	protected float hourAngle;
	protected float minuteAngle;
	protected float clientHourAngleDiff;
	protected float clientMinuteAngleDiff;

	protected boolean running;
	protected boolean assembleNextTick;

	protected ScrollOptionBehaviour<ClockHands> operationMode;

	public ClockworkBearingTileEntity(BellBlockEntity<? extends ClockworkBearingTileEntity> type) {
		super(type);
		setLazyTickRate(3);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		operationMode = new ScrollOptionBehaviour<>(ClockHands.class,
			Lang.translate("contraptions.clockwork.clock_hands"), this, getMovementModeSlot());
		operationMode.requiresWrench();
		behaviours.add(operationMode);
	}

	@Override
	public boolean isWoodenTop() {
		return false;
	}

	@Override
	public void aj_() {
		super.aj_();

		if (d.v) {
			clientMinuteAngleDiff /= 2;
			clientHourAngleDiff /= 2;
		}

		if (!d.v && assembleNextTick) {
			assembleNextTick = false;
			if (running) {
				boolean canDisassemble = true;
				if (speed == 0 && (canDisassemble || hourHand == null || hourHand.getContraption()
					.getBlocks()
					.isEmpty())) {
					if (hourHand != null)
						hourHand.getContraption()
							.stop(d);
					if (minuteHand != null)
						minuteHand.getContraption()
							.stop(d);
					disassemble();
				}
				return;
			} else
				assemble();
			return;
		}

		if (!running)
			return;

		if (!(hourHand != null && hourHand.isStalled())) {
			float newAngle = hourAngle + getHourArmSpeed();
			hourAngle = (float) (newAngle % 360);
		}

		if (!(minuteHand != null && minuteHand.isStalled())) {
			float newAngle = minuteAngle + getMinuteArmSpeed();
			minuteAngle = (float) (newAngle % 360);
		}

		applyRotations();
	}

	protected void applyRotations() {
		PistonHandler blockState = p();
		Axis axis = Axis.X;
		
		if (blockState.b(BambooLeaves.M))
			axis = blockState.c(BambooLeaves.M)
				.getAxis();
		
		if (hourHand != null) {
			hourHand.setAngle(hourAngle);
			hourHand.setRotationAxis(axis);
		}
		if (minuteHand != null) {
			minuteHand.setAngle(minuteAngle);
			minuteHand.setRotationAxis(axis);
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (hourHand != null && !d.v)
			sendData();
	}

	public float getHourArmSpeed() {
		float speed = getAngularSpeed() / 2f;

		if (speed != 0) {
			ClockHands mode = ClockHands.values()[operationMode.getValue()];
			float hourTarget = mode == ClockHands.HOUR_FIRST ? getHourTarget(false)
				: mode == ClockHands.MINUTE_FIRST ? getMinuteTarget() : getHourTarget(true);
			float shortestAngleDiff = AngleHelper.getShortestAngleDiff(hourAngle, hourTarget);
			if (shortestAngleDiff < 0) {
				speed = Math.max(speed, shortestAngleDiff);
			} else {
				speed = Math.min(-speed, shortestAngleDiff);
			}
		}

		return speed + clientHourAngleDiff / 3f;
	}

	public float getMinuteArmSpeed() {
		float speed = getAngularSpeed();

		if (speed != 0) {
			ClockHands mode = ClockHands.values()[operationMode.getValue()];
			float minuteTarget = mode == ClockHands.MINUTE_FIRST ? getHourTarget(false) : getMinuteTarget();
			float shortestAngleDiff = AngleHelper.getShortestAngleDiff(minuteAngle, minuteTarget);
			if (shortestAngleDiff < 0) {
				speed = Math.max(speed, shortestAngleDiff);
			} else {
				speed = Math.min(-speed, shortestAngleDiff);
			}
		}

		return speed + clientMinuteAngleDiff / 3f;
	}

	protected float getHourTarget(boolean cycle24) {
		int dayTime = (int) (d.T() % 24000);
		int hours = (dayTime / 1000 + 6) % 24;
		int offset = p().c(ClockworkBearingBlock.FACING)
			.getDirection()
			.offset();
		float hourTarget = (float) (offset * -360 / (cycle24 ? 24f : 12f) * (hours % (cycle24 ? 24 : 12)));
		return hourTarget;
	}

	protected float getMinuteTarget() {
		int dayTime = (int) (d.T() % 24000);
		int minutes = (dayTime % 1000) * 60 / 1000;
		int offset = p().c(ClockworkBearingBlock.FACING)
			.getDirection()
			.offset();
		float minuteTarget = (float) (offset * -360 / 60f * (minutes));
		return minuteTarget;
	}

	public float getAngularSpeed() {
		float speed = -Math.abs(getSpeed() * 3 / 10f);
		if (d.v)
			speed *= ServerSpeedProvider.get();
		return speed;
	}

	public void assemble() {
		if (!(d.d_(e)
			.b() instanceof ClockworkBearingBlock))
			return;

		Direction direction = p().c(BambooLeaves.M);

		// Collect Construct
		Pair<ClockworkContraption, ClockworkContraption> contraption =
			ClockworkContraption.assembleClockworkAt(d, e, direction);
		if (contraption == null)
			return;
		if (contraption.getLeft() == null)
			return;
		if (contraption.getLeft()
			.getBlocks()
			.isEmpty())
			return;
		BlockPos anchor = e.offset(direction);

		contraption.getLeft()
			.removeBlocksFromWorld(d, BlockPos.ORIGIN);
		hourHand = ControlledContraptionEntity.create(d, this, contraption.getLeft());
		hourHand.d(anchor.getX(), anchor.getY(), anchor.getZ());
		hourHand.setRotationAxis(direction.getAxis());
		d.c(hourHand);

		AllTriggers.triggerForNearbyPlayers(AllTriggers.CLOCKWORK_BEARING, d, e, 5);

		if (contraption.getRight() != null) {
			anchor = e.offset(direction, contraption.getRight().offset + 1);
			contraption.getRight()
				.removeBlocksFromWorld(d, BlockPos.ORIGIN);
			minuteHand = ControlledContraptionEntity.create(d, this, contraption.getRight());
			minuteHand.d(anchor.getX(), anchor.getY(), anchor.getZ());
			minuteHand.setRotationAxis(direction.getAxis());
			d.c(minuteHand);
		}

		// Run
		running = true;
		hourAngle = 0;
		minuteAngle = 0;
		sendData();
	}

	public void disassemble() {
		if (!running && hourHand == null && minuteHand == null)
			return;

		hourAngle = 0;
		minuteAngle = 0;
		applyRotations();

		if (hourHand != null) {
			hourHand.disassemble();
		}
		if (minuteHand != null)
			minuteHand.disassemble();

		hourHand = null;
		minuteHand = null;
		running = false;
		sendData();
	}

	@Override
	public void attach(ControlledContraptionEntity contraption) {
		if (!(contraption.getContraption() instanceof ClockworkContraption))
			return;

		ClockworkContraption cc = (ClockworkContraption) contraption.getContraption();
		X_();
		Direction facing = p().c(BambooLeaves.M);
		BlockPos anchor = e.offset(facing, cc.offset + 1);
		if (cc.handType == HandType.HOUR) {
			this.hourHand = contraption;
			hourHand.d(anchor.getX(), anchor.getY(), anchor.getZ());
		} else {
			this.minuteHand = contraption;
			minuteHand.d(anchor.getX(), anchor.getY(), anchor.getZ());
		}
		if (!d.v) {
			this.running = true;
			sendData();
		}
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		compound.putBoolean("Running", running);
		compound.putFloat("HourAngle", hourAngle);
		compound.putFloat("MinuteAngle", minuteAngle);
		super.write(compound, clientPacket);
	}

	@Override
	protected void fromTag(PistonHandler state, CompoundTag compound, boolean clientPacket) {
		float hourAngleBefore = hourAngle;
		float minuteAngleBefore = minuteAngle;

		running = compound.getBoolean("Running");
		hourAngle = compound.getFloat("HourAngle");
		minuteAngle = compound.getFloat("MinuteAngle");
		super.fromTag(state, compound, clientPacket);

		if (!clientPacket)
			return;

		if (running) {
			clientHourAngleDiff = AngleHelper.getShortestAngleDiff(hourAngleBefore, hourAngle);
			clientMinuteAngleDiff = AngleHelper.getShortestAngleDiff(minuteAngleBefore, minuteAngle);
			hourAngle = hourAngleBefore;
			minuteAngle = minuteAngleBefore;
		} else {
			hourHand = null;
			minuteHand = null;
		}
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		assembleNextTick = true;
	}

	@Override
	public boolean isValid() {
		return !q();
	}

	@Override
	public float getInterpolatedAngle(float partialTicks) {
		if (hourHand == null || hourHand.isStalled())
			partialTicks = 0;
		return afj.g(partialTicks, hourAngle, hourAngle + getHourArmSpeed());
	}

	@Override
	public void onStall() {
		if (!d.v)
			sendData();
	}

	@Override
	public void al_() {
		if (!d.v)
			disassemble();
		super.al_();
	}

	@Override
	public void collided() {}

	@Override
	public boolean isAttachedTo(AbstractContraptionEntity contraption) {
		if (!(contraption.getContraption() instanceof ClockworkContraption))
			return false;
		ClockworkContraption cc = (ClockworkContraption) contraption.getContraption();
		if (cc.handType == HandType.HOUR)
			return this.hourHand == contraption;
		else
			return this.minuteHand == contraption;
	}

	public boolean isRunning() {
		return running;
	}

	static enum ClockHands implements INamedIconOptions {

		HOUR_FIRST(AllIcons.I_HOUR_HAND_FIRST),
		MINUTE_FIRST(AllIcons.I_MINUTE_HAND_FIRST),
		HOUR_FIRST_24(AllIcons.I_HOUR_HAND_FIRST_24),

		;

		private String translationKey;
		private AllIcons icon;

		private ClockHands(AllIcons icon) {
			this.icon = icon;
			translationKey = "contraptions.clockwork." + Lang.asId(name());
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

	@Override
	public BlockPos getBlockPosition() {
		return e;
	}

}