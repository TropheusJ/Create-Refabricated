package com.simibubi.kinetic_api.foundation.gui.widgets;

import com.google.common.collect.ImmutableList;
import com.simibubi.kinetic_api.foundation.gui.AllGuiTextures;
import net.minecraft.client.render.BufferVertexConsumer;
import net.minecraft.text.Text;

public class Indicator extends AbstractSimiWidget {
	
	public enum State {
		OFF, ON,
		RED, YELLOW, GREEN;
	}
	
	public State state;
	
	public Indicator(int x, int y, Text tooltip) {
		super(x, y, AllGuiTextures.INDICATOR.width, AllGuiTextures.INDICATOR.height);
		this.toolTip = ImmutableList.of(tooltip);
		this.state = State.OFF;
	}
	
	@Override
	public void a(BufferVertexConsumer matrixStack, int mouseX, int mouseY, float partialTicks ) {
		AllGuiTextures toDraw;
		switch(state) {
			case ON: toDraw = AllGuiTextures.INDICATOR_WHITE; break;
			case OFF: toDraw = AllGuiTextures.INDICATOR; break;
			case RED: toDraw = AllGuiTextures.INDICATOR_RED; break;
			case YELLOW: toDraw = AllGuiTextures.INDICATOR_YELLOW; break;
			case GREEN: toDraw = AllGuiTextures.INDICATOR_GREEN; break;
			default: toDraw = AllGuiTextures.INDICATOR; break;
		}
		toDraw.draw(matrixStack, this, l, m);
	}
	
}
