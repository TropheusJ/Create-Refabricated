package com.tropheus_jay.kinetic_api.foundation.utility.placement;

import com.tropheus_jay.kinetic_api.KineticAPIClient;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// uh its probably fine, riiiiiiiiiiiight?
//@MethodsReturnNonnullByDefault
public interface IPlacementHelper {

	/**
	 * @return a predicate that gets tested with the items held in the players hands,
	 * should return true if this placement helper is active with the given item
	 */
	Predicate<ItemCooldownManager> getItemPredicate();

	/**
	 * @return a predicate that gets tested with the blockstate the player is looking at
	 * should return true if this placement helper is active with the given blockstate
	 */
	Predicate<PistonHandler> getStatePredicate();

	/**
	 * @return PlacementOffset.fail() if no valid offset could be found.
	 * PlacementOffset.success(newPos) with newPos being the new position the block should be placed at
	 */
	PlacementOffset getOffset(GameMode world, PistonHandler state, BlockPos pos, dcg ray);

	//only gets called when placementOffset is successful
	default void renderAt(BlockPos pos, PistonHandler state, dcg ray, PlacementOffset offset) {
		IPlacementHelper.renderArrow(VecHelper.getCenterOf(pos), VecHelper.getCenterOf(offset.getPos()), ray.b());
	}

	static void renderArrow(EntityHitResult center, EntityHitResult target, Direction arrowPlane) {
		renderArrow(center, target, arrowPlane, 1D);
	}
	static void renderArrow(EntityHitResult center, EntityHitResult target, Direction arrowPlane, double distanceFromCenter) {
		EntityHitResult direction = target.d(center).d();
		EntityHitResult facing = EntityHitResult.b(arrowPlane.getVector());
		EntityHitResult start = center.e(direction);
		EntityHitResult offset = direction.a(distanceFromCenter-1);
		EntityHitResult offsetA = direction.c(facing).d().a(.25);
		EntityHitResult offsetB = facing.c(direction).d().a(.25);
		EntityHitResult endA = center.e(direction.a(.75)).e(offsetA);
		EntityHitResult endB = center.e(direction.a(.75)).e(offsetB);
		KineticAPIClient.outliner.showLine("placementArrowA" + center + target, start.e(offset), endA.e(offset)).lineWidth(1/16f);
		KineticAPIClient.outliner.showLine("placementArrowB" + center + target, start.e(offset), endB.e(offset)).lineWidth(1/16f);
	}
// note: this comment was not my doing
	/*@OnlyIn(Dist.CLIENT)
	static void renderArrow(Vec3d center, Direction towards, BlockRayTraceResult ray) {
		Direction hitFace = ray.getFace();

		if (hitFace.getAxis() == towards.getAxis())
			return;

		//get the two perpendicular directions to form the arrow
		Direction[] directions = Arrays.stream(Direction.Axis.values()).filter(axis -> axis != hitFace.getAxis() && axis != towards.getAxis()).map(Iterate::directionsInAxis).findFirst().orElse(new Direction[]{});
		Vec3d startOffset = new Vec3d(towards.getDirectionVec());
		Vec3d start = center.add(startOffset);
		for (Direction dir : directions) {
			Vec3d arrowOffset = new Vec3d(dir.getDirectionVec()).scale(.25);
			Vec3d target = center.add(startOffset.scale(0.75)).add(arrowOffset);
			KineticAPIClient.outliner.showLine("placementArrow" + towards + dir, start, target).lineWidth(1/16f);
		}
	}*/

	static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, EntityHitResult hit, Direction.Axis axis) {
		return orderedByDistance(pos, hit, dir -> dir.getAxis() == axis);
	}

	static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, EntityHitResult hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
		return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() == axis).and(includeDirection));
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, EntityHitResult hit, Direction.Axis axis) {
		return orderedByDistance(pos, hit, dir -> dir.getAxis() != axis);
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, EntityHitResult hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
		return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() != axis).and(includeDirection));
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, EntityHitResult hit, Direction.Axis first, Direction.Axis second) {
		return orderedByDistanceExceptAxis(pos, hit, first, d -> d.getAxis() != second);
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, EntityHitResult hit, Direction.Axis first, Direction.Axis second, Predicate<Direction> includeDirection) {
		return orderedByDistanceExceptAxis(pos, hit, first, ((Predicate<Direction>) d -> d.getAxis() != second).and(includeDirection));
	}

	static List<Direction> orderedByDistance(BlockPos pos, EntityHitResult hit) {
		return orderedByDistance(pos, hit, _$ -> true);
	}

	static List<Direction> orderedByDistance(BlockPos pos, EntityHitResult hit, Predicate<Direction> includeDirection) {
		EntityHitResult centerToHit = hit.d(VecHelper.getCenterOf(pos));
		return Arrays.stream(Iterate.directions)
				.filter(includeDirection)
				.map(dir -> Pair.of(dir, EntityHitResult.b(dir.getVector()).f(centerToHit)))
				.sorted(Comparator.comparingDouble(Pair::getSecond))
				.map(Pair::getFirst)
				.collect(Collectors.toList());
	}

	default boolean matchesItem(ItemCooldownManager item) {
		return getItemPredicate().test(item);
	}

	default boolean matchesState(PistonHandler state) {
		return getStatePredicate().test(state);
	}
}