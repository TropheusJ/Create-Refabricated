package com.simibubi.kinetic_api.foundation.advancement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simibubi.kinetic_api.AllBlocks;
import com.simibubi.kinetic_api.AllFluids;
import com.simibubi.kinetic_api.AllItems;
import com.simibubi.kinetic_api.Create;
import com.simibubi.kinetic_api.content.curiosities.zapper.blockzapper.BlockzapperItem;
import com.simibubi.kinetic_api.content.curiosities.zapper.blockzapper.BlockzapperItem.ComponentTier;
import com.simibubi.kinetic_api.content.curiosities.zapper.blockzapper.BlockzapperItem.Components;
import com.simibubi.kinetic_api.foundation.advancement.AllAdvancements.TaskType;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.Advancement.Task;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.PlacedBlockCriterion;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.data.DataCache;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

@SuppressWarnings("unused") // dont warn about unused avancements
public class AllAdvancements implements DataProvider {

	static final String LANG = "advancement." + Create.ID + ".";

	public void register(Consumer<Advancement> t) {
		String id = Create.ID;

		Advancement root = Advancement.Task.create()
			.a(AllItems.BRASS_HAND.asStack(), new TranslatableText(LANG + "root"),
				new TranslatableText(LANG + "root.desc"),
				new Identifier(Create.ID, "textures/block/palettes/gabbro/bricks.png"), AdvancementFrame.TASK, false,
				false, false)
			.criterion("0", InventoryChangedCriterion.Conditions.a(new GameRules[] {}))
			.build(t, id + ":root");

		Advancement andesite_alloy =
			advancement("andesite_alloy", AllItems.ANDESITE_ALLOY.get(), TaskType.NORMAL).parent(root)
				.criterion("0", itemGathered(AllItems.ANDESITE_ALLOY.get()))
				.build(t, id + ":andesite_alloy");

		kineticsBranch(t, andesite_alloy);

		Advancement aesthetics =
			advancement("aesthetics", AllBlocks.WOODEN_BRACKET.get(), TaskType.NORMAL).parent(andesite_alloy)
				.criterion("0", AllTriggers.BRACKET_SHAFT.instance())
				.criterion("1", AllTriggers.BRACKET_COG.instance())
				.criterion("2", AllTriggers.BRACKET_PIPE.instance())
				.build(t, id + ":aesthetics");

		Advancement reinforced =
			advancement("reinforced", AllBlocks.ANDESITE_ENCASED_SHAFT.get(), TaskType.NORMAL).parent(aesthetics)
				.criterion("0", AllTriggers.CASING_SHAFT.instance())
				.criterion("1", AllTriggers.CASING_BELT.instance())
				.criterion("2", AllTriggers.CASING_PIPE.instance())
				.build(t, id + ":reinforced");

		Advancement water_wheel =
			advancement("water_wheel", AllBlocks.WATER_WHEEL.get(), TaskType.NORMAL).parent(andesite_alloy)
				.criterion("0", placeBlock(AllBlocks.WATER_WHEEL.get()))
				.criterion("1", AllTriggers.WATER_WHEEL.instance())
				.build(t, id + ":water_wheel");

		Advancement lava_wheel = advancement("lava_wheel", AliasedBlockItem.lM, TaskType.SECRET).parent(water_wheel)
			.criterion("0", AllTriggers.LAVA_WHEEL.instance())
			.build(t, id + ":lava_wheel");

		Advancement chocolate_wheel = advancement("chocolate_wheel", AllFluids.CHOCOLATE.get()
			.a(), TaskType.SECRET).parent(water_wheel)
				.criterion("0", AllTriggers.CHOCOLATE_WHEEL.instance())
				.build(t, id + ":chocolate_wheel");

		Advancement millstone =
			kinecticAdvancement("millstone", AllBlocks.MILLSTONE.get(), TaskType.NORMAL).parent(andesite_alloy)
				.build(t, id + ":millstone");

		Advancement cuckoo =
			advancement("cuckoo", AllBlocks.CUCKOO_CLOCK.get(), TaskType.NORMAL).parent(andesite_alloy)
				.criterion("0", AllTriggers.CUCKOO.instance())
				.build(t, id + ":cuckoo");

		Advancement windmill =
			advancement("windmill", AllBlocks.WINDMILL_BEARING.get(), TaskType.NORMAL).parent(andesite_alloy)
				.criterion("0", AllTriggers.WINDMILL.instance())
				.build(t, id + ":windmill");

		Advancement maxed_windmill =
			advancement("maxed_windmill", AllBlocks.WINDMILL_BEARING.get(), TaskType.GOAL).parent(windmill)
				.criterion("0", AllTriggers.MAXED_WINDMILL.instance())
				.build(t, id + ":maxed_windmill");

		Advancement andesite_casing =
			advancement("andesite_casing", AllBlocks.ANDESITE_CASING.get(), TaskType.GOAL).parent(andesite_alloy)
				.criterion("0", itemGathered(AllBlocks.ANDESITE_CASING.get()))
				.build(t, id + ":andesite_casing");

		Advancement drill = kinecticAdvancement("mechanical_drill", AllBlocks.MECHANICAL_DRILL.get(), TaskType.NORMAL)
			.parent(andesite_casing)
			.build(t, id + ":mechanical_drill");

		Advancement press =
			advancement("press", AllBlocks.MECHANICAL_PRESS.get(), TaskType.MILESTONE).parent(andesite_casing)
				.criterion("0", AllTriggers.BONK.instance())
				.build(t, id + ":press");

		Advancement fan = advancement("fan", AllBlocks.ENCASED_FAN.get(), TaskType.NORMAL).parent(press)
			.criterion("0", AllTriggers.FAN.instance())
			.build(t, id + ":fan");

		Advancement fan_lava = advancement("fan_lava", AliasedBlockItem.lM, TaskType.NORMAL).parent(fan)
			.criterion("0", AllTriggers.FAN_LAVA.instance())
			.build(t, id + ":fan_lava");

		Advancement fan_smoke = advancement("fan_smoke", AliasedBlockItem.rn, TaskType.NORMAL).parent(fan)
			.criterion("0", AllTriggers.FAN_SMOKE.instance())
			.build(t, id + ":fan_smoke");

		Advancement fan_water = advancement("fan_water", AliasedBlockItem.lL, TaskType.NORMAL).parent(fan)
			.criterion("0", AllTriggers.FAN_WATER.instance())
			.build(t, id + ":fan_water");

		Advancement rose_quartz =
			itemAdvancement("polished_rose_quartz", AllItems.POLISHED_ROSE_QUARTZ, TaskType.NORMAL)
				.parent(andesite_casing)
				.build(t, id + ":polished_rose_quartz");

		Advancement electron_tube =
			itemAdvancement("electron_tube", AllItems.ELECTRON_TUBE, TaskType.NORMAL).parent(rose_quartz)
				.build(t, id + ":electron_tube");

		Advancement saw =
			kinecticAdvancement("mechanical_saw", AllBlocks.MECHANICAL_SAW.get(), TaskType.NORMAL).parent(press)
				.build(t, id + ":mechanical_saw");

		Advancement basin = advancement("basin", AllBlocks.BASIN.get(), TaskType.NORMAL).parent(press)
			.criterion("0", placeBlock(AllBlocks.BASIN.get()))
			.criterion("1", AllTriggers.BASIN_THROW.instance())
			.build(t, id + ":basin");

		Advancement mixer = advancement("mixer", AllBlocks.MECHANICAL_MIXER.get(), TaskType.MILESTONE)
			.criterion("0", placeBlock(AllBlocks.MECHANICAL_MIXER.get()))
			.criterion("1", isPowered(AllBlocks.MECHANICAL_MIXER.get()))
			.criterion("2", AllTriggers.MIXER_MIX.instance())
			.parent(basin)
			.build(t, id + ":mixer");

		Advancement compact = advancement("compact", BellBlock.bF, TaskType.NORMAL)
			.criterion("0", AllTriggers.PRESS_COMPACT.instance())
			.parent(basin)
			.build(t, id + ":compact");

		Advancement blaze_burner =
			itemAdvancement("blaze_burner", AllBlocks.BLAZE_BURNER, TaskType.NORMAL).parent(mixer)
				.build(t, id + ":blaze_burner");

		Advancement brass = itemAdvancement("brass", AllItems.BRASS_INGOT, TaskType.NORMAL).parent(blaze_burner)
			.build(t, id + ":brass");

		brassAge(t, brass);
		copperAge(t, press);
	}

