package com.roguelike.command;

import com.roguelike.armor.ArmorSetManager;
import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomItemStackFactory;
import com.roguelike.item.CustomWeapon;
import com.roguelike.ticket.TicketManager;
import com.roguelike.ticket.TicketType;
import com.roguelike.util.Message;
import com.roguelike.weapon.WeaponManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GiveCommand {
    private static final int MAX_GIVE_AMOUNT = 64;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    public static void init(RoguelikePlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new GiveGuiListener(), plugin);
    }

    static List<String> ticketIds() {
        return Arrays.stream(TicketType.values())
                .map(TicketType::getId)
                .toList();
    }

    boolean handleGive(CommandSender sender, String[] args) {
        if (isGuiMode(args)) {
            if (!(sender instanceof Player player)) {
                Message.send(sender, "&c控制台请使用: /rw give <weapon|item|armor|ticket> <id> <玩家> [数量]");
                return true;
            }
            openCategoryGui(player);
            return true;
        }
        if (args.length < 3) {
            Message.send(sender, "&c用法: /rw give 或 /rw give <weapon|item|armor|ticket> <id> [玩家] [数量]");
            return true;
        }
        String type = args[1].toLowerCase();
        String id = args[2];
        Player target = args.length >= 4 ? Bukkit.getPlayer(args[3]) : (sender instanceof Player p ? p : null);
        int amount = args.length >= 5 ? parseAmount(args[4]) : 1;
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
                giveCopies(target, stack, amount);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + weapon.getName());
            }
            case "item" -> {
                CustomItem item = ConfigManager.getItem(id);
                if (item == null) {
                    Message.send(sender, "&c找不到物品: " + id);
                    return true;
                }
                ItemStack stack = CustomItemStackFactory.createItemStack(item);
                giveCopies(target, stack, amount);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + item.getName());
            }
            case "armor" -> {
                ItemStack stack = ArmorSetManager.createSetItem(id);
                if (stack == null) {
                    Message.send(sender, "&c找不到防具: " + id);
                    return true;
                }
                String name = ArmorSetManager.armorDefinitions().get(id.toLowerCase()).name();
                giveCopies(target, stack, amount);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 个 " + name);
            }
            case "ticket" -> {
                TicketType ticket = TicketType.fromId(id);
                if (ticket == null) {
                    Message.send(sender, "&c可用券类型: " + String.join(", ", ticketIds()));
                    return true;
                }
                ItemStack stack = TicketManager.createTicket(ticket);
                giveCopies(target, stack, amount);
                Message.send(sender, "&a已给予 " + target.getName() + " " + amount + " 张 " + ticket.getDisplayName());
            }
            default -> Message.send(sender, "&c用法: /rw give <weapon|item|armor|ticket> <id> [玩家] [数量]");
        }
        return true;
    }

    static boolean isGuiMode(String[] args) {
        return args.length == 1;
    }

    static int clampPage(int page, int itemCount) {
        if (itemCount <= 0) return 0;
        int maxPage = Math.max(0, (itemCount - 1) / PAGE_SIZE);
        return Math.max(0, Math.min(page, maxPage));
    }

    static int parseAmount(String raw) {
        if (raw == null) return 1;
        try {
            int amount = Integer.parseInt(raw);
            return Math.max(1, Math.min(MAX_GIVE_AMOUNT, amount));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static void giveCopies(Player target, ItemStack template, int amount) {
        template.setAmount(1);
        for (int i = 0; i < amount; i++) {
            target.getInventory().addItem(template.clone());
        }
    }

    private static void openCategoryGui(Player player) {
        Inventory inventory = Bukkit.createInventory(new GiveGuiHolder(GiveGuiType.CATEGORY, 0), 27, Message.toComponent("&6选择给予类型"));
        fill(inventory);
        inventory.setItem(10, guiItem(Material.IRON_SWORD, "&e武器", List.of("&7点击打开武器列表")));
        inventory.setItem(12, guiItem(Material.PAPER, "&e物品", List.of("&7点击打开物品列表")));
        inventory.setItem(14, guiItem(Material.IRON_CHESTPLATE, "&e防具", List.of("&7点击打开防具列表")));
        inventory.setItem(16, guiItem(Material.NAME_TAG, "&e强化券", List.of("&7点击打开券列表")));
        player.openInventory(inventory);
    }

    private static void openListGui(Player player, GiveGuiType type, int page) {
        List<GiveEntry> entries = entries(type);
        int clamped = clampPage(page, entries.size());
        Inventory inventory = Bukkit.createInventory(new GiveGuiHolder(type, clamped), 54, Message.toComponent("&6给予: " + type.displayName));
        fill(inventory);
        int start = clamped * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            GiveEntry entry = entries.get(i);
            inventory.setItem(i - start, guiItem(entry.icon, "&e" + entry.name, List.of(
                    "&7ID: &f" + entry.id,
                    "&7类型: &f" + type.displayName,
                    "&e点击给予自己 1 个"
            )));
        }
        inventory.setItem(PREV_SLOT, guiItem(Material.ARROW, "&e上一页", List.of("&7第 " + (clamped + 1) + " 页")));
        inventory.setItem(INFO_SLOT, guiItem(Material.BOOK, "&6" + type.displayName, List.of(
                "&7总数: &f" + entries.size(),
                "&7页码: &f" + (clamped + 1) + "/" + (Math.max(0, (entries.size() - 1) / PAGE_SIZE) + 1),
                "&7左键条目直接给予"
        )));
        inventory.setItem(NEXT_SLOT, guiItem(Material.ARROW, "&e下一页", List.of("&7第 " + (clamped + 1) + " 页")));
        player.openInventory(inventory);
    }

    private static List<GiveEntry> entries(GiveGuiType type) {
        List<GiveEntry> entries = new ArrayList<>();
        switch (type) {
            case WEAPON -> ConfigManager.getWeapons().stream()
                    .sorted(Comparator.comparing(CustomWeapon::getId))
                    .forEach(w -> entries.add(new GiveEntry(w.getId(), w.getName(), material(w.getItem(), Material.IRON_SWORD))));
            case ITEM -> ConfigManager.getItems().stream()
                    .sorted(Comparator.comparing(CustomItem::getId))
                    .forEach(i -> entries.add(new GiveEntry(i.getId(), i.getName(), material(i.getItem(), Material.PAPER))));
            case ARMOR -> ArmorSetManager.armorDefinitions().entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey()))
                    .forEach(e -> entries.add(new GiveEntry(e.getKey(), e.getValue().name(), Material.IRON_CHESTPLATE)));
            case TICKET -> Arrays.stream(TicketType.values())
                    .sorted(Comparator.comparing(TicketType::getId))
                    .forEach(t -> entries.add(new GiveEntry(t.getId(), t.getPlainDisplayName(), t.getMaterial())));
            default -> {
            }
        }
        return entries;
    }

    private static boolean giveGuiEntry(Player player, GiveGuiType type, GiveEntry entry) {
        ItemStack stack = switch (type) {
            case WEAPON -> {
                CustomWeapon weapon = ConfigManager.getWeapon(entry.id);
                yield weapon == null ? null : WeaponManager.createWeaponStack(weapon, null);
            }
            case ITEM -> {
                CustomItem item = ConfigManager.getItem(entry.id);
                yield item == null ? null : CustomItemStackFactory.createItemStack(item);
            }
            case ARMOR -> ArmorSetManager.createSetItem(entry.id);
            case TICKET -> {
                TicketType ticket = TicketType.fromId(entry.id);
                yield ticket == null ? null : TicketManager.createTicket(ticket);
            }
            default -> null;
        };
        if (stack == null) return false;
        player.getInventory().addItem(stack);
        Message.send(player, "&a已给予: &f" + entry.name + " &8(" + entry.id + ")");
        return true;
    }

    private static Material material(String key, Material fallback) {
        if (key == null || key.isBlank()) return fallback;
        String normalized = key.toUpperCase().replace("MINECRAFT:", "").replace(':', '_');
        Material material = Material.matchMaterial(normalized);
        return material == null ? fallback : material;
    }

    private static void fill(Inventory inventory) {
        ItemStack filler = guiItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private static ItemStack guiItem(Material material, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Message.toComponent(name));
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) lore.add(Message.toComponent(line));
                meta.lore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private enum GiveGuiType {
        CATEGORY("分类"),
        WEAPON("武器"),
        ITEM("物品"),
        ARMOR("防具"),
        TICKET("强化券");

        final String displayName;

        GiveGuiType(String displayName) {
            this.displayName = displayName;
        }
    }

    private record GiveEntry(String id, String name, Material icon) {
    }

    private record GiveGuiHolder(GiveGuiType type, int page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class GiveGuiListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!(event.getView().getTopInventory().getHolder() instanceof GiveGuiHolder holder)) return;
            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

            int slot = event.getSlot();
            if (holder.type == GiveGuiType.CATEGORY) {
                switch (slot) {
                    case 10 -> openListGui(player, GiveGuiType.WEAPON, 0);
                    case 12 -> openListGui(player, GiveGuiType.ITEM, 0);
                    case 14 -> openListGui(player, GiveGuiType.ARMOR, 0);
                    case 16 -> openListGui(player, GiveGuiType.TICKET, 0);
                    default -> {
                    }
                }
                return;
            }

            List<GiveEntry> entries = entries(holder.type);
            if (slot == PREV_SLOT) {
                openListGui(player, holder.type, holder.page - 1);
                return;
            }
            if (slot == NEXT_SLOT) {
                openListGui(player, holder.type, holder.page + 1);
                return;
            }
            int index = holder.page * PAGE_SIZE + slot;
            if (slot >= 0 && slot < PAGE_SIZE && index < entries.size()) {
                giveGuiEntry(player, holder.type, entries.get(index));
            }
        }
    }
}
