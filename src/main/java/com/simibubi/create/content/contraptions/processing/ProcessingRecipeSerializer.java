package com.simibubi.create.content.contraptions.processing;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.FireworkRocketRecipe;
import net.minecraft.recipe.MapExtendingRecipe;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistryEntry;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ProcessingRecipeSerializer<T extends ProcessingRecipe<?>> extends ForgeRegistryEntry<MapExtendingRecipe<?>>
	implements MapExtendingRecipe<T> {

	private final ProcessingRecipeFactory<T> factory;

	public ProcessingRecipeSerializer(ProcessingRecipeFactory<T> factory) {
		this.factory = factory;
	}

	protected void writeToJson(JsonObject json, T recipe) {
		JsonArray jsonIngredients = new JsonArray();
		JsonArray jsonOutputs = new JsonArray();

		recipe.a()
			.forEach(i -> jsonIngredients.add(i.c()));
		recipe.getFluidIngredients()
			.forEach(i -> jsonIngredients.add(i.serialize()));

		recipe.getRollableResults()
			.forEach(o -> jsonOutputs.add(o.serialize()));
		recipe.getFluidResults()
			.forEach(o -> jsonOutputs.add(FluidHelper.serializeFluidStack(o)));

		json.add("ingredients", jsonIngredients);
		json.add("results", jsonOutputs);

		int processingDuration = recipe.getProcessingDuration();
		if (processingDuration > 0)
			json.addProperty("processingTime", processingDuration);

		HeatCondition requiredHeat = recipe.getRequiredHeat();
		if (requiredHeat != HeatCondition.NONE)
			json.addProperty("heatRequirement", requiredHeat.serialize());

		recipe.writeAdditional(json);
	}

	protected T readFromJson(Identifier recipeId, JsonObject json) {
		ProcessingRecipeBuilder<T> builder = new ProcessingRecipeBuilder<>(factory, recipeId);
		DefaultedList<FireworkRocketRecipe> ingredients = DefaultedList.of();
		DefaultedList<FluidIngredient> fluidIngredients = DefaultedList.of();
		DefaultedList<ProcessingOutput> results = DefaultedList.of();
		DefaultedList<FluidStack> fluidResults = DefaultedList.of();

		for (JsonElement je : OrderedText.u(json, "ingredients")) {
			if (FluidIngredient.isFluidIngredient(je))
				fluidIngredients.add(FluidIngredient.deserialize(je));
			else
				ingredients.add(FireworkRocketRecipe.a(je));
		}

		for (JsonElement je : OrderedText.u(json, "results")) {
			JsonObject jsonObject = je.getAsJsonObject();
			if (OrderedText.g(jsonObject, "fluid"))
				fluidResults.add(FluidHelper.deserializeFluidStack(jsonObject));
			else
				results.add(ProcessingOutput.deserialize(je));
		}

		builder.withItemIngredients(ingredients)
			.withItemOutputs(results)
			.withFluidIngredients(fluidIngredients)
			.withFluidOutputs(fluidResults);

		if (OrderedText.g(json, "processingTime"))
			builder.duration(OrderedText.n(json, "processingTime"));
		if (OrderedText.g(json, "heatRequirement"))
			builder.requiresHeat(HeatCondition.deserialize(OrderedText.h(json, "heatRequirement")));

		return builder.build();
	}

	protected void writeToBuffer(PacketByteBuf buffer, T recipe) {
		DefaultedList<FireworkRocketRecipe> ingredients = recipe.a();
		DefaultedList<FluidIngredient> fluidIngredients = recipe.getFluidIngredients();
		DefaultedList<ProcessingOutput> outputs = recipe.getRollableResults();
		DefaultedList<FluidStack> fluidOutputs = recipe.getFluidResults();

		buffer.writeVarInt(ingredients.size());
		ingredients.forEach(i -> i.a(buffer));
		buffer.writeVarInt(fluidIngredients.size());
		fluidIngredients.forEach(i -> i.write(buffer));

		buffer.writeVarInt(outputs.size());
		outputs.forEach(o -> o.write(buffer));
		buffer.writeVarInt(fluidOutputs.size());
		fluidOutputs.forEach(o -> o.writeToPacket(buffer));

		buffer.writeVarInt(recipe.getProcessingDuration());
		buffer.writeVarInt(recipe.getRequiredHeat()
			.ordinal());
	}

	protected T readFromBuffer(Identifier recipeId, PacketByteBuf buffer) {
		DefaultedList<FireworkRocketRecipe> ingredients = DefaultedList.of();
		DefaultedList<FluidIngredient> fluidIngredients = DefaultedList.of();
		DefaultedList<ProcessingOutput> results = DefaultedList.of();
		DefaultedList<FluidStack> fluidResults = DefaultedList.of();

		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			ingredients.add(FireworkRocketRecipe.b(buffer));
		
		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			fluidIngredients.add(FluidIngredient.read(buffer));
		
		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			results.add(ProcessingOutput.read(buffer));
		
		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			fluidResults.add(FluidStack.readFromPacket(buffer));

		return new ProcessingRecipeBuilder<>(factory, recipeId).withItemIngredients(ingredients)
			.withItemOutputs(results)
			.withFluidIngredients(fluidIngredients)
			.withFluidOutputs(fluidResults)
			.duration(buffer.readVarInt())
			.requiresHeat(HeatCondition.values()[buffer.readVarInt()])
			.build();
	}

	public final void write(JsonObject json, T recipe) {
		writeToJson(json, recipe);
	}

	@Override
	public final T a(Identifier id, JsonObject json) {
		return readFromJson(id, json);
	}

	@Override
	public final void write(PacketByteBuf buffer, T recipe) {
		writeToBuffer(buffer, recipe);
	}

	@Override
	public final T a(Identifier id, PacketByteBuf buffer) {
		return readFromBuffer(id, buffer);
	}

	public ProcessingRecipeFactory<T> getFactory() {
		return factory;
	}

}
