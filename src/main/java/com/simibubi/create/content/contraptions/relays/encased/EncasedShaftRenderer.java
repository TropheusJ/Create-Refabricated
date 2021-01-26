package com.simibubi.create.content.contraptions.relays.encased;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import ebv;
import net.minecraft.block.piston.PistonHandler;

public class EncasedShaftRenderer extends KineticTileEntityRenderer {

	public EncasedShaftRenderer(ebv dispatcher) {
		super(dispatcher);
	}

	@Override
	protected PistonHandler getRenderedBlockState(KineticTileEntity te) {
		return shaft(getRotationAxisOf(te));
	}

}
