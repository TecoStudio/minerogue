package com.roguelike.util;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;

import java.util.Iterator;

public class DisplayCleanup {
    public static void clearPlayer(Player player) {
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        player.playerListName(null);
        player.clearTitle();
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
    }

    public static int clearBossBars() {
        int removed = 0;
        Iterator<KeyedBossBar> bars = Bukkit.getBossBars();
        while (bars.hasNext()) {
            KeyedBossBar keyedBar = bars.next();
            BossBar bar = keyedBar;
            bar.removeAll();
            if (Bukkit.removeBossBar(keyedBar.getKey())) {
                removed++;
            }
        }
        return removed;
    }

    public static int clearAll() {
        Bukkit.getOnlinePlayers().forEach(DisplayCleanup::clearPlayer);
        return clearBossBars();
    }
}
