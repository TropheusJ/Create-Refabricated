package com.tropheus_jay.kinetic_api.foundation.utility.placement;

import com.tropheus_jay.kinetic_api.foundation.utility.Iterate;
import com.tropheus_jay.kinetic_api.foundation.utility.Pair;
import com.tropheus_jay.kinetic_api.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

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
	Predicate<ItemStack> getItemPredicate();

	/**
	 * @return a predicate that gets tested with the blockstate the player is looking at
	 * should return true if this placement helper is active with the given blockstate
	 */
	Predicate<BlockState> getStatePredicate();

	/**
	 * @return PlacementOffset.fail() if no valid offset could be found.
	 * PlacementOffset.success(newPos) with newPos being the new position the block should be placed at
	 */
	/*todo: placementOffsets
	PlacementOffset getOffset(GameMode world, PistonHandler state, BlockPos pos, dcg ray);

	//only gets called when placementOffset is successful
	default void renderAt(BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset) {
		IPlacementHelper.renderArrow(VecHelper.getCenterOf(pos), VecHelper.getCenterOf(offset.getPos()), ray.b());
	}
*/
	static void renderArrow(Vec3d center, Vec3d target, Direction arrowPlane) {
		renderArrow(center, target, arrowPlane, 1D);
	}
	static void renderArrow(Vec3d center, Vec3d target, Direction arrowPlane, double distanceFromCenter) {
		Vec3d direction = target.subtract(center).normalize();
		Vec3d facing = Vec3d.of(arrowPlane.getVector());
		Vec3d start = center.add(direction);
		Vec3d offset = direction.multiply(distanceFromCenter-1);
		Vec3d offsetA = direction.crossProduct(facing).normalize().multiply(.25);
		Vec3d offsetB = facing.crossProduct(direction).normalize().multiply(.25);
		Vec3d endA = center.add(direction.multiply(.75)).add(offsetA);
		Vec3d endB = center.add(direction.multiply(.75)).add(offsetB);
		/*todo: outliner
		KineticAPIClient.outliner.showLine("placementArrowA" + center + target, start.e(offset), endA.e(offset)).lineWidth(1/16f);
		KineticAPIClient.outliner.showLine("placementArrowB" + center + target, start.e(offset), endB.e(offset)).lineWidth(1/16f);
	*/
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

	static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3d hit, Direction.Axis axis) {
		return orderedByDistance(pos, hit, dir -> dir.getAxis() == axis);
	}

	static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3d hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
		return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() == axis).and(includeDirection));
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis axis) {
		return orderedByDistance(pos, hit, dir -> dir.getAxis() != axis);
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
		return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() != axis).and(includeDirection));
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis first, Direction.Axis second) {
		return orderedByDistanceExceptAxis(pos, hit, first, d -> d.getAxis() != second);
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis first, Direction.Axis second, Predicate<Direction> includeDirection) {
		return orderedByDistanceExceptAxis(pos, hit, first, ((Predicate<Direction>) d -> d.getAxis() != second).and(includeDirection));
	}

	static List<Direction> orderedByDistance(BlockPos pos, Vec3d hit) {
		return orderedByDistance(pos, hit, _$ -> true);
	}

	static List<Direction> orderedByDistance(BlockPos pos, Vec3d hit, Predicate<Direction> includeDirection) {
		Vec3d centerToHit = hit.subtract(VecHelper.getCenterOf(pos));
		return Arrays.stream(Iterate.directions)
				.filter(includeDirection)
				.map(dir -> Pair.of(dir, Vec3d.of(dir.getVector()).distanceTo(centerToHit)))
				.sorted(Comparator.comparingDouble(Pair::getSecond))
				.map(Pair::getFirst)
				.collect(Collectors.toList());
	}

	default boolean matchesItem(ItemStack item) {
		return getItemPredicate().test(item);
	}

	default boolean matchesState(BlockState state) {
		return getStatePredicate().test(state);
	}
}
