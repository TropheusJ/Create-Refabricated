package com.simibubi.kinetic_api.content.logistics.block.redstone;

import static com.simibubi.kinetic_api.foundation.gui.AllGuiTextures.STOCKSWITCH;

import com.simibubi.kinetic_api.AllBlocks;
import com.simibubi.kinetic_api.content.logistics.packet.ConfigureStockswitchPacket;
import com.simibubi.kinetic_api.foundation.gui.AbstractSimiScreen;
import com.simibubi.kinetic_api.foundation.gui.AllGuiTextures;
import com.simibubi.kinetic_api.foundation.gui.AllIcons;
import com.simibubi.kinetic_api.foundation.gui.GuiGameElement;
import com.simibubi.kinetic_api.foundation.gui.widgets.IconButton;
import com.simibubi.kinetic_api.foundation.gui.widgets.ScrollInput;
import com.simibubi.kinetic_api.foundation.networking.AllPackets;
import com.simibubi.kinetic_api.foundation.utility.Lang;
import com.simibubi.kinetic_api.foundation.utility.LerpedFloat;
import com.simibubi.kinetic_api.foundation.utility.LerpedFloat.Chaser;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.BufferVertexConsumer;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class StockpileSwitchScreen extends AbstractSimiScreen {

	private ScrollInput offBelow;
	private ScrollInput onAbove;
	private IconButton confirmButton;
	private IconButton flipSignals;

	private final Text title = Lang.translate("gui.stockpile_switch.title");
	private final Text invertSignal = Lang.translate("gui.stockpile_switch.invert_signal");
	private final ItemCooldownManager renderedItem = new ItemCooldownManager(AllBlocks.STOCKPILE_SWITCH.get());

	private int lastModification;
	private StockpileSwitchTileEntity te;

	private LerpedFloat cursor;
	private LerpedFloat cursorLane;

	public StockpileSwitchScreen(StockpileSwitchTileEntity te) {
		this.te = te;
		lastModification = -1;
	}

	@Override
	protected void b() {
		AllGuiTextures background = STOCKSWITCH;
		setWindowSize(background.width + 50, background.height);
		super.b();
		widgets.clear();

		cursor = LerpedFloat.linear()
			.startWithValue(te.getLevelForDisplay());
		cursorLane = LerpedFloat.linear()
			.startWithValue(te.getState() ? 1 : 0);

		offBelow = new ScrollInput(guiLeft + 36, guiTop + 40, 102, 18).withRange(0, 100)
			.titled(LiteralText.EMPTY.copy())
			.calling(state -> {
				lastModification = 0;
				offBelow.titled(Lang.translate("gui.stockpile_switch.move_to_upper_at", state));
				if (onAbove.getState() <= state) {
					onAbove.setState(state + 1);
					onAbove.onChanged();
				}
			})
			.setState((int) (te.offWhenBelow * 100));

		onAbove = new ScrollInput(guiLeft + 36, guiTop + 18, 102, 18).withRange(1, 101)
			.titled(LiteralText.EMPTY.copy())
			.calling(state -> {
				lastModification = 0;
				onAbove.titled(Lang.translate("gui.stockpile_switch.move_to_lower_at", state));
				if (offBelow.getState() >= state) {
					offBelow.setState(state - 1);
					offBelow.onChanged();
				}
			})
			.setState((int) (te.onWhenAbove * 100));

		onAbove.onChanged();
		offBelow.onChanged();

		widgets.add(onAbove);
		widgets.add(offBelow);

		confirmButton =
			new IconButton(guiLeft + background.width - 33, guiTop + background.height - 24, AllIcons.I_CONFIRM);
		widgets.add(confirmButton);

		flipSignals = new IconButton(guiLeft + 14, guiTop + 40, AllIcons.I_FLIP);
		flipSignals.setToolTip(invertSignal);
		widgets.add(flipSignals);
	}

	@Override
	protected void renderWindow(BufferVertexConsumer matrixStack, int mouseX, int mouseY, float partialTicks) {
		STOCKSWITCH.draw(matrixStack, this, guiLeft, guiTop);

		AllGuiTextures.STOCKSWITCH_POWERED_LANE.draw(matrixStack, this, guiLeft + 36, guiTop + (te.isInverted() ? 18 : 40));
		AllGuiTextures.STOCKSWITCH_UNPOWERED_LANE.draw(matrixStack, this, guiLeft + 36, guiTop + (te.isInverted() ? 40 : 18));
		o.a(matrixStack, d, guiLeft - 3 + (STOCKSWITCH.width - o.a(d)) / 2, guiTop + 3,
			0xffffff);

		AllGuiTextures sprite = AllGuiTextures.STOCKSWITCH_INTERVAL;
		float lowerBound = offBelow.getState();
		float upperBound = onAbove.getState();

		sprite.bind();
		b(matrixStack, (int) (guiLeft + upperBound) + 37, guiTop + 18, (int) (sprite.startX + upperBound), sprite.startY,
			(int) (sprite.width - upperBound), sprite.height);
		b(matrixStack, guiLeft + 37, guiTop + 40, sprite.startX, sprite.startY, (int) (lowerBound), sprite.height);

		AllGuiTextures.STOCKSWITCH_ARROW_UP.draw(matrixStack, this, (int) (guiLeft + lowerBound + 36) - 2, guiTop + 35);
		AllGuiTextures.STOCKSWITCH_ARROW_DOWN.draw(matrixStack, this, (int) (guiLeft + upperBound + 36) - 3, guiTop + 17);

		if (te.currentLevel != -1) {
			AllGuiTextures cursor = AllGuiTextures.STOCKSWITCH_CURSOR;
			matrixStack.a();
			matrixStack.a(Math.min(99, this.cursor.getValue(partialTicks) * sprite.width),
				cursorLane.getValue(partialTicks) * 22, 0);
			cursor.draw(matrixStack, this, guiLeft + 34, guiTop + 19);
			matrixStack.b();
		}

		matrixStack.a();
		GuiGameElement.of(renderedItem)
				.at(guiLeft + STOCKSWITCH.width + 15, guiTop + 120, -250)
				.scale(5)
				.render(matrixStack);
		matrixStack.b();
	}

	@Override
	public void d() {
		super.tick();

		cursor.chase(te.getLevelForDisplay(), 1 / 4f, Chaser.EXP);
		cursor.tickChaser();
		cursorLane.chase(te.getState() ? 1 : 0, 1 / 4f, Chaser.EXP);
		cursorLane.tickChaser();

		if (lastModification >= 0)
			lastModification++;

		if (lastModification >= 20) {
			lastModification = -1;
			send(te.isInverted());
		}
	}

	@Override
	public void e() {
		send(te.isInverted());
	}

	protected void send(boolean invert) {
		AllPackets.channel.sendToServer(new ConfigureStockswitchPacket(te.o(), offBelow.getState() / 100f,
			onAbove.getState() / 100f, invert));
	}

	@Override
	public boolean a(double x, double y, int button) {
		if (flipSignals.g()) 
			send(!te.isInverted());
		if (confirmButton.g()) {
			KeyBinding.B().s.m();
			return true;
		}
		return super.a(x, y, button);
	}

}
