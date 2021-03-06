package com.simibubi.create.content.logistics.block.redstone;

import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.MatrixStacker;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class FilteredDetectorFilterSlot extends ValueBoxTransform {
	Vec3d position = VecHelper.voxelSpace(8f, 15.5f, 11f);

	@Override
	protected Vec3d getLocalOffset(BlockState state) {
		return rotateHorizontally(state, position);
	}

	@Override
	protected void rotate(BlockState state, MatrixStack ms) {
		float yRot = AngleHelper.horizontalAngle(state.get(HorizontalFacingBlock.FACING)) + 180;
		MatrixStacker.of(ms)
			.rotateY(yRot)
			.rotateX(90);
	}

}
