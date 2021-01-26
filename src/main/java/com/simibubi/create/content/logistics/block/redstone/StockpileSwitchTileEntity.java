package com.simibubi.create.content.logistics.block.redstone;

import java.util.List;
import afj;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.inventory.InvManipulationBehaviour.InterfaceProvider;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraftforge.items.IItemHandler;

public class StockpileSwitchTileEntity extends SmartTileEntity {

	public float onWhenAbove;
	public float offWhenBelow;
	public float currentLevel;
	private boolean state;
	private boolean inverted;

	private FilteringBehaviour filtering;
	private InvManipulationBehaviour observedInventory;

	public StockpileSwitchTileEntity(BellBlockEntity<?> typeIn) {
		super(typeIn);
		onWhenAbove = .75f;
		offWhenBelow = .25f;
		currentLevel = -1;
		state = false;
		inverted = false;
		setLazyTickRate(10);
	}

	@Override
	protected void fromTag(PistonHandler blockState, CompoundTag compound, boolean clientPacket) {
		onWhenAbove = compound.getFloat("OnAbove");
		offWhenBelow = compound.getFloat("OffBelow");
		currentLevel = compound.getFloat("Current");
		state = compound.getBoolean("Powered");
		inverted = compound.getBoolean("Inverted");
		super.fromTag(blockState, compound, clientPacket);
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		compound.putFloat("OnAbove", onWhenAbove);
		compound.putFloat("OffBelow", offWhenBelow);
		compound.putFloat("Current", currentLevel);
		compound.putBoolean("Powered", state);
		compound.putBoolean("Inverted", inverted);
		super.write(compound, clientPacket);
	}

	public float getLevel() {
		return currentLevel;
	}

	public void updateCurrentLevel() {
		boolean changed = false;
		if (!observedInventory.hasInventory()) {
			if (currentLevel == -1)
				return;
			d.a(e, p().a(StockpileSwitchBlock.INDICATOR, 0), 3);
			currentLevel = -1;
			state = false;
			d.a(e, p().b());
			sendData();
			return;
		}

		float occupied = 0;
		float totalSpace = 0;
		IItemHandler inv = observedInventory.getInventory();

		for (int slot = 0; slot < inv.getSlots(); slot++) {
			ItemCooldownManager stackInSlot = inv.getStackInSlot(slot);
			int space = Math.min(stackInSlot.c(), inv.getSlotLimit(slot));
			int count = stackInSlot.E();

			if (space == 0)
				continue;

			totalSpace += 1;

			if (filtering.test(stackInSlot))
				occupied += count * (1f / space);
		}

		float level = (float) occupied / totalSpace;
		if (currentLevel != level)
			changed = true;
		currentLevel = level;
		currentLevel = afj.a(currentLevel, 0, 1);

		boolean previouslyPowered = state;
		if (state && currentLevel <= offWhenBelow)
			state = false;
		else if (!state && currentLevel >= onWhenAbove)
			state = true;
		boolean update = previouslyPowered != state;

		int displayLevel = 0;
		if (currentLevel > 0)
			displayLevel = (int) (currentLevel * 6);
		d.a(e, p().a(StockpileSwitchBlock.INDICATOR, displayLevel), update ? 3 : 2);
		if (update)
			d.a(e, p().b());
		if (changed || update)
			sendData();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (d.v)
			return;
		updateCurrentLevel();
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		filtering = new FilteringBehaviour(this, new FilteredDetectorFilterSlot()).moveText(new EntityHitResult(0, 5, 0))
			.withCallback($ -> updateCurrentLevel());
		behaviours.add(filtering);

		observedInventory = new InvManipulationBehaviour(this, InterfaceProvider.towardBlockFacing()).bypassSidedness();
		behaviours.add(observedInventory);
	}

	public float getLevelForDisplay() {
		return currentLevel == -1 ? 0 : currentLevel;
	}
	
	public boolean getState() {
		return state;
	}
	
	public boolean isPowered() {
		return inverted != state;
	}
	
	public boolean isInverted() {
		return inverted;
	}
	
	public void setInverted(boolean inverted) {
		if (inverted == this.inverted)
			return;
		this.inverted = inverted;
		d.a(e, p().b());
	}
}
