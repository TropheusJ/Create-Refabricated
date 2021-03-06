package com.simibubi.create.content.contraptions.components.fan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.particle.AirFlowParticleData;
import com.simibubi.create.content.logistics.InWorldProcessing;
import com.simibubi.create.content.logistics.InWorldProcessing.Type;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class AirCurrent {

	private static final DamageSource damageSourceFire = new DamageSource("create.fan_fire").setScaledWithDifficulty()
		.setFire();
	private static final DamageSource damageSourceLava = new DamageSource("create.fan_lava").setScaledWithDifficulty()
		.setFire();

	public final IAirCurrentSource source;
	public Box bounds = new Box(0, 0, 0, 0, 0, 0);
	public List<AirCurrentSegment> segments = new ArrayList<>();
	public Direction direction;
	public boolean pushing;
	public float maxDistance;

	protected List<Pair<TransportedItemStackHandlerBehaviour, InWorldProcessing.Type>> affectedItemHandlers =
		new ArrayList<>();
	protected List<Entity> caughtEntities = new ArrayList<>();

	public AirCurrent(IAirCurrentSource source) {
		this.source = source;
	}

	public void tick() {
		if (direction == null)
			rebuild();
		World world = source.getAirCurrentWorld();
		Direction facing = direction;
		if (world != null && world.isClient) {
			float offset = pushing ? 0.5f : maxDistance + .5f;
			Vec3d pos = VecHelper.getCenterOf(source.getAirCurrentPos())
				.add(Vec3d.of(facing.getVector()).multiply(offset));
			if (world.random.nextFloat() < AllConfigs.CLIENT.fanParticleDensity.get())
				world.addParticle(new AirFlowParticleData(source.getAirCurrentPos()), pos.x, pos.y, pos.z, 0, 0, 0);
		}

		tickAffectedEntities(world, facing);
		tickAffectedHandlers();
	}

	protected void tickAffectedEntities(World world, Direction facing) {
		for (Iterator<Entity> iterator = caughtEntities.iterator(); iterator.hasNext();) {
			Entity entity = iterator.next();
			if (!entity.isAlive() || !entity.getBoundingBox()
				.intersects(bounds)) {
				iterator.remove();
				continue;
			}

			Vec3d center = VecHelper.getCenterOf(source.getAirCurrentPos());
			Vec3i flow = (pushing ? facing : facing.getOpposite()).getVector();

			float sneakModifier = entity.isSneaking() ? 4096f : 512f;
			float speed = Math.abs(source.getSpeed());
			double entityDistance = entity.getPos()
				.distanceTo(center);
			float acceleration = (float) (speed / sneakModifier / (entityDistance / maxDistance));
			Vec3d previousMotion = entity.getVelocity();
			float maxAcceleration = 5;

			double xIn =
				MathHelper.clamp(flow.getX() * acceleration - previousMotion.x, -maxAcceleration, maxAcceleration);
			double yIn =
				MathHelper.clamp(flow.getY() * acceleration - previousMotion.y, -maxAcceleration, maxAcceleration);
			double zIn =
				MathHelper.clamp(flow.getZ() * acceleration - previousMotion.z, -maxAcceleration, maxAcceleration);

			entity.setVelocity(previousMotion.add(new Vec3d(xIn, yIn, zIn).multiply(1 / 8f)));
			entity.fallDistance = 0;

			if (entity instanceof ServerPlayerEntity)
				((ServerPlayerEntity) entity).networkHandler.floatingTicks = 0;

			entityDistance -= .5f;
			InWorldProcessing.Type processingType = getSegmentAt((float) entityDistance);

			if (processingType == null) {
				if (entity instanceof ServerPlayerEntity)
					AllTriggers.triggerFor(AllTriggers.FAN, (PlayerEntity) entity);
				continue;
			}

			if (entity instanceof ItemEntity) {
				InWorldProcessing.spawnParticlesForProcessing(world, entity.getPos(), processingType);
				ItemEntity itemEntity = (ItemEntity) entity;
				if (world.isClient)
					continue;
				if (InWorldProcessing.canProcess(itemEntity, processingType))
					InWorldProcessing.applyProcessing(itemEntity, processingType);
				continue;
			}

			if (world.isClient)
				continue;

			switch (processingType) {
			case BLASTING:
				if (!entity.isFireImmune()) {
					entity.setOnFireFor(10);
					entity.damage(damageSourceLava, 4);
				}
				if (entity instanceof ServerPlayerEntity)
					AllTriggers.triggerFor(AllTriggers.FAN_LAVA, (PlayerEntity) entity);
				break;
			case SMOKING:
				if (!entity.isFireImmune()) {
					entity.setOnFireFor(2);
					entity.damage(damageSourceFire, 2);
				}
				if (entity instanceof ServerPlayerEntity)
					AllTriggers.triggerFor(AllTriggers.FAN_SMOKE, (PlayerEntity) entity);
				break;
			case SPLASHING:
				if (entity instanceof EndermanEntity || entity.getType() == EntityType.SNOW_GOLEM
					|| entity.getType() == EntityType.BLAZE) {
					entity.damage(DamageSource.DROWN, 2);
				}
				if (entity instanceof ServerPlayerEntity)
					AllTriggers.triggerFor(AllTriggers.FAN_WATER, (PlayerEntity) entity);
				if (!entity.isOnFire())
					break;
				entity.extinguish();
				world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE,
					SoundCategory.NEUTRAL, 0.7F, 1.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
				break;
			default:
				break;
			}
		}

	}

	public void rebuild() {
		if (source.getSpeed() == 0) {
			maxDistance = 0;
			segments.clear();
			bounds = new Box(0, 0, 0, 0, 0, 0);
			return;
		}

		direction = source.getAirflowOriginSide();
		pushing = source.getAirFlowDirection() == direction;
		maxDistance = source.getMaxDistance();

		World world = source.getAirCurrentWorld();
		BlockPos start = source.getAirCurrentPos();
		float max = this.maxDistance;
		Direction facing = direction;
		Vec3d directionVec = Vec3d.of(facing.getVector());
		maxDistance = getFlowLimit(world, start, max, facing);

		// Determine segments with transported fluids/gases
		AirCurrentSegment currentSegment = new AirCurrentSegment();
		segments.clear();
		currentSegment.startOffset = 0;
		InWorldProcessing.Type type = null;

		int limit = (int) (maxDistance + .5f);
		int searchStart = pushing ? 0 : limit;
		int searchEnd = pushing ? limit : 0;
		int searchStep = pushing ? 1 : -1;

		for (int i = searchStart; i * searchStep <= searchEnd * searchStep; i += searchStep) {
			BlockPos currentPos = start.offset(direction, i);
			InWorldProcessing.Type newType = InWorldProcessing.Type.byBlock(world, currentPos);
			if (newType != null)
				type = newType;
			if (currentSegment.type != type || currentSegment.startOffset == 0) {
				currentSegment.endOffset = i;
				if (currentSegment.startOffset != 0)
					segments.add(currentSegment);
				currentSegment = new AirCurrentSegment();
				currentSegment.startOffset = i;
				currentSegment.type = type;
			}
		}
		currentSegment.endOffset = searchEnd + searchStep;
		segments.add(currentSegment);

		// Build Bounding Box
		if (maxDistance < 0.25f)
			bounds = new Box(0, 0, 0, 0, 0, 0);
		else {
			float factor = maxDistance - 1;
			Vec3d scale = directionVec.multiply(factor);
			if (factor > 0)
				bounds = new Box(start.offset(direction)).stretch(scale);
			else {
				bounds = new Box(start.offset(direction)).shrink(scale.x, scale.y, scale.z)
					.offset(scale);
			}
		}
		findAffectedHandlers();
	}

	public static float getFlowLimit(World world, BlockPos start, float max, Direction facing) {
		Vec3d directionVec = Vec3d.of(facing.getVector());
		Vec3d planeVec = VecHelper.axisAlingedPlaneOf(directionVec);

		// 4 Rays test for holes in the shapes blocking the flow
		float offsetDistance = .25f;
		Vec3d[] offsets = new Vec3d[] { planeVec.multiply(offsetDistance, offsetDistance, offsetDistance),
			planeVec.multiply(-offsetDistance, -offsetDistance, offsetDistance),
			planeVec.multiply(offsetDistance, -offsetDistance, -offsetDistance),
			planeVec.multiply(-offsetDistance, offsetDistance, -offsetDistance), };

		float limitedDistance = 0;

		// Determine the distance of the air flow
		Outer: for (int i = 1; i <= max; i++) {
			BlockPos currentPos = start.offset(facing, i);
			if (!world.canSetBlock(currentPos))
				break;
			BlockState state = world.getBlockState(currentPos);
			if (shouldAlwaysPass(state))
				continue;
			VoxelShape voxelshape = state.getCollisionShape(world, currentPos, ShapeContext.absent());
			if (voxelshape.isEmpty())
				continue;
			if (voxelshape == VoxelShapes.fullCube()) {
				max = i - 1;
				break;
			}

			for (Vec3d offset : offsets) {
				Vec3d rayStart = VecHelper.getCenterOf(currentPos)
					.subtract(directionVec.multiply(.5f + 1 / 32f))
					.add(offset);
				Vec3d rayEnd = rayStart.add(directionVec.multiply(1 + 1 / 32f));
				BlockHitResult blockraytraceresult =
					world.raycastBlock(rayStart, rayEnd, currentPos, voxelshape, state);
				if (blockraytraceresult == null)
					continue Outer;

				double distance = i - 1 + blockraytraceresult.getPos()
					.distanceTo(rayStart);
				if (limitedDistance < distance)
					limitedDistance = (float) distance;
			}

			max = limitedDistance;
			break;
		}
		return max;
	}

	public void findEntities() {
		caughtEntities.clear();
		caughtEntities = source.getAirCurrentWorld()
			.getOtherEntities(null, bounds);
	}

	public void findAffectedHandlers() {
		World world = source.getAirCurrentWorld();
		BlockPos start = source.getAirCurrentPos();
		affectedItemHandlers.clear();
		for (int i = 0; i < maxDistance + 1; i++) {
			Type type = getSegmentAt(i);
			if (type == null)
				continue;

			for (int offset : Iterate.zeroAndOne) {
				BlockPos pos = start.offset(direction, i)
					.down(offset);
				TransportedItemStackHandlerBehaviour behaviour =
					TileEntityBehaviour.get(world, pos, TransportedItemStackHandlerBehaviour.TYPE);
				if (behaviour != null)
					affectedItemHandlers.add(Pair.of(behaviour, type));
				if (direction.getAxis()
					.isVertical())
					break;
			}
		}
	}

	public void tickAffectedHandlers() {
		for (Pair<TransportedItemStackHandlerBehaviour, Type> pair : affectedItemHandlers) {
			TransportedItemStackHandlerBehaviour handler = pair.getKey();
			World world = handler.getWorld();
			InWorldProcessing.Type processingType = pair.getRight();

			handler.handleProcessingOnAllItems((transported) -> {
				InWorldProcessing.spawnParticlesForProcessing(world, handler.getWorldPositionOf(transported),
					processingType);
				if (world.isClient)
					return TransportedResult.doNothing();
				return InWorldProcessing.applyProcessing(transported, world, processingType);
			});
		}
	}

	private static boolean shouldAlwaysPass(BlockState state) {
		return AllTags.AllBlockTags.FAN_TRANSPARENT.matches(state);
	}

	public InWorldProcessing.Type getSegmentAt(float offset) {
		for (AirCurrentSegment airCurrentSegment : segments) {
			if (offset > airCurrentSegment.endOffset && pushing)
				continue;
			if (offset < airCurrentSegment.endOffset && !pushing)
				continue;
			return airCurrentSegment.type;
		}
		return null;
	}

	public static class AirCurrentSegment {
		InWorldProcessing.Type type;
		int startOffset;
		int endOffset;

	}

}
