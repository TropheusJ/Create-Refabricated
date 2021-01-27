package com.simibubi.create.content.contraptions.fluids.tank;

import afj;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.widgets.InterpolatedChasingValue;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import ebv;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferVertexConsumer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class FluidTankRenderer extends SafeTileEntityRenderer<FluidTankTileEntity> {

	public FluidTankRenderer(ebv dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void renderSafe(FluidTankTileEntity te, float partialTicks, BufferVertexConsumer ms, BackgroundRenderer buffer,
		int light, int overlay) {
		if (!te.isController())
			return;
		if (!te.window)
			return;

		InterpolatedChasingValue fluidLevel = te.fluidLevel;
		if (fluidLevel == null)
			return;

		float capHeight = 1 / 4f;
		float tankHullWidth = 1 / 16f + 1 / 128f;
		float minPuddleHeight = 1 / 16f;
		float totalHeight = te.height - 2 * capHeight - minPuddleHeight;

		float level = fluidLevel.get(partialTicks);
		if (level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = afj.a(level * totalHeight, 0, totalHeight);

		FluidTank tank = te.tankInventory;
		FluidStack fluidStack = tank.getFluid();

		if (fluidStack.isEmpty())
			return;
		
		boolean top = fluidStack.getFluid()
			.getAttributes()
			.isLighterThanAir();

		float xMin = tankHullWidth;
		float xMax = xMin + te.width - 2 * tankHullWidth;
		float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
		float yMax = yMin + clampedLevel;

		if (top) {
			yMin += totalHeight - clampedLevel;
			yMax += totalHeight - clampedLevel;
		}

		float zMin = tankHullWidth;
		float zMax = zMin + te.width - 2 * tankHullWidth;

		ms.a();
		ms.a(0, clampedLevel - totalHeight, 0);
		FluidRenderer.renderTiledFluidBB(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms, light, false);
		ms.b();
	}

	@Override
	public boolean isGlobalRenderer(FluidTankTileEntity te) {
		return te.isController();
	}

}