	void kineticsBranch(Consumer<Advancement> t, Advancement root) {
		String id = Create.ID;

		Advancement its_alive = advancement("its_alive", AllBlocks.COGWHEEL.get(), TaskType.NORMAL).parent(root)
			.criterion("0", AllTriggers.ROTATION.instance())
			.build(t, id + ":its_alive");

		Advancement belt = advancement("belt", AllItems.BELT_CONNECTOR.get(), TaskType.NORMAL).parent(its_alive)
			.criterion("0", AllTriggers.CONNECT_BELT.instance())
			.build(t, id + ":belt");

		Advancement tunnel = advancement("tunnel", AllBlocks.ANDESITE_TUNNEL.get(), TaskType.NORMAL).parent(belt)
			.criterion("0", AllTriggers.PLACE_TUNNEL.instance())
			.build(t, id + ":tunnel");

		Advancement splitter_tunnel =
			advancement("splitter_tunnel", AllBlocks.BRASS_TUNNEL.get(), TaskType.MILESTONE).parent(tunnel)
				.criterion("0", AllTriggers.CONNECT_TUNNEL.instance())
				.build(t, id + ":splitter_tunnel");

		Advancement chute = advancement("chute", AllBlocks.CHUTE.get(), TaskType.NORMAL).parent(belt)
			.criterion("0", placeBlock(AllBlocks.CHUTE.get()))
			.build(t, id + ":chute");

		Advancement upward_chute =
			advancement("upward_chute", AllBlocks.ENCASED_FAN.get(), TaskType.NORMAL).parent(chute)
				.criterion("0", AllTriggers.UPWARD_CHUTE.instance())
				.build(t, id + ":upward_chute");

		Advancement belt_funnel =
			advancement("belt_funnel", AllBlocks.ANDESITE_FUNNEL.get(), TaskType.NORMAL).parent(belt)
				.criterion("0", AllTriggers.BELT_FUNNEL.instance())
				.build(t, id + ":belt_funnel");

		Advancement belt_funnel_kiss =
			advancement("belt_funnel_kiss", AllBlocks.BRASS_FUNNEL.get(), TaskType.SECRET).parent(belt_funnel)
				.criterion("0", AllTriggers.BELT_FUNNEL_KISS.instance())
				.build(t, id + ":belt_funnel_kiss");

		Advancement wrench = itemAdvancement("wrench", AllItems.WRENCH, TaskType.NORMAL).parent(its_alive)
			.build(t, id + ":wrench");

		Advancement goggles = itemAdvancement("goggles", AllItems.GOGGLES, TaskType.NORMAL).parent(its_alive)
			.build(t, id + ":goggles");

		Advancement speed_gauge =
			kinecticAdvancement("speedometer", AllBlocks.SPEEDOMETER.get(), TaskType.NORMAL).parent(goggles)
				.build(t, id + ":speedometer");

		Advancement stress_gauge =
			kinecticAdvancement("stressometer", AllBlocks.STRESSOMETER.get(), TaskType.NORMAL).parent(goggles)
				.build(t, id + ":stressometer");

		Advancement shifting_gears =
			advancement("shifting_gears", AllBlocks.LARGE_COGWHEEL.get(), TaskType.NORMAL).parent(its_alive)
				.criterion("0", AllTriggers.SHIFTING_GEARS.instance())
				.build(t, id + ":shifting_gears");

		Advancement overstressed = advancement("overstressed", AliasedBlockItem.fJ, TaskType.SECRET).parent(its_alive)
			.criterion("0", AllTriggers.OVERSTRESSED.instance())
			.build(t, id + ":overstressed");

	}

