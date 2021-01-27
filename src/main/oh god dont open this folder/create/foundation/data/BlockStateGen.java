
package com.simibubi.create.foundation.data;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.contraptions.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.LinearChassisBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.RadialChassisBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.components.tracks.ReinforcedRailBlock;
import com.simibubi.create.content.contraptions.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.contraptions.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.palettes.PavedBlock;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.foundation.worldgen.OxidizingBlock;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;

public class BlockStateGen {

	// Functions

	public static <T extends BeetrootsBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> axisBlockProvider(
		boolean customItem) {
		return (c, p) -> axisBlock(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends BeetrootsBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> directionalBlockProvider(
		boolean customItem) {
		return (c, p) -> p.directionalBlock(c.get(), getBlockModel(customItem, c, p));
	}

	public static <T extends BeetrootsBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> directionalBlockProviderIgnoresWaterlogged(
		boolean customItem) {
		return (c, p) -> directionalBlockIgnoresWaterlogged(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends BeetrootsBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> horizontalBlockProvider(
		boolean customItem) {
		return (c, p) -> p.horizontalBlock(c.get(), getBlockModel(customItem, c, p));
	}

	public static <T extends BeetrootsBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> horizontalAxisBlockProvider(
		boolean customItem) {
		return (c, p) -> horizontalAxisBlock(c, p, getBlockModel(customItem, c, p));
	}

	public static <T extends DirectionalAxisKineticBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> directionalAxisBlockProvider() {
		return (c, p) -> directionalAxisBlock(c, p, ($, vertical) -> p.models()
			.getExistingFile(p.modLoc("block/" + c.getName() + "/" + (vertical ? "vertical" : "horizontal"))));
	}

	public static <T extends BeetrootsBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, T>, RegistrateBlockstateProvider> horizontalWheelProvider(
		boolean customItem) {
		return (c, p) -> horizontalWheel(c, p, getBlockModel(customItem, c, p));
	}

	public static <P> NonNullUnaryOperator<BlockBuilder<OxidizingBlock, P>> oxidizedBlockstate() {
		return b -> b.blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry())
			.forAllStates(state -> {
				String name = AssetLookup.getOxidizedModel(ctx.getName(), state.c(OxidizingBlock.OXIDIZATION));
				return ConfiguredModel.builder()
					.modelFile(prov.models()
						.cubeAll(name, prov.modLoc(name)))
					.build();
			}));
	}

	// Utility

	private static <T extends BeetrootsBlock> Function<PistonHandler, ModelFile> getBlockModel(boolean customItem,
		DataGenContext<BeetrootsBlock, T> c, RegistrateBlockstateProvider p) {
		return $ -> customItem ? AssetLookup.partialBaseModel(c, p) : AssetLookup.standardModel(c, p);
	}

	// Generators

	public static <T extends BeetrootsBlock> void directionalBlockIgnoresWaterlogged(DataGenContext<BeetrootsBlock, T> ctx,
		RegistrateBlockstateProvider prov, Function<PistonHandler, ModelFile> modelFunc) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStatesExcept(state -> {
				Direction dir = state.c(BambooLeaves.M);
				return ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.rotationX(dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 90 : 0)
					.rotationY(dir.getAxis()
						.isVertical() ? 0 : (((int) dir.asRotation()) + 180) % 360)
					.build();
			}, BambooLeaves.C);
	}

	public static <T extends BeetrootsBlock> void axisBlock(DataGenContext<BeetrootsBlock, T> ctx, RegistrateBlockstateProvider prov,
		Function<PistonHandler, ModelFile> modelFunc) {
		axisBlock(ctx, prov, modelFunc, false);
	}

	public static <T extends BeetrootsBlock> void axisBlock(DataGenContext<BeetrootsBlock, T> ctx, RegistrateBlockstateProvider prov,
		Function<PistonHandler, ModelFile> modelFunc, boolean uvLock) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStatesExcept(state -> {
				Axis axis = state.c(BambooLeaves.F);
				return ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.uvLock(uvLock)
					.rotationX(axis == Axis.Y ? 0 : 90)
					.rotationY(axis == Axis.X ? 90 : axis == Axis.Z ? 180 : 0)
					.build();
			}, BambooLeaves.C);
	}

