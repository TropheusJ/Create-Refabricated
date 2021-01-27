package com.simibubi.create.content.logistics.item.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicates;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.logistics.InWorldProcessing;
import com.simibubi.create.content.logistics.item.filter.attribute.BookAuthorAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.BookCopyAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.EnchantAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.FluidContentsAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemNameAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.astralsorcery.AstralSorceryAmuletAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.astralsorcery.AstralSorceryAttunementAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.astralsorcery.AstralSorceryCrystalAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.astralsorcery.AstralSorceryPerkGemAttribute;
import com.simibubi.create.foundation.utility.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.WallWitherSkullBlock;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.item.HoeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public interface ItemAttribute {

	static List<ItemAttribute> types = new ArrayList<>();

	static ItemAttribute standard = register(StandardTraits.DUMMY);
	static ItemAttribute inTag = register(new InTag(new Identifier("dummy")));
	static ItemAttribute inItemGroup = register(new InItemGroup(ChorusFruitItem.f));
	static ItemAttribute addedBy = register(new InItemGroup.AddedBy("dummy"));
	static ItemAttribute hasEnchant = register(EnchantAttribute.EMPTY);
	static ItemAttribute hasFluid = register(FluidContentsAttribute.EMPTY);
	static ItemAttribute hasName = register(new ItemNameAttribute("dummy"));
	static ItemAttribute astralAmulet = register(new AstralSorceryAmuletAttribute("dummy", -1));
	static ItemAttribute astralAttunement = register(new AstralSorceryAttunementAttribute("dummy"));
	static ItemAttribute astralCrystal = register(new AstralSorceryCrystalAttribute("dummy"));
	static ItemAttribute astralPerkGem = register(new AstralSorceryPerkGemAttribute("dummy"));
	static ItemAttribute bookAuthor = register(new BookAuthorAttribute("dummy"));
	static ItemAttribute bookCopy = register(new BookCopyAttribute(-1));

	static ItemAttribute register(ItemAttribute attributeType) {
		types.add(attributeType);
		return attributeType;
	}

	default boolean appliesTo(ItemCooldownManager stack, GameMode world) {
		return appliesTo(stack);
	}

	boolean appliesTo(ItemCooldownManager stack);

	default List<ItemAttribute> listAttributesOf(ItemCooldownManager stack, GameMode world) {
		return listAttributesOf(stack);
	}

	public List<ItemAttribute> listAttributesOf(ItemCooldownManager stack);

	public String getTranslationKey();

	void writeNBT(CompoundTag nbt);

	ItemAttribute readNBT(CompoundTag nbt);

	public default void serializeNBT(CompoundTag nbt) {
		CompoundTag compound = new CompoundTag();
		writeNBT(compound);
		nbt.put(getNBTKey(), compound);
	}

	public static ItemAttribute fromNBT(CompoundTag nbt) {
		for (ItemAttribute itemAttribute : types) {
			if (!itemAttribute.canRead(nbt))
				continue;
			return itemAttribute.readNBT(nbt.getCompound(itemAttribute.getNBTKey()));
		}
		return null;
	}

	default Object[] getTranslationParameters() {
		return new String[0];
	}

	default boolean canRead(CompoundTag nbt) {
		return nbt.contains(getNBTKey());
	}

	default String getNBTKey() {
		return getTranslationKey();
	}

	@Environment(EnvType.CLIENT)
	default TranslatableText format(boolean inverted) {
		return Lang.translate("item_attributes." + getTranslationKey() + (inverted ? ".inverted" : ""),
			getTranslationParameters());
	}

	public static enum StandardTraits implements ItemAttribute {

		DUMMY(s -> false),
		PLACEABLE(s -> s.b() instanceof BannerItem),
		CONSUMABLE(ItemCooldownManager::F),
		FLUID_CONTAINER(s -> s.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()),
		ENCHANTED(ItemCooldownManager::x),
		RENAMED(ItemCooldownManager::t),
		DAMAGED(ItemCooldownManager::f),
		BADLY_DAMAGED(s -> s.f() && s.g() / s.h() > 3 / 4f),
		NOT_STACKABLE(Predicates.not(ItemCooldownManager::d)),
		EQUIPABLE(s -> s.getEquipmentSlot() != null),
		FURNACE_FUEL(WallWitherSkullBlock::b),
		WASHABLE(InWorldProcessing::isWashable),
		CRUSHABLE((s, w) -> testRecipe(s, w, AllRecipeTypes.CRUSHING.getType())
			|| testRecipe(s, w, AllRecipeTypes.MILLING.getType())),
		SMELTABLE((s, w) -> testRecipe(s, w, Recipe.b)),
		SMOKABLE((s, w) -> testRecipe(s, w, Recipe.d)),
		BLASTABLE((s, w) -> testRecipe(s, w, Recipe.c));

		private static final RecipeWrapper RECIPE_WRAPPER = new RecipeWrapper(new ItemStackHandler(1));
		private Predicate<ItemCooldownManager> test;
		private BiPredicate<ItemCooldownManager, GameMode> testWithWorld;

		private StandardTraits(Predicate<ItemCooldownManager> test) {
			this.test = test;
		}

		private static boolean testRecipe(ItemCooldownManager s, GameMode w, Recipe<? extends Ingredient<BossBar>> type) {
			RECIPE_WRAPPER.a(0, s.i());
			return w.o()
				.a(type, RECIPE_WRAPPER, w)
				.isPresent();
		}

		private StandardTraits(BiPredicate<ItemCooldownManager, GameMode> test) {
			this.testWithWorld = test;
		}

		@Override
		public boolean appliesTo(ItemCooldownManager stack, GameMode world) {
			if (testWithWorld != null)
				return testWithWorld.test(stack, world);
			return appliesTo(stack);
		}

		@Override
		public boolean appliesTo(ItemCooldownManager stack) {
			return test.test(stack);
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemCooldownManager stack, GameMode world) {
			List<ItemAttribute> attributes = new ArrayList<>();
			for (StandardTraits trait : values())
				if (trait.appliesTo(stack, world))
					attributes.add(trait);
			return attributes;
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemCooldownManager stack) {
			return null;
		}

		@Override
		public String getTranslationKey() {
			return Lang.asId(name());
		}

		@Override
		public String getNBTKey() {
			return "standard_trait";
		}

		@Override
		public void writeNBT(CompoundTag nbt) {
			nbt.putBoolean(name(), true);
		}

		@Override
		public ItemAttribute readNBT(CompoundTag nbt) {
			for (StandardTraits trait : values())
				if (nbt.contains(trait.name()))
					return trait;
			return null;
		}

	}

	public static class InTag implements ItemAttribute {

		Identifier tagName;

		public InTag(Identifier tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean appliesTo(ItemCooldownManager stack) {
			return stack.b()
				.getTags()
				.contains(tagName);
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemCooldownManager stack) {
			return stack.b()
				.getTags()
				.stream()
				.map(InTag::new)
				.collect(Collectors.toList());
		}

		@Override
		public String getTranslationKey() {
			return "in_tag";
		}

		@Override
		public Object[] getTranslationParameters() {
			return new Object[] { "#" + tagName.toString() };
		}

		@Override
		public void writeNBT(CompoundTag nbt) {
			nbt.putString("space", tagName.getNamespace());
			nbt.putString("path", tagName.getPath());
		}

		@Override
		public ItemAttribute readNBT(CompoundTag nbt) {
			return new InTag(new Identifier(nbt.getString("space"), nbt.getString("path")));
		}

	}

	public static class InItemGroup implements ItemAttribute {

		private ChorusFruitItem group;

		public InItemGroup(ChorusFruitItem group) {
			this.group = group;
		}

		@Override
		public boolean appliesTo(ItemCooldownManager stack) {
			HoeItem item = stack.b();
			return item.q() == group;
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemCooldownManager stack) {
			ChorusFruitItem group = stack.b()
				.q();
			return group == null ? Collections.emptyList() : Arrays.asList(new InItemGroup(group));
		}

		@Override
		public String getTranslationKey() {
			return "in_item_group";
		}

		@Override
		@Environment(EnvType.CLIENT)
		public TranslatableText format(boolean inverted) {
			return Lang.translate("item_attributes." + getTranslationKey() + (inverted ? ".inverted" : ""),
				group.c());
		}

		@Override
		public void writeNBT(CompoundTag nbt) {
			nbt.putString("path", group.b());
		}

		@Override
		public ItemAttribute readNBT(CompoundTag nbt) {
			String readPath = nbt.getString("path");
			for (ChorusFruitItem group : ChorusFruitItem.a)
				if (group.b()
					.equals(readPath))
					return new InItemGroup(group);
			return null;
		}

	}

	public static class AddedBy implements ItemAttribute {

		private String modId;

		public AddedBy(String modId) {
			this.modId = modId;
		}

		@Override
		public boolean appliesTo(ItemCooldownManager stack) {
			return modId.equals(stack.b()
				.getCreatorModId(stack));
		}

		@Override
		public List<ItemAttribute> listAttributesOf(ItemCooldownManager stack) {
			String id = stack.b()
				.getCreatorModId(stack);
			return id == null ? Collections.emptyList() : Arrays.asList(new AddedBy(id));
		}

		@Override
		public String getTranslationKey() {
			return "added_by";
		}

		@Override
		public Object[] getTranslationParameters() {
			Optional<? extends ModContainer> modContainerById = ModList.get()
				.getModContainerById(modId);
			String name = modContainerById.map(ModContainer::getModInfo)
				.map(IModInfo::getDisplayName)
				.orElse(StringUtils.capitalize(modId));
			return new Object[] { name };
		}

		@Override
		public void writeNBT(CompoundTag nbt) {
			nbt.putString("id", modId);
		}

		@Override
		public ItemAttribute readNBT(CompoundTag nbt) {
			return new AddedBy(nbt.getString("id"));
		}

	}

}