	void copperAge(Consumer<Advancement> t, Advancement root) {
		String id = Create.ID;

		Advancement copper_casing =
			advancement("copper_casing", AllBlocks.COPPER_CASING.get(), TaskType.GOAL).parent(root)
				.criterion("0", itemGathered(AllBlocks.COPPER_CASING.get()))
				.build(t, id + ":copper_casing");

		Advancement item_drain =
			advancement("item_drain", AllBlocks.ITEM_DRAIN.get(), TaskType.NORMAL).parent(copper_casing)
				.criterion("0", AllTriggers.ITEM_DRAIN.instance())
				.build(t, id + ":item_drain");

		Advancement chained_item_drain =
			advancement("chained_item_drain", AllBlocks.ITEM_DRAIN.get(), TaskType.SECRET).parent(item_drain)
				.criterion("0", AllTriggers.CHAINED_ITEM_DRAIN.instance())
				.build(t, id + ":chained_item_drain");

		Advancement spout = advancement("spout", AllBlocks.SPOUT.get(), TaskType.NORMAL).parent(copper_casing)
			.criterion("0", AllTriggers.SPOUT.instance())
			.build(t, id + ":spout");

		Advancement spout_potion = advancement("spout_potion", AliasedBlockItem.nv, TaskType.GOAL).parent(spout)
			.criterion("0", AllTriggers.SPOUT_POTION.instance())
			.build(t, id + ":spout_potion");

		Advancement chocolate = itemAdvancement("chocolate", () -> AllFluids.CHOCOLATE.get()
			.a(), TaskType.GOAL).parent(spout)
				.build(t, id + ":chocolate");

		Advancement glass_pipe =
			advancement("glass_pipe", AllBlocks.FLUID_PIPE.get(), TaskType.NORMAL).parent(copper_casing)
				.criterion("0", AllTriggers.GLASS_PIPE.instance())
				.build(t, id + ":glass_pipe");

		Advancement pipe_collision =
			advancement("pipe_collision", AllBlocks.FLUID_VALVE.get(), TaskType.NORMAL).parent(glass_pipe)
				.criterion("0", AllTriggers.PIPE_COLLISION.instance())
				.build(t, id + ":pipe_collision");

		Advancement pipe_spill = advancement("pipe_spill", AliasedBlockItem.lK, TaskType.NORMAL).parent(glass_pipe)
			.criterion("0", AllTriggers.PIPE_SPILL.instance())
			.build(t, id + ":pipe_spill");

		Advancement hose_pulley =
			advancement("hose_pulley", AllBlocks.HOSE_PULLEY.get(), TaskType.NORMAL).parent(pipe_spill)
				.criterion("0", AllTriggers.HOSE_PULLEY.instance())
				.build(t, id + ":hose_pulley");

		Advancement infinite_water =
			advancement("infinite_water", AliasedBlockItem.lL, TaskType.NORMAL).parent(hose_pulley)
				.criterion("0", AllTriggers.INFINITE_WATER.instance())
				.build(t, id + ":infinite_water");

		Advancement infinite_lava =
			advancement("infinite_lava", AliasedBlockItem.lM, TaskType.GOAL).parent(hose_pulley)
				.criterion("0", AllTriggers.INFINITE_LAVA.instance())
				.build(t, id + ":infinite_lava");

		Advancement infinite_chocolate = advancement("infinite_chocolate", AllFluids.CHOCOLATE.get()
			.a(), TaskType.CHALLENGE).parent(hose_pulley)
				.criterion("0", AllTriggers.INFINITE_CHOCOLATE.instance())
				.build(t, id + ":infinite_chocolate");
	}

