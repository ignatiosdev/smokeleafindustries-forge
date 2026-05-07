package net.micaxs.smokeleafindustry.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.micaxs.smokeleafindustry.SmokeleafIndustryMod;
import net.micaxs.smokeleafindustry.effect.ModEffects;
import net.micaxs.smokeleafindustry.item.ModItems;
import net.micaxs.smokeleafindustry.item.custom.BaseWeedItem;
import net.micaxs.smokeleafindustry.item.custom.ConsumableWeedItem;
import net.micaxs.smokeleafindustry.utils.WeedEffectHelper;
import net.micaxs.smokeleafindustry.villager.ModVillagers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod.EventBusSubscriber(modid = SmokeleafIndustryMod.MOD_ID)
public class ModEvents {
        private static final Logger LOGGER = LogManager.getLogger();

        @SubscribeEvent
        public static void addCustomTrades(VillagerTradesEvent event) {

                if (event.getType() == ModVillagers.HERB_DEALER.get()) {

                        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

                        // Level 1 Trades
                        trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.IRON_INGOT, 3),
                                        new ItemStack(ModItems.BUBBLE_KUSH_BAG.get(), 1),
                                        64, 2, 0.02f));

                        trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.IRON_INGOT, 2),
                                        new ItemStack(ModItems.LEMON_HAZE_BAG.get(), 1),
                                        64, 2, 0.02f));

                        // Level 2 Trades
                        trades.get(2).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.GOLD_INGOT, 3),
                                        new ItemStack(ModItems.SOUR_DIESEL_BAG.get(), 1),
                                        64, 4, 0.02f));

                        trades.get(2).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.GOLD_INGOT, 2),
                                        new ItemStack(ModItems.BLUE_ICE_BAG.get(), 1),
                                        64, 4, 0.02f));

                        // Level 3 Trades
                        trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.DIAMOND, 3),
                                        new ItemStack(ModItems.BUBBLEGUM_BAG.get(), 1),
                                        64, 6, 0.02f));

                        trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.DIAMOND, 2),
                                        new ItemStack(ModItems.BLUE_ICE_BAG.get(), 1),
                                        64, 6, 0.02f));

                        // Level 4 Trades
                        trades.get(4).add((pTrader, pRandom) -> new MerchantOffer(
                                        new ItemStack(Items.NETHERITE_INGOT, 1),
                                        new ItemStack(ModItems.PURPLE_HAZE_BAG.get(), 1),
                                        64, 8, 0.02f));

                }

        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
                LivingEntity entity = event.getEntity();
                if (!(entity instanceof Player player) || player.level().isClientSide) {
                        return;
                }

                // Check if player was smoking a consumable weed item
                ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (mainHand.getItem() instanceof ConsumableWeedItem && mainHand.getTag() != null
                                && mainHand.getTag().contains("ticks_held")) {
                        // Broadcast custom death message to all players
                        Component deathMessage = Component
                                        .literal("§c" + player.getName().getString() + " found out smoking kills");
                        player.level().getServer().getPlayerList().broadcastSystemMessage(deathMessage, false);
                }
        }

        @SubscribeEvent
        public static void onItemUsageTick(LivingEntityUseItemEvent.Tick event) {
                ItemStack eventStack = event.getItem();
                if (!(eventStack.getItem() instanceof ConsumableWeedItem consumableWeed)) {
                        return;
                }

                LivingEntity entity = event.getEntity();
                if (entity.level().isClientSide) {
                        return;
                }

                if (!(entity instanceof Player player)) {
                        return;
                }

                // Get the actual itemstack from player's hand to persist changes
                ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                CompoundTag tag = itemStack.getOrCreateTag();

                // Initialize on first use
                if (!tag.contains("ticks_held")) {
                        tag.putInt("ticks_held", 0);
                        LOGGER.info("[ConsumableWeed] INITIALIZED: {} max durability", itemStack.getMaxDamage());
                }

                // Check if item is broken
                if (itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
                        LOGGER.warn("[ConsumableWeed] ITEM BROKEN - stopping use. Player health: {}",
                                        player.getHealth());
                        player.stopUsingItem();
                        return;
                }

                int ticksHeld = tag.getInt("ticks_held") + 1;
                tag.putInt("ticks_held", ticksHeld);

                // Every 20 ticks (1 second)
                if (ticksHeld % 20 == 0) {
                        int damageValue = itemStack.getDamageValue();
                        LOGGER.info("[ConsumableWeed] TICK {} - durability: {}/{}, health: {}", ticksHeld, damageValue,
                                        itemStack.getMaxDamage(), player.getHealth());
                        applyConsumableEffects(itemStack, entity, player, consumableWeed, tag);

                        // Apply damage to the item (this is the standard way to reduce durability)
                        itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));

                        LOGGER.info("[ConsumableWeed] DURABILITY AFTER: {}/{}", itemStack.getDamageValue(),
                                        itemStack.getMaxDamage());

                        if (itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
                                LOGGER.info("[ConsumableWeed] ITEM BROKEN - stopping");
                                player.stopUsingItem();
                        }
                }

                // Persist ticks_held
                itemStack.setTag(tag);
        }

        @SubscribeEvent
        public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
                ItemStack eventStack = event.getItem();
                if (!(eventStack.getItem() instanceof ConsumableWeedItem)) {
                        return;
                }

                LivingEntity entity = event.getEntity();
                if (entity.level().isClientSide) {
                        return;
                }

                if (!(entity instanceof Player player)) {
                        return;
                }

                LOGGER.info("[ConsumableWeed] FINISH EVENT CALLED");

                // Get the actual itemstack from player's hand
                ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                int damageValue = itemStack.getDamageValue();
                int maxDamage = itemStack.getMaxDamage();

                LOGGER.info("[ConsumableWeed] FINISH - durability: {}/{}", damageValue, maxDamage);

                // Check if item is broken
                if (damageValue >= maxDamage) {
                        LOGGER.info("[ConsumableWeed] ITEM BROKEN - removing from inventory");
                        itemStack.shrink(1);
                } else {
                        LOGGER.info("[ConsumableWeed] USER RELEASED - item preserved with durability: {}/{}",
                                        damageValue, maxDamage);
                        // Clear ticks_held to reset when player uses again
                        CompoundTag tag = itemStack.getTag();
                        if (tag != null) {
                                tag.remove("ticks_held");
                        }
                }
        }

        private static void applyConsumableEffects(ItemStack itemStack, LivingEntity entity, Player player,
                        ConsumableWeedItem consumableWeed, CompoundTag tag) {
                float healthBefore = entity.getHealth();

                // Damage
                entity.hurt(entity.damageSources().generic(), consumableWeed.getDamagePerSecond());
                LOGGER.info("[ConsumableWeed] DAMAGE - health before: {}, after: {}", healthBefore, entity.getHealth());

                // Particles
                spawnConsumableParticles(entity);

                // Effects
                BaseWeedItem activeWeed = WeedEffectHelper.getActiveWeedIngredient(itemStack);
                if (activeWeed != null) {
                        int prevDuration = 0;
                        if (entity.hasEffect(activeWeed.getEffect())) {
                                prevDuration = entity.getEffect(activeWeed.getEffect()).getDuration();
                        }
                        // Use strain's base duration multiplied by consumable's duration multiplier
                        int durationToAdd = (int) (activeWeed.getDuration() * consumableWeed.getEffectFactor());
                        entity.addEffect(new MobEffectInstance(activeWeed.getEffect(), prevDuration + durationToAdd,
                                        activeWeed.getEffectAmplifier()));



                                        
                        LOGGER.info("[ConsumableWeed] EFFECT APPLIED: {} (duration: {} * {}) = {}, new total: {}",
                                        activeWeed.getEffect().getDescriptionId(), activeWeed.getDuration(),
                                        consumableWeed.getEffectFactor(),
                                        durationToAdd, prevDuration + durationToAdd);
                }

                // 50% chance to add 2 seconds of stoned per charge consumed.
                if (entity.level().random.nextDouble() <= 0.5f) {
                        int prevStonedDuration = 0;
                        if (entity.hasEffect(ModEffects.STONED.get())) {
                                prevStonedDuration = entity.getEffect(ModEffects.STONED.get()).getDuration();
                        }
                        int stonedDurationToAdd = 40;
                        entity.addEffect(new MobEffectInstance(ModEffects.STONED.get(),
                                        prevStonedDuration + stonedDurationToAdd, 1));
                        LOGGER.info("[ConsumableWeed] STONED APPLIED (new duration: {})",
                                        prevStonedDuration + stonedDurationToAdd);
                }
        }

        private static void spawnConsumableParticles(LivingEntity entity) {
                Level level = entity.level();
                if (level.isClientSide) {
                        return; // Particles spawn on client side only
                }

                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
                for (int i = 0; i < 5; i++) {
                        double xOffset = level.random.nextGaussian() * 0.02D;
                        double yOffset = level.random.nextGaussian() * 0.02D;
                        double zOffset = level.random.nextGaussian() * 0.02D;
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                        entity.getX() + entity.getBbWidth() * (level.random.nextDouble() - 0.5D),
                                        entity.getEyeY(),
                                        entity.getZ() + entity.getBbWidth() * (level.random.nextDouble() - 0.5D),
                                        1, xOffset, yOffset, zOffset, 0.0D);
                }
        }

}

// mcmeta
// animation {
// interpolate
//
// }