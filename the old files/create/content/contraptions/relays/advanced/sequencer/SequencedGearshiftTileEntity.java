package com.simibubi.create.content.contraptions.relays.advanced.sequencer;

import java.util.Vector;

import com.simibubi.create.content.contraptions.relays.encased.SplitShaftTileEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.util.Constants.NBT;

public class SequencedGearshiftTileEntity extends SplitShaftTileEntity {

	Vector<Instruction> instructions;
	int currentInstruction;
	int currentInstructionDuration;
	int timer;

	public SequencedGearshiftTileEntity(BlockEntityType<? extends SequencedGearshiftTileEntity> type) {
		super(type);
		instructions = Instruction.createDefault();
		currentInstruction = -1;
		currentInstructionDuration = -1;
		timer = 0;
	}

	@Override
	public void tick() {
		super.tick();

		if (isIdle())
			return;
		if (world.isClient)
			return;
		if (timer < currentInstructionDuration) {
			timer++;
			return;
		}
		run(currentInstruction + 1);
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (isIdle())
			return;
		float currentSpeed = Math.abs(speed);
		if (Math.abs(previousSpeed) == currentSpeed)
			return;
		Instruction instruction = getInstruction(currentInstruction);
		if (instruction == null)
			return;
		if (getSpeed() == 0)
			run(-1);

		// Update instruction time with regards to new speed
		float initialProgress = timer / (float) currentInstructionDuration;
		currentInstructionDuration = instruction.getDuration(initialProgress, getTheoreticalSpeed());
		timer = 0;
	}

	public boolean isIdle() {
		return currentInstruction == -1;
	}

	public void onRedstoneUpdate() {
		if (!isIdle())
			return;
		if (!world.isReceivingRedstonePower(pos)) {
			world.setBlockState(pos, getCachedState().with(SequencedGearshiftBlock.STATE, 0), 3);
			return;
		}
		if (getSpeed() == 0)
			return;
		run(0);
	}

	protected void run(int instructionIndex) {
		Instruction instruction = getInstruction(instructionIndex);
		if (instruction == null || instruction.instruction == SequencerInstructions.END) {
			if (getModifier() != 0)
				detachKinetics();
			currentInstruction = -1;
			currentInstructionDuration = -1;
			timer = 0;
			if (!world.isReceivingRedstonePower(pos))
				world.setBlockState(pos, getCachedState().with(SequencedGearshiftBlock.STATE, 0), 3);
			else
				sendData();
			return;
		}

		detachKinetics();
		currentInstructionDuration = instruction.getDuration(0, getTheoreticalSpeed());
		currentInstruction = instructionIndex;
		timer = 0;
		world.setBlockState(pos, getCachedState().with(SequencedGearshiftBlock.STATE, instructionIndex + 1), 3);
	}

	public Instruction getInstruction(int instructionIndex) {
		return instructionIndex >= 0 && instructionIndex < instructions.size() ? instructions.get(instructionIndex)
				: null;
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		compound.putInt("InstructionIndex", currentInstruction);
		compound.putInt("InstructionDuration", currentInstructionDuration);
		compound.putInt("Timer", timer);
		compound.put("Instructions", Instruction.serializeAll(instructions));
		super.write(compound, clientPacket);
	}

	@Override
	protected void fromTag(BlockState state, CompoundTag compound, boolean clientPacket) {
		currentInstruction = compound.getInt("InstructionIndex");
		currentInstructionDuration = compound.getInt("InstructionDuration");
		timer = compound.getInt("Timer");
		instructions = Instruction.deserializeAll(compound.getList("Instructions", NBT.TAG_COMPOUND));
		super.fromTag(state, compound, clientPacket);
	}

	@Override
	public float getRotationSpeedModifier(Direction face) {
		return (!hasSource() || face == getSourceFacing()) ? 1 : getModifier();
	}

	public int getModifier() {
		if (currentInstruction >= instructions.size())
			return 0;
		return isIdle() ? 0 : instructions.get(currentInstruction).getSpeedModifier();
	}

}
