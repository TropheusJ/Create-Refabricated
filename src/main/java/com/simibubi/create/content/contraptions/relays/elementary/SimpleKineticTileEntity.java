package com.simibubi.create.content.contraptions.relays.elementary;

import java.util.List;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.world.timer.Timer;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;

public class SimpleKineticTileEntity extends KineticTileEntity {

	public SimpleKineticTileEntity(BellBlockEntity<? extends SimpleKineticTileEntity> type) {
		super(type);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		behaviours.add(
			new BracketedTileEntityBehaviour(this, state -> state.b() instanceof AbstractShaftBlock).withTrigger(
				state -> state.b() instanceof ShaftBlock ? AllTriggers.BRACKET_SHAFT : AllTriggers.BRACKET_COG));
		super.addBehaviours(behaviours);
	}

	@Override
	public Timer getRenderBoundingBox() {
		return new Timer(e).g(1);
	}

}
