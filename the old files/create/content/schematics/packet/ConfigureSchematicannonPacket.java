package com.simibubi.kinetic_api.content.schematics.packet;

import java.util.function.Supplier;

import com.simibubi.kinetic_api.content.schematics.block.SchematicannonTileEntity;
import com.simibubi.kinetic_api.content.schematics.block.SchematicannonTileEntity.State;
import com.simibubi.kinetic_api.foundation.networking.SimplePacketBase;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class ConfigureSchematicannonPacket extends SimplePacketBase {

	public static enum Option {
		DONT_REPLACE, REPLACE_SOLID, REPLACE_ANY, REPLACE_EMPTY, SKIP_MISSING, SKIP_TILES, PLAY, PAUSE, STOP;
	}

	private Option option;
	private boolean set;
	private BlockPos pos;

	public static ConfigureSchematicannonPacket setOption(BlockPos pos, Option option, boolean set) {
		ConfigureSchematicannonPacket packet = new ConfigureSchematicannonPacket(pos);
		packet.option = option;
		packet.set = set;
		return packet;
	}

	public ConfigureSchematicannonPacket(BlockPos pos) {
		this.pos = pos;
	}

	public ConfigureSchematicannonPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
		option = Option.values()[buffer.readInt()];
		set = buffer.readBoolean();
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeInt(option.ordinal());
		buffer.writeBoolean(set);
	}

	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayerEntity player = context.get().getSender();
			if (player == null)
				return;
			GameMode world = player.l;
			if (world == null || !world.p(pos))
				return;

			BeehiveBlockEntity tileEntity = world.c(pos);
			if (!(tileEntity instanceof SchematicannonTileEntity))
				return;

			SchematicannonTileEntity te = (SchematicannonTileEntity) tileEntity;
			switch (option) {
			case DONT_REPLACE:
			case REPLACE_ANY:
			case REPLACE_EMPTY:
			case REPLACE_SOLID:
				te.replaceMode = option.ordinal();
				break;
			case SKIP_MISSING:
				te.skipMissing = set;
				break;
			case SKIP_TILES:
				te.replaceTileEntities = set;
				break;

			case PLAY:
				te.state = State.RUNNING;
				te.statusMsg = "running";
				break;
			case PAUSE:
				te.state = State.PAUSED;
				te.statusMsg = "paused";
				break;
			case STOP:
				te.state = State.STOPPED;
				te.statusMsg = "stopped";
				break;
			default:
				break;
			}

			te.sendUpdate = true;
		});
		context.get().setPacketHandled(true);
	}

}