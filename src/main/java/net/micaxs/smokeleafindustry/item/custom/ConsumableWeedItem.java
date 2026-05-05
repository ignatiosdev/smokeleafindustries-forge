package net.micaxs.smokeleafindustry.item.custom;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class ConsumableWeedItem extends WeedDerivedItem {
    private final int maxCharges;
    private final float damagePerSecond;

    public ConsumableWeedItem(Properties pProperties, float effectDurationMultiplier, float stonedChance, UseAnim useAnimation, int maxCharges, float damagePerSecond) {
        super(pProperties, effectDurationMultiplier, stonedChance, useAnimation, Integer.MAX_VALUE);
        this.maxCharges = maxCharges;
        this.damagePerSecond = damagePerSecond;
    }

    public int getMaxCharges() {
        return this.maxCharges;
    }

    public float getDamagePerSecond() {
        return this.damagePerSecond;
    }

    public int getChargesRemaining(ItemStack pStack) {
        var tag = pStack.getTag();
        if (tag == null || !tag.contains("charges_remaining")) {
            return this.maxCharges;
        }
        return tag.getInt("charges_remaining");
    }
}