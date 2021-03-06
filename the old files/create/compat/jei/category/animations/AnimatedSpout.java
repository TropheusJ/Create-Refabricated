package com.simibubi.create.compat.jei.category.animations;

import java.util.List;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;

public class AnimatedSpout extends AnimatedKinetics {

	private List<FluidStack> fluids;

	public AnimatedSpout withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
		return this;
	}

	@Override
	public void draw(MatrixStack matrixStack, int xOffset, int yOffset) {
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 100);
		matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-15.5f));
		matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(22.5f));
		int scale = 20;

		GuiGameElement.of(AllBlocks.SPOUT.getDefaultState())
			.scale(scale)
			.render(matrixStack);

		float cycle = AnimationTickHolder.getRenderTick() % 30;
		float squeeze = cycle < 20 ? MathHelper.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		matrixStack.push();

		GuiGameElement.of(AllBlockPartials.SPOUT_TOP)
			.scale(scale)
			.render(matrixStack);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);
		GuiGameElement.of(AllBlockPartials.SPOUT_MIDDLE)
			.scale(scale)
			.render(matrixStack);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);
		GuiGameElement.of(AllBlockPartials.SPOUT_BOTTOM)
			.scale(scale)
			.render(matrixStack);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);

		matrixStack.pop();

		GuiGameElement.of(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.render(matrixStack);

		Immediate buffer = VertexConsumerProvider.immediate(Tessellator.getInstance()
			.getBuffer());
		matrixStack.push();
		matrixStack.scale(16, -16, 16);
		float from = 2/16f;
		float to = 1f - from;
		FluidRenderer.renderTiledFluidBB(fluids.get(0), from, from, from, to, to, to, buffer, matrixStack, 0xf000f0, false);
		matrixStack.pop();

		float width = 1 / 128f * squeeze;
		matrixStack.translate(scale / 2f, scale * 1.5f, scale / 2f);
		matrixStack.scale(16, -16, 16);
		matrixStack.translate(-width / 2, 0, -width / 2);
		FluidRenderer.renderTiledFluidBB(fluids.get(0), 0, -0.001f, 0, width, 2.001f, width, buffer, matrixStack, 0xf000f0,
			false);
		buffer.draw();

		matrixStack.pop();
	}

}
