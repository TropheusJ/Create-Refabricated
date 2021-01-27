package com.simibubi.create.content.contraptions.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.Lang;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.FireworkRocketRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.MapExtendingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraftforge.fluids.FluidStack;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ProcessingRecipe<T extends BossBar> implements Ingredient<T> {

	protected Identifier id;
	protected DefaultedList<FireworkRocketRecipe> ingredients;
	protected DefaultedList<ProcessingOutput> results;
	protected DefaultedList<FluidIngredient> fluidIngredients;
	protected DefaultedList<FluidStack> fluidResults;
	protected int processingDuration;
	protected HeatCondition requiredHeat;

	private Recipe<?> type;
	private MapExtendingRecipe<?> serializer;
	private AllRecipeTypes enumType;

	public ProcessingRecipe(AllRecipeTypes recipeType, ProcessingRecipeParams params) {

		this.enumType = recipeType;
		this.processingDuration = params.processingDuration;
		this.fluidIngredients = params.fluidIngredients;
		this.fluidResults = params.fluidResults;
		this.serializer = recipeType.serializer;
		this.requiredHeat = params.requiredHeat;
		this.ingredients = params.ingredients;
		this.type = recipeType.type;
		this.results = params.results;
		this.id = params.id;

		validate(Lang.asId(recipeType.name()));
	}

	// Recipe type options:

	protected abstract int getMaxInputCount();

	protected abstract int getMaxOutputCount();

	protected boolean canRequireHeat() {
		return false;
	}

	protected boolean canSpecifyDuration() {
		return true;
	}

	protected int getMaxFluidInputCount() {
		return 0;
	}

	protected int getMaxFluidOutputCount() {
		return 0;
	}

	//

	private void validate(String recipeTypeName) {
		String messageHeader = "Your custom " + recipeTypeName + " recipe (" + id.toString() + ")";
		Logger logger = Create.logger;
		int ingredientCount = ingredients.size();
		int outputCount = results.size();

		if (ingredientCount > getMaxInputCount())
			logger.warn(messageHeader + " has more item inputs (" + ingredientCount + ") than supported ("
				+ getMaxInputCount() + ").");

		if (outputCount > getMaxOutputCount())
			logger.warn(messageHeader + " has more item outputs (" + outputCount + ") than supported ("
				+ getMaxOutputCount() + ").");

		if (processingDuration > 0 && !canSpecifyDuration())
			logger.warn(messageHeader + " specified a duration. Durations have no impact on this type of recipe.");

		if (requiredHeat != HeatCondition.NONE && !canRequireHeat())
			logger.warn(
				messageHeader + " specified a heat condition. Heat conditions have no impact on this type of recipe.");

		ingredientCount = fluidIngredients.size();
		outputCount = fluidResults.size();

		if (ingredientCount > getMaxFluidInputCount())
			logger.warn(messageHeader + " has more fluid inputs (" + ingredientCount + ") than supported ("
				+ getMaxFluidInputCount() + ").");

		if (outputCount > getMaxFluidOutputCount())
			logger.warn(messageHeader + " has more fluid outputs (" + outputCount + ") than supported ("
				+ getMaxFluidOutputCount() + ").");
	}

	@Override
	public DefaultedList<FireworkRocketRecipe> a() {
		return ingredients;
	}
	
	public DefaultedList<FluidIngredient> getFluidIngredients() {
		return fluidIngredients;
	}
	
	public DefaultedList<ProcessingOutput> getRollableResults() {
		return results;
	}
	
	public DefaultedList<FluidStack> getFluidResults() {
		return fluidResults;
	}

	public List<ItemCooldownManager> getRollableResultsAsItemStacks() {
		return getRollableResults().stream()
			.map(ProcessingOutput::getStack)
			.collect(Collectors.toList());
	}

	public List<ItemCooldownManager> rollResults() {
		List<ItemCooldownManager> results = new ArrayList<>();
		for (ProcessingOutput output : getRollableResults()) {
			ItemCooldownManager stack = output.rollOutput();
			if (!stack.a())
				results.add(stack);
		}
		return results;
	}

	public int getProcessingDuration() {
		return processingDuration;
	}

	public HeatCondition getRequiredHeat() {
		return requiredHeat;
	}
	
	// IRecipe<> paperwork
	
	@Override
	public ItemCooldownManager a(T inv) {
		return c();
	}

	@Override
	public boolean a(int width, int height) {
		return true;
	}

	@Override
	public ItemCooldownManager c() {
		return getRollableResults().isEmpty() ? ItemCooldownManager.tick
			: getRollableResults().get(0)
				.getStack();
	}

	@Override
	public Identifier f() {
		return id;
	}

	@Override
	public MapExtendingRecipe<?> ag_() {
		return serializer;
	}

	// Processing recipes do not show up in the recipe book
	@Override
	public String d() {
		return "processing";
	}

	@Override
	public Recipe<?> g() {
		return type;
	}

	// Additional Data added by subtypes

	public void readAdditional(JsonObject json) {}

	public void readAdditional(PacketByteBuf buffer) {}

	public void writeAdditional(JsonObject json) {}

	public void writeAdditional(PacketByteBuf buffer) {}

	public AllRecipeTypes getEnumType() {
		return enumType;
	}

}
