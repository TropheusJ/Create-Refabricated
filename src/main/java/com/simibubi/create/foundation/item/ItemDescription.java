package com.simibubi.create.foundation.item;

import static com.simibubi.create.foundation.item.TooltipHelper.cutStringTextComponent;
import static com.simibubi.create.foundation.item.TooltipHelper.cutTextComponent;
import static net.minecraft.util.Formatting.AQUA;
import static net.minecraft.util.Formatting.BLUE;
import static net.minecraft.util.Formatting.DARK_GRAY;
import static net.minecraft.util.Formatting.DARK_GREEN;
import static net.minecraft.util.Formatting.DARK_PURPLE;
import static net.minecraft.util.Formatting.DARK_RED;
import static net.minecraft.util.Formatting.GOLD;
import static net.minecraft.util.Formatting.GRAY;
import static net.minecraft.util.Formatting.GREEN;
import static net.minecraft.util.Formatting.ITALIC;
import static net.minecraft.util.Formatting.LIGHT_PURPLE;
import static net.minecraft.util.Formatting.RED;
import static net.minecraft.util.Formatting.STRIKETHROUGH;
import static net.minecraft.util.Formatting.WHITE;
import static net.minecraft.util.Formatting.YELLOW;

import aqc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.IRotate.SpeedLevel;
import com.simibubi.create.content.contraptions.base.IRotate.StressImpact;
import com.simibubi.create.content.contraptions.components.fan.EncasedFanBlock;
import com.simibubi.create.content.contraptions.components.flywheel.engine.EngineBlock;
import com.simibubi.create.content.contraptions.components.flywheel.engine.FurnaceEngineBlock;
import com.simibubi.create.content.contraptions.components.waterwheel.WaterWheelBlock;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.CKinetics;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.client.gui.screen.PresetsScreen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class ItemDescription {

	public static final ItemDescription MISSING = new ItemDescription(null);
	public static Text trim =
		new LiteralText("                          ").formatted(WHITE, STRIKETHROUGH);

	public enum Palette {

		Blue(BLUE, AQUA),
		Green(DARK_GREEN, GREEN),
		Yellow(GOLD, YELLOW),
		Red(DARK_RED, RED),
		Purple(DARK_PURPLE, LIGHT_PURPLE),
		Gray(DARK_GRAY, GRAY),

		;

		private Palette(Formatting primary, Formatting highlight) {
			color = primary;
			hColor = highlight;
		}

		public Formatting color;
		public Formatting hColor;
	}

	private List<Text> lines;
	private List<Text> linesOnShift;
	private List<Text> linesOnCtrl;
	private Palette palette;

	public ItemDescription(Palette palette) {
		this.palette = palette;
		lines = new ArrayList<>();
		linesOnShift = new ArrayList<>();
		linesOnCtrl = new ArrayList<>();
	}

	public ItemDescription withSummary(Text summary) {
		addStrings(linesOnShift, cutTextComponent(summary, palette.color, palette.hColor));
		add(linesOnShift, LiteralText.EMPTY);
		return this;
	}

	public ItemDescription withKineticStats(BeetrootsBlock block) {

		boolean isEngine = block instanceof EngineBlock;
		CKinetics config = AllConfigs.SERVER.kinetics;
		SpeedLevel minimumRequiredSpeedLevel =
			isEngine ? SpeedLevel.NONE : ((IRotate) block).getMinimumRequiredSpeedLevel();
		boolean hasSpeedRequirement = minimumRequiredSpeedLevel != SpeedLevel.NONE;
		Identifier id = block.getRegistryName();
		Map<Identifier, ConfigValue<Double>> impacts = config.stressValues.getImpacts();
		Map<Identifier, ConfigValue<Double>> capacities = config.stressValues.getCapacities();
		boolean hasStressImpact = impacts.containsKey(id) && impacts.get(id)
			.get() > 0 && StressImpact.isEnabled();
		boolean hasStressCapacity = capacities.containsKey(id) && StressImpact.isEnabled();
		boolean hasGlasses =
			AllItems.GOGGLES.get() == KeyBinding.B().s.b(aqc.f)
				.b();

		Text rpmUnit = Lang.translate("generic.unit.rpm");
		if (hasSpeedRequirement) {
			List<Text> speedLevels = Lang.translatedOptions("tooltip.speedRequirement", "none", "medium", "high");
			int index = minimumRequiredSpeedLevel.ordinal();
			MutableText level = new LiteralText(makeProgressBar(3, index)).append(speedLevels.get(index)).formatted(minimumRequiredSpeedLevel.getTextColor());

			if (hasGlasses)
				level.append(" (" + minimumRequiredSpeedLevel.getSpeedValue()).append(rpmUnit).append("+)");

			add(linesOnShift, Lang.translate("tooltip.speedRequirement").formatted(GRAY));
			add(linesOnShift, level);
		}

		if (hasStressImpact && !(!isEngine && ((IRotate) block).hideStressImpact())) {
			List<Text> stressLevels = Lang.translatedOptions("tooltip.stressImpact", "low", "medium", "high");
			double impact = impacts.get(id)
				.get();
			StressImpact impactId = impact >= config.highStressImpact.get() ? StressImpact.HIGH
				: (impact >= config.mediumStressImpact.get() ? StressImpact.MEDIUM : StressImpact.LOW);
			int index = impactId.ordinal();
			MutableText level = new LiteralText(makeProgressBar(3, index)).append(stressLevels.get(index)).formatted(impactId.getAbsoluteColor());

			if (hasGlasses)
				level.append(" (" + impacts.get(id).get()).append("x ").append(rpmUnit).append(")");

			add(linesOnShift, Lang.translate("tooltip.stressImpact").formatted(GRAY));
			add(linesOnShift, level);
		}

		if (hasStressCapacity) {
			List<Text> stressCapacityLevels =
				Lang.translatedOptions("tooltip.capacityProvided", "low", "medium", "high");
			double capacity = capacities.get(id)
				.get();
			StressImpact impactId = capacity >= config.highCapacity.get() ? StressImpact.LOW
				: (capacity >= config.mediumCapacity.get() ? StressImpact.MEDIUM : StressImpact.HIGH);
			int index = StressImpact.values().length - 2 - impactId.ordinal();
			MutableText level = new LiteralText(makeProgressBar(3, index)).append(stressCapacityLevels.get(index)).formatted(impactId.getAbsoluteColor());

			if (hasGlasses)
				level.append(" (" + capacity).append("x ").append(rpmUnit).append(")");
			if (!isEngine && ((IRotate) block).showCapacityWithAnnotation())
				level.append(" ").append(Lang.translate("tooltip.capacityProvided.asGenerator").formatted(DARK_GRAY, ITALIC));

			add(linesOnShift, Lang.translate("tooltip.capacityProvided").formatted(GRAY));
			add(linesOnShift, level);

			MutableText genSpeed = generatorSpeed(block, rpmUnit);
			if (!genSpeed.asString().equals("")) {
				add(linesOnShift, new LiteralText(" ").append(genSpeed).formatted(GREEN));
			}
		}

		if (hasSpeedRequirement || hasStressImpact || hasStressCapacity)
			add(linesOnShift, LiteralText.EMPTY);
		return this;
	}

	public static String makeProgressBar(int length, int filledLength) {
		String bar = " ";
		int emptySpaces = length - 1 - filledLength;
		for (int i = 0; i <= filledLength; i++)
			bar += "\u2588";
		for (int i = 0; i < emptySpaces; i++)
			bar += "\u2592";
		return bar + " ";
	}

	public ItemDescription withBehaviour(String condition, String behaviour) {
		add(linesOnShift, new LiteralText(condition).formatted(GRAY));
		addStrings(linesOnShift, cutStringTextComponent(behaviour, palette.color, palette.hColor, 1));
		return this;
	}

	public ItemDescription withControl(String condition, String action) {
		add(linesOnCtrl, new LiteralText(condition).formatted(GRAY));
		addStrings(linesOnCtrl, cutStringTextComponent(action, palette.color, palette.hColor, 1));
		return this;
	}

	public ItemDescription createTabs() {
		boolean hasDescription = !linesOnShift.isEmpty();
		boolean hasControls = !linesOnCtrl.isEmpty();

		if (hasDescription || hasControls) {
			String[] holdKey = TooltipHelper.getUnformattedDeepText(Lang.translate("tooltip.holdKey", "$"))
				.split("\\$");
			String[] holdKeyOrKey = TooltipHelper.getUnformattedDeepText(Lang.translate("tooltip.holdKeyOrKey", "$", "$"))
				.split("\\$");
			Text keyShift = Lang.translate("tooltip.keyShift");
			Text keyCtrl = Lang.translate("tooltip.keyCtrl");
			for (List<Text> list : Arrays.asList(lines, linesOnShift, linesOnCtrl)) {
				boolean shift = list == linesOnShift;
				boolean ctrl = list == linesOnCtrl;

				if (holdKey.length != 2 || holdKeyOrKey.length != 3) {
					list.add(0, new LiteralText("Invalid lang formatting!"));
					continue;
				}

				MutableText tabBuilder = LiteralText.EMPTY.copy();
				if (hasDescription && hasControls) {
					tabBuilder.append(holdKeyOrKey[0]);
					tabBuilder.append(keyShift.copy().formatted(shift ? palette.hColor : palette.color));
					tabBuilder.append(holdKeyOrKey[1]);
					tabBuilder.append(keyCtrl.copy().formatted(ctrl ? palette.hColor : palette.color));
					tabBuilder.append(holdKeyOrKey[2]);

				} else {
					tabBuilder.append(holdKey[0]);
					tabBuilder.append((hasDescription ? keyShift : keyCtrl).copy().formatted((hasDescription ? shift : ctrl) ? palette.hColor : palette.color));
					tabBuilder.append(holdKey[1]);
				}
				tabBuilder.formatted(DARK_GRAY);
				list.add(0, tabBuilder);
				if (shift || ctrl)
					list.add(1, LiteralText.EMPTY);
			}
		}

		if (!hasDescription)
			linesOnShift = lines;
		if (!hasControls)
			linesOnCtrl = lines;

		return this;
	}

	public static String hightlight(String s, Palette palette) {
		return palette.hColor + s + palette.color;
	}

	public static void addStrings(List<Text> infoList, List<Text> textLines) {
		textLines.forEach(s -> add(infoList, s));
	}

	public static void add(List<Text> infoList, List<Text> textLines) {
		infoList.addAll(textLines);
	}

	public static void add(List<Text> infoList, Text line) {
		infoList.add(line);
	}

	public Palette getPalette() {
		return palette;
	}

	public List<Text> addInformation(List<Text> tooltip) {
		if (PresetsScreen.y()) {
			tooltip.addAll(linesOnShift);
			return tooltip;
		}

		if (PresetsScreen.x()) {
			tooltip.addAll(linesOnCtrl);
			return tooltip;
		}

		tooltip.addAll(lines);
		return tooltip;
	}

	public List<Text> getLines() {
		return lines;
	}

	public List<Text> getLinesOnCtrl() {
		return linesOnCtrl;
	}

	public List<Text> getLinesOnShift() {
		return linesOnShift;
	}

	private MutableText generatorSpeed(BeetrootsBlock block, Text unitRPM) {
		String value = "";

		if (block instanceof WaterWheelBlock) {
			int baseSpeed = AllConfigs.SERVER.kinetics.waterWheelBaseSpeed.get();
			int speedmod = AllConfigs.SERVER.kinetics.waterWheelFlowSpeed.get();
			value = (speedmod + baseSpeed) + "-" + (baseSpeed + (speedmod * 3));
		}

		else if (block instanceof EncasedFanBlock)
			value = AllConfigs.SERVER.kinetics.generatingFanSpeed.get()
				.toString();

		else if (block instanceof FurnaceEngineBlock) {
			int baseSpeed = AllConfigs.SERVER.kinetics.furnaceEngineSpeed.get();
			value = baseSpeed + "-" + (baseSpeed * 2);
		}

		return !value.equals("") ? Lang.translate("tooltip.generationSpeed", value, unitRPM) : LiteralText.EMPTY.copy();
	}

}