	void brassAge(Consumer<Advancement> t, Advancement root) {
		String id = Create.ID;

		Advancement brass_casing =
			advancement("brass_casing", AllBlocks.BRASS_CASING.get(), TaskType.GOAL).parent(root)
				.criterion("0", itemGathered(AllBlocks.BRASS_CASING.get()))
				.build(t, id + ":brass_casing");

		Advancement nixie_tube =
			advancement("nixie_tube", AllBlocks.NIXIE_TUBE.get(), TaskType.NORMAL).parent(brass_casing)
				.criterion("0", placeBlock(AllBlocks.NIXIE_TUBE.get()))
				.build(t, id + ":nixie_tube");

		Advancement crafter = kinecticAdvancement("crafter", AllBlocks.MECHANICAL_CRAFTER.get(), TaskType.MILESTONE)
			.parent(brass_casing)
			.build(t, id + ":crafter");

		Advancement flywheel = advancement("flywheel", AllBlocks.FLYWHEEL.get(), TaskType.NORMAL).parent(crafter)
			.criterion("0", AllTriggers.FLYWHEEL.instance())
			.build(t, id + ":flywheel");

		Advancement overstress_flywheel =
			advancement("overstress_flywheel", AllBlocks.FURNACE_ENGINE.get(), TaskType.CHALLENGE).parent(flywheel)
				.criterion("0", AllTriggers.OVERSTRESS_FLYWHEEL.instance())
				.build(t, id + ":overstress_flywheel");

		Advancement integrated_circuit =
			itemAdvancement("integrated_circuit", AllItems.INTEGRATED_CIRCUIT, TaskType.NORMAL).parent(crafter)
				.build(t, id + ":integrated_circuit");

		Advancement integrated_circuit_eob = deadEnd().parent(integrated_circuit)
			.criterion("0", itemGathered(AllItems.INTEGRATED_CIRCUIT.get()))
			.build(t, id + ":integrated_circuit_eob");

		Advancement speed_controller =
			kinecticAdvancement("speed_controller", AllBlocks.ROTATION_SPEED_CONTROLLER.get(), TaskType.NORMAL)
				.parent(integrated_circuit)
				.build(t, id + ":speed_controller");

		Advancement clockwork_bearing =
			advancement("clockwork_bearing", AllBlocks.CLOCKWORK_BEARING.get(), TaskType.NORMAL)
				.parent(brass_casing)
				.criterion("0", AllTriggers.CLOCKWORK_BEARING.instance())
				.build(t, id + ":clockwork_bearing");

		Advancement extendo_grip =
			advancement("extendo_grip", AllItems.EXTENDO_GRIP.get(), TaskType.NORMAL).parent(crafter)
				.criterion("0", AllTriggers.EXTENDO.instance())
				.build(t, id + ":extendo_grip");

		Advancement dual_extendo_grip =
			advancement("dual_extendo_grip", AllItems.EXTENDO_GRIP.get(), TaskType.SECRET).parent(extendo_grip)
				.criterion("0", AllTriggers.GIGA_EXTENDO.instance())
				.build(t, id + ":dual_extendo_grip");

		Advancement mechanical_arm = advancement("mechanical_arm", AllBlocks.MECHANICAL_ARM.get(), TaskType.MILESTONE)
			.criterion("0", AllTriggers.MECHANICAL_ARM.instance())
			.parent(brass_casing)
			.build(t, id + ":mechanical_arm");

		Advancement musical_arm = advancement("musical_arm", AliasedBlockItem.qz, TaskType.MILESTONE)
			.criterion("0", AllTriggers.MUSICAL_ARM.instance())
			.parent(mechanical_arm)
			.build(t, id + ":musical_arm");

		Advancement arm_many_targets = advancement("arm_many_targets", AllBlocks.BRASS_FUNNEL.get(), TaskType.MILESTONE)
			.criterion("0", AllTriggers.ARM_MANY_TARGETS.instance())
			.parent(mechanical_arm)
			.build(t, id + ":arm_many_targets");

		Advancement arm_blaze_burner = advancement("arm_blaze_burner", AllBlocks.BLAZE_BURNER.get(), TaskType.NORMAL)
			.criterion("0", AllTriggers.ARM_BLAZE_BURNER.instance())
			.parent(mechanical_arm)
			.build(t, id + ":arm_blaze_burner");

		Advancement deployer =
			kinecticAdvancement("deployer", AllBlocks.DEPLOYER.get(), TaskType.MILESTONE).parent(brass_casing)
				.build(t, id + ":deployer");

		Advancement fist_bump = advancement("fist_bump", AllBlocks.DEPLOYER.get(), TaskType.SECRET).parent(deployer)
			.criterion("0", AllTriggers.DEPLOYER_BOOP.instance())
			.build(t, id + ":fist_bump");

		Advancement crushing_wheel =
			advancement("crushing_wheel", AllBlocks.CRUSHING_WHEEL.get(), TaskType.MILESTONE).parent(crafter)
				.criterion("0", itemGathered(AllBlocks.CRUSHING_WHEEL.get()))
				.build(t, id + ":crushing_wheel");

		Advancement blaze_cake =
			itemAdvancement("blaze_cake", AllItems.BLAZE_CAKE, TaskType.NORMAL).parent(crushing_wheel)
				.build(t, id + ":blaze_cake");

		Advancement chromatic_compound =
			itemAdvancement("chromatic_compound", AllItems.CHROMATIC_COMPOUND, TaskType.NORMAL).parent(blaze_cake)
				.build(t, id + ":chromatic_compound");

		Advancement shadow_steel =
			itemAdvancement("shadow_steel", AllItems.SHADOW_STEEL, TaskType.GOAL).parent(chromatic_compound)
				.build(t, id + ":shadow_steel");

		Advancement refined_radiance =
			itemAdvancement("refined_radiance", AllItems.REFINED_RADIANCE, TaskType.GOAL).parent(chromatic_compound)
				.build(t, id + ":refined_radiance");

		Advancement chromatic_age = advancement("chromatic_age", AllBlocks.REFINED_RADIANCE_CASING.get(), TaskType.GOAL)
			.parent(chromatic_compound)
			.criterion("0", itemGathered(AllBlocks.SHADOW_STEEL_CASING.get()))
			.criterion("1", itemGathered(AllBlocks.REFINED_RADIANCE_CASING.get()))
			.build(t, id + "chromatic_age");

		Advancement chromatic_eob = deadEnd().parent(chromatic_age)
			.criterion("0", itemGathered(AllBlocks.SHADOW_STEEL_CASING.get()))
			.criterion("1", itemGathered(AllBlocks.REFINED_RADIANCE_CASING.get()))
			.build(t, id + ":chromatic_eob");

		Advancement deforester =
			itemAdvancement("deforester", AllItems.DEFORESTER, TaskType.NORMAL).parent(refined_radiance)
				.build(t, id + ":deforester");

		Advancement zapper =
			itemAdvancement("zapper", AllItems.BLOCKZAPPER, TaskType.NORMAL).parent(refined_radiance)
				.build(t, id + ":zapper");

		ItemCooldownManager gunWithPurpurStuff = AllItems.BLOCKZAPPER.asStack();
		for (Components c : Components.values())
			BlockzapperItem.setTier(c, ComponentTier.Chromatic, gunWithPurpurStuff);
		Advancement upgraded_zapper = advancement("upgraded_zapper", gunWithPurpurStuff, TaskType.CHALLENGE)
			.criterion("0", AllTriggers.UPGRADED_ZAPPER.instance())
			.parent(zapper)
			.build(t, id + ":upgraded_zapper");

		Advancement symmetry_wand =
			itemAdvancement("wand_of_symmetry", AllItems.WAND_OF_SYMMETRY, TaskType.NORMAL).parent(refined_radiance)
				.build(t, id + ":wand_of_symmetry");

	}

