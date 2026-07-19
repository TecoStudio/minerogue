package com.roguelike.mana;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ManaManager {
    private static final Map<UUID, ManaState> mana = new HashMap<>();
    private static int taskId = -1;

    private ManaManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        shutdown();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ManaManager::tick, 1L, 1L);
    }

    public static void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        mana.clear();
    }

    public static void track(Player player) {
        if (player == null) return;
        ManaState state = mana.computeIfAbsent(player.getUniqueId(), ignored -> new ManaState(maxMana()));
        state.maximum = maxMana();
        state.current = Math.min(state.maximum, Math.max(0.0, state.current));
        render(player, state);
    }

    public static void untrack(Player player) {
        if (player == null) return;
        mana.remove(player.getUniqueId());
    }

    public static boolean consume(Player player, double amount) {
        if (amount <= 0.0) return true;
        ManaState state = mana.computeIfAbsent(player.getUniqueId(), ignored -> new ManaState(maxMana()));
        if (state.current < amount) {
            render(player, state);
            return false;
        }
        state.current -= amount;
        render(player, state);
        return true;
    }

    public static double current(Player player) {
        return mana.computeIfAbsent(player.getUniqueId(), ignored -> new ManaState(maxMana())).current;
    }

    public static double maximum() {
        return maxMana();
    }

    static float progress(double current, double maximum) {
        if (maximum <= 0.0) return 0.0f;
        return (float) Math.max(0.0, Math.min(1.0, current / maximum));
    }

    private static void tick() {
        double maximum = maxMana();
        double regen = manaRegenPerTick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ManaState state = mana.computeIfAbsent(player.getUniqueId(), ignored -> new ManaState(maximum));
            state.maximum = maximum;
            state.current = Math.min(maximum, Math.max(0.0, state.current + regen));
            render(player, state);
        }
    }

    private static void render(Player player, ManaState state) {
        player.setLevel((int) Math.round(state.current));
        player.setExp(progress(state.current, state.maximum));
    }

    private static double maxMana() {
        if (ConfigManager.getPlugin() == null) return 100.0;
        return Math.max(1.0, ConfigManager.getPluginConfig().getDouble("gameplay.mana.max", 100.0));
    }

    private static double manaRegenPerTick() {
        if (ConfigManager.getPlugin() == null) return 0.05;
        return Math.max(0.0, ConfigManager.getPluginConfig().getDouble("gameplay.mana.regen-per-second", 1.0)) / 20.0;
    }

    private static final class ManaState {
        double current;
        double maximum;

        ManaState(double maximum) {
            this.maximum = maximum;
            this.current = maximum;
        }
    }
}
