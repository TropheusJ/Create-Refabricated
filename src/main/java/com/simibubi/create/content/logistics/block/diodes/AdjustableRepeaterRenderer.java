package com.simibubi.create.content.logistics.block.diodes;

import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.foundation.tileEntity.renderer.ColoredOverlayTileEntityRenderer;
import com.simibubi.create.foundation.utility.ColorHelper;
import com.simibubi.create.foundation.utility.SuperByteBuffer;
import ebv;

public class AdjustableRepeaterRenderer extends ColoredOverlayTileEntityRenderer<AdjustableRepeaterTileEntity> {

	public AdjustableRepeaterRenderer(ebv dispatcher) {
		super(dispatcher);
	}

	@Override
	protected int getColor(AdjustableRepeaterTileEntity te, float partialTicks) {
		return ColorHelper.mixColors(0x2C0300, 0xCD0000, te.state / (float) te.maxState.getValue());
	}

	@Override
	protected SuperByteBuffer getOverlayBuffer(AdjustableRepeaterTileEntity te) {
		return AllBlockPartials.FLEXPEATER_INDICATOR.renderOn(te.p());
	}

}
