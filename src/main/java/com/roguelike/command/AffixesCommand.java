package com.roguelike.command;

import com.roguelike.equipment.EquipmentKind;
import com.roguelike.equipment.affix.AffixManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

class AffixesCommand {
    boolean handleAffixes(CommandSender sender, String[] args) {
        if (isHeldMode(args)) {
            Player target = heldTarget(args, sender);
            if (target != null) {
                showHeldWeaponAffixes(sender, target);
            } else if (sender instanceof Player player) {
                showHeldWeaponAffixes(player, player);
            } else {
                Message.send(sender, "&c只有玩家可以查看手持武器词条，或使用 /rw affixes held <玩家>。");
            }
            return true;
        }
        if (args.length >= 2) {
            Message.send(sender, "&c用法: /rw affixes [held]");
            return true;
        }

        Message.send(sender, "&6&l═══ 可用武器词条 ═══");
        Message.send(sender, "&e基础属性:");
        for (String stat : TicketManager.getBaseStatKeys()) {
            sender.sendMessage("§7- §f" + TicketManager.getStatDisplayName(stat) + " §8(" + stat + ")");
        }
        Message.send(sender, "&e效果词条:");
        for (String stat : TicketManager.getEffectStatKeys()) {
            sender.sendMessage("§7- §f" + TicketManager.getStatDisplayName(stat) + " §8(" + stat + ")");
        }
        Message.send(sender, "&6&l═══ 可用防具词条 ═══");
        Message.send(sender, "&e防具专属词条:");
        for (String stat : AffixManager.armorEffectIds()) {
            sender.sendMessage("§7- §f" + AffixManager.displayName(EquipmentKind.ARMOR, stat) + " §8(" + stat + ")");
        }
        return true;
    }

    static boolean isHeldMode(String[] args) {
        return (args.length == 2 || args.length == 3) && (args[1].equalsIgnoreCase("held") || args[1].equalsIgnoreCase("hand"));
    }

    static String heldTargetName(String[] args) {
        return isHeldMode(args) && args.length == 3 ? args[2] : null;
    }

    private Player heldTarget(String[] args, CommandSender sender) {
        String name = heldTargetName(args);
        if (name == null) return null;
        Player target = Bukkit.getPlayer(name);
        if (target == null) Message.send(sender, "&c找不到玩家: " + name);
        return target;
    }

    private void showHeldWeaponAffixes(CommandSender viewer, Player holder) {
        ItemStack hand = holder.getInventory().getItemInMainHand();
        CustomWeapon template = WeaponManager.getTemplate(hand);
        WeaponInstanceData data = WeaponManager.getData(hand);
        if (template == null || data == null) {
            Message.send(viewer, "&c" + holder.getName() + " 手上没有 Roguelike 武器。");
            return;
        }

        Message.send(viewer, "&6&l═══ 当前武器词条 ═══");
        Message.send(viewer, "&7玩家: &f" + holder.getName());
        Message.send(viewer, "&7模板: &f" + template.getName() + " &8(" + template.getId() + ")");
        Message.send(viewer, "&7等级/品质: &f" + data.getGearLevel() + " / " + data.getQuality()
                + " &7强度: &f" + data.getGearPower()
                + " &7随机槽: &f" + data.getRandomAffixCount() + "/" + data.getRandomAffixSlotLimit(template)
                + (template.allowsOverflowAffixes() ? " &d可突破" : ""));
        sendAffixLine(viewer, "damage", template.getBaseDamage(), data.getTotalDamage(template));
        sendAffixLine(viewer, "attack_speed", template.getAttackSpeed(), data.getTotalAttackSpeed(template));
        sendAffixLine(viewer, "attack_range", template.getEffect("attack_range", 3.0), data.getTotalEffect(template, "attack_range", 3.0));
        for (String stat : TicketManager.getEffectStatKeys()) {
            if (stat.equals("attack_range")) continue;
            double base = template.getEffect(stat, 0.0);
            double total = data.getTotalEffect(template, stat, 0.0);
            if (base != 0.0 || total != 0.0) {
                sendAffixLine(viewer, stat, base, total);
            }
        }
        Message.send(viewer, "&7强化券使用次数: &f" + data.getTicketAUses() + " &7连续失败: &f" + data.getTicketAFailStreak());
        Message.send(viewer, "&7开发券使用次数: &f" + data.getTicketBUses() + " &7移除券: &f" + data.getTicketCUses());
    }

    private void sendAffixLine(CommandSender sender, String stat, double base, double total) {
        String text = "§7- §f" + TicketManager.getStatDisplayName(stat)
                + " §8(" + stat + ") §7基础: §f" + TicketManager.formatStatValue(stat, base)
                + " §7当前: §e" + TicketManager.formatStatValue(stat, total);
        if (base != total) {
            String sign = total > base ? "+" : "";
            text += " §a(" + sign + TicketManager.formatStatValue(stat, total - base) + ")";
        }
        sender.sendMessage(text);
    }
}
