package com.simibubi.create.content.logistics.item.filter.attribute.astralsorcery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import com.simibubi.create.content.logistics.item.filter.ItemAttribute;

public class AstralSorceryCrystalAttribute implements ItemAttribute {
    String traitName;

    public AstralSorceryCrystalAttribute(String traitName) {
        this.traitName = traitName;
    }

    @Override
    public boolean appliesTo(ItemCooldownManager itemStack) {
        for (Tag trait : extractTraitList(itemStack)) {
            if(((CompoundTag) trait).getString("property").equals(this.traitName))
                return true;
        }
        return false;
    }

    @Override
    public List<ItemAttribute> listAttributesOf(ItemCooldownManager itemStack) {
        ListTag traits = extractTraitList(itemStack);
        List<ItemAttribute> atts = new ArrayList<>();
        for (int i = 0; i < traits.size(); i++) {
            atts.add(new AstralSorceryCrystalAttribute(traits.getCompound(i).getString("property")));
        }
        return atts;
    }

    @Override
    public String getTranslationKey() {
        return "astralsorcery_crystal";
    }

    @Override
    public Object[] getTranslationParameters() {
        Identifier traitResource = new Identifier(traitName);
        String something = new TranslatableText(String.format("crystal.property.%s.%s.name", traitResource.getNamespace(), traitResource.getPath())).getString();
        return new Object[] { something };
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        nbt.putString("property", this.traitName);
    }

    @Override
    public ItemAttribute readNBT(CompoundTag nbt) {
        return new AstralSorceryCrystalAttribute(nbt.getString("property"));
    }

    private ListTag extractTraitList(ItemCooldownManager stack) {
        return stack.o() != null ? stack.o().getCompound("astralsorcery").getCompound("crystalProperties").getList("attributes", 10) : new ListTag();
    }
}
