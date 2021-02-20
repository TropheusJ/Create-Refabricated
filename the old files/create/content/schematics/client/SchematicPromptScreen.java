package com.simibubi.create.content.schematics.client;

import org.lwjgl.glfw.GLFW;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.gui.widgets.IconButton;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class SchematicPromptScreen extends AbstractSimiScreen {

	private final Text title = Lang.translate("schematicAndQuill.title");
	private final Text convertLabel = Lang.translate("schematicAndQuill.convert");
	private final Text abortLabel = Lang.translate("action.discard");
	private final Text confirmLabel = Lang.translate("action.saveToFile");

	private TextFieldWidget nameField;
	private IconButton confirm;
	private IconButton abort;
	private IconButton convert;

	@Override
	public void init() {
		super.init();
		AllGuiTextures background = AllGuiTextures.SCHEMATIC_PROMPT;
		setWindowSize(background.width, background.height + 30);

		nameField = new TextFieldWidget(textRenderer, guiLeft + 49, guiTop + 26, 131, 10, LiteralText.EMPTY);
		nameField.setEditableColor(-1);
		nameField.setUneditableColor(-1);
		nameField.setHasBorder(false);
		nameField.setMaxLength(35);
		nameField.changeFocus(true);

		abort = new IconButton(guiLeft + 7, guiTop + 53, AllIcons.I_TRASH);
		abort.setToolTip(abortLabel);
		widgets.add(abort);

		confirm = new IconButton(guiLeft + 158, guiTop + 53, AllIcons.I_CONFIRM);
		confirm.setToolTip(confirmLabel);
		widgets.add(confirm);

		convert = new IconButton(guiLeft + 180, guiTop + 53, AllIcons.I_SCHEMATIC);
		convert.setToolTip(convertLabel);
		widgets.add(convert);

		widgets.add(confirm);
		widgets.add(convert);
		widgets.add(abort);
		widgets.add(nameField);
	}

	@Override
	protected void renderWindow(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		AllGuiTextures.SCHEMATIC_PROMPT.draw(ms, this, guiLeft, guiTop);
		textRenderer.drawWithShadow(ms, title, guiLeft + (sWidth / 2) - (textRenderer.getWidth(title) / 2), guiTop + 3,
			0xffffff);
		ms.push();
		ms.translate(guiLeft + 22, guiTop + 39, 0);
		GuiGameElement.of(AllItems.SCHEMATIC.asStack()).render(ms);
		ms.pop();
	}

	@Override
	public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			confirm(false);
			return true;
		}
		if (keyCode == 256 && this.shouldCloseOnEsc()) {
			this.onClose();
			return true;
		}
		return nameField.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (confirm.isHovered()) {
			confirm(false);
			return true;
		}
		if (abort.isHovered()) {
			CreateClient.schematicAndQuillHandler.discard();
			MinecraftClient.getInstance().player.updateSubmergedInWaterState();
			return true;
		}
		if (convert.isHovered()) {
			confirm(true);
			return true;
		}
		return super.mouseClicked(x, y, button);
	}

	private void confirm(boolean convertImmediately) {
		CreateClient.schematicAndQuillHandler.saveSchematic(nameField.getText(), convertImmediately);
		MinecraftClient.getInstance().player.updateSubmergedInWaterState();
	}

}
