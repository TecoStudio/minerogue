package com.roguelike.ticket;

import com.roguelike.RoguelikePlugin;
import com.roguelike.armor.affix.ArmorAffix;
import com.roguelike.armor.affix.ArmorAffixManager;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerDataManager;
import com.roguelike.equipment.EquipmentTypeResolver;
import com.roguelike.equipment.affix.AffixManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.DevLog;
import com.roguelike.util.Message;
import com.roguelike.weapon.affix.WeaponAffixManager;
import com.roguelike.weapon.WeaponManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TicketManager {
    private static RoguelikePlugin plugin;
    private static NamespacedKey KEY;
    private static final Random RANDOM = ThreadLocalRandom.current();
    private static final int RANDOM_DEVELOP_SLOT = 13;
    private static final int INFO_SLOT = 4;
    private static final int CANCEL_SLOT = 49;
    private static final double REMOVE_AFFIX_SUCCESS_BONUS = 0.10;
    private static final int[] STRENGTHEN_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final Map<UUID, TicketAChoice> pendingAChoices = new HashMap<>();
    private static final Map<UUID, TicketBChoice> pendingBChoices = new HashMap<>();
    private static final Map<UUID, TicketCChoice> pendingCChoices = new HashMap<>();

    public static void init(RoguelikePlugin plugin) {
        TicketManager.plugin = plugin;
        KEY = new NamespacedKey(plugin, "ticket_type");
        plugin.getServer().getPluginManager().registerEvents(new TicketChoiceListener(), plugin);
    }

    public static ItemStack createTicket(TicketType type) {
        ItemStack stack = new ItemStack(type.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Message.toComponent(type.getDisplayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Message.toComponent(type.getDescription()));
            lore.add(Message.toComponent("§7─────────────────"));
            if (type == TicketType.TICKET_B) {
                lore.add(Message.toComponent("§7手持此券，另一手拿任意物品"));
            } else if (type == TicketType.TOOL_TICKET_B) {
                lore.add(Message.toComponent("§7手持此券，另一手拿 Roguelike 工具"));
            } else {
                lore.add(Message.toComponent("§7手持此券，另一手拿武器"));
            }
            lore.add(Message.toComponent("§7右键使用"));
            meta.lore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, type.getId());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static TicketType getTicketType(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return TicketType.fromId(id);
    }

    public static void giveLevelUpTickets(Player player, int oldLevel, int newLevel) {
        int strengthenTickets = 0;
        int developmentTickets = 0;
        int removeTickets = 0;

        for (int level = oldLevel + 1; level <= newLevel; level++) {
            strengthenTickets++;
            if (level == 2 || level % 3 == 0) developmentTickets++;
            if (level == 2 || level % 5 == 0) removeTickets++;
        }

        for (int i = 0; i < strengthenTickets; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_A));
        }
        for (int i = 0; i < developmentTickets; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_B));
        }
        for (int i = 0; i < removeTickets; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_C));
        }
    }

    public static List<String> getBaseStatKeys() {
        return List.of("damage", "attack_speed", "attack_range");
    }

    public static List<String> getEffectStatKeys() {
        return WeaponAffixManager.rollableEffectIds();
    }

    public static List<String> getAllStatKeys() {
        List<String> stats = new ArrayList<>(getBaseStatKeys());
        stats.addAll(getEffectStatKeys());
        return stats;
    }

    public static String getStatDisplayName(String stat) {
        return statName(stat);
    }

    public static String formatStatValue(String stat, double value) {
        return format(value, stat);
    }

    static boolean canDevelopWeaponAffix(CustomWeapon template, WeaponInstanceData data) {
        return data != null && data.hasOpenRandomAffixSlot(template);
    }

    public static boolean applyTicket(Player player, ItemStack ticketStack, ItemStack weaponStack) {
        TicketType type = getTicketType(ticketStack);
        if (type == null) return false;

        if (weaponStack != null && EquipmentTypeResolver.isWearable(weaponStack.getType())) {
            return applyArmorTicket(player, ticketStack, weaponStack, type);
        }

        CustomWeapon template = WeaponManager.getTemplate(weaponStack);
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(weaponStack);
        if (type == TicketType.TICKET_B && (template == null || data == null)) {
            return applyWeaponDevelopment(player, ticketStack, weaponStack, type);
        }
        if (template == null || data == null) {
            Message.send(player, "&c目标物品不是 Roguelike 武器！");
            return false;
        }

        switch (type) {
            case TICKET_A -> {
                return applyTicketA(player, ticketStack, template, data, weaponStack, false);
            }
            case SUPER_TICKET_A -> {
                return applyTicketA(player, ticketStack, template, data, weaponStack, true);
            }
            case TICKET_B -> {
                return applyTicketB(player, ticketStack, template, data, weaponStack);
            }
            case TOOL_TICKET_B -> {
                return applyToolTicketB(player, ticketStack, template, data, weaponStack);
            }
            case TICKET_C -> {
                return applyTicketC(player, ticketStack, template, data, weaponStack);
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean applyArmorTicket(Player player, ItemStack ticketStack, ItemStack armorStack, TicketType type) {
        return switch (type) {
            case TICKET_A, SUPER_TICKET_A -> strengthenArmorAffix(player, ticketStack, armorStack, type);
            case TICKET_B -> addArmorAffix(player, ticketStack, armorStack);
            case TICKET_C -> applyArmorRemoveTicket(player, ticketStack, armorStack);
            default -> false;
        };
    }

    private static boolean addArmorAffix(Player player, ItemStack ticketStack, ItemStack armorStack) {
        List<String> available = availableArmorAffixes(armorStack);
        if (available.isEmpty()) {
            Message.send(player, "&c该防具已经拥有所有可用防具词条。");
            return false;
        }
        TicketBChoice choice = new TicketBChoice(true, null);
        pendingBChoices.put(player.getUniqueId(), choice);
        openTicketBChoiceGui(player, choice);
        return false;
    }

    private static boolean strengthenArmorAffix(Player player, ItemStack ticketStack, ItemStack armorStack, TicketType type) {
        List<String> strengthenable = strengthenableArmorAffixes(armorStack);
        if (strengthenable.isEmpty()) {
            Message.send(player, "&c该防具没有可强化的防具词条。");
            return false;
        }
        String id = strengthenable.get(RANDOM.nextInt(strengthenable.size()));
        ArmorAffix affix = ArmorAffixManager.get(id);
        int current = ArmorAffixManager.getAppliedLevel(armorStack, id);
        int next = AffixManager.strengthenArmor(id, current);
        AffixManager.applyArmorEnchant(armorStack, id, next);
        consumeTicket(ticketStack);
        recordTicketUse(player, type);
        Message.send(player, "&a防具词条强化成功: &f" + affix.displayName() + " &7" + AffixManager.formatArmor(id, current) + " &f-> &e" + AffixManager.formatArmor(id, next));
        return true;
    }

    private static boolean applyArmorRemoveTicket(Player player, ItemStack ticketStack, ItemStack armorStack) {
        List<String> current = currentArmorAffixes(armorStack);
        if (current.isEmpty()) {
            Message.send(player, "&c该防具没有可移除的防具词条。");
            return false;
        }
        TicketCChoice choice = new TicketCChoice(ticketStack, armorStack, current, null);
        pendingCChoices.put(player.getUniqueId(), choice);
        openTicketCChoiceGui(player, choice);
        return false;
    }

    private static List<String> availableArmorAffixes(ItemStack armorStack) {
        List<String> available = new ArrayList<>();
        for (String id : AffixManager.armorEffectIds()) {
            ArmorAffix affix = ArmorAffixManager.get(id);
            if (affix != null && ArmorAffixManager.isApplicable(id, armorStack.getType()) && !ArmorAffixManager.hasAppliedAffix(armorStack, id)) {
                available.add(id);
            }
        }
        return available;
    }

    private static List<String> currentArmorAffixes(ItemStack armorStack) {
        List<String> current = new ArrayList<>();
        for (String id : AffixManager.armorEffectIds()) {
            ArmorAffix affix = ArmorAffixManager.get(id);
            if (affix != null && ArmorAffixManager.hasAppliedAffix(armorStack, id)) current.add(id);
        }
        return current;
    }

    private static List<String> strengthenableArmorAffixes(ItemStack armorStack) {
        List<String> strengthenable = new ArrayList<>();
        for (String id : currentArmorAffixes(armorStack)) {
            ArmorAffix affix = ArmorAffixManager.get(id);
            if (affix != null && ArmorAffixManager.getAppliedLevel(armorStack, id) < affix.maxLevel()) strengthenable.add(id);
        }
        return strengthenable;
    }

    private static boolean applyWeaponDevelopment(Player player, ItemStack ticketStack, ItemStack targetStack, TicketType ticketType) {
        if (targetStack == null || targetStack.getType().isAir()) {
            Message.send(player, "&c另一只手需要拿着要开发的物品。");
            return false;
        }
        if (WeaponManager.getTemplate(targetStack) != null) {
            Message.send(player, "&c目标物品已经是 Roguelike 武器。");
            return false;
        }
        if (getTicketType(targetStack) != null) {
            Message.send(player, "&c不能将强化券开发为武器。");
            return false;
        }
        CustomWeapon template = ConfigManager.getWeapon("special_weapon");
        if (template == null) {
            Message.send(player, "&c缺少 special_weapon 武器模板，请检查 weapons 配置。");
            return false;
        }

        ItemStack developed = targetStack.clone();
        developed.setAmount(1);
        WeaponManager.makeSpecialWeaponPreservingAttributes(developed, template);
        targetStack.setAmount(targetStack.getAmount() - 1);
        giveOrDrop(player, developed);
        WeaponManager.refreshHeldWeapon(player);
        consumeTicket(ticketStack);
        recordTicketUse(player, ticketType);
        DevLog.debug(player.getName() + " developed item into weapon template " + template.getId());
        Message.send(player, "&d开发成功！已获得一个特殊品质武器，可继续使用开发券添加词条。");
        return true;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private static boolean applyTicketA(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack, boolean guaranteed) {
        List<String> availableStats = getStrengthenableStats(template, data, weaponStack.getType());
        if (availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可强化的词条！");
            return false;
        }

        int useCount = data.getTicketAUses();
        int failStreak = data.getTicketAFailStreak();
        double baseSuccessRate = calculateSuccessRate(useCount, template.getRarity());
        double failBonus = data.getTicketAFailBonus();
        double successRate = guaranteed ? 1.0 : calculateSuccessRate(useCount, failBonus, template.getRarity());
        TicketType ticketType = guaranteed ? TicketType.SUPER_TICKET_A : TicketType.TICKET_A;
        TicketAChoice choice = new TicketAChoice(ticketStack, weaponStack, template, data, availableStats, useCount, successRate, guaranteed, ticketType);
        pendingAChoices.put(player.getUniqueId(), choice);
        DevLog.debug(player.getName() + " opened " + (guaranteed ? "super_ticket_a" : "ticket_a") + " GUI for " + template.getId() + ", baseSuccessRate=" + formatPercent(baseSuccessRate) + ", failStreak=" + failStreak + ", failBonus=" + formatPercent(failBonus) + ", successRate=" + formatPercent(successRate));
        openTicketAChoiceGui(player, choice);
        return false;
    }

    private static void openTicketAChoiceGui(Player player, TicketAChoice choice) {
        Inventory inventory = Bukkit.createInventory(new TicketChoiceHolder(player.getUniqueId(), TicketGuiType.TICKET_A), 54, Message.toComponent(choice.guaranteed ? "&f选择超级强化词条" : "&c选择强化词条"));
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(INFO_SLOT, createGuiItem(Material.PAPER, "&e强化说明", List.of(
                "&7武器品质: &f" + WeaponManager.getRarityDisplayName(choice.template.getRarity()),
                "&7成功率: &f" + formatPercent(choice.successRate),
                "&7品质成功率倍率: &f" + WeaponManager.format(successRateMultiplier(choice.template.getRarity()), 2) + "x",
                "&7品质强化倍率: &f" + formatStrengthenRange(choice.template.getRarity()),
                "&7已使用强化券: &f" + choice.useCount + " 次",
                "&7连续失败: &f" + choice.failStreak + " 次",
                "&7失败加成: &f+" + formatPercent(choice.failBonus),
                "&7可强化词条: &f" + choice.availableStats.size() + " 个",
                "&7羊毛: 基础属性",
                "&7陶瓦: 效果词条（按《死亡细胞》式状态/协同/击杀触发区分）",
                "&7点击词条后立即尝试强化",
                choice.guaranteed ? "&7超级强化不会失败" : "&7失败后下次成功率随机 +3% 到 +13%",
                "&7成功后清空失败加成"
        )));
        for (int i = 0; i < choice.availableStats.size() && i < STRENGTHEN_SLOTS.length; i++) {
            String stat = choice.availableStats.get(i);
            double value = getStatBaseValue(choice.template, choice.initialData, stat);
            inventory.setItem(STRENGTHEN_SLOTS[i], createGuiItem(strengthenMaterial(stat, i), "&a" + statName(stat), List.of(
                    "&7当前值: &e" + format(value, stat),
                    "&7成功率: &f" + formatPercent(choice.successRate),
                    "&7强化倍率: &f" + formatStrengthenRange(choice.template.getRarity()),
                    choice.guaranteed ? "&7本次必定成功" : "&7失败也会消耗强化券",
                    "&e点击强化此词条"
            )));
        }
        inventory.setItem(CANCEL_SLOT, createGuiItem(Material.RED_CONCRETE, "&c取消", List.of(
                "&7关闭界面，不消耗强化券"
        )));
        player.openInventory(inventory);
    }

    private static void confirmTicketA(Player player, TicketAChoice choice, String stat) {
        ActiveTicketUse active = resolveActiveTicketUse(player, choice.ticketType, choice.weaponInstanceId);
        if (active == null) {
            Message.send(player, "&c强化目标已变化，请重新使用强化券。");
            return;
        }
        CustomWeapon template = active.template;
        WeaponInstanceData data = active.data;
        ItemStack weapon = active.weapon;
        List<String> currentStats = getStrengthenableStats(template, data, weapon.getType());
        if (choice.availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可强化的词条！");
            return;
        }
        if (!currentStats.contains(stat)) {
            Message.send(player, "&c该词条当前不可强化。");
            return;
        }

        if (!choice.guaranteed && RANDOM.nextDouble() > choice.successRate) {
            double bonus = 0.03 + RANDOM.nextDouble() * 0.10;
            data.incrementTicketAUses();
            data.incrementTicketAFailStreak();
            data.addTicketAFailBonus(bonus);
            data.saveToItemStack(weapon);
            WeaponManager.updateLore(weapon, template, data);
            WeaponManager.clearAttributes(player);
            consumeTicket(active.ticket);
            recordTicketUse(player, choice.ticketType);
            DevLog.debug(player.getName() + " ticket_a failed on " + template.getId() + ", stat=" + stat + ", useCount=" + choice.useCount);
            Message.send(player, "&c强化失败！");
            Message.send(player, "&7本次成功率: " + formatPercent(choice.successRate));
            Message.send(player, "&7下次成功率加成: &a+" + formatPercent(bonus));
            return;
        }

        double baseValue = getStatBaseValue(template, data, stat);
        double newValue = strengthenStat(template, stat, baseValue, choice.useCount);

        setStatValue(template, data, stat, newValue);
        if (!choice.guaranteed) {
            data.incrementTicketAUses();
        }
        data.resetTicketAFailStreak();
        data.saveToItemStack(weapon);
        WeaponManager.updateLore(weapon, template, data);
        WeaponManager.clearAttributes(player);
        consumeTicket(active.ticket);
        recordTicketUse(player, choice.ticketType);
        DevLog.debug(player.getName() + " " + choice.ticketType.getId() + " succeeded on " + template.getId() + ", stat=" + stat + ", old=" + baseValue + ", new=" + newValue);

        Message.send(player, "&a强化成功！ &f" + statName(stat) + " &7" + format(baseValue, stat) + " &f-> &e" + format(newValue, stat));
        Message.send(player, "&7本次成功率: " + formatPercent(choice.successRate));
    }

    private static boolean applyTicketB(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack) {
        List<String> availableEffects = getAvailableEffects(template, data, weaponStack.getType());
        if (availableEffects.isEmpty()) {
            Message.send(player, "&c武器已经拥有所有可能的词条！");
            return false;
        }
        TicketBChoice choice = new TicketBChoice(false, data.getInstanceId());
        pendingBChoices.put(player.getUniqueId(), choice);
        DevLog.debug(player.getName() + " opened random ticket_b development for " + template.getId() + ", available=" + availableEffects.size());
        openTicketBChoiceGui(player, choice);

        return false;
    }

    private static boolean applyToolTicketB(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack) {
        if (!EquipmentTypeResolver.isTool(weaponStack.getType())) {
            Message.send(player, "&c工具开发券只能用于 Roguelike 镐或斧。");
            return false;
        }
        List<String> availableEffects = getAvailableToolEffects(template, data);
        if (availableEffects.isEmpty()) {
            Message.send(player, "&c该工具已经拥有所有工具类词条！");
            return false;
        }
        TicketBChoice choice = new TicketBChoice(false, data.getInstanceId(), TicketType.TOOL_TICKET_B);
        pendingBChoices.put(player.getUniqueId(), choice);
        DevLog.debug(player.getName() + " opened tool_ticket_b development for " + template.getId() + ", available=" + availableEffects.size());
        openTicketBChoiceGui(player, choice);

        return false;
    }

    private static void openTicketBChoiceGui(Player player, TicketBChoice choice) {
        Inventory inventory = Bukkit.createInventory(new TicketChoiceHolder(player.getUniqueId(), TicketGuiType.TICKET_B), 27, Message.toComponent("&6随机开发词条"));
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(RANDOM_DEVELOP_SLOT, createGuiItem(Material.PAPER, "&a随机获得一个词条", List.of(
                choice.armor ? "&7目标: &f防具" : (choice.ticketType == TicketType.TOOL_TICKET_B ? "&7目标: &f工具" : "&7目标: &f武器"),
                "&7点击后从当前可用词条中随机获得一个",
                "&7关闭界面不会消耗" + choice.ticketType.getPlainDisplayName()
        )));
        player.openInventory(inventory);
    }

    private static ItemStack createGuiItem(Material material, String name, List<String> loreLines) {
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

    private static boolean applyTicketC(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack) {
        List<String> availableStats = getRemovableWeaponStats(data);
        if (availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可移除的词条！");
            return false;
        }
        TicketCChoice choice = new TicketCChoice(ticketStack, weaponStack, availableStats, data.getInstanceId());
        pendingCChoices.put(player.getUniqueId(), choice);
        openTicketCChoiceGui(player, choice);
        return false;
    }

    private static void openTicketCChoiceGui(Player player, TicketCChoice choice) {
        Inventory inventory = Bukkit.createInventory(new TicketChoiceHolder(player.getUniqueId(), TicketGuiType.TICKET_C), 54, Message.toComponent("&9选择移除词条"));
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(INFO_SLOT, createGuiItem(Material.PAPER, "&9移除说明", List.of(
                "&7点击一个词条将其移除",
                "&7移除后下次普通强化券成功率 &a+" + formatPercent(REMOVE_AFFIX_SUCCESS_BONUS),
                "&7此加成会和失败加成叠加",
                "&7关闭界面不会消耗移除券"
        )));
        for (int i = 0; i < choice.removableStats.size() && i < STRENGTHEN_SLOTS.length; i++) {
            String stat = choice.removableStats.get(i);
            inventory.setItem(STRENGTHEN_SLOTS[i], createGuiItem(strengthenMaterial(stat, i), "&9" + choice.displayName(stat), List.of(
                    "&7当前值: &e" + choice.formatCurrent(stat),
                    "&7强化成功率加成: &a+" + formatPercent(REMOVE_AFFIX_SUCCESS_BONUS),
                    "&e点击移除此词条"
            )));
        }
        inventory.setItem(CANCEL_SLOT, createGuiItem(Material.RED_CONCRETE, "&c取消", List.of(
                "&7关闭界面，不消耗移除券"
        )));
        player.openInventory(inventory);
    }

    static double calculateSuccessRate(int useCount) {
        if (useCount <= 0) return 1.0;
        if (useCount == 1) return 1.0;
        if (useCount == 2) return 0.90;
        if (useCount == 3) return 0.80;
        if (useCount == 4) return 0.65;
        if (useCount == 5) return 0.50;
        if (useCount == 6) return 0.35;
        if (useCount == 7) return 0.20;
        if (useCount == 8) return 0.10;
        return Math.max(0.01, 0.10 * Math.pow(0.5, useCount - 8));
    }

    private static double calculateSuccessRate(int useCount, String rarity) {
        return Math.min(1.0, calculateSuccessRate(useCount) * successRateMultiplier(rarity));
    }

    private static double calculateSuccessRate(int useCount, double failBonus, String rarity) {
        return Math.min(1.0, calculateSuccessRate(useCount, rarity) + Math.max(0, failBonus));
    }

    private static double successRateMultiplier(String rarity) {
        return switch (normalizeRarity(rarity)) {
            case "common" -> 1.10;
            case "epic" -> 0.90;
            case "legendary" -> 0.75;
            default -> 1.00;
        };
    }

    private static List<String> getNonZeroStats(CustomWeapon template, WeaponInstanceData data) {
        List<String> stats = new ArrayList<>();
        if (data.getTotalDamage(template) != template.getBaseDamage()) stats.add("damage");
        if (data.getTotalAttackSpeed(template) != template.getAttackSpeed()) stats.add("attack_speed");
        if (data.getTotalEffect(template, "attack_range", 3.0) != template.getEffect("attack_range", 3.0)) stats.add("attack_range");
        for (String key : AffixManager.weaponEffectIds()) {
            if (data.getTotalEffect(template, key, 0.0) != template.getEffect(key, 0.0)) {
                stats.add(key);
            }
        }
        return stats;
    }

    private static List<String> getRemovableWeaponStats(WeaponInstanceData data) {
        List<String> stats = new ArrayList<>();
        for (Map.Entry<String, Double> entry : data.getEffectBonuses().entrySet()) {
            if (entry.getValue() != 0.0) stats.add(entry.getKey());
        }
        stats.sort(Comparator.comparing(TicketManager::statName));
        return stats;
    }

    private static List<String> getStrengthenableStats(CustomWeapon template, WeaponInstanceData data) {
        return getStrengthenableStats(template, data, null);
    }

    private static List<String> getStrengthenableStats(CustomWeapon template, WeaponInstanceData data, Material material) {
        List<String> stats = new ArrayList<>();
        stats.add("damage");
        stats.add("attack_speed");
        stats.add("attack_range");
        for (String key : AffixManager.weaponEffectIds()) {
            if (AffixManager.isWeaponAffixStrengthenable(template, data, key, material)) {
                stats.add(key);
            }
        }
        return stats;
    }

    private static List<String> getAvailableEffects(CustomWeapon template, WeaponInstanceData data) {
        return getAvailableEffects(template, data, null);
    }

    private static List<String> getAvailableEffects(CustomWeapon template, WeaponInstanceData data, Material material) {
        List<String> available = new ArrayList<>();
        for (String key : AffixManager.weaponEffectIds()) {
            if (AffixManager.isWeaponAffixAvailable(template, data, key, material)) {
                available.add(key);
            }
        }
        return available;
    }

    private static List<String> getAvailableToolEffects(CustomWeapon template, WeaponInstanceData data) {
        List<String> available = new ArrayList<>();
        for (String key : AffixManager.toolOnlyEffectIds()) {
            if (AffixManager.isToolOnlyWeaponAffixAvailable(template, data, key)) {
                available.add(key);
            }
        }
        return available;
    }

    private static double getStatBaseValue(CustomWeapon template, WeaponInstanceData data, String stat) {
        return switch (stat) {
            case "damage" -> data.getTotalDamage(template);
            case "attack_speed" -> data.getTotalAttackSpeed(template);
            case "attack_range" -> data.getTotalEffect(template, "attack_range", 3.0);
            default -> data.getTotalEffect(template, stat, 0.0);
        };
    }

    private static void setStatValue(CustomWeapon template, WeaponInstanceData data, String stat, double value) {
        switch (stat) {
            case "damage" -> data.setDamageBonus(value - data.getScaledBaseDamage(template));
            case "attack_speed" -> data.setAttackSpeedBonus(value - template.getAttackSpeed());
            case "attack_range" -> data.setEffectBonus(stat, value - template.getEffect(stat, 3.0));
            default -> data.setEffectBonus(stat, value - template.getEffect(stat, 0.0));
        }
    }

    private static double strengthenStat(CustomWeapon template, String stat, double currentValue, int useCount) {
        return switch (stat) {
            case "damage" -> strengthenDamage(currentValue);
            case "chain_targets", "slow_level" -> currentValue + 1;
            case "damage_store_hit_reduction" -> Math.min(15, currentValue + 1);
            case "hyper" -> Math.min(3, currentValue + 1);
            case "durability_restore" -> Math.min(5, currentValue + 1);
            default -> strengthenByRarity(currentValue, template.getRarity(), stat.endsWith("_chance"));
        };
    }

    private static double strengthenByRarity(double currentValue, String rarity, boolean chance) {
        double[] range = strengthenRange(rarity);
        double multiplier = range[0] + (range[1] - range[0]) * Math.pow(RANDOM.nextDouble(), 2.0);
        double value = currentValue * multiplier;
        return chance ? Math.min(1.0, value) : value;
    }

    private static double strengthenDamage(double currentValue) {
        double multiplier = 1.10 + 0.04 * Math.pow(RANDOM.nextDouble(), 2.0);
        return currentValue * multiplier;
    }

    private static double[] strengthenRange(String rarity) {
        return switch (normalizeRarity(rarity)) {
            case "common" -> new double[]{1.06, 1.18};
            case "rare" -> new double[]{1.05, 1.16};
            case "epic" -> new double[]{1.04, 1.14};
            case "legendary" -> new double[]{1.03, 1.12};
            default -> new double[]{1.04, 1.16};
        };
    }

    static String formatStrengthenRangeForTesting(String rarity) {
        return formatStrengthenRange(rarity);
    }

    private static String formatStrengthenRange(String rarity) {
        double[] range = strengthenRange(rarity);
        return WeaponManager.format(range[0], 2) + "x - " + WeaponManager.format(range[1], 2) + "x";
    }

    private static String normalizeRarity(String rarity) {
        return rarity == null ? "common" : rarity.toLowerCase(Locale.ROOT);
    }

    private static String statName(String stat) {
        return switch (stat) {
            case "damage" -> "基础伤害";
            case "attack_speed" -> "攻击速度";
            case "attack_range" -> "攻击距离";
            default -> AffixManager.displayName(com.roguelike.equipment.EquipmentKind.WEAPON, stat);
        };
    }

    private static String format(double value, String stat) {
        if (!stat.equals("damage") && !stat.equals("attack_speed") && !stat.equals("attack_range")) {
            return AffixManager.formatWeapon(stat, value);
        }
        return WeaponManager.format(value, 2);
    }

    private static Material strengthenMaterial(String stat, int index) {
        Material[] wool = {
                Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
                Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
                Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL
        };
        Material[] terracotta = {
                Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.MAGENTA_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA,
                Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA, Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA,
                Material.LIGHT_GRAY_TERRACOTTA, Material.CYAN_TERRACOTTA, Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA,
                Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA, Material.RED_TERRACOTTA, Material.BLACK_TERRACOTTA
        };
        boolean base = stat.equals("damage") || stat.equals("attack_speed") || stat.equals("attack_range");
        return base ? wool[index % wool.length] : terracotta[index % terracotta.length];
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }

    private static void consumeTicket(ItemStack ticketStack) {
        ticketStack.setAmount(ticketStack.getAmount() - 1);
    }

    private static void recordTicketUse(Player player, TicketType type) {
        DevLog.debug(player.getName() + " 使用了 " + type.getPlainDisplayName());
        PlayerDataManager.get(player).addTicketUse(type.getId());
        PlayerDataManager.save(player);
    }

    private static class TicketBChoice {
        final boolean armor;
        final String weaponInstanceId;
        final TicketType ticketType;

        TicketBChoice(boolean armor, String weaponInstanceId) {
            this(armor, weaponInstanceId, TicketType.TICKET_B);
        }

        TicketBChoice(boolean armor, String weaponInstanceId, TicketType ticketType) {
            this.armor = armor;
            this.weaponInstanceId = weaponInstanceId;
            this.ticketType = ticketType;
        }
    }

    private static class TicketCChoice {
        final boolean armor;
        final List<String> removableStats;
        final String weaponInstanceId;
        final Map<String, String> currentValues = new HashMap<>();

        TicketCChoice(ItemStack ticket, ItemStack target, List<String> removableStats, String weaponInstanceId) {
            this.armor = weaponInstanceId == null;
            this.removableStats = List.copyOf(removableStats);
            this.weaponInstanceId = weaponInstanceId;
            for (String stat : removableStats) {
                currentValues.put(stat, readCurrentValue(target, stat));
            }
        }

        String displayName(String stat) {
            return armor ? AffixManager.displayName(com.roguelike.equipment.EquipmentKind.ARMOR, stat) : statName(stat);
        }

        String formatCurrent(String stat) {
            return currentValues.getOrDefault(stat, "未知");
        }

        private String readCurrentValue(ItemStack target, String stat) {
            if (armor) {
                int level = ArmorAffixManager.getAppliedLevel(target, stat);
                return AffixManager.formatArmor(stat, level);
            }
            CustomWeapon template = WeaponManager.getTemplate(target);
            WeaponInstanceData data = WeaponManager.getData(target);
            if (template == null || data == null) return "未知";
            return format(data.getTotalEffect(template, stat, 0.0), stat);
        }
    }

    private static class TicketAChoice {
        final CustomWeapon template;
        final WeaponInstanceData initialData;
        final List<String> availableStats;
        final int useCount;
        final int failStreak;
        final double failBonus;
        final double successRate;
        final boolean guaranteed;
        final TicketType ticketType;
        final String weaponInstanceId;

        TicketAChoice(ItemStack ticket, ItemStack weapon, CustomWeapon template, WeaponInstanceData data,
                      List<String> availableStats, int useCount, double successRate, boolean guaranteed, TicketType ticketType) {
            this.template = template;
            this.initialData = data;
            this.availableStats = availableStats;
            this.useCount = useCount;
            this.failStreak = data.getTicketAFailStreak();
            this.failBonus = data.getTicketAFailBonus();
            this.successRate = successRate;
            this.guaranteed = guaranteed;
            this.ticketType = ticketType;
            this.weaponInstanceId = data.getInstanceId();
        }
    }

    private enum TicketGuiType {
        TICKET_A,
        TICKET_B,
        TICKET_C
    }

    private static class TicketChoiceHolder implements InventoryHolder {
        final UUID playerId;
        final TicketGuiType type;

        TicketChoiceHolder(UUID playerId, TicketGuiType type) {
            this.playerId = playerId;
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class TicketChoiceListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!(event.getView().getTopInventory().getHolder() instanceof TicketChoiceHolder holder)) return;
            if (!holder.playerId.equals(player.getUniqueId())) return;

            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

            UUID uuid = player.getUniqueId();
            if (holder.type == TicketGuiType.TICKET_A) {
                handleTicketAClick(player, event, uuid);
                return;
            }
            if (holder.type == TicketGuiType.TICKET_C) {
                handleTicketCClick(player, event, uuid);
                return;
            }

            handleTicketBClick(player, event, uuid);
        }

        private void handleTicketBClick(Player player, InventoryClickEvent event, UUID uuid) {
            TicketBChoice choice = pendingBChoices.get(uuid);
            if (choice == null || event.getSlot() != RANDOM_DEVELOP_SLOT) return;

            pendingBChoices.remove(uuid);
            player.closeInventory();
            if (choice.armor) {
                confirmArmorTicketB(player);
            } else {
                confirmWeaponTicketB(player, choice);
            }
        }

        private void confirmWeaponTicketB(Player player, TicketBChoice choice) {
            ActiveTicketUse active = resolveActiveTicketUse(player, choice.ticketType, choice.weaponInstanceId);
            if (active == null) {
                Message.send(player, "&c开发目标已变化，请重新使用开发券。");
                return;
            }
            List<String> availableEffects = choice.ticketType == TicketType.TOOL_TICKET_B
                    ? getAvailableToolEffects(active.template, active.data)
                    : getAvailableEffects(active.template, active.data, active.weapon.getType());
            if (availableEffects.isEmpty()) {
                Message.send(player, choice.ticketType == TicketType.TOOL_TICKET_B ? "&c该工具已经拥有所有工具类词条！" : "&c武器已经拥有所有可能的词条！");
                return;
            }
            String selectedStat = availableEffects.get(RANDOM.nextInt(availableEffects.size()));
            double baseValue = generateBaseValue(selectedStat);
            active.data.setEffectBonus(selectedStat, baseValue);
            active.data.incrementTicketBUses();
            active.data.saveToItemStack(active.weapon);
            WeaponManager.updateLore(active.weapon, active.template, active.data);
            WeaponManager.clearAttributes(player);
            consumeTicket(active.ticket);
            recordTicketUse(player, choice.ticketType);

            DevLog.debug(player.getName() + " randomly developed " + choice.ticketType.getId() + " stat " + selectedStat + "=" + baseValue + " for " + active.template.getId());
            Message.send(player, "&a随机获得词条: &f" + statName(selectedStat) + " &e" + format(baseValue, selectedStat));
        }

        private void confirmArmorTicketB(Player player) {
            ActiveArmorTicketUse active = resolveActiveArmorTicketUse(player, TicketType.TICKET_B);
            if (active == null) {
                Message.send(player, "&c开发目标已变化，请重新使用开发券。");
                return;
            }
            List<String> available = availableArmorAffixes(active.armor);
            if (available.isEmpty()) {
                Message.send(player, "&c该防具已经拥有所有可用防具词条。");
                return;
            }

            String id = available.get(RANDOM.nextInt(available.size()));
            int level = AffixManager.generateArmorBaseLevel(id, RANDOM);
            AffixManager.applyArmorEnchant(active.armor, id, level);
            consumeTicket(active.ticket);
            recordTicketUse(player, TicketType.TICKET_B);
            Message.send(player, "&a随机获得防具词条: &f" + AffixManager.displayName(com.roguelike.equipment.EquipmentKind.ARMOR, id) + " &e" + AffixManager.formatArmor(id, level));
        }

        private void handleTicketAClick(Player player, InventoryClickEvent event, UUID uuid) {
            TicketAChoice choice = pendingAChoices.get(uuid);
            if (choice == null) return;

            if (event.getSlot() == CANCEL_SLOT) {
                pendingAChoices.remove(uuid);
                player.closeInventory();
                Message.send(player, "&7已取消强化，未消耗强化券。");
                return;
            }
            for (int i = 0; i < STRENGTHEN_SLOTS.length && i < choice.availableStats.size(); i++) {
                if (event.getSlot() == STRENGTHEN_SLOTS[i]) {
                    String stat = choice.availableStats.get(i);
                    pendingAChoices.remove(uuid);
                    player.closeInventory();
                    confirmTicketA(player, choice, stat);
                    return;
                }
            }
        }

        private void handleTicketCClick(Player player, InventoryClickEvent event, UUID uuid) {
            TicketCChoice choice = pendingCChoices.get(uuid);
            if (choice == null) return;

            if (event.getSlot() == CANCEL_SLOT) {
                pendingCChoices.remove(uuid);
                player.closeInventory();
                Message.send(player, "&7已取消移除，未消耗移除券。");
                return;
            }
            for (int i = 0; i < STRENGTHEN_SLOTS.length && i < choice.removableStats.size(); i++) {
                if (event.getSlot() == STRENGTHEN_SLOTS[i]) {
                    String stat = choice.removableStats.get(i);
                    pendingCChoices.remove(uuid);
                    player.closeInventory();
                    if (choice.armor) {
                        confirmArmorTicketC(player, stat);
                    } else {
                        confirmWeaponTicketC(player, choice, stat);
                    }
                    return;
                }
            }
        }

        private void confirmWeaponTicketC(Player player, TicketCChoice choice, String stat) {
            ActiveTicketUse active = resolveActiveTicketUse(player, TicketType.TICKET_C, choice.weaponInstanceId);
            if (active == null) {
                Message.send(player, "&c移除目标已变化，请重新使用移除券。");
                return;
            }
            if (!getRemovableWeaponStats(active.data).contains(stat)) {
                Message.send(player, "&c该词条当前不可移除，请重新使用移除券。");
                return;
            }

            active.data.removeEffectBonus(stat);
            active.data.addTicketAFailBonus(REMOVE_AFFIX_SUCCESS_BONUS);
            active.data.incrementTicketCUses();
            active.data.saveToItemStack(active.weapon);
            WeaponManager.updateLore(active.weapon, active.template, active.data);
            WeaponManager.clearAttributes(player);
            consumeTicket(active.ticket);
            recordTicketUse(player, TicketType.TICKET_C);
            DevLog.debug(player.getName() + " removed stat " + stat + " from " + active.template.getId() + " with ticket_c");
            Message.send(player, "&9移除成功！ &f" + statName(stat) + " &7已移除");
            Message.send(player, "&7下次普通强化券成功率: &a+" + formatPercent(REMOVE_AFFIX_SUCCESS_BONUS));
        }

        private void confirmArmorTicketC(Player player, String stat) {
            ActiveArmorTicketUse active = resolveActiveArmorTicketUse(player, TicketType.TICKET_C);
            if (active == null) {
                Message.send(player, "&c移除目标已变化，请重新使用移除券。");
                return;
            }
            if (!currentArmorAffixes(active.armor).contains(stat)) {
                Message.send(player, "&c该防具词条当前不可移除，请重新使用移除券。");
                return;
            }

            ArmorAffix affix = ArmorAffixManager.get(stat);
            ArmorAffixManager.removeAppliedAffix(active.armor, stat);
            consumeTicket(active.ticket);
            recordTicketUse(player, TicketType.TICKET_C);
            Message.send(player, "&9已移除防具词条: &f" + (affix == null ? stat : affix.displayName()));
            Message.send(player, "&7防具强化为必定成功，不使用强化成功率加成。");
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof TicketChoiceHolder holder)) return;
            if (holder.type == TicketGuiType.TICKET_A) {
                pendingAChoices.remove(holder.playerId);
            } else if (holder.type == TicketGuiType.TICKET_C) {
                pendingCChoices.remove(holder.playerId);
            }
            if (holder.type == TicketGuiType.TICKET_B) {
                pendingBChoices.remove(holder.playerId);
            }
        }
    }

    private static double generateBaseValue(String stat) {
        return switch (stat) {
            default -> AffixManager.generateWeaponBaseValue(stat, RANDOM);
        };
    }

    private static ActiveTicketUse resolveActiveTicketUse(Player player, TicketType ticketType, String weaponInstanceId) {
        ActiveTicketUse mainTicket = resolveActiveTicketUse(player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand(), ticketType, weaponInstanceId);
        if (mainTicket != null) return mainTicket;
        return resolveActiveTicketUse(player.getInventory().getItemInOffHand(), player.getInventory().getItemInMainHand(), ticketType, weaponInstanceId);
    }

    private static ActiveArmorTicketUse resolveActiveArmorTicketUse(Player player, TicketType ticketType) {
        ActiveArmorTicketUse mainTicket = resolveActiveArmorTicketUse(player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand(), ticketType);
        if (mainTicket != null) return mainTicket;
        return resolveActiveArmorTicketUse(player.getInventory().getItemInOffHand(), player.getInventory().getItemInMainHand(), ticketType);
    }

    private static ActiveArmorTicketUse resolveActiveArmorTicketUse(ItemStack ticket, ItemStack armor, TicketType ticketType) {
        if (getTicketType(ticket) != ticketType) return null;
        if (armor == null || armor.getType().isAir() || !EquipmentTypeResolver.isWearable(armor.getType())) return null;
        return new ActiveArmorTicketUse(ticket, armor);
    }

    private static ActiveTicketUse resolveActiveTicketUse(ItemStack ticket, ItemStack weapon, TicketType ticketType, String weaponInstanceId) {
        if (getTicketType(ticket) != ticketType) return null;
        CustomWeapon template = WeaponManager.getTemplate(weapon);
        WeaponInstanceData data = WeaponManager.getData(weapon);
        if (template == null || data == null || !data.getInstanceId().equals(weaponInstanceId)) return null;
        return new ActiveTicketUse(ticket, weapon, template, data);
    }

    private record ActiveTicketUse(ItemStack ticket, ItemStack weapon, CustomWeapon template, WeaponInstanceData data) {
    }

    private record ActiveArmorTicketUse(ItemStack ticket, ItemStack armor) {
    }
}
