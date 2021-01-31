package com.simibubi.kinetic_api.content.contraptions.components.actors.dispenser;

import javax.annotation.Nullable;

import com.simibubi.kinetic_api.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.kinetic_api.foundation.utility.BlockHelper;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@MethodsReturnNonnullByDefault
public class ContraptionBlockSource implements BlockPointer {
	private final BlockPos pos;
	private final MovementContext context;
	private final Direction overrideFacing;

	public ContraptionBlockSource(MovementContext context, BlockPos pos) {
		this(context, pos, null);
	}

	public ContraptionBlockSource(MovementContext context, BlockPos pos, @Nullable Direction overrideFacing) {
		this.pos = pos;
		this.context = context;
		this.overrideFacing = overrideFacing;
	}

	@Override
	public double getX() {
		return (double)this.pos.getX() + 0.5D;
	}

	@Override
	public double getY() {
		return (double)this.pos.getY() + 0.5D;
	}

	@Override
	public double getZ() {
		return (double)this.pos.getZ() + 0.5D;
	}

	@Override
	public BlockPos getBlockPos() {
		return pos;
	}

	@Override
	public PistonHandler e() {
		if(BlockHelper.hasBlockStateProperty(context.state, BambooLeaves.M) && overrideFacing != null)
			return context.state.a(BambooLeaves.M, overrideFacing);
		return context.state;
	}

	@Override
	@Nullable
	public <T extends BeehiveBlockEntity> T g() {
		return null;
	}

	@Override
	@Nullable
	public ServerWorld getWorld() {
		MinecraftServer server = context.world.l();
		return server != null ? server.getWorld(context.world.X()) : null;
	}
}