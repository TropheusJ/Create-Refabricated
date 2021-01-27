package com.simibubi.create.content.contraptions.relays.gauge;

import java.util.List;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.simibubi.create.content.contraptions.base.IRotate.StressImpact;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.utility.ColorHelper;
import com.simibubi.create.foundation.utility.Lang;

public class StressGaugeTileEntity extends GaugeTileEntity {

	public StressGaugeTileEntity(BellBlockEntity<? extends StressGaugeTileEntity> type) {
		super(type);
	}

	@Override
	public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
		super.updateFromNetwork(maxStress, currentStress, networkSize);

		if (!StressImpact.isEnabled())
			dialTarget = 0;
		else if (isOverStressed())
			dialTarget = 1.125f;
		else if (maxStress == 0)
			dialTarget = 0;
		else
			dialTarget = currentStress / maxStress;

		if (dialTarget > 0) {
			if (dialTarget < .5f)
				color = ColorHelper.mixColors(0x00FF00, 0xFFFF00, dialTarget * 2);
			else if (dialTarget < 1)
				color = ColorHelper.mixColors(0xFFFF00, 0xFF0000, (dialTarget) * 2 - 1);
			else
				color = 0xFF0000;
		}

		sendData();
		X_();
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		if (getSpeed() == 0) {
			dialTarget = 0;
			X_();
			return;
		}

		updateFromNetwork(capacity, stress, getOrCreateNetwork().getSize());
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		if (!StressImpact.isEnabled())
			return false;

		super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		double capacity = getNetworkCapacity();
		double stressFraction = getNetworkStress() / (capacity == 0 ? 1 : capacity);

		tooltip.add(componentSpacing.copy().append(Lang.translate("gui.stressometer.title").formatted(Formatting.GRAY)));

		if (getTheoreticalSpeed() == 0)
			tooltip.add(new LiteralText(spacing + ItemDescription.makeProgressBar(3, -1)).append(Lang.translate("gui.stressometer.no_rotation")).formatted(Formatting.DARK_GRAY));
		//	tooltip.add(new StringTextComponent(TextFormatting.DARK_GRAY + ItemDescription.makeProgressBar(3, -1)
		//			+ Lang.translate("gui.stressometer.no_rotation")));
		else {
			tooltip.add(componentSpacing.copy().append(StressImpact.getFormattedStressText(stressFraction)));

			tooltip.add(componentSpacing.copy().append(Lang.translate("gui.stressometer.capacity").formatted(Formatting.GRAY)));

			double remainingCapacity = capacity - getNetworkStress();

			Text su = Lang.translate("generic.unit.stress");
			MutableText stressTooltip = componentSpacing.copy()
					.append(new LiteralText(" " + IHaveGoggleInformation.format(remainingCapacity))
							.append(su.copy())
							.formatted(StressImpact.of(stressFraction).getRelativeColor()));
			if (remainingCapacity != capacity) {
				stressTooltip
						.append(new LiteralText(" / ").formatted(Formatting.GRAY))
						.append(new LiteralText(IHaveGoggleInformation.format(capacity))
								.append(su.copy())
								.formatted(Formatting.DARK_GRAY));
			}
			tooltip.add(stressTooltip);
		}

		return true;
	}

	public float getNetworkStress() {
		return stress;
	}

	public float getNetworkCapacity() {
		return capacity;
	}

}
