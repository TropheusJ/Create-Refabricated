package com.simibubi.create.content.schematics.client;

import static java.lang.Math.abs;

import com.simibubi.create.foundation.gui.widgets.InterpolatedChasingAngle;
import com.simibubi.create.foundation.gui.widgets.InterpolatedChasingValue;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.MatrixStacker;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;

public class SchematicTransformation {

	private InterpolatedChasingValue x, y, z, scaleFrontBack, scaleLeftRight;
	private InterpolatedChasingAngle rotation;
	private double xOrigin;
	private double zOrigin;

	public SchematicTransformation() {
		x = new InterpolatedChasingValue();
		y = new InterpolatedChasingValue();
		z = new InterpolatedChasingValue();
		scaleFrontBack = new InterpolatedChasingValue();
		scaleLeftRight = new InterpolatedChasingValue();
		rotation = new InterpolatedChasingAngle();
	}

	public void init(BlockPos anchor, StructurePlacementData settings, Box bounds) {
		int leftRight = settings.getMirror() == BlockMirror.LEFT_RIGHT ? -1 : 1;
		int frontBack = settings.getMirror() == BlockMirror.FRONT_BACK ? -1 : 1;
		getScaleFB().start(frontBack);
		getScaleLR().start(leftRight);
		xOrigin = bounds.getXLength() / 2f;
		zOrigin = bounds.getZLength() / 2f;

		int r = -(settings.getRotation()
			.ordinal() * 90);
		rotation.start(r);

		Vec3d vec = fromAnchor(anchor);
		x.start((float) vec.x);
		y.start((float) vec.y);
		z.start((float) vec.z);
	}

	public void applyGLTransformations(MatrixStack ms) {
		float pt = AnimationTickHolder.getPartialTicks();

		// Translation
		ms.translate(x.get(pt), y.get(pt), z.get(pt));
		Vec3d rotationOffset = getRotationOffset(true);

		// Rotation & Mirror
		float fb = getScaleFB().get(pt);
		float lr = getScaleLR().get(pt);
		float rot = rotation.get(pt) + ((fb < 0 && lr < 0) ? 180 : 0);
		ms.translate(xOrigin, 0, zOrigin);
		MatrixStacker.of(ms)
			.translate(rotationOffset)
			.rotateY(rot)
			.translateBack(rotationOffset);
		ms.scale(abs(fb), 1, abs(lr));
		ms.translate(-xOrigin, 0, -zOrigin);

	}

	public boolean isFlipped() {
		return getMirrorModifier(Axis.X) < 0 != getMirrorModifier(Axis.Z) < 0;
	}

	public Vec3d getRotationOffset(boolean ignoreMirrors) {
		Vec3d rotationOffset = Vec3d.ZERO;
		if ((int) (zOrigin * 2) % 2 != (int) (xOrigin * 2) % 2) {
			boolean xGreaterZ = xOrigin > zOrigin;
			float xIn = (xGreaterZ ? 0 : .5f);
			float zIn = (!xGreaterZ ? 0 : .5f);
			if (!ignoreMirrors) {
				xIn *= getMirrorModifier(Axis.X);
				zIn *= getMirrorModifier(Axis.Z);
			}
			rotationOffset = new Vec3d(xIn, 0, zIn);
		}
		return rotationOffset;
	}

	public Vec3d toLocalSpace(Vec3d vec) {
		float pt = AnimationTickHolder.getPartialTicks();
		Vec3d rotationOffset = getRotationOffset(true);

		vec = vec.subtract(x.get(pt), y.get(pt), z.get(pt));
		vec = vec.subtract(xOrigin + rotationOffset.x, 0, zOrigin + rotationOffset.z);
		vec = VecHelper.rotate(vec, -rotation.get(pt), Axis.Y);
		vec = vec.add(rotationOffset.x, 0, rotationOffset.z);
		vec = vec.multiply(getScaleFB().get(pt), 1, getScaleLR().get(pt));
		vec = vec.add(xOrigin, 0, zOrigin);

		return vec;
	}

