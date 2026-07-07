package com.roguelike.command;

import com.roguelike.armor.ArmorSetManager;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

class GiveCommand {
    static List<String> ticketIds() {
        return Arrays.stream(TicketType.values())
                .map(TicketType::getId)
                .toList();
    }

    boolean handleGive(CommandSender sender, String[] args) {
        String type = args[1].toLowerCase();
        String id = args[2];
        Player target = args.length >= 4 ? Bukkit.getPlayer(args[3]) : (sender instanceof Player p ? p : null);
        int amount = 1;
        if (args.length >= 5) {
            try { amount = Integer.parseInt(args[4]); } catch (NumberFormatException e) { amount = 1; }
        }
        if (target == null) {
            Message.send(sender, "&c找不到玩家。");
            return true;
        }

        switch (type) {
            case "weapon" -> {
                CustomWeapon weapon = ConfigManager.getWeapon(id);
                if (weapon == null) {
                    Message.send(sender, "&c找不到武器: " + id);
                    return true;
                }
                ItemStack stack = WeaponManager.createWeaponStack(weapon, null);
                stack.setAmount(1);
                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(stack.clone());
                }
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + weapon.getName());
            }
            case "item" -> {
                CustomItem item = ConfigManager.getItem(id);
                if (item == null) {
                    Message.send(sender, "&c找不到物品: " + id);
                    return true;
                }
                ItemStack stack = new ItemStack(Material.PAPER);
                var meta = stack.getItemMeta();
                if (meta != null) {
                    meta.displayName(Message.toComponent("&f" + item.getName()));
                    stack.setItemMeta(meta);
                }
                stack.setAmount(amount);
                target.getInventory().addItem(stack);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + item.getName());
            }
            case "armor" -> {
                ItemStack stack = ArmorSetManager.createSetItem(id);
                if (stack == null) {
                    Message.send(sender, "&c找不到防具: " + id);
                    return true;
                }
                String name = ArmorSetManager.armorDefinitions().get(id.toLowerCase()).name();
                stack.setAmount(1);
                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(stack.clone());
                }
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + name);
            }
            case "ticket" -> {
                TicketType ticket = TicketType.fromId(id);
                if (ticket == null) {
                    Message.send(sender, "&c可用券类型: " + String.join(", ", ticketIds()));
                    return true;
                }
                ItemStack stack = TicketManager.createTicket(ticket);
                stack.setAmount(amount);
                target.getInventory().addItem(stack);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 张 " + ticket.getDisplayName());
            }
            default -> Message.send(sender, "&c用法: /rw give <weapon|item|armor|ticket> <id> [玩家] [数量]");
        }
        return true;
    }
}
