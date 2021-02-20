package com.simibubi.create.compat.jei.category;

import java.util.Arrays;
import java.util.List;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.processing.ProcessingOutput;
import com.simibubi.create.content.curiosities.tools.SandPaperPolishingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.GuiGameElement;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.collection.DefaultedList;

public class PolishingCategory extends CreateRecipeCategory<SandPaperPolishingRecipe> {

	private ItemStack renderedSandpaper;

	public PolishingCategory() {
		super(itemIcon(AllItems.SAND_PAPER.get()), emptyBackground(177, 55));
		renderedSandpaper = AllItems.SAND_PAPER.asStack();
	}

	@Override
	public Class<? extends SandPaperPolishingRecipe> getRecipeClass() {
		return SandPaperPolishingRecipe.class;
	}

	@Override
	public void setIngredients(SandPaperPolishingRecipe recipe, IIngredients ingredients) {
		ingredients.setInputIngredients(recipe.getPreviewInputs());
		ingredients.setOutputs(VanillaTypes.ITEM, recipe.getRollableResultsAsItemStacks());
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, SandPaperPolishingRecipe recipe, IIngredients ingredients) {
		IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
		List<ProcessingOutput> results = recipe.getRollableResults();

		itemStacks.init(0, true, 26, 28);
		itemStacks.set(0, Arrays.asList(recipe.getPreviewInputs()
			.get(0)
			.getMatchingStacksClient()));
		itemStacks.init(1, false, 131, 28);
		itemStacks.set(1, results.get(0)
			.getStack());

		addStochasticTooltip(itemStacks, results);
	}

	@Override
	public void draw(SandPaperPolishingRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
		matrixStack.push();
		AllGuiTextures.JEI_SLOT.draw(matrixStack, 26, 28);
		getRenderedSlot(recipe, 0).draw(matrixStack, 131, 28);
		AllGuiTextures.JEI_SHADOW.draw(matrixStack, 61, 21);
		AllGuiTextures.JEI_LONG_ARROW.draw(matrixStack, 52, 32);

		DefaultedList<Ingredient> ingredients = recipe.getPreviewInputs();
		ItemStack[] matchingStacks = ingredients.get(0)
			.getMatchingStacksClient();
		if (matchingStacks.length == 0)
			return;


		CompoundTag tag = renderedSandpaper.getOrCreateTag();
		tag.put("Polishing", matchingStacks[0].serializeNBT());
		tag.putBoolean("JEI", true);
		matrixStack.translate(0, 30, 0);
		matrixStack.scale(2, 2, 2);
		matrixStack.translate(getBackground().getWidth() / 4 - 8, 1, 0);
		GuiGameElement.of(renderedSandpaper).render(matrixStack);
		matrixStack.pop();
	}

}
