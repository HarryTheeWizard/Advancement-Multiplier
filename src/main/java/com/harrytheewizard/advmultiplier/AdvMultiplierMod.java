package com.harrytheewizard.advmultiplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvMultiplierMod implements ModInitializer {

    public static final String MOD_ID = "adv_multiplier_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Adv Multiplier Mod initializing");

        // Raise MAX_STACK_SIZE component on all items so NBT codec validation
        // accepts counts up to Integer.MAX_VALUE / 2 — without this, stacks above
        // the vanilla limit (e.g. 64) are silently discarded on world load/relog.
        DefaultItemComponentEvents.MODIFY.register(context ->
            context.modify(
                item -> {
                    Integer maxDmg = item.components().get(DataComponents.MAX_DAMAGE);
                    return maxDmg == null || maxDmg <= 0;
                },
                (builder, item) -> builder.set(DataComponents.MAX_STACK_SIZE, Integer.MAX_VALUE / 2)
            ));

        // Track the last player to break a block so cascaded drops (e.g. bed head
        // auto-breaking when the foot is broken) can still be multiplied even though
        // the cascade loot context has no THIS_ENTITY.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer sp) {
                MultiplierManager.setLastBreakingPlayer(sp);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(MultiplierManager::init);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            MultiplierManager.onPlayerJoin(handler.player));

        LootTableEvents.MODIFY_DROPS.register((lootTable, context, drops) -> {
            ServerPlayer player = null;

            // Entity kills — most specific first
            Player lastDamagePlayer = context.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);
            if (lastDamagePlayer instanceof ServerPlayer sp) player = sp;

            // Direct attacker (catches player vs. pet-assisted kills)
            if (player == null) {
                Entity attacker = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
                if (attacker instanceof ServerPlayer sp) player = sp;
            }

            // Interacting entity — chest / container opens
            if (player == null) {
                Entity interacting = context.getOptionalParameter(LootContextParams.INTERACTING_ENTITY);
                if (interacting instanceof ServerPlayer sp) player = sp;
            }

            // THIS_ENTITY — block breaks (the breaking entity) and fishing
            // Skip player death drops: death context always has DAMAGE_SOURCE present
            if (player == null) {
                Entity thisEntity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
                if (thisEntity instanceof ServerPlayer sp
                        && context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE) == null) {
                    player = sp;
                }
            }

            // Cascaded block drops (e.g. bed head auto-drops when foot is broken):
            // THIS_ENTITY is null in the cascade but the breaking player was just recorded
            if (player == null && context.getOptionalParameter(LootContextParams.BLOCK_STATE) != null) {
                player = MultiplierManager.getLastBreakingPlayer();
            }

            if (player == null) return;

            long multiplier = MultiplierManager.getMultiplierForPlayer(player);
            if (multiplier <= 1) return;

            for (var stack : drops) {
                if (stack.isEmpty()) continue;
                long newCount = stack.getCount() * multiplier;
                stack.setCount((int) Math.min(newCount, (long) (Integer.MAX_VALUE / 2)));
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) ->
            dispatcher.register(Commands.literal("advmultiplier")
                .then(Commands.literal("recalculate")
                    .executes(ctx -> {
                        try {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MultiplierManager.recalculate(player);
                            return 1;
                        } catch (Exception e) {
                            return 0;
                        }
                    }))));

        LOGGER.info("Adv Multiplier Mod ready");
    }
}
