package com.simibubi.kinetic_api;

import net.minecraft.client.options.KeyBinding;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT)
public enum AllSpecialTextures {

	BLANK("blank.png"),
	CHECKERED("checkerboard.png"),
	THIN_CHECKERED("thin_checkerboard.png"),
	CUTOUT_CHECKERED("cutout_checkerboard.png"),
	HIGHLIGHT_CHECKERED("highlighted_checkerboard.png"),
	SELECTION("selection.png"),
	
	;

	public static final String ASSET_PATH = "textures/special/";
	private Identifier location;

	private AllSpecialTextures(String filename) {
		location = new Identifier(Create.ID, ASSET_PATH + filename);
	}

	public void bind() {
		KeyBinding.B().L().a(location);
	}
	
	public Identifier getLocation() {
		return location;
	}

}