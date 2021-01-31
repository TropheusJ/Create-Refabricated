package com.simibubi.kinetic_api.foundation.data.recipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.simibubi.kinetic_api.AllRecipeTypes;
import com.simibubi.kinetic_api.Create;
import com.simibubi.kinetic_api.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.kinetic_api.content.contraptions.processing.ProcessingRecipeBuilder;
import com.simibubi.kinetic_api.content.contraptions.processing.ProcessingRecipeSerializer;
import net.minecraft.data.DataCache;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.recipe.FireworkRocketRecipe;
import net.minecraft.world.GameRules;
import net.minecraftforge.fluids.FluidAttributes;

public abstract class ProcessingRecipeGen extends CreateRecipeProvider {

	protected static List<ProcessingRecipeGen> generators = new ArrayList<>();
	protected static final int BUCKET = FluidAttributes.BUCKET_VOLUME;
	protected static final int BOTTLE = 250;

	public static void registerAll(DataGenerator gen) {
		generators.add(new CrushingRecipeGen(gen));
		generators.add(new MillingRecipeGen(gen));
		generators.add(new CuttingRecipeGen(gen));
		generators.add(new WashingRecipeGen(gen));
		generators.add(new PolishingRecipeGen(gen));
		generators.add(new MixingRecipeGen(gen));
		generators.add(new CompactingRecipeGen(gen));
		generators.add(new PressingRecipeGen(gen));
		generators.add(new FillingRecipeGen(gen));
		generators.add(new EmptyingRecipeGen(gen));
		
		gen.install(new DataProvider() {
			
			@Override
			public String getName() {
				return "KineticAPI's Processing Recipes";
			}
			
			@Override
			public void run(DataCache dc) throws IOException {
				generators.forEach(g -> {
					try {
						g.run(dc);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		});
	}
	
	public ProcessingRecipeGen(DataGenerator p_i48262_1_) {
		super(p_i48262_1_);
	}
	
	/**
	 * KineticAPI a processing recipe with a single itemstack ingredient, using its id
	 * as the name of the recipe
	 */
	protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(Supplier<GameRules> singleIngredient,
		UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		ProcessingRecipeSerializer<T> serializer = getSerializer();
		GeneratedRecipe generatedRecipe = c -> {
			GameRules iItemProvider = singleIngredient.get();
			transform
				.apply(new ProcessingRecipeBuilder<>(serializer.getFactory(), Create.asResource(iItemProvider.h()
					.getRegistryName()
					.getPath())).withItemIngredients(FireworkRocketRecipe.a(iItemProvider)))
				.build(c);
		};
		all.add(generatedRecipe);
		return generatedRecipe;
	}

	/**
	 * KineticAPI a new processing recipe, with recipe definitions provided by the
	 * function
	 */
	protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(String name,
		UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		ProcessingRecipeSerializer<T> serializer = getSerializer();
		GeneratedRecipe generatedRecipe =
			c -> transform.apply(new ProcessingRecipeBuilder<>(serializer.getFactory(), Create.asResource(name)))
				.build(c);
		all.add(generatedRecipe);
		return generatedRecipe;
	}

	@SuppressWarnings("unchecked")
	private <T extends ProcessingRecipe<?>> ProcessingRecipeSerializer<T> getSerializer() {
		ProcessingRecipeSerializer<T> serializer = (ProcessingRecipeSerializer<T>) getRecipeType().serializer;
		return serializer;
	}

	@Override
	public final String getName() {
		return "KineticAPI's Processing Recipes: " + getRecipeType();
	}
	
	protected abstract AllRecipeTypes getRecipeType();

}