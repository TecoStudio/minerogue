package com.roguelike.command;

import com.roguelike.armor.ArmorSetManager;
import com.roguelike.config.ConfigManager;
import com.roguelike.mob.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RoguelikeTabCompleter {
    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        List<String> list = new ArrayList<>();
        if (cmd.equals("rl")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("status", "tickets", "trade", "help"));
            }
        } else if (cmd.equals("rw") || cmd.equals("roguelike")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("export", "backup", "debug", "affixes", "reload", "give", "exp", "list", "stats", "reset", "monster", "fixhand", "help"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                list.addAll(Arrays.asList("weapon", "item", "armor", "ticket"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
                list.addAll(Arrays.asList("on", "off", "status"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
                list.add("top");
                Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            } else if (args.length == 3 && args[0].equalsIgnoreCase("stats") && args[1].equalsIgnoreCase("top")) {
                list.addAll(Arrays.asList("level", "kills", "deaths"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("monster")) {
                list.add("spawn");
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                switch (args[1].toLowerCase()) {
                    case "weapon" -> ConfigManager.getWeapons().forEach(w -> list.add(w.getId()));
                    case "item" -> ConfigManager.getItems().forEach(i -> list.add(i.getId()));
                    case "armor" -> list.addAll(ArmorSetManager.armorDefinitions().keySet());
                    case "ticket" -> list.addAll(GiveCommand.ticketIds());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("monster") && args[1].equalsIgnoreCase("spawn")) {
                list.addAll(MobManager.getSpawnableMobIds());
            } else if ((args.length == 4 && args[0].equalsIgnoreCase("give")) ||
                       (args.length == 2 && args[0].equalsIgnoreCase("reset"))) {
                Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
                list.addAll(Arrays.asList("weapons", "items", "armor"));
            }
        }
        return list;
    }
}
