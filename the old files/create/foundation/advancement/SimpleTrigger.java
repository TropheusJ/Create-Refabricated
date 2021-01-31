package com.simibubi.kinetic_api.foundation.advancement;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import com.google.gson.JsonObject;

public class SimpleTrigger extends CriterionTriggerBase<SimpleTrigger.Instance> implements ITriggerable {

	public SimpleTrigger(String id) {
		super(id);
	}

	@Override
	public com.simibubi.kinetic_api.foundation.advancement.SimpleTrigger.Instance conditionsFromJson(JsonObject json, AdvancementEntityPredicateDeserializer context) {
		return new com.simibubi.kinetic_api.foundation.advancement.SimpleTrigger.Instance(getId());
	}

	public void trigger(ServerPlayerEntity player){
		super.trigger(player, null);
	}
	
	public com.simibubi.kinetic_api.foundation.advancement.SimpleTrigger.Instance instance() {
		return new com.simibubi.kinetic_api.foundation.advancement.SimpleTrigger.Instance(getId());
	}

	public static class Instance extends CriterionTriggerBase.Instance {

		public Instance(Identifier idIn) {
			super(idIn, EntityPredicate.Extended.EMPTY); // FIXME: Is this right?
		}

		@Override
		protected boolean test(List<Supplier<Object>> suppliers) {
			return true;
		}
	}
}
