package com.harrytheewizard.advmultiplier;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.List;

public class MultiplierManager {

    private static MinecraftServer server;
    private static ServerPlayer lastBreakingPlayer = null;

    public static void setLastBreakingPlayer(ServerPlayer player) {
        lastBreakingPlayer = player;
    }

    public static ServerPlayer getLastBreakingPlayer() {
        return lastBreakingPlayer;
    }

    private static final Identifier BOSSBAR_ID =
        Identifier.fromNamespaceAndPath("adv_multiplier", "multiplier");

    // Matches recalculate.mcfunction exactly — story/root intentionally excluded
    private static final List<Identifier> TRACKED = List.of(
        // adventure (46)
        mc("adventure/adventuring_time"), mc("adventure/arbalistic"),
        mc("adventure/avoid_vibration"), mc("adventure/blowback"),
        mc("adventure/brush_armadillo"), mc("adventure/bullseye"),
        mc("adventure/craft_decorated_pot_using_only_sherds"),
        mc("adventure/crafters_crafting_crafters"),
        mc("adventure/fall_from_world_height"), mc("adventure/heart_transplanter"),
        mc("adventure/hero_of_the_village"), mc("adventure/honey_block_slide"),
        mc("adventure/kill_a_mob"), mc("adventure/kill_all_mobs"),
        mc("adventure/kill_mob_near_sculk_catalyst"), mc("adventure/lighten_up"),
        mc("adventure/lightning_rod_with_villager_no_fire"),
        mc("adventure/minecraft_trials_edition"), mc("adventure/ol_betsy"),
        mc("adventure/overoverkill"), mc("adventure/play_jukebox_in_meadows"),
        mc("adventure/read_power_of_chiseled_bookshelf"), mc("adventure/revaulting"),
        mc("adventure/salvage_sherd"), mc("adventure/shoot_arrow"),
        mc("adventure/sleep_in_bed"), mc("adventure/sniper_duel"),
        mc("adventure/spear_many_mobs"), mc("adventure/spyglass_at_dragon"),
        mc("adventure/spyglass_at_ghast"), mc("adventure/spyglass_at_parrot"),
        mc("adventure/summon_iron_golem"), mc("adventure/throw_trident"),
        mc("adventure/totem_of_undying"), mc("adventure/trade"),
        mc("adventure/trade_at_world_height"),
        mc("adventure/trim_with_all_exclusive_armor_patterns"),
        mc("adventure/trim_with_any_armor_pattern"),
        mc("adventure/two_birds_one_arrow"), mc("adventure/under_lock_and_key"),
        mc("adventure/use_lodestone"), mc("adventure/very_very_frightening"),
        mc("adventure/voluntary_exile"),
        mc("adventure/walk_on_powder_snow_with_leather_boots"),
        mc("adventure/who_needs_rockets"), mc("adventure/whos_the_pillager_now"),
        // end (8)
        mc("end/dragon_breath"), mc("end/dragon_egg"), mc("end/elytra"),
        mc("end/enter_end_gateway"), mc("end/find_end_city"), mc("end/kill_dragon"),
        mc("end/levitate"), mc("end/respawn_dragon"),
        // husbandry (29)
        mc("husbandry/allay_deliver_cake_to_note_block"),
        mc("husbandry/allay_deliver_item_to_player"),
        mc("husbandry/axolotl_in_a_bucket"), mc("husbandry/balanced_diet"),
        mc("husbandry/bred_all_animals"), mc("husbandry/breed_an_animal"),
        mc("husbandry/complete_catalogue"), mc("husbandry/feed_snifflet"),
        mc("husbandry/fishy_business"), mc("husbandry/froglights"),
        mc("husbandry/kill_axolotl_target"), mc("husbandry/leash_all_frog_variants"),
        mc("husbandry/make_a_sign_glow"), mc("husbandry/obtain_netherite_hoe"),
        mc("husbandry/obtain_sniffer_egg"), mc("husbandry/place_dried_ghast_in_water"),
        mc("husbandry/plant_any_sniffer_seed"), mc("husbandry/plant_seed"),
        mc("husbandry/remove_wolf_armor"), mc("husbandry/repair_wolf_armor"),
        mc("husbandry/ride_a_boat_with_a_goat"), mc("husbandry/safely_harvest_honey"),
        mc("husbandry/silk_touch_nest"), mc("husbandry/tactical_fishing"),
        mc("husbandry/tadpole_in_a_bucket"), mc("husbandry/tame_an_animal"),
        mc("husbandry/wax_off"), mc("husbandry/wax_on"), mc("husbandry/whole_pack"),
        // nether (22)
        mc("nether/all_effects"), mc("nether/all_potions"), mc("nether/brew_potion"),
        mc("nether/charge_respawn_anchor"), mc("nether/create_beacon"),
        mc("nether/create_full_beacon"), mc("nether/distract_piglin"),
        mc("nether/explore_nether"), mc("nether/fast_travel"),
        mc("nether/find_bastion"), mc("nether/find_fortress"),
        mc("nether/get_wither_skull"), mc("nether/loot_bastion"),
        mc("nether/netherite_armor"), mc("nether/obtain_ancient_debris"),
        mc("nether/obtain_blaze_rod"), mc("nether/obtain_crying_obsidian"),
        mc("nether/return_to_sender"), mc("nether/ride_strider"),
        mc("nether/ride_strider_in_overworld_lava"),
        mc("nether/summon_wither"), mc("nether/uneasy_alliance"),
        // story (15) — story/root excluded
        mc("story/cure_zombie_villager"), mc("story/deflect_arrow"),
        mc("story/enchant_item"), mc("story/enter_the_end"),
        mc("story/enter_the_nether"), mc("story/follow_ender_eye"),
        mc("story/form_obsidian"), mc("story/iron_tools"), mc("story/lava_bucket"),
        mc("story/mine_diamond"), mc("story/mine_stone"), mc("story/obtain_armor"),
        mc("story/shiny_gear"), mc("story/smelt_iron"), mc("story/upgrade_tools")
    );

