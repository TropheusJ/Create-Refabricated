package com.simibubi.create.content.contraptions.components.structureMovement;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

public class OrientedContraptionEntityRenderer extends ContraptionEntityRenderer<OrientedContraptionEntity> {

	public OrientedContraptionEntityRenderer(EntityRenderDispatcher p_i46179_1_) {
		super(p_i46179_1_);
	}

	@Override
	public boolean shouldRender(OrientedContraptionEntity entity, Frustum p_225626_2_, double p_225626_3_,
		double p_225626_5_, double p_225626_7_) {
		if (!super.shouldRender(entity, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_))
			return false;
		if (entity.getContraption()
				  .getType() == ContraptionType.MOUNTED && entity.getVehicle() == null)
			return false;
		return true;
	}

}
