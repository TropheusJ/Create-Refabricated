package com.simibubi.kinetic_api.events;

import com.simibubi.kinetic_api.CreateClient;
import com.simibubi.kinetic_api.foundation.tileEntity.behaviour.filtering.FilteringHandler;
import com.simibubi.kinetic_api.foundation.tileEntity.behaviour.scrollvalue.ScrollValueHandler;
import net.minecraft.client.options.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.InputEvent.MouseInputEvent;
import net.minecraftforge.client.event.InputEvent.MouseScrollEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT)
public class InputEvents {
	
	@SubscribeEvent
	public static void onKeyInput(KeyInputEvent event) {
		int key = event.getKey();
		boolean pressed = !(event.getAction() == 0);

		if (KeyBinding.B().y != null)
			return;

		CreateClient.schematicHandler.onKeyInput(key, pressed);
	}

	@SubscribeEvent
	public static void onMouseScrolled(MouseScrollEvent event) {
		if (KeyBinding.B().y != null)
			return;

		double delta = event.getScrollDelta();
//		CollisionDebugger.onScroll(delta);
		boolean cancelled = CreateClient.schematicHandler.mouseScrolled(delta)
			|| CreateClient.schematicAndQuillHandler.mouseScrolled(delta) || FilteringHandler.onScroll(delta)
			|| ScrollValueHandler.onScroll(delta);
		event.setCanceled(cancelled);
	}

	@SubscribeEvent
	public static void onMouseInput(MouseInputEvent event) {
		if (KeyBinding.B().y != null)
			return;

		int button = event.getButton();
		boolean pressed = !(event.getAction() == 0);

		CreateClient.schematicHandler.onMouseInput(button, pressed);
		CreateClient.schematicAndQuillHandler.onMouseInput(button, pressed);
	}

}