    private static Identifier mc(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    // -------------------------------------------------------------------------

    public static void init(MinecraftServer srv) {
        server = srv;
        ServerScoreboard sb = srv.getScoreboard();

        if (sb.getObjective("adv.count") == null) {
            sb.addObjective("adv.count", ObjectiveCriteria.DUMMY,
                Component.literal("Advancement Count"),
                ObjectiveCriteria.RenderType.INTEGER, false, null);
        }
        if (sb.getObjective("multiplier.max") == null) {
            sb.addObjective("multiplier.max", ObjectiveCriteria.DUMMY,
                Component.literal("Drop Multiplier"),
                ObjectiveCriteria.RenderType.INTEGER, false, null);
        }

        if (srv.getCustomBossEvents().get(BOSSBAR_ID) == null) {
            CustomBossEvent bar = srv.getCustomBossEvents()
                .create(RandomSource.create(), BOSSBAR_ID,
                    Component.literal("Drop Multiplier: 1x"));
            bar.setColor(BossEvent.BossBarColor.PURPLE);
            bar.setVisible(true);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        CustomBossEvent bar = getBossbar();
        if (bar != null) bar.addPlayer(player);
        recalculate(player);
    }

    public static void recalculate(ServerPlayer player) {
        int count = 0;
        var advancements = player.getAdvancements();
        for (Identifier id : TRACKED) {
            AdvancementHolder holder = server.getAdvancements().get(id);
            if (holder == null) continue;
            AdvancementProgress progress = advancements.getOrStartProgress(holder);
            if (progress.isDone()) count++;
        }
        applyCount(player, count);
    }

    public static void onAdvancementGranted(ServerPlayer player, AdvancementHolder holder) {
        if (!TRACKED.contains(holder.id())) return;
        applyCount(player, getAdvCount(player) + 1);
    }

    public static long getMultiplierForPlayer(ServerPlayer player) {
        return computeMultiplier(getAdvCount(player));
    }

    // -------------------------------------------------------------------------

    private static void applyCount(ServerPlayer player, int count) {
        setScore(player, "adv.count", count);
        long multiplier = computeMultiplier(count);
        setScore(ScoreHolder.forNameOnly("#multiplier"), "multiplier.max", (int) Math.min(multiplier, Integer.MAX_VALUE));
        updateDisplay(player, multiplier);
    }

    private static long computeMultiplier(int advCount) {
        if (advCount <= 0) return 1L;
        if (advCount >= 62) return Long.MAX_VALUE / 2;
        return 1L << advCount;
    }

    private static int getAdvCount(ServerPlayer player) {
        ServerScoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective("adv.count");
        if (obj == null) return 0;
        ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(player, obj);
        return info != null ? info.value() : 0;
    }

    private static void setScore(ScoreHolder holder, String objectiveName, int value) {
        ServerScoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective(objectiveName);
        if (obj == null) return;
        sb.getOrCreatePlayerScore(holder, obj).set(value);
    }

    private static String formatMultiplier(long multiplier) {
        if (multiplier >= 1_000_000_000_000_000_000L) {
            return String.format("%.1fQi", multiplier / 1_000_000_000_000_000_000.0);
        } else if (multiplier >= 1_000_000_000_000_000L) {
            return String.format("%.1fQ", multiplier / 1_000_000_000_000_000.0);
        } else if (multiplier >= 1_000_000_000_000L) {
            return String.format("%.1fT", multiplier / 1_000_000_000_000.0);
        } else if (multiplier >= 1_000_000_000L) {
            return String.format("%.1fB", multiplier / 1_000_000_000.0);
        } else if (multiplier >= 1_000_000L) {
            return String.format("%.1fM", multiplier / 1_000_000.0);
        } else if (multiplier >= 1_000L) {
            return String.format("%.1fK", multiplier / 1_000.0);
        } else {
            return String.valueOf(multiplier);
        }
    }

    private static void updateDisplay(ServerPlayer player, long multiplier) {
        String label = formatMultiplier(multiplier) + "x";
        CustomBossEvent bar = getBossbar();
        if (bar != null) {
            bar.setName(Component.literal("Drop Multiplier: " + label)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
            bar.addPlayer(player);
        }
        player.sendSystemMessage(
            Component.literal("[Multiplier] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Drop multiplier: " + label)
                    .withStyle(ChatFormatting.YELLOW)));
    }
    private static CustomBossEvent getBossbar() {
        return server != null ? server.getCustomBossEvents().get(BOSSBAR_ID) : null;
    }
}
