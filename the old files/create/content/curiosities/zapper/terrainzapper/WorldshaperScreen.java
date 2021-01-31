package com.simibubi.kinetic_api.content.curiosities.zapper.terrainzapper;

import java.util.List;
import java.util.Vector;
import com.simibubi.kinetic_api.content.curiosities.zapper.ZapperScreen;
import com.simibubi.kinetic_api.foundation.gui.AllGuiTextures;
import com.simibubi.kinetic_api.foundation.gui.widgets.IconButton;
import com.simibubi.kinetic_api.foundation.gui.widgets.Label;
import com.simibubi.kinetic_api.foundation.gui.widgets.ScrollInput;
import com.simibubi.kinetic_api.foundation.gui.widgets.SelectionScrollInput;
import com.simibubi.kinetic_api.foundation.utility.Lang;
import com.simibubi.kinetic_api.foundation.utility.NBTHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.BufferVertexConsumer;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class WorldshaperScreen extends ZapperScreen {

	protected final Text placementSection = Lang.translate("gui.terrainzapper.placement");
	protected final Text toolSection = Lang.translate("gui.terrainzapper.tool");
	protected final List<Text> brushOptions =
		Lang.translatedOptions("gui.terrainzapper.brush", "cuboid", "sphere", "cylinder");

	protected Vector<IconButton> toolButtons;
	protected Vector<IconButton> placementButtons;

	protected ScrollInput brushInput;
	protected Label brushLabel;
	protected Vector<ScrollInput> brushParams;
	protected Vector<Label> brushParamLabels;
	private int i;
	private int j;
	private CompoundTag nbt;

	public WorldshaperScreen(ItemCooldownManager zapper, boolean offhand) {
		super(AllGuiTextures.TERRAINZAPPER, zapper, offhand);
		fontColor = 0x767676;
		d = Lang.translate("gui.terrainzapper.title");
		nbt = zapper.p();
	}

	@Override
	protected void b() {
		super.b();

		i = guiLeft - 20;
		j = guiTop;

		brushLabel = new Label(i + 61, j + 23, LiteralText.EMPTY).withShadow();
		brushInput = new SelectionScrollInput(i + 56, j + 18, 77, 18).forOptions(brushOptions)
			.titled(Lang.translate("gui.terrainzapper.brush"))
			.writingTo(brushLabel)
			.calling(this::brushChanged);
		if (nbt.contains("Brush"))
			brushInput.setState(NBTHelper.readEnum(nbt, "Brush", TerrainBrushes.class)
				.ordinal());

		widgets.add(brushLabel);
		widgets.add(brushInput);
		initBrushParams();

		toolButtons = new Vector<>(6);
		TerrainTools[] toolValues = TerrainTools.values();
		for (int id = 0; id < toolValues.length; id++) {
			TerrainTools tool = toolValues[id];
			toolButtons.add(new IconButton(i + 7 + id * 18, j + 77, tool.icon));
			toolButtons.get(id)
				.setToolTip(Lang.translate("gui.terrainzapper.tool." + tool.translationKey));
		}

		if (nbt.contains("Tool"))
			toolButtons.get(NBTHelper.readEnum(nbt, "Tool", TerrainTools.class)
				.ordinal()).o = false;
		widgets.addAll(toolButtons);

		placementButtons = new Vector<>(3);
		PlacementOptions[] placementValues = PlacementOptions.values();
		for (int id = 0; id < placementValues.length; id++) {
			PlacementOptions option = placementValues[id];
			placementButtons.add(new IconButton(i + 136 + id * 18, j + 77, option.icon));
			placementButtons.get(id)
				.setToolTip(Lang.translate("gui.terrainzapper.placement." + option.translationKey));
		}

		if (nbt.contains("Placement"))
			placementButtons.get(NBTHelper.readEnum(nbt, "Placement", PlacementOptions.class)
				.ordinal()).o = false;
		widgets.addAll(placementButtons);

	}

	public void initBrushParams() {
		if (brushParams != null) {
			nbt.put("BrushParams", NbtHelper.fromBlockPos(new BlockPos(brushParams.get(0)
				.getState(),
				brushParams.get(1)
					.getState(),
				brushParams.get(2)
					.getState())));

			widgets.removeAll(brushParamLabels);
			widgets.removeAll(brushParams);
		}

		brushParamLabels = new Vector<>(3);
		brushParams = new Vector<>(3);
		BlockPos data = NbtHelper.toBlockPos(nbt.getCompound("BrushParams"));
		int[] params = new int[] { data.getX(), data.getY(), data.getZ() };
		Brush currentBrush = TerrainBrushes.values()[brushInput.getState()].get();
		for (int index = 0; index < 3; index++) {

			Label label = new Label(i + 65 + 20 * index, j + 43, LiteralText.EMPTY).withShadow();
			brushParamLabels.add(label);
			int indexFinal = index;
			ScrollInput input = new ScrollInput(i + 56 + 20 * index, j + 38, 18, 18)
				.withRange(currentBrush.getMin(index), currentBrush.getMax(index) + 1)
				.writingTo(label)
				.titled(currentBrush.getParamLabel(index).copy())
				.calling(state -> {
					label.l = i + 65 + 20 * indexFinal - o.a(label.text) / 2;
				});
			input.setState(params[index]);
			input.onChanged();
			if (index >= currentBrush.amtParams) {
				input.p = false;
				label.p = false;
				input.o = false;
			}

			brushParams.add(input);
		}

		widgets.addAll(brushParamLabels);
		widgets.addAll(brushParams);
	}

	private void brushChanged(int brushIndex) {
		initBrushParams();
	}

	@Override
	public boolean a(double x, double y, int button) {
		CompoundTag nbt = zapper.o();

		for (IconButton placementButton : placementButtons) {
			if (placementButton.g()) {
				placementButtons.forEach(b -> b.o = true);
				placementButton.o = false;
				placementButton.a(KeyBinding.B()
					.V());
				nbt.putString("Placement", PlacementOptions.values()[placementButtons.indexOf(placementButton)].name());
			}
		}

		for (IconButton toolButton : toolButtons) {
			if (toolButton.g()) {
				toolButtons.forEach(b -> b.o = true);
				toolButton.o = false;
				toolButton.a(KeyBinding.B()
					.V());
				nbt.putString("Tool", TerrainTools.values()[toolButtons.indexOf(toolButton)].name());
			}
		}

		return super.a(x, y, button);
	}

	@Override
	protected void drawOnBackground(BufferVertexConsumer matrixStack, int i, int j) {
		super.drawOnBackground(matrixStack, i, j);

		Brush currentBrush = TerrainBrushes.values()[brushInput.getState()].get();
		for (int index = 2; index >= currentBrush.amtParams; index--) 
			AllGuiTextures.TERRAINZAPPER_INACTIVE_PARAM.draw(matrixStack, i + 56 + 20 * index, j + 38);

		o.b(matrixStack, toolSection, i + 7, j + 66, fontColor);
		o.b(matrixStack, placementSection, i + 136, j + 66, fontColor);
	}

	@Override
	protected void writeAdditionalOptions(CompoundTag nbt) {
		super.writeAdditionalOptions(nbt);
		NBTHelper.writeEnum(nbt, "Brush", TerrainBrushes.values()[brushInput.getState()]);
		nbt.put("BrushParams", NbtHelper.fromBlockPos(new BlockPos(brushParams.get(0)
			.getState(),
			brushParams.get(1)
				.getState(),
			brushParams.get(2)
				.getState())));
	}

}
