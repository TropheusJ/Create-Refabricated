package com.simibubi.create.content.contraptions.relays.gauge;

import java.util.List;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import afj;
import com.simibubi.create.content.contraptions.base.IRotate.SpeedLevel;
import com.simibubi.create.content.contraptions.goggles.GogglesItem;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.utility.ColorHelper;
import com.simibubi.create.foundation.utility.Lang;

public class SpeedGaugeTileEntity extends GaugeTileEntity{

	public SpeedGaugeTileEntity(BellBlockEntity<? extends SpeedGaugeTileEntity> type) {
		super(type);
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		float speed = Math.abs(getSpeed());
		float medium = AllConfigs.SERVER.kinetics.mediumSpeed.get().floatValue();
		float fast = AllConfigs.SERVER.kinetics.fastSpeed.get().floatValue();
		float max = AllConfigs.SERVER.kinetics.maxRotationSpeed.get().floatValue();
		color = ColorHelper.mixColors(SpeedLevel.of(speed).getColor(), 0xffffff, .25f);

		if (speed == 69)
			AllTriggers.triggerForNearbyPlayers(AllTriggers.SPEED_READ, d, e, 6,
					GogglesItem::canSeeParticles);
		if (speed == 0) {
			dialTarget = 0;
			color = 0x333333;
		} else if (speed < medium) {
			dialTarget = afj.g(speed / medium, 0, .45f);
		} else if (speed < fast) {
			dialTarget = afj.g((speed - medium) / (fast - medium), .45f, .75f);
		} else {
			dialTarget = afj.g((speed - fast) / (max - fast), .75f, 1.125f);
		}
		
		X_();
	}

	@Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		tooltip.add(componentSpacing.copy().append(Lang.translate("gui.speedometer.title").formatted(Formatting.GRAY)));
		tooltip.add(componentSpacing.copy().append(SpeedLevel.getFormattedSpeedText(speed, isOverStressed())));

		return true;
	}
}