	public static <T extends BeetrootsBlock> void horizontalAxisBlock(DataGenContext<BeetrootsBlock, T> ctx,
		RegistrateBlockstateProvider prov, Function<PistonHandler, ModelFile> modelFunc) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStates(state -> {
				Axis axis = state.c(BambooLeaves.E);
				return ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.rotationY(axis == Axis.X ? 90 : 0)
					.build();
			});
	}

	public static <T extends DirectionalAxisKineticBlock> void directionalAxisBlock(DataGenContext<BeetrootsBlock, T> ctx,
		RegistrateBlockstateProvider prov, BiFunction<PistonHandler, Boolean, ModelFile> modelFunc) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStates(state -> {

				boolean alongFirst = state.c(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
				Direction direction = state.c(DirectionalAxisKineticBlock.FACING);
				boolean vertical = direction.getAxis()
					.isHorizontal() && (direction.getAxis() == Axis.X) == alongFirst;
				int xRot = direction == Direction.DOWN ? 270 : direction == Direction.UP ? 90 : 0;
				int yRot = direction.getAxis()
					.isVertical() ? alongFirst ? 0 : 90 : (int) direction.asRotation();

				return ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state, vertical))
					.rotationX(xRot)
					.rotationY(yRot)
					.build();
			});
	}

	public static <T extends BeetrootsBlock> void horizontalWheel(DataGenContext<BeetrootsBlock, T> ctx,
		RegistrateBlockstateProvider prov, Function<PistonHandler, ModelFile> modelFunc) {
		prov.getVariantBuilder(ctx.get())
			.forAllStates(state -> ConfiguredModel.builder()
				.modelFile(modelFunc.apply(state))
				.rotationX(90)
				.rotationY(((int) state.c(BambooLeaves.O)
					.asRotation() + 180) % 360)
				.build());
	}

	public static <T extends BeetrootsBlock> void cubeAll(DataGenContext<BeetrootsBlock, T> ctx, RegistrateBlockstateProvider prov,
		String textureSubDir) {
		cubeAll(ctx, prov, textureSubDir, ctx.getName());
	}

	public static <T extends BeetrootsBlock> void cubeAll(DataGenContext<BeetrootsBlock, T> ctx, RegistrateBlockstateProvider prov,
		String textureSubDir, String name) {
		String texturePath = "block/" + textureSubDir + name;
		prov.simpleBlock(ctx.get(), prov.models()
			.cubeAll(ctx.getName(), prov.modLoc(texturePath)));
	}

	public static <T extends BeetrootsBlock> void pavedBlock(DataGenContext<BeetrootsBlock, T> ctx, RegistrateBlockstateProvider prov,
		ModelFile top, ModelFile covered) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStates(state -> ConfiguredModel.builder()
				.modelFile(state.c(PavedBlock.COVERED) ? covered : top)
				.build());
	}

	public static NonNullBiConsumer<DataGenContext<BeetrootsBlock, CartAssemblerBlock>, RegistrateBlockstateProvider> cartAssembler() {
		return (c, p) -> p.getVariantBuilder(c.get())
			.forAllStates(state -> {
				CartAssembleRailType type = state.c(CartAssemblerBlock.RAIL_TYPE);
				Boolean powered = state.c(CartAssemblerBlock.POWERED);
				Instrument shape = state.c(CartAssemblerBlock.RAIL_SHAPE);
				int yRotation = shape == Instrument.EAST_WEST ? 270 : 0;
				if (type == CartAssembleRailType.CONTROLLER_RAIL_BACKWARDS) {
					yRotation += 180;
					type = CartAssembleRailType.CONTROLLER_RAIL;
				}

				return ConfiguredModel.builder()
					.modelFile(p.models()
						.getExistingFile(p
							.modLoc("block/" + c.getName() + "/block_" + type.a() + (powered ? "_powered" : ""))))
					.rotationY(yRotation % 360)
					.build();
			});
	}

	public static NonNullBiConsumer<DataGenContext<BeetrootsBlock, BlazeBurnerBlock>, RegistrateBlockstateProvider> blazeHeater() {
		return (c, p) -> ConfiguredModel.builder()
			.modelFile(p.models()
				.getExistingFile(p.modLoc("block/" + c.getName() + "/block")))
			.build();
	}

	public static NonNullBiConsumer<DataGenContext<BeetrootsBlock, ReinforcedRailBlock>, RegistrateBlockstateProvider> reinforcedRail() {
		return (c, p) -> p.getVariantBuilder(c.get())
			.forAllStates(state -> {
				return ConfiguredModel.builder()
					.modelFile(p.models()
						.getExistingFile(p.modLoc(
							"block/" + c.getName() + "/block" + (state.c(ReinforcedRailBlock.CONNECTS_S) ? "_s" : "")
								+ (state.c(ReinforcedRailBlock.CONNECTS_N) ? "_n" : ""))))
					.rotationY(state.c(ReinforcedRailBlock.RAIL_SHAPE) == Instrument.EAST_WEST ? 90 : 0)
					.build();
			});
	}

	public static <B extends LinearChassisBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, B>, RegistrateBlockstateProvider> linearChassis() {
		return (c, p) -> {
			Identifier side = p.modLoc("block/" + c.getName() + "_side");
			Identifier top = p.modLoc("block/linear_chassis_end");
			Identifier top_sticky = p.modLoc("block/linear_chassis_end_sticky");

			Vector<ModelFile> models = new Vector<>(4);
			for (boolean isTopSticky : Iterate.trueAndFalse)
				for (boolean isBottomSticky : Iterate.trueAndFalse)
					models.add(p.models()
						.withExistingParent(
							c.getName() + (isTopSticky ? "_top" : "") + (isBottomSticky ? "_bottom" : ""),
							"block/cube_bottom_top")
						.texture("side", side)
						.texture("bottom", isBottomSticky ? top_sticky : top)
						.texture("top", isTopSticky ? top_sticky : top));
			BiFunction<Boolean, Boolean, ModelFile> modelFunc = (t, b) -> models.get((t ? 0 : 2) + (b ? 0 : 1));

			axisBlock(c, p, state -> modelFunc.apply(state.c(LinearChassisBlock.STICKY_TOP),
				state.c(LinearChassisBlock.STICKY_BOTTOM)));
		};
	}

	public static <B extends RadialChassisBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, B>, RegistrateBlockstateProvider> radialChassis() {
		return (c, p) -> {
			String path = "block/" + c.getName();
			Identifier side = p.modLoc(path + "_side");
			Identifier side_sticky = p.modLoc(path + "_side_sticky");

			String templateModelPath = "block/radial_chassis";
			ModelFile base = p.models()
				.getExistingFile(p.modLoc(templateModelPath + "/base"));
			Vector<ModelFile> faces = new Vector<>(3);
			Vector<ModelFile> stickyFaces = new Vector<>(3);

			for (Axis axis : Iterate.axes) {
				String suffix = "side_" + axis.a();
				faces.add(p.models()
					.withExistingParent("block/" + c.getName() + "_" + suffix,
						p.modLoc(templateModelPath + "/" + suffix))
					.texture("side", side));
			}
			for (Axis axis : Iterate.axes) {
				String suffix = "side_" + axis.a();
				stickyFaces.add(p.models()
					.withExistingParent("block/" + c.getName() + "_" + suffix + "_sticky",
						p.modLoc(templateModelPath + "/" + suffix))
					.texture("side", side_sticky));
			}

			MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
			PistonHandler propertyGetter = c.get()
				.n()
				.a(RadialChassisBlock.e, Axis.Y);

			for (Axis axis : Iterate.axes)
				builder.part()
					.modelFile(base)
					.rotationX(axis != Axis.Y ? 90 : 0)
					.rotationY(axis != Axis.X ? 0 : 90)
					.addModel()
					.condition(RadialChassisBlock.e, axis)
					.end();

			for (Direction face : Iterate.horizontalDirections) {
				for (boolean sticky : Iterate.trueAndFalse) {
					for (Axis axis : Iterate.axes) {
						int horizontalAngle = (int) (face.asRotation());
						int index = axis.ordinal();
						int xRot = 0;
						int yRot = 0;

						if (axis == Axis.X)
							xRot = -horizontalAngle + 180;
						if (axis == Axis.Y)
							yRot = horizontalAngle;
						if (axis == Axis.Z) {
							yRot = -horizontalAngle + 270;

							// blockstates can't have zRot, so here we are
							if (face.getAxis() == Axis.Z) {
								index = 0;
								xRot = horizontalAngle + 180;
								yRot = 90;
							}
						}

						builder.part()
							.modelFile((sticky ? stickyFaces : faces).get(index))
							.rotationX((xRot + 360) % 360)
							.rotationY((yRot + 360) % 360)
							.addModel()
							.condition(RadialChassisBlock.e, axis)
							.condition(c.get()
								.getGlueableSide(propertyGetter, face), sticky)
							.end();
					}
				}
			}
		};
	}

	public static <P extends EncasedPipeBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, P>, RegistrateBlockstateProvider> encasedPipe() {
		return (c, p) -> {
			ModelFile open = AssetLookup.partialBaseModel(c, p, "open");
			ModelFile flat = AssetLookup.partialBaseModel(c, p, "flat");
			MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
			for (boolean flatPass : Iterate.trueAndFalse)
				for (Direction d : Iterate.directions) {
					int verticalAngle = d == Direction.UP ? 90 : d == Direction.DOWN ? -90 : 0;
					builder.part()
						.modelFile(flatPass ? flat : open)
						.rotationX(verticalAngle)
						.rotationY((int) (d.asRotation() + (d.getAxis()
							.isVertical() ? 90 : 0)) % 360)
						.addModel()
						.condition(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(d), !flatPass)
						.end();
				}
		};
	}

	public static <P extends FluidPipeBlock> NonNullBiConsumer<DataGenContext<BeetrootsBlock, P>, RegistrateBlockstateProvider> pipe() {
		return (c, p) -> {
			String path = "block/" + c.getName();

			String LU = "lu";
			String RU = "ru";
			String LD = "ld";
			String RD = "rd";
			String LR = "lr";
			String UD = "ud";
			String NONE = "none";

			List<String> orientations = ImmutableList.of(LU, RU, LD, RD, LR, UD, NONE);
			Map<String, Pair<Integer, Integer>> uvs = ImmutableMap.<String, Pair<Integer, Integer>>builder()
				.put(LU, Pair.of(8, 12))
				.put(RU, Pair.of(0, 12))
				.put(LD, Pair.of(12, 8))
				.put(RD, Pair.of(8, 8))
				.put(LR, Pair.of(4, 12))
				.put(UD, Pair.of(0, 8))
				.put(NONE, Pair.of(12, 12))
				.build();

			Map<Axis, Identifier> coreTemplates = new IdentityHashMap<>();
			Map<Pair<String, Axis>, ModelFile> coreModels = new HashMap<>();

			for (Axis axis : Iterate.axes)
				coreTemplates.put(axis, p.modLoc(path + "/core_" + axis.a()));
			ModelFile end = AssetLookup.partialBaseModel(c, p, "end");

			for (Axis axis : Iterate.axes) {
				Identifier parent = coreTemplates.get(axis);
				for (String s : orientations) {
					Pair<String, Axis> key = Pair.of(s, axis);
					String modelName = path + "/" + s + "_" + axis.a();
					coreModels.put(key, p.models()
						.withExistingParent(modelName, parent)
						.element()
						.from(4, 4, 4)
						.to(12, 12, 12)
						.face(Direction.get(AxisDirection.POSITIVE, axis))
						.end()
						.face(Direction.get(AxisDirection.NEGATIVE, axis))
						.end()
						.faces((d, builder) -> {
							Pair<Integer, Integer> pair = uvs.get(s);
							float u = pair.getKey();
							float v = pair.getValue();
							if (d == Direction.UP)
								builder.uvs(u, v + 4, u + 4, v);
							else if (d.getDirection() == AxisDirection.POSITIVE)
								builder.uvs(u + 4, v, u, v + 4);
							else
								builder.uvs(u, v, u + 4, v + 4);
							builder.texture("#0");
						})
						.end());
				}
			}

			MultiPartBlockStateBuilder builder = p.getMultipartBuilder(c.get());
			for (Direction d : Iterate.directions)
				builder.part()
					.modelFile(end)
					.rotationX(d == Direction.UP ? 0 : d == Direction.DOWN ? 180 : 90)
					.rotationY((int) (d.asRotation() + 180) % 360)
					.addModel()
					.condition(FluidPipeBlock.g.get(d), true)
					.end();

			for (Axis axis : Iterate.axes) {
				putPart(coreModels, builder, axis, LU, true, false, true, false);
				putPart(coreModels, builder, axis, RU, true, false, false, true);
				putPart(coreModels, builder, axis, LD, false, true, true, false);
				putPart(coreModels, builder, axis, RD, false, true, false, true);
				putPart(coreModels, builder, axis, UD, true, true, false, false);
				putPart(coreModels, builder, axis, UD, true, false, false, false);
				putPart(coreModels, builder, axis, UD, false, true, false, false);
				putPart(coreModels, builder, axis, LR, false, false, true, true);
				putPart(coreModels, builder, axis, LR, false, false, true, false);
				putPart(coreModels, builder, axis, LR, false, false, false, true);
				putPart(coreModels, builder, axis, NONE, false, false, false, false);
			}
		};
	}

	private static void putPart(Map<Pair<String, Axis>, ModelFile> coreModels, MultiPartBlockStateBuilder builder,
		Axis axis, String s, boolean up, boolean down, boolean left, boolean right) {
		Direction positiveAxis = Direction.get(AxisDirection.POSITIVE, axis);
		Map<Direction, BedPart> propertyMap = FluidPipeBlock.g;
		builder.part()
			.modelFile(coreModels.get(Pair.of(s, axis)))
			.addModel()
			.condition(propertyMap.get(Pointing.UP.getCombinedDirection(positiveAxis)), up)
			.condition(propertyMap.get(Pointing.LEFT.getCombinedDirection(positiveAxis)), left)
			.condition(propertyMap.get(Pointing.RIGHT.getCombinedDirection(positiveAxis)), right)
			.condition(propertyMap.get(Pointing.DOWN.getCombinedDirection(positiveAxis)), down)
			.end();
	}
}