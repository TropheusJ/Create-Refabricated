package com.simibubi.kinetic_api.compat.jei.category;

import java.util.Arrays;
import java.util.List;
import com.simibubi.kinetic_api.AllBlocks;
import com.simibubi.kinetic_api.AllItems;
import com.simibubi.kinetic_api.compat.jei.category.animations.AnimatedPress;
import com.simibubi.kinetic_api.content.contraptions.components.press.PressingRecipe;
import com.simibubi.kinetic_api.content.contraptions.processing.ProcessingOutput;
import com.simibubi.kinetic_api.foundation.gui.AllGuiTextures;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.render.BufferVertexConsumer;

public class PressingCategory extends CreateRecipeCategory<PressingRecipe> {

	private AnimatedPress press = new AnimatedPress(false);

	public PressingCategory() {
		super(doubleItemIcon(AllBlocks.MECHANICAL_PRESS.get(), AllItems.IRON_SHEET.get()), emptyBackground(177, 70));
	}

	@Override
	public Class<? extends PressingRecipe> getRecipeClass() {
		return PressingRecipe.class;
	}

	@Override
	public void setIngredients(PressingRecipe recipe, IIngredients ingredients) {
		ingredients.setInputIngredients(recipe.a());
		ingredients.setOutputs(VanillaTypes.ITEM, recipe.getRollableResultsAsItemStacks());
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, PressingRecipe recipe, IIngredients ingredients) {
		IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
		itemStacks.init(0, true, 26, 50);
		itemStacks.set(0, Arrays.asList(recipe.a()
			.get(0)
			.a()));

		List<ProcessingOutput> results = recipe.getRollableResults();
		for (int outputIndex = 0; outputIndex < results.size(); outputIndex++) {
			itemStacks.init(outputIndex + 1, false, 131 + 19 * outputIndex, 50);
			itemStacks.set(outputIndex + 1, results.get(outputIndex)
				.getStack());
		}

		addStochasticTooltip(itemStacks, results);
	}

	@Override
	public void draw(PressingRecipe recipe, BufferVertexConsumer matrixStack,  double mouseX, double mouseY) {
		AllGuiTextures.JEI_SLOT.draw(matrixStack, 26, 50);
		getRenderedSlot(recipe, 0).draw(matrixStack, 131, 50);
		if (recipe.getRollableResults()
			.size() > 1)
			getRenderedSlot(recipe, 1).draw(matrixStack, 131 + 19, 50);
		AllGuiTextures.JEI_SHADOW.draw(matrixStack, 61, 41);
		AllGuiTextures.JEI_LONG_ARROW.draw(matrixStack, 52, 54);
		press.draw(matrixStack, getBackground().getWidth() / 2 - 17, 22);
	}

}