	public StructurePlacementData toSettings() {
		StructurePlacementData settings = new StructurePlacementData();

		int i = (int) rotation.getTarget();

		boolean mirrorlr = getScaleLR().getTarget() < 0;
		boolean mirrorfb = getScaleFB().getTarget() < 0;
		if (mirrorlr && mirrorfb) {
			mirrorlr = mirrorfb = false;
			i += 180;
		}
		i = i % 360;
		if (i < 0)
			i += 360;

		BlockRotation rotation = BlockRotation.NONE;
		switch (i) {
		case 90:
			rotation = BlockRotation.COUNTERCLOCKWISE_90;
			break;
		case 180:
			rotation = BlockRotation.CLOCKWISE_180;
			break;
		case 270:
			rotation = BlockRotation.CLOCKWISE_90;
			break;
		default:
		}

		settings.setRotation(rotation);
		if (mirrorfb)
			settings.setMirror(BlockMirror.FRONT_BACK);
		if (mirrorlr)
			settings.setMirror(BlockMirror.LEFT_RIGHT);

		return settings;
	}

	public BlockPos getAnchor() {
		Vec3d vec = Vec3d.ZERO.add(.5, 0, .5);
		Vec3d rotationOffset = getRotationOffset(false);
		vec = vec.subtract(xOrigin, 0, zOrigin);
		vec = vec.subtract(rotationOffset.x, 0, rotationOffset.z);
		vec = vec.multiply(getScaleFB().getTarget(), 1, getScaleLR().getTarget());
		vec = VecHelper.rotate(vec, rotation.getTarget(), Axis.Y);
		vec = vec.add(xOrigin, 0, zOrigin);

		vec = vec.add(x.getTarget(), y.getTarget(), z.getTarget());
		return new BlockPos(vec.x, vec.y, vec.z);
	}

	public Vec3d fromAnchor(BlockPos pos) {
		Vec3d vec = Vec3d.ZERO.add(.5, 0, .5);
		Vec3d rotationOffset = getRotationOffset(false);
		vec = vec.subtract(xOrigin, 0, zOrigin);
		vec = vec.subtract(rotationOffset.x, 0, rotationOffset.z);
		vec = vec.multiply(getScaleFB().getTarget(), 1, getScaleLR().getTarget());
		vec = VecHelper.rotate(vec, rotation.getTarget(), Axis.Y);
		vec = vec.add(xOrigin, 0, zOrigin);

		return Vec3d.of(pos.subtract(new BlockPos(vec.x, vec.y, vec.z)));
	}

	public int getRotationTarget() {
		return (int) rotation.getTarget();
	}

	public int getMirrorModifier(Axis axis) {
		if (axis == Axis.Z)
			return (int) getScaleLR().getTarget();
		return (int) getScaleFB().getTarget();
	}

	public float getCurrentRotation() {
		float pt = AnimationTickHolder.getPartialTicks();
		return rotation.get(pt);
	}

	public void tick() {
		x.tick();
		y.tick();
		z.tick();
		getScaleLR().tick();
		getScaleFB().tick();
		rotation.tick();
	}

	public void flip(Axis axis) {
		if (axis == Axis.X)
			getScaleLR().target(getScaleLR().getTarget() * -1);
		if (axis == Axis.Z)
			getScaleFB().target(getScaleFB().getTarget() * -1);
	}

	public void rotate90(boolean clockwise) {
		rotation.target(rotation.getTarget() + (clockwise ? -90 : 90));
	}

	public void move(float xIn, float yIn, float zIn) {
		moveTo(x.getTarget() + xIn, y.getTarget() + yIn, z.getTarget() + zIn);
	}

	public void startAt(BlockPos pos) {
		x.start(pos.getX());
		y.start(0);
		z.start(pos.getZ());
		moveTo(pos);
	}
	
	public void moveTo(BlockPos pos) {
		moveTo(pos.getX(), pos.getY(), pos.getZ());
	}

	public void moveTo(float xIn, float yIn, float zIn) {
		x.target(xIn);
		y.target(yIn);
		z.target(zIn);
	}

	public InterpolatedChasingValue getScaleFB() {
		return scaleFrontBack;
	}

	public InterpolatedChasingValue getScaleLR() {
		return scaleLeftRight;
	}

}
