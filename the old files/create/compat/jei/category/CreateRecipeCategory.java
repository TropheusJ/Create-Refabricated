package com.simibubi.kinetic_api.compat.jei.category;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.simibubi.kinetic_api.AllFluids;
import com.simibubi.kinetic_api.Create;
import com.simibubi.kinetic_api.compat.jei.DoubleItemIcon;
import com.simibubi.kinetic_api.compat.jei.EmptyBackground;
import com.simibubi.kinetic_api.content.contraptions.fluids.potion.PotionFluidHandler;
import com.simibubi.kinetic_api.content.contraptions.processing.ProcessingOutput;
import com.simibubi.kinetic_api.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.kinetic_api.foundation.fluid.FluidIngredient;
import com.simibubi.kinetic_api.foundation.gui.AllGuiTextures;
import com.simibubi.kinetic_api.foundation.utility.Lang;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraftforge.fluids.FluidStack;

public abstract class CreateRecipeCategory<T extends Ingredient<?>> implements IRecipeCategory<T> {

	public List<Supplier<? extends Object>> recipeCatalysts = new ArrayList<>();
	public List<Supplier<List<? extends Ingredient<?>>>> recipes = new ArrayList<>();
	public Identifier uid;

	protected String name;
	private IDrawable icon;
	private IDrawable background;

	public CreateRecipeCategory(IDrawable icon, IDrawable background) {
		this.background = background;
		this.icon = icon;
	}

	public void setCategoryId(String name) {
		this.uid = new Identifier(Create.ID, name);
		this.name = name;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public Identifier getUid() {
		return uid;
	}

	@Override
	public String getTitle() {
		return Lang.translate("recipe." + name)
			.getString();
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	protected static AllGuiTextures getRenderedSlot(Ingredient<?> recipe, int index) {
		AllGuiTextures jeiSlot = AllGuiTextures.JEI_SLOT;
		if (!(recipe instanceof ProcessingRecipe))
			return jeiSlot;
		ProcessingRecipe<?> processingRecipe = (ProcessingRecipe<?>) recipe;
		List<ProcessingOutput> rollableResults = processingRecipe.getRollableResults();
		if (rollableResults.size() <= index)
			return jeiSlot;
		if (processingRecipe.getRollableResults()
			.get(index)
			.getChance() == 1)
			return jeiSlot;
		return AllGuiTextures.JEI_CHANCE_SLOT;
	}

	protected static IDrawable emptyBackground(int width, int height) {
		return new EmptyBackground(width, height);
	}

	protected static IDrawable doubleItemIcon(GameRules item1, GameRules item2) {
		return new DoubleItemIcon(() -> new ItemCooldownManager(item1), () -> new ItemCooldownManager(item2));
	}

	protected static IDrawable itemIcon(GameRules item) {
		return new DoubleItemIcon(() -> new ItemCooldownManager(item), () -> ItemCooldownManager.tick);
	}

	protected static void addStochasticTooltip(IGuiItemStackGroup itemStacks, List<ProcessingOutput> results) {
		itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
			if (input)
				return;
			ProcessingOutput output = results.get(slotIndex - 1);
			float chance = output.getChance();
			if (chance != 1)
				tooltip.add(1, Lang.translate("recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
					.formatted(Formatting.GOLD));
		});
	}
	
	public List<FluidStack> withImprovedVisibility(List<FluidStack> stacks) {
		return stacks.stream()
			.map(this::withImprovedVisibility)
			.collect(Collectors.toList());
	}

	public FluidStack withImprovedVisibility(FluidStack stack) {
		FluidStack display = stack.copy();
		int displayedAmount = (int) (stack.getAmount() * .75f) + 250;
		display.setAmount(displayedAmount);
		return display;
	}

	protected static void addFluidTooltip(IGuiFluidStackGroup fluidStacks, List<FluidIngredient> inputs,
		List<FluidStack> outputs) {
		List<Integer> amounts = new ArrayList<>();
		inputs.forEach(f -> amounts.add(f.getRequiredAmount()));
		outputs.forEach(f -> amounts.add(f.getAmount()));

		fluidStacks.addTooltipCallback((slotIndex, input, fluid, tooltip) -> {
			if (fluid.getFluid()
				.a(AllFluids.POTION.get())) {
				Text name = PotionFluidHandler.getPotionName(fluid);
				if (tooltip.isEmpty())
					tooltip.add(0, name);
				else
					tooltip.set(0, name);

				ArrayList<Text> potionTooltip = new ArrayList<>();
				PotionFluidHandler.addPotionTooltip(fluid, potionTooltip, 1);
				tooltip.addAll(1, potionTooltip.stream()
					.collect(Collectors.toList()));
			}

			int amount = amounts.get(slotIndex);
			Text text = (Lang.translate("generic.unit.millibuckets", amount)).formatted(Formatting.GOLD);
			if (tooltip.isEmpty())
				tooltip.add(0, text);
			else {
				List<Text> siblings = tooltip.get(0).getSiblings();
				siblings.add(new LiteralText(" "));
				siblings.add(text);
			}
		});
	}

}
