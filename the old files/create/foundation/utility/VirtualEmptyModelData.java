package com.simibubi.kinetic_api.foundation.utility;

import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * This model data_unused instance is passed whenever a model is rendered without
 * available in-world context. IBakedModel#getModelData can react accordingly
 * and avoid looking for model data_unused itself
 **/
public enum VirtualEmptyModelData implements IModelData {

	INSTANCE;

	@Override
	public boolean hasProperty(ModelProperty<?> prop) {
		return false;
	}

	@Override
	public <T> T getData(ModelProperty<T> prop) {
		return null;
	}

	@Override
	public <T> T setData(ModelProperty<T> prop, T data) {
		return null;
	}

}
