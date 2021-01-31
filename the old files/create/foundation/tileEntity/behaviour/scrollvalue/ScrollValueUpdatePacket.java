package com.simibubi.kinetic_api.foundation.tileEntity.behaviour.scrollvalue;

import com.simibubi.kinetic_api.foundation.networking.TileEntityConfigurationPacket;
import com.simibubi.kinetic_api.foundation.tileEntity.SmartTileEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class ScrollValueUpdatePacket extends TileEntityConfigurationPacket<SmartTileEntity> {

	int value;
	
	public ScrollValueUpdatePacket(PacketByteBuf buffer) {
		super(buffer);
	}
	
	public ScrollValueUpdatePacket(BlockPos pos, int amount) {
		super(pos);
		this.value = amount;
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeInt(value);
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		value = buffer.readInt();
	}

	@Override
	protected void applySettings(SmartTileEntity te) {
		ScrollValueBehaviour behaviour = te.getBehaviour(ScrollValueBehaviour.TYPE);
		if (behaviour == null)
			return;
		behaviour.setValue(value);
	}

}
