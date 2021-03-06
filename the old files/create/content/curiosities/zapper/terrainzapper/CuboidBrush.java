package com.simibubi.create.content.curiosities.zapper.terrainzapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;

public class CuboidBrush extends Brush {

	public static final int MAX_SIZE = 32;
	private List<BlockPos> positions;

	public CuboidBrush() {
		super(3);
		positions = new ArrayList<>();
	}

	@Override
	public void set(int param0, int param1, int param2) {
		boolean updateShape = this.param0 != param0 || this.param1 != param1 || this.param2 != param2;
		super.set(param0, param1, param2);
		if (updateShape) {
			BlockPos zero = BlockPos.ORIGIN;
			positions = BlockPos
				.stream(zero.add((param0 - 1) / -2, (param1 - 1) / -2, (param2 - 1) / -2),
					zero.add((param0) / 2, (param1) / 2, (param2) / 2))
				.map(BlockPos::new)
				.collect(Collectors.toList());
		}
	}

	@Override
	int getMin(int paramIndex) {
		return 1;
	}

	@Override
	int getMax(int paramIndex) {
		return MAX_SIZE;
	}

	@Override
	public BlockPos getOffset(Vec3d ray, Direction face, PlacementOptions option) {
		if (option == PlacementOptions.Merged)
			return BlockPos.ORIGIN;

		int offset =
			option == PlacementOptions.Attached ? face.getDirection() == AxisDirection.NEGATIVE ? 2 : 1 : 0;
		int x = (param0 + (param0 == 0 ? 0 : offset)) / 2;
		int y = (param1 + (param1 == 0 ? 0 : offset)) / 2;
		int z = (param2 + (param2 == 0 ? 0 : offset)) / 2;

		return BlockPos.ORIGIN.offset(face, face.getAxis()
			.choose(x, y, z) * (option == PlacementOptions.Attached ? 1 : -1));
	}

	@Override
	List<BlockPos> getIncludedPositions() {
		return positions;
	}

}
