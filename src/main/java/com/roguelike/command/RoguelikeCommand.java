package com.roguelike.command;

import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerData;
import com.roguelike.mob.MobManager;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import com.roguelike.level.LevelManager;
import com.roguelike.scoreboard.ScoreboardManager;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RoguelikeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("rl")) {
            return handlePlayer(sender, args);
        } else if (cmd.equals("rw") || cmd.equals("roguelike")) {
            return handleAdmin(sender, args);
        }
        return false;
    }

    private boolean handlePlayer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令。");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showStatus(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "weapon", "w" -> showWeapon(player);
            case "tickets" -> showTickets(player);
            case "trade" -> showTrade(player);
            case "help", "?" -> showHelp(player);
            default -> Message.send(player, "&c未知命令。使用 /rl help");
        }
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("roguelike.admin")) {
            Message.send(sender, "&c你没有权限。");
            return true;
        }
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                ConfigManager.reload();
                Message.send(sender, "&a配置已重载。");
            }
            case "init" -> {
                return handleInit(sender);
            }
            case "give" -> {
                if (args.length < 3) {
                    Message.send(sender, "&c用法: /roguelike give <weapon|item|ticket> <id> [玩家] [数量]");
                    return true;
                }
                return handleGive(sender, args);
            }
            case "exp" -> {
                if (args.length < 2) {
                    Message.send(sender, "&c用法: /roguelike exp <数量> [玩家]");
                    return true;
                }
                Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    Message.send(sender, "&c找不到玩家。");
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    Message.send(sender, "&c经验数量必须是数字。");
                    return true;
                }
                LevelManager.addExperience(target, amount);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 经验。");
            }
            case "list" -> {
                if (args.length < 2) {
                    Message.send(sender, "&c用法: /rw list <weapons|items>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("weapons")) {
                    Message.send(sender, "&6可用武器:");
                    for (CustomWeapon w : ConfigManager.getWeapons()) {
                        sender.sendMessage("§7- " + WeaponManager.getRarityColor(w.getRarity()) + w.getName() + " §7(" + w.getId() + ")");
                    }
                } else if (args[1].equalsIgnoreCase("items")) {
                    Message.send(sender, "&6可用物品:");
                    for (CustomItem i : ConfigManager.getItems()) {
                        sender.sendMessage("§7- " + i.getName() + " (" + i.getId() + ")");
                    }
                }
            }
            case "stats" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    Message.send(sender, "&c找不到玩家。");
                    return true;
                }
                PlayerData data = PlayerDataManager.get(target);
                Message.send(sender, "&6&l═══ " + target.getName() + " 的状态 ═══");
                Message.send(sender, "&6等级: &e" + data.getLevel());
                Message.send(sender, "&6经验: &e" + data.getExpForCurrentLevel() + "/" + data.getExpToNextLevel());
                Message.send(sender, "&6击杀: &c" + data.getKills());
                Message.send(sender, "&6死亡: &4" + data.getDeaths());
            }
            case "reset" -> {
                Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    Message.send(sender, "&c找不到玩家。");
                    return true;
                }
                PlayerDataManager.unload(target);
                PlayerDataManager.get(target);
                LevelManager.updateExpBar(target);
                ScoreboardManager.updatePlayer(target);
                Message.send(sender, "&a已重置 " + target.getName() + " 的 Roguelike 数据。");
            }
            case "monster" -> {
                if (!(sender instanceof Player player)) {
                    Message.send(sender, "&c只有玩家可以生成怪物。");
                    return true;
                }
                if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
                    Message.send(sender, "&c用法: /rw monster spawn <zombie|skeleton|creeper|...>");
                    return true;
                }
                org.bukkit.entity.EntityType entityType;
                try {
                    entityType = org.bukkit.entity.EntityType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    Message.send(sender, "&c无效的怪物类型。");
                    return true;
                }
                var entity = player.getWorld().spawnEntity(player.getLocation(), entityType);
                if (entity instanceof org.bukkit.entity.LivingEntity living) {
                    com.roguelike.mob.MobManager.applyToMob(living);
                }
                Message.send(sender, "&a已生成 " + args[2] + "。");
            }
            case "fixhand" -> {
                if (!(sender instanceof Player player)) {
                    Message.send(sender, "&c只有玩家可以刷新手持武器。");
                    return true;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (!WeaponManager.refreshWeapon(hand)) {
                    Message.send(sender, "&c你手上没有 Roguelike 武器。");
                    return true;
                }
                WeaponManager.refreshHeldWeapon(player);
                Message.send(sender, "&a已刷新手持武器属性。");
            }
            case "help", "?" -> showAdminHelp(sender);
            default -> Message.send(sender, "&c未知命令。使用 /rw help");
        }
        return true;
    }

    private boolean handleInit(CommandSender sender) {
        var plugin = ConfigManager.getPlugin();
        plugin.getConfig().set("scoreboard.enabled", false);
        plugin.saveConfig();
        Bukkit.getOnlinePlayers().forEach(ScoreboardManager::clear);

        Path serverRoot = plugin.getDataFolder().toPath().getParent();
        Path tabScoreboard = serverRoot.resolve("TAB").resolve("scoreboards").resolve("roguelike.yml");
        Path mythicMob = serverRoot.resolve("MythicMobs").resolve("Mobs").resolve("RoguelikeElite.yml");
        Path mythicDrop = serverRoot.resolve("MythicMobs").resolve("DropTables").resolve("RoguelikeDrops.yml");
        Path notes = plugin.getDataFolder().toPath().resolve("init-notes.yml");

        try {
            writeFile(tabScoreboard, "scoreboards:\n" +
                    "  roguelike:\n" +
                    "    title: '&6&lRoguelike'\n" +
                    "    lines:\n" +
                    "      - '&7等级: &e%roguelike_level%'\n" +
                    "      - '&7经验: &e%roguelike_exp%/%roguelike_exp_next%'\n" +
                    "      - '&7击杀: &c%roguelike_kills%'\n" +
                    "      - '&7死亡: &4%roguelike_deaths%'\n" +
                    "      - '&7武器: &f%roguelike_weapon_name%'\n");
            writeFile(mythicMob, "RoguelikeSkeletonKnight:\n" +
                    "  Type: SKELETON\n" +
                    "  Display: '&6Skeletal Knight'\n" +
                    "  Health: 40\n" +
                    "  Damage: 6\n" +
                    "  Options:\n" +
                    "    PreventOtherDrops: false\n" +
                    "  Drops:\n" +
                    "    - RoguelikeEliteDrops\n");
            writeFile(mythicDrop, "RoguelikeEliteDrops:\n" +
                    "  Drops:\n" +
                    "    - exp 20 1\n" +
                    "    - paper 1 0.2\n");
            writeFile(notes, "roguelike-init:\n" +
                    "  scoreboard: 'disabled in Roguelike/config.yml; use TAB + PlaceholderAPI instead'\n" +
                    "  commandapi: 'Bukkit commands are registered; CommandAPI is optional'\n" +
                    "  placeholderapi: 'Use %roguelike_*% placeholders in TAB or other display plugins'\n" +
                    "  mythicmobs: 'Example mob/drop files were generated under plugins/MythicMobs'\n" +
                    "  nova: 'Set weapon item fields to nova:<namespace>:<id> in Roguelike weapon json files'\n");
        } catch (IOException e) {
            Message.send(sender, "&c初始化配置失败: " + e.getMessage());
            return true;
        }

        Message.send(sender, "&aRoguelike 初始化完成。");
        Message.send(sender, "&7已关闭内置侧边栏，并生成 TAB/MythicMobs 示例配置。请重载对应插件使配置生效。");
        return true;
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private boolean handleGive(CommandSender sender, String[] args) {
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
                // 简化为给予一个带名称的纸
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
            case "ticket" -> {
                TicketType ticket = TicketType.fromId(id);
                if (ticket == null) {
                    Message.send(sender, "&c可用券类型: ticket_a, ticket_b, ticket_c, weapon_development");
                    return true;
                }
                ItemStack stack = TicketManager.createTicket(ticket);
                stack.setAmount(amount);
                target.getInventory().addItem(stack);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 张 " + ticket.getDisplayName());
            }
            default -> Message.send(sender, "&c用法: /rw give <weapon|item|ticket> <id> [玩家] [数量]");
        }
        return true;
    }

    private void showStatus(Player player) {
        PlayerData data = PlayerDataManager.get(player);
        Message.send(player, "&6&l═══ 状态 ═══");
        Message.send(player, "&6等级: &e" + data.getLevel());
        Message.send(player, "&6经验: &e" + data.getExpForCurrentLevel() + "/" + data.getExpToNextLevel());
        Message.send(player, "&6击杀: &c" + data.getKills());
        Message.send(player, "&6死亡: &4" + data.getDeaths());
    }

    private void showWeapon(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomWeapon template = WeaponManager.getTemplate(hand);
        if (template == null) {
            Message.send(player, "&c你手上没有 Roguelike 武器。");
            return;
        }
        Message.send(player, "&6&l═══ 武器信息 ═══");
        Message.send(player, "&7模板: &f" + template.getName() + " (" + template.getId() + ")");
        Message.send(player, "&7总伤害: &f" + WeaponManager.format(WeaponManager.getData(hand).getTotalDamage(template), 1));
        Message.send(player, "&7总攻速: &f" + WeaponManager.format(WeaponManager.getData(hand).getTotalAttackSpeed(template), 2));
        Message.send(player, "&7材质原版攻速: &f" + WeaponManager.format(WeaponManager.getVanillaMainHandAttackSpeed(hand.getType()), 2));
        Message.send(player, "&7品质: " + WeaponManager.getRarityColor(template.getRarity()) + template.getRarity());
    }

    private void showTickets(Player player) {
        int a = 0, b = 0, c = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            TicketType type = TicketManager.getTicketType(stack);
            if (type == null) continue;
            switch (type) {
                case TICKET_A -> a += stack.getAmount();
                case TICKET_B -> b += stack.getAmount();
                case TICKET_C -> c += stack.getAmount();
            }
        }
        Message.send(player, "&6&l═══ 武器券 ═══");
        Message.send(player, "&c强化券: &f" + a);
        Message.send(player, "&a开发券: &f" + b);
        Message.send(player, "&9重置券: &f" + c);
    }

    private void showTrade(Player player) {
        Message.send(player, "&6&l═══ 自由交易 ═══");
        Message.send(player, "&7本服不使用金币、余额、商店或价格系统。");
        Message.send(player, "&7武器、强化券和掉落物都可以由玩家自行协商交换。");
        Message.send(player, "&e建议使用原版容器、投掷物品或其他服务器已有安全交易插件完成交换。");
    }

    private void showHelp(Player player) {
        Message.send(player, "&6&l═══ Roguelike 帮助 ═══");
        Message.send(player, "&e/rl &7- 查看状态");
        Message.send(player, "&e/rl weapon &7- 查看手持武器");
        Message.send(player, "&e/rl tickets &7- 查看武器券");
        Message.send(player, "&e/rl trade &7- 查看自由交易说明");
    }

    private void showAdminHelp(CommandSender sender) {
        Message.send(sender, "&6&l═══ Roguelike 管理员帮助 ═══");
        Message.send(sender, "&e/rw init &7- 生成推荐外部插件配置并关闭内置侧边栏");
        Message.send(sender, "&e/rw reload &7- 重载配置");
        Message.send(sender, "&e/rw give weapon <id> [玩家] [数量] &7- 给予武器");
        Message.send(sender, "&e/rw exp <数量> [玩家] &7- 给予经验");
        Message.send(sender, "&e/rw list <weapons|items> &7- 列出模板");
        Message.send(sender, "&e/rw stats [玩家] &7- 查看玩家状态");
        Message.send(sender, "&e/rw reset [玩家] &7- 重置玩家数据");
        Message.send(sender, "&e/rw monster spawn <类型> &7- 生成自定义怪物");
        Message.send(sender, "&e/rw fixhand &7- 刷新手持武器属性");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        List<String> list = new ArrayList<>();
        if (cmd.equals("rl")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("status", "weapon", "tickets", "trade", "help"));
            }
        } else if (cmd.equals("rw") || cmd.equals("roguelike")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("init", "reload", "give", "exp", "list", "stats", "reset", "monster", "fixhand", "help"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                list.addAll(Arrays.asList("weapon", "item", "ticket"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("monster")) {
                list.add("spawn");
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                switch (args[1].toLowerCase()) {
                    case "weapon" -> ConfigManager.getWeapons().forEach(w -> list.add(w.getId()));
                    case "item" -> ConfigManager.getItems().forEach(i -> list.add(i.getId()));
                    case "ticket" -> list.addAll(Arrays.asList("ticket_a", "ticket_b", "ticket_c", "weapon_development"));
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("monster") && args[1].equalsIgnoreCase("spawn")) {
                list.addAll(Arrays.asList("zombie", "skeleton", "creeper", "spider", "enderman", "blaze"));
            } else if ((args.length == 4 && args[0].equalsIgnoreCase("give")) ||
                       ((args.length == 2 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("reset"))))) {
                Bukkit.getOnlinePlayers().forEach(p -> list.add(p.getName()));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
                list.addAll(Arrays.asList("weapons", "items"));
            }
        }
        return list;
    }
}
