package com.roguelike.integration;

import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerData;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlaceholderBridge {
    public static void register(com.roguelike.RoguelikePlugin plugin) {
        plugin.getLogger().info("PlaceholderAPI detected. Use the documented Roguelike placeholders through external boards after installing a PlaceholderAPI expansion build.");
    }

    public static String resolve(Player player, String identifier) {
        if (player == null || identifier == null) return "";
        PlayerData data = PlayerDataManager.get(player);
        return switch (identifier.toLowerCase()) {
            case "level" -> String.valueOf(data.getLevel());
            case "total_exp" -> String.valueOf(data.getTotalExp());
            case "exp" -> String.valueOf(data.getExpForCurrentLevel());
            case "exp_next" -> String.valueOf(data.getExpToNextLevel());
            case "kills" -> String.valueOf(data.getKills());
            case "deaths" -> String.valueOf(data.getDeaths());
            case "weapon_name" -> weaponName(player);
            case "weapon_id" -> weaponId(player);
            case "weapon_damage" -> weaponDamage(player);
            case "weapon_speed" -> weaponSpeed(player);
            case "tickets_a" -> String.valueOf(countTickets(player, TicketType.TICKET_A));
            case "tickets_b" -> String.valueOf(countTickets(player, TicketType.TICKET_B));
            case "tickets_c" -> String.valueOf(countTickets(player, TicketType.TICKET_C));
            default -> "";
        };
    }

    private static String weaponName(Player player) {
        CustomWeapon weapon = currentWeapon(player);
        return weapon == null ? "无" : weapon.getName();
    }

    private static String weaponId(Player player) {
        CustomWeapon weapon = currentWeapon(player);
        return weapon == null ? "none" : weapon.getId();
    }

    private static String weaponDamage(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon weapon = WeaponManager.getTemplate(hand);
        WeaponInstanceData weaponData = WeaponManager.getData(hand);
        if (weapon == null || weaponData == null) return "0";
        return WeaponManager.format(weaponData.getTotalDamage(weapon), 1);
    }

    private static String weaponSpeed(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon weapon = WeaponManager.getTemplate(hand);
        WeaponInstanceData weaponData = WeaponManager.getData(hand);
        if (weapon == null || weaponData == null) return "0";
        return WeaponManager.format(weaponData.getTotalAttackSpeed(weapon), 2);
    }

    private static CustomWeapon currentWeapon(Player player) {
        return WeaponManager.getTemplate(player.getInventory().getItemInMainHand());
    }

    private static int countTickets(Player player, TicketType ticketType) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (TicketManager.getTicketType(stack) == ticketType) {
                count += stack.getAmount();
            }
        }
        return count;
    }
}
