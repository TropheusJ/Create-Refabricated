package com.simibubi.kinetic_api.foundation.tileEntity.behaviour.scrollvalue;

import com.simibubi.kinetic_api.foundation.tileEntity.SmartTileEntity;
import com.simibubi.kinetic_api.foundation.tileEntity.behaviour.ValueBoxTransform;
import net.minecraft.text.Text;

public class ScrollOptionBehaviour<E extends Enum<E> & INamedIconOptions> extends ScrollValueBehaviour {

	private E[] options;

	public ScrollOptionBehaviour(Class<E> enum_, Text label, SmartTileEntity te, ValueBoxTransform slot) {
		super(label, te, slot);
		options = enum_.getEnumConstants();
		between(0, options.length - 1);
		withStepFunction((c) -> -1);
	}

	INamedIconOptions getIconForSelected() {
		return get();
	}
	
	public E get() {
		return options[scrollableValue];
	}

}
