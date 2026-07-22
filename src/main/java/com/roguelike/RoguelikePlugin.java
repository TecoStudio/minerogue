package com.roguelike;

import com.roguelike.command.RoguelikeCommand;
import com.roguelike.command.GiveCommand;
import com.roguelike.armor.ArmorSetManager;
import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.boss.BossEventManager;
import com.roguelike.combat.CombatHandler;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.forge.ForgeTableManager;
import com.roguelike.forge.ForgeRecipeManager;
import com.roguelike.integration.IntegrationManager;
import com.roguelike.level.LevelManager;
import com.roguelike.listener.EventListener;
import com.roguelike.mana.ManaManager;
import com.roguelike.mob.MobManager;
import com.roguelike.scoreboard.RoguelikeScoreboard;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.DevLog;
import com.roguelike.weapon.ToolAbilityManager;
import com.roguelike.weapon.BowAbilityManager;
import com.roguelike.weapon.WeaponAbilityManager;
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
        GiveCommand.init(this);
        TicketManager.init(this);
        MobManager.init(this);
        BossEventManager.init(this);
        LevelManager.init(this);
        ManaManager.init(this);
        CombatHandler.init(this);
        BowAbilityManager.init(this);
        WeaponAbilityManager.init(this);
        ToolAbilityManager.init(this);
        ArmorSetManager.init(this);
        ArmorAffixManager.init(this);
        ForgeRecipeManager.init(this);
        ForgeTableManager.init(this);
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
        BossEventManager.shutdown();
        MobManager.shutdown();
        WeaponAbilityManager.shutdown();
        BowAbilityManager.shutdown();
        RoguelikeScoreboard.shutdown();
        ManaManager.shutdown();
        PlayerDataManager.shutdown();
        DevLog.info("Roguelike plugin disabled.");
    }

    public static RoguelikePlugin getInstance() {
        return instance;
    }
}
