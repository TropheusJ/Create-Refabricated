package com.simibubi.create.compat.jei.category;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.GuiGameElement;

import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.SmokingRecipe;

public class FanSmokingCategory extends ProcessingViaFanCategory<SmokingRecipe> {

	public FanSmokingCategory() {
		super(doubleItemIcon(AllItems.PROPELLER.get(), Items.BLAZE_POWDER));
	}

	@Override
	public Class<? extends SmokingRecipe> getRecipeClass() {
		return SmokingRecipe.class;
	}

	@Override
	public void renderAttachedBlock(MatrixStack matrixStack) {

		GuiGameElement.of(Blocks.FIRE.getDefaultState())
				.scale(24)
				.atLocal(0, 0, 2)
				.render(matrixStack);

	}
}