	// Datagen

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting()
		.create();
	private final DataGenerator generator;

	public AllAdvancements(DataGenerator generatorIn) {
		this.generator = generatorIn;
	}

	@Override
	public void run(DataCache cache) throws IOException {
		Path path = this.generator.getOutput();
		Set<Identifier> set = Sets.newHashSet();
		Consumer<Advancement> consumer = (p_204017_3_) -> {
			if (!set.add(p_204017_3_.getId()))
				throw new IllegalStateException("Duplicate advancement " + p_204017_3_.getId());

			Path path1 = getPath(path, p_204017_3_);

			try {
				DataProvider.writeToPath(GSON, cache, p_204017_3_.createTask()
					.toJson(), path1);
			} catch (IOException ioexception) {
				LOGGER.error("Couldn't save advancement {}", path1, ioexception);
			}
		};

		register(consumer);
	}

	private static Path getPath(Path pathIn, Advancement advancementIn) {
		return pathIn.resolve("data_unused/" + advancementIn.getId()
			.getNamespace() + "/advancements/"
			+ advancementIn.getId()
				.getPath()
			+ ".json");
	}

	@Override
	public String getName() {
		return "KineticAPI's Advancements";
	}

	public PlacedBlockCriterion.Conditions placeBlock(BeetrootsBlock block) {
		return PlacedBlockCriterion.Conditions.a(block);
	}

