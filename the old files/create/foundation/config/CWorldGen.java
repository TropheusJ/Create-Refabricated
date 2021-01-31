package com.simibubi.kinetic_api.foundation.config;

import com.simibubi.kinetic_api.foundation.worldgen.AllWorldFeatures;

import net.minecraftforge.common.ForgeConfigSpec.Builder;

public class CWorldGen extends ConfigBase {

	public ConfigBool disable = b(false, "disableWorldGen", Comments.disable);

	@Override
	protected void registerAll(Builder builder) {
		super.registerAll(builder);
		AllWorldFeatures.fillConfig(builder);
	}

	@Override
	public String getName() {
		return "worldgen.v" + AllWorldFeatures.forcedUpdateVersion;
	}

	private static class Comments {
		static String disable = "Prevents all worldgen added by KineticAPI from taking effect";
	}

}