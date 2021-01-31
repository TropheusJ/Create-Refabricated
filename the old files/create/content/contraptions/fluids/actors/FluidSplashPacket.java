package com.simibubi.kinetic_api.content.contraptions.fluids.actors;

import java.util.function.Supplier;

import com.simibubi.kinetic_api.content.contraptions.fluids.FluidFX;
import com.simibubi.kinetic_api.foundation.networking.SimplePacketBase;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class FluidSplashPacket extends SimplePacketBase {

	private BlockPos pos;
	private FluidStack fluid;

	public FluidSplashPacket(BlockPos pos, FluidStack fluid) {
		this.pos = pos;
		this.fluid = fluid;
	}

	public FluidSplashPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
		fluid = buffer.readFluidStack();
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeFluidStack(fluid);
	}

	public void handle(Supplier<Context> ctx) {
		ctx.get()
			.enqueueWork(() -> DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
				if (KeyBinding.B().s.cz()
					.f(new EntityHitResult(pos.getX(), pos.getY(), pos.getZ())) > 100)
					return;
				FluidFX.splash(pos, fluid);
			}));
		ctx.get()
			.setPacketHandled(true);
	}

}
