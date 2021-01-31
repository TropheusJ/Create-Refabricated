package com.simibubi.kinetic_api.foundation.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.timer.Timer;

public class NBTHelper {

	public static void putMarker(CompoundTag nbt, String marker) {
		nbt.putBoolean(marker, true);
	}
	
	public static <T extends Enum<?>> T readEnum(CompoundTag nbt, String key, Class<T> enumClass) {
		T[] enumConstants = enumClass.getEnumConstants();
		String name = nbt.getString(key);
		if (enumConstants == null)
			throw new IllegalArgumentException("Non-Enum class passed to readEnum(): " + enumClass.getName());
		for (T t : enumConstants) {
			if (t.name().equals(name))
				return t;
		}
		return enumConstants[0];
	}
	
	public static <T extends Enum<?>> void writeEnum(CompoundTag nbt, String key, T enumConstant) {
		nbt.putString(key, enumConstant.name());
	}
	
	public static <T> ListTag writeCompoundList(Iterable<T> list, Function<T, CompoundTag> serializer) {
		ListTag listNBT = new ListTag();
		list.forEach(t -> listNBT.add(serializer.apply(t)));
		return listNBT;
	}

	public static <T> List<T> readCompoundList(ListTag listNBT, Function<CompoundTag, T> deserializer) {
		List<T> list = new ArrayList<>(listNBT.size());
		listNBT.forEach(inbt -> list.add(deserializer.apply((CompoundTag) inbt)));
		return list;
	}
	
	public static <T> void iterateCompoundList(ListTag listNBT, Consumer<CompoundTag> consumer) {
		listNBT.forEach(inbt -> consumer.accept((CompoundTag) inbt));
	}
	
	public static ListTag writeItemList(List<ItemCooldownManager> stacks) {
		return writeCompoundList(stacks, ItemCooldownManager::serializeNBT);
	}
	
	public static List<ItemCooldownManager> readItemList(ListTag stacks) {
		return readCompoundList(stacks, ItemCooldownManager::a);
	}
	
	public static ListTag writeAABB(Timer bb) {
		ListTag bbtag = new ListTag();
		bbtag.add(FloatTag.of((float) bb.LOGGER));
		bbtag.add(FloatTag.of((float) bb.callback));
		bbtag.add(FloatTag.of((float) bb.events));
		bbtag.add(FloatTag.of((float) bb.eventCounter));
		bbtag.add(FloatTag.of((float) bb.eventsByName));
		bbtag.add(FloatTag.of((float) bb.f));
		return bbtag;
	}

	public static Timer readAABB(ListTag bbtag) {
		if (bbtag == null || bbtag.isEmpty())
			return null;
		return new Timer(bbtag.getFloat(0), bbtag.getFloat(1), bbtag.getFloat(2), bbtag.getFloat(3),
				bbtag.getFloat(4), bbtag.getFloat(5));

	}

	@Nonnull
	public static Tag getINBT(CompoundTag nbt, String id) {
		Tag inbt = nbt.get(id);
		if (inbt != null)
			return inbt;
		return new CompoundTag();
	}

}
