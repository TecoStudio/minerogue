package com.roguelike.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class RoguelikeCommand implements CommandExecutor, TabCompleter {
    private final PlayerCommandHandler playerCommands = new PlayerCommandHandler();
    private final AdminCommandHandler adminCommands = new AdminCommandHandler();
    private final RoguelikeTabCompleter tabCompleter = new RoguelikeTabCompleter();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("rl")) {
            return playerCommands.handlePlayer(sender, args);
        } else if (cmd.equals("rw") || cmd.equals("roguelike")) {
            return adminCommands.handleAdmin(sender, args);
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabCompleter.onTabComplete(sender, command, alias, args);
    }
}
