package com.simibubi.create.content.curiosities.symmetry.mirror;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.MatrixStacker;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.client.render.BufferVertexConsumer;
import net.minecraft.client.util.SmoothUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;

public class CrossPlaneMirror extends SymmetryMirror {

	public static enum Align implements SmoothUtil {
		Y("y"), D("d");

		private final String name;

		private Align(String name) {
			this.name = name;
		}

		@Override
		public String a() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public CrossPlaneMirror(EntityHitResult pos) {
		super(pos);
		orientation = Align.Y;
	}

	@Override
	protected void setOrientation() {
		if (orientationIndex < 0)
			orientationIndex += Align.values().length;
		if (orientationIndex >= Align.values().length)
			orientationIndex -= Align.values().length;
		orientation = Align.values()[orientationIndex];
	}

	@Override
	public void setOrientation(int index) {
		this.orientation = Align.values()[index];
		orientationIndex = index;
	}

	@Override
	public Map<BlockPos, PistonHandler> process(BlockPos position, PistonHandler block) {
		Map<BlockPos, PistonHandler> result = new HashMap<>();

		switch ((Align) orientation) {
		case D:
			result.put(flipD1(position), flipD1(block));
			result.put(flipD2(position), flipD2(block));
			result.put(flipD1(flipD2(position)), flipD1(flipD2(block)));
			break;
		case Y:
			result.put(flipX(position), flipX(block));
			result.put(flipZ(position), flipZ(block));
			result.put(flipX(flipZ(position)), flipX(flipZ(block)));
			break;
		default:
			break;
		}

		return result;
	}

	@Override
	public String typeName() {
		return CROSS_PLANE;
	}

	@Override
	public AllBlockPartials getModel() {
		return AllBlockPartials.SYMMETRY_CROSSPLANE;
	}

	@Override
	public void applyModelTransform(BufferVertexConsumer ms) {
		super.applyModelTransform(ms);
		MatrixStacker.of(ms)
			.centre()
			.rotateY(((Align) orientation) == Align.Y ? 0 : 45)
			.unCentre();
	}

	@Override
	public List<Text> getAlignToolTips() {
		return ImmutableList.of(Lang.translate("orientation.orthogonal"), Lang.translate("orientation.diagonal"));
	}

}