	public KineticBlockTrigger.Instance isPowered(BeetrootsBlock block) {
		return AllTriggers.KINETIC_BLOCK.forBlock(block);
	}

	public InventoryChangedCriterion.Conditions itemGathered(GameRules itemprovider) {
		return InventoryChangedCriterion.Conditions.a(itemprovider);
	}

	static enum TaskType {

		NORMAL(AdvancementFrame.TASK, true, false, false),
		MILESTONE(AdvancementFrame.TASK, true, true, false),
		GOAL(AdvancementFrame.GOAL, true, true, false),
		SECRET(AdvancementFrame.GOAL, true, true, true),
		SILENT_GATE(AdvancementFrame.CHALLENGE, false, false, false),
		CHALLENGE(AdvancementFrame.CHALLENGE, true, true, false),

		;

		private AdvancementFrame frame;
		private boolean toast;
		private boolean announce;
		private boolean hide;

		private TaskType(AdvancementFrame frame, boolean toast, boolean announce, boolean hide) {
			this.frame = frame;
			this.toast = toast;
			this.announce = announce;
			this.hide = hide;
		}
	}

	public Task kinecticAdvancement(String name, BeetrootsBlock block, TaskType type) {
		return advancement(name, block, type).criterion("0", placeBlock(block));
//			.withCriterion("1", isPowered(block)); Duplicate toast
	}

	public Task advancement(String name, GameRules icon, TaskType type) {
		return advancement(name, new ItemCooldownManager(icon), type);
	}

	public Task deadEnd() {
		return advancement("eob", AliasedBlockItem.x, TaskType.SILENT_GATE);
	}

	public Task advancement(String name, ItemCooldownManager icon, TaskType type) {
		return Advancement.Task.create()
			.a(icon, new TranslatableText(LANG + name),
				new TranslatableText(LANG + name + ".desc"), null, type.frame, type.toast, type.announce,
				type.hide);
	}

	public Task itemAdvancement(String name, Supplier<? extends GameRules> item, TaskType type) {
		return advancement(name, item.get(), type).criterion("0", itemGathered(item.get()));
	}

}
