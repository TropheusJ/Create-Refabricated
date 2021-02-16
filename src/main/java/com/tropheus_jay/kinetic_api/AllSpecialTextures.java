package com.tropheus_jay.kinetic_api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

//todo: ohno
//@EventBusSubscriber(value = Dist.CLIENT)
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
		location = new Identifier(KineticAPI.ID, ASSET_PATH + filename);
	}

	public void bind() {
		MinecraftClient.getInstance().getTextureManager().bindTexture(location);
	}
	
	public Identifier getLocation() {
		return location;
	}

}