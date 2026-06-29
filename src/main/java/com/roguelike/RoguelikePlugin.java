package com.roguelike;

import com.roguelike.command.RoguelikeCommand;
import com.roguelike.combat.CombatHandler;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.level.LevelManager;
import com.roguelike.listener.EventListener;
import com.roguelike.mob.MobManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.DevLog;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RoguelikePlugin extends JavaPlugin {
    private static RoguelikePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        DevLog.init(this);

        ConfigManager.loadAll(this);
        IntegrationManager.init(this);
        PlayerDataManager.init(this);
        WeaponManager.init(this);
        TicketManager.init(this);
        MobManager.init(this);
        LevelManager.init(this);
        CombatHandler.init(this);
        RoguelikeScoreboard.init(this);

        getServer().getPluginManager().registerEvents(new EventListener(), this);

        RoguelikeCommand command = new RoguelikeCommand();
        getCommand("rl").setExecutor(command);
        getCommand("rl").setTabCompleter(command);
        getCommand("rw").setExecutor(command);
        getCommand("rw").setTabCompleter(command);

        DevLog.info("Roguelike plugin enabled.");
    }

    @Override
    public void onDisable() {
        RoguelikeScoreboard.shutdown();
        PlayerDataManager.shutdown();
        DevLog.info("Roguelike plugin disabled.");
    }

    public static RoguelikePlugin getInstance() {
        return instance;
    }
}
