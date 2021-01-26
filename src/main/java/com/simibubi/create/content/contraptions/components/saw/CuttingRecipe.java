package com.simibubi.create.content.contraptions.components.saw;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeParams;
import net.minecraft.world.GameMode;
import net.minecraftforge.items.wrapper.RecipeWrapper;

@ParametersAreNonnullByDefault
public class CuttingRecipe extends ProcessingRecipe<RecipeWrapper> {

	public CuttingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.CUTTING, params);
	}

	@Override
	public boolean matches(RecipeWrapper inv, GameMode worldIn) {
		if (inv.c())
			return false;
		return ingredients.get(0)
			.a(inv.a(0));
	}
	
	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

}
