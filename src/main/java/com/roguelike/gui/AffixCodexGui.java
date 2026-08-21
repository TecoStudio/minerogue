package com.roguelike.gui;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.equipment.EquipmentKind;
import com.roguelike.equipment.affix.AffixDescriptions;
import com.roguelike.equipment.affix.AffixManager;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.Message;
import com.roguelike.weapon.affix.WeaponAffixManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 词条图鉴：只读分页 GUI，展示武器/防具/基础属性词条，鼠标悬停查看具体效果。
 * 入口：/rl affixes、/rw affixes（玩家调用时打开 GUI）。
 */
public final class AffixCodexGui {
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private AffixCodexGui() {
    }

    public static void init(RoguelikePlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new AffixCodexListener(), plugin);
    }

    public static void open(Player player) {
        openCategory(player);
    }

    private static void openCategory(Player player) {
        Inventory inventory = Bukkit.createInventory(new AffixCodexHolder(CodexType.CATEGORY, 0), 27, Message.toComponent("&6词条图鉴"));
        fill(inventory);
        inventory.setItem(11, guiItem(Material.IRON_SWORD, "&e武器词条", List.of("&7点击查看武器/工具/弓词条")));
        inventory.setItem(13, guiItem(Material.IRON_CHESTPLATE, "&e防具词条", List.of("&7点击查看防具套装词条")));
        inventory.setItem(15, guiItem(Material.PAPER, "&e基础属性", List.of("&7点击查看武器基础属性")));
        player.openInventory(inventory);
    }

    private static void openList(Player player, CodexType type, int page) {
        List<CodexEntry> entries = entries(type);
        int clamped = clampPage(page, entries.size());
        Inventory inventory = Bukkit.createInventory(new AffixCodexHolder(type, clamped), 54, Message.toComponent("&6词条图鉴: " + type.displayName));
        fill(inventory);
        int start = clamped * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CodexEntry entry = entries.get(i);
            inventory.setItem(i - start, guiItem(entry.material, "&e" + entry.name, entry.lore));
        }
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        inventory.setItem(PREV_SLOT, guiItem(Material.ARROW, "&e上一页", List.of("&7第 " + (clamped + 1) + " 页")));
        inventory.setItem(INFO_SLOT, guiItem(Material.BOOK, "&6" + type.displayName, List.of(
                "&7总数: &f" + entries.size(),
                "&7页码: &f" + (clamped + 1) + "/" + (maxPage + 1),
                "&7悬停查看词条效果"
        )));
        inventory.setItem(NEXT_SLOT, guiItem(Material.ARROW, "&e下一页", List.of("&7第 " + (clamped + 1) + " 页")));
        player.openInventory(inventory);
    }

    private static List<CodexEntry> entries(CodexType type) {
        List<CodexEntry> entries = new ArrayList<>();
        switch (type) {
            case WEAPON -> {
                for (String id : AffixManager.weaponEffectIds()) {
                    entries.add(weaponEntry(id));
                }
            }
            case ARMOR -> {
                for (String id : AffixManager.armorEffectIds()) {
                    entries.add(armorEntry(id));
                }
            }
            case BASE -> {
                for (String stat : TicketManager.getBaseStatKeys()) {
                    entries.add(baseEntry(stat));
                }
            }
            default -> {
            }
        }
        return entries;
    }

    private static CodexEntry weaponEntry(String id) {
        String name = AffixManager.displayName(EquipmentKind.WEAPON, id);
        String sample = WeaponAffixManager.format(id, sampleValue(id));
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + id);
        lore.add("&7分类: &f" + WeaponAffixManager.category(id));
        lore.add("&7适用: &f" + weaponApplicable(id));
        lore.add("&7数值示例: &f" + sample);
        lore.add("&7机制: &f" + AffixDescriptions.weapon(id));
        return new CodexEntry(id, name, Material.ENCHANTED_BOOK, lore);
    }

    private static CodexEntry armorEntry(String id) {
        String name = AffixManager.displayName(EquipmentKind.ARMOR, id);
        String sample = ArmorAffixManager.format(id, 1);
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + id);
        lore.add("&7分类: &f防具词条");
        lore.add("&7适用: &f" + armorApplicable(id));
        lore.add("&7数值示例: &f" + sample);
        lore.add("&7机制: &f" + AffixDescriptions.armor(id));
        return new CodexEntry(id, name, Material.ENCHANTED_BOOK, lore);
    }

    private static CodexEntry baseEntry(String stat) {
        String name = TicketManager.getStatDisplayName(stat);
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + stat);
        lore.add("&7分类: &f基础属性");
        lore.add("&7适用: &f武器");
        lore.add("&7数值示例: &f" + TicketManager.formatStatValue(stat, baseSample(stat)));
        lore.add("&7机制: &f" + AffixDescriptions.base(stat));
        return new CodexEntry(stat, name, Material.PAPER, lore);
    }

    private static String weaponApplicable(String id) {
        if (WeaponAffixManager.isToolOnly(id)) {
            return WeaponAffixManager.isApplicable(id, Material.DIAMOND_AXE) ? "仅工具" : "仅镐";
        }
        boolean bow = WeaponAffixManager.isApplicable(id, EquipmentKind.BOW);
        boolean weapon = WeaponAffixManager.isApplicable(id, EquipmentKind.WEAPON);
        if (weapon && bow) return "武器/弓/工具";
        if (bow) return "仅弓";
        return "武器/工具";
    }

    private static String armorApplicable(String id) {
        boolean leggings = ArmorAffixManager.isApplicable(id, Material.LEATHER_LEGGINGS);
        boolean chest = ArmorAffixManager.isApplicable(id, Material.LEATHER_CHESTPLATE);
        if (leggings && !chest) return "仅护腿";
        return "防具";
    }

    private static double sampleValue(String id) {
        return WeaponAffixManager.generateBaseValue(id, new Random(id.hashCode()));
    }

    private static double baseSample(String stat) {
        return switch (stat) {
            case "damage" -> 7.0;
            case "attack_speed" -> 4.0;
            case "attack_range" -> 3.0;
            default -> 1.0;
        };
    }

    private static int clampPage(int page, int itemCount) {
        if (itemCount <= 0) return 0;
        int maxPage = Math.max(0, (itemCount - 1) / PAGE_SIZE);
        return Math.max(0, Math.min(page, maxPage));
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

    private enum CodexType {
        CATEGORY("分类"),
        WEAPON("武器词条"),
        ARMOR("防具词条"),
        BASE("基础属性");

        final String displayName;

        CodexType(String displayName) {
            this.displayName = displayName;
        }
    }

    private record CodexEntry(String id, String name, Material material, List<String> lore) {
    }

    private record AffixCodexHolder(CodexType type, int page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class AffixCodexListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!(event.getView().getTopInventory().getHolder() instanceof AffixCodexHolder holder)) return;
            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

            int slot = event.getSlot();
            if (holder.type == CodexType.CATEGORY) {
                switch (slot) {
                    case 11 -> openList(player, CodexType.WEAPON, 0);
                    case 13 -> openList(player, CodexType.ARMOR, 0);
                    case 15 -> openList(player, CodexType.BASE, 0);
                    default -> {
                    }
                }
                return;
            }

            if (slot == PREV_SLOT) {
                openList(player, holder.type, holder.page - 1);
                return;
            }
            if (slot == NEXT_SLOT) {
                openList(player, holder.type, holder.page + 1);
            }
        }
    }
}
