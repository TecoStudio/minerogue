package com.roguelike.ticket;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.data.PlayerDataManager;
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
    private static final int[] CHOICE_SLOTS = {11, 13, 15};
    private static final int INFO_SLOT = 4;
    private static final int CANCEL_SLOT = 49;
    private static final int[] STRENGTHEN_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final Map<UUID, TicketAChoice> pendingAChoices = new HashMap<>();
    private static final Map<UUID, TicketBChoice> pendingBChoices = new HashMap<>();

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
            if (type == TicketType.TICKET_B || type == TicketType.WEAPON_DEVELOPMENT) {
                lore.add(Message.toComponent("§7手持此券，另一手拿任意物品"));
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
        int resetTickets = 0;

        for (int level = oldLevel + 1; level <= newLevel; level++) {
            strengthenTickets++;
            if (level == 2 || level % 3 == 0) developmentTickets++;
            if (level == 2 || level % 5 == 0) resetTickets++;
        }

        for (int i = 0; i < strengthenTickets; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_A));
        }
        for (int i = 0; i < developmentTickets; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_B));
        }
        for (int i = 0; i < resetTickets; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_C));
        }
    }

    public static List<String> getBaseStatKeys() {
        return List.of("damage", "attack_speed", "attack_range");
    }

    public static List<String> getEffectStatKeys() {
        return WeaponAffixManager.effectIds();
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

    public static boolean applyTicket(Player player, ItemStack ticketStack, ItemStack weaponStack) {
        TicketType type = getTicketType(ticketStack);
        if (type == null) return false;
        if (type == TicketType.WEAPON_DEVELOPMENT) {
            return applyWeaponDevelopment(player, weaponStack, type);
        }

        CustomWeapon template = WeaponManager.getTemplate(weaponStack);
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(weaponStack);
        if (type == TicketType.TICKET_B && (template == null || data == null)) {
            return applyWeaponDevelopment(player, weaponStack, type);
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
            case TICKET_C -> {
                return applyTicketC(player, ticketStack, template, data, weaponStack);
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean applyWeaponDevelopment(Player player, ItemStack targetStack, TicketType ticketType) {
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

        WeaponManager.makeWeapon(targetStack, template);
        WeaponManager.refreshHeldWeapon(player);
        recordTicketUse(player, ticketType);
        DevLog.debug(player.getName() + " developed item into weapon template " + template.getId());
        Message.send(player, "&d开发成功！目标物品已成为特殊品质武器，可继续使用开发券添加词条。");
        return true;
    }

    private static boolean applyTicketA(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack, boolean guaranteed) {
        List<String> availableStats = getStrengthenableStats(template, data);
        if (availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可强化的词条！");
            return false;
        }

        int useCount = data.getTicketAUses();
        int failStreak = data.getTicketAFailStreak();
        double baseSuccessRate = calculateSuccessRate(useCount);
        double successRate = guaranteed ? 1.0 : calculateSuccessRate(useCount, failStreak);
        TicketAChoice choice = new TicketAChoice(ticketStack, weaponStack, template, data, availableStats, useCount, successRate, guaranteed);
        pendingAChoices.put(player.getUniqueId(), choice);
        DevLog.debug(player.getName() + " opened " + (guaranteed ? "super_ticket_a" : "ticket_a") + " GUI for " + template.getId() + ", baseSuccessRate=" + formatPercent(baseSuccessRate) + ", failStreak=" + failStreak + ", successRate=" + formatPercent(successRate));
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
                "&7成功率: &f" + formatPercent(choice.successRate),
                "&7已使用强化券: &f" + choice.useCount + " 次",
                "&7连续失败: &f" + choice.failStreak + " 次",
                "&7失败加成: &f+" + formatPercent(choice.failStreak * 0.25),
                "&7可强化词条: &f" + choice.availableStats.size() + " 个",
                "&7羊毛: 基础属性",
                "&7陶瓦: 效果词条",
                "&7点击词条后立即尝试强化",
                choice.guaranteed ? "&7超级强化不会失败" : "&7失败后下次成功率 +25%",
                "&7成功后清空失败加成"
        )));
        for (int i = 0; i < choice.availableStats.size() && i < STRENGTHEN_SLOTS.length; i++) {
            String stat = choice.availableStats.get(i);
            double value = getStatBaseValue(choice.template, choice.data, stat);
            inventory.setItem(STRENGTHEN_SLOTS[i], createGuiItem(strengthenMaterial(stat, i), "&a" + statName(stat), List.of(
                    "&7当前值: &e" + format(value, stat),
                    "&7成功率: &f" + formatPercent(choice.successRate),
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
        if (choice.ticket.getAmount() <= 0) {
            Message.send(player, "&c强化券已不存在。");
            return;
        }
        if (choice.availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可强化的词条！");
            return;
        }
        if (!choice.availableStats.contains(stat)) {
            Message.send(player, "&c该词条当前不可强化。");
            return;
        }

        if (!choice.guaranteed && RANDOM.nextDouble() > choice.successRate) {
            choice.data.incrementTicketAUses();
            choice.data.incrementTicketAFailStreak();
            choice.data.saveToItemStack(choice.weapon);
            WeaponManager.updateLore(choice.weapon, choice.template, choice.data);
            WeaponManager.clearAttributes(player);
            consumeTicket(choice.ticket);
            recordTicketUse(player, choice.guaranteed ? TicketType.SUPER_TICKET_A : TicketType.TICKET_A);
            DevLog.debug(player.getName() + " ticket_a failed on " + choice.template.getId() + ", stat=" + stat + ", useCount=" + choice.useCount);
            Message.send(player, "&c强化失败！");
            Message.send(player, "&7本次成功率: " + formatPercent(choice.successRate));
            return;
        }

        double baseValue = getStatBaseValue(choice.template, choice.data, stat);
        double newValue = strengthenStat(stat, baseValue, choice.useCount);

        setStatValue(choice.template, choice.data, stat, newValue);
        if (!choice.guaranteed) {
            choice.data.incrementTicketAUses();
        }
        choice.data.resetTicketAFailStreak();
        choice.data.saveToItemStack(choice.weapon);
        WeaponManager.updateLore(choice.weapon, choice.template, choice.data);
        WeaponManager.clearAttributes(player);
        consumeTicket(choice.ticket);
        recordTicketUse(player, choice.guaranteed ? TicketType.SUPER_TICKET_A : TicketType.TICKET_A);
        DevLog.debug(player.getName() + " " + (choice.guaranteed ? "super_ticket_a" : "ticket_a") + " succeeded on " + choice.template.getId() + ", stat=" + stat + ", old=" + baseValue + ", new=" + newValue);

        Message.send(player, "&a强化成功！ &f" + statName(stat) + " &7" + format(baseValue, stat) + " &f-> &e" + format(newValue, stat));
        Message.send(player, "&7本次成功率: " + formatPercent(choice.successRate));
    }

    private static boolean applyTicketB(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack) {
        List<String> availableEffects = getAvailableEffects(template, data);
        if (availableEffects.isEmpty()) {
            Message.send(player, "&c武器已经拥有所有可能的词条！");
            return false;
        }

        TicketBChoice choice = pendingBChoices.get(player.getUniqueId());
        if (choice == null || !choice.matches(ticketStack, weaponStack)) {
            Collections.shuffle(availableEffects);
            List<String> choices = new ArrayList<>(availableEffects.subList(0, Math.min(3, availableEffects.size())));
            choice = new TicketBChoice(ticketStack, weaponStack, template, data, choices);
            pendingBChoices.put(player.getUniqueId(), choice);
            DevLog.debug(player.getName() + " opened ticket_b choices for " + template.getId() + ": " + String.join(",", choices));
        }
        openTicketBChoiceGui(player, choice);

        return false;
    }

    private static void openTicketBChoiceGui(Player player, TicketBChoice choice) {
        Inventory inventory = Bukkit.createInventory(new TicketChoiceHolder(player.getUniqueId(), TicketGuiType.TICKET_B), 27, Message.toComponent("&6选择开发词条"));
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 0; i < choice.choices.size(); i++) {
            String stat = choice.choices.get(i);
            double preview = generateBaseValue(stat);
            inventory.setItem(CHOICE_SLOTS[i], createGuiItem(Material.PAPER, "&a" + statName(stat), List.of(
                    "&7预览数值: &e" + format(preview, stat),
                    "&7点击选择此词条"
            )));
            choice.previewValues.put(stat, preview);
        }
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
        List<String> availableStats = getNonZeroStats(template, data);
        if (availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可重置的词条！");
            return false;
        }

        String stat = availableStats.get(RANDOM.nextInt(availableStats.size()));
        double baseValue = template.getEffect(stat, 0.0);

        setStatValue(template, data, stat, baseValue);
        data.incrementTicketCUses();
        data.saveToItemStack(weaponStack);
        WeaponManager.updateLore(weaponStack, template, data);
        WeaponManager.clearAttributes(player);
        recordTicketUse(player, TicketType.TICKET_C);
        DevLog.debug(player.getName() + " used ticket_c on " + template.getId() + ", stat=" + stat + ", resetValue=" + baseValue);

        Message.send(player, "&9重置成功！ &f" + statName(stat) + " &7已重置为初始值 &e" + format(baseValue, stat));
        return true;
    }

    private static double calculateSuccessRate(int useCount) {
        if (useCount <= 0) return 1.0;
        if (useCount <= 5) return 1.0 - (useCount - 1) * 0.05;
        if (useCount == 6) return 0.70;
        if (useCount == 7) return 0.60;
        if (useCount == 8) return 0.50;
        if (useCount == 9) return 0.25;
        return Math.pow(0.5, useCount - 8);
    }

    private static double calculateSuccessRate(int useCount, int failStreak) {
        return Math.min(1.0, calculateSuccessRate(useCount) + Math.max(0, failStreak) * 0.25);
    }

    private static List<String> getNonZeroStats(CustomWeapon template, WeaponInstanceData data) {
        List<String> stats = new ArrayList<>();
        if (data.getTotalDamage(template) != template.getBaseDamage()) stats.add("damage");
        if (data.getTotalAttackSpeed(template) != template.getAttackSpeed()) stats.add("attack_speed");
        if (data.getTotalEffect(template, "attack_range", 3.0) != template.getEffect("attack_range", 3.0)) stats.add("attack_range");
        for (String key : WeaponAffixManager.effectIds()) {
            if (data.getTotalEffect(template, key, 0.0) != template.getEffect(key, 0.0)) {
                stats.add(key);
            }
        }
        return stats;
    }

    private static List<String> getStrengthenableStats(CustomWeapon template, WeaponInstanceData data) {
        List<String> stats = new ArrayList<>();
        stats.add("damage");
        stats.add("attack_speed");
        stats.add("attack_range");
        for (String key : WeaponAffixManager.effectIds()) {
            if (WeaponAffixManager.isStrengthenable(template, data, key)) {
                stats.add(key);
            }
        }
        return stats;
    }

    private static List<String> getAvailableEffects(CustomWeapon template, WeaponInstanceData data) {
        List<String> available = new ArrayList<>();
        for (String key : WeaponAffixManager.effectIds()) {
            if (WeaponAffixManager.isAvailable(template, data, key)) {
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
            case "damage" -> data.setDamageBonus(value - template.getBaseDamage());
            case "attack_speed" -> data.setAttackSpeedBonus(value - template.getAttackSpeed());
            case "attack_range" -> data.setEffectBonus(stat, value - template.getEffect(stat, 3.0));
            default -> data.setEffectBonus(stat, value - template.getEffect(stat, 0.0));
        }
    }

    private static double strengthenStat(String stat, double currentValue, int useCount) {
        if (stat.equals("damage") || stat.equals("attack_speed") || stat.equals("attack_range")) {
            double multiplierRange = 0.35 / Math.pow(2, useCount);
            double multiplier = 0.35 + RANDOM.nextDouble() * multiplierRange;
            return currentValue * (1 + multiplier);
        }
        return WeaponAffixManager.strengthen(stat, currentValue, useCount, RANDOM);
    }

    private static String statName(String stat) {
        return switch (stat) {
            case "damage" -> "基础伤害";
            case "attack_speed" -> "攻击速度";
            case "attack_range" -> "攻击距离";
            default -> WeaponAffixManager.displayName(stat);
        };
    }

    private static String format(double value, String stat) {
        if (!stat.equals("damage") && !stat.equals("attack_speed") && !stat.equals("attack_range")) {
            return WeaponAffixManager.format(stat, value);
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
        PlayerDataManager.get(player).addTicketUse(type.getId());
        PlayerDataManager.save(player);
    }

    private static class TicketBChoice {
        final ItemStack ticket;
        final ItemStack weapon;
        final CustomWeapon template;
        final WeaponInstanceData data;
        final List<String> choices;
        final Map<String, Double> previewValues = new HashMap<>();

        TicketBChoice(ItemStack ticket, ItemStack weapon, CustomWeapon template, WeaponInstanceData data, List<String> choices) {
            this.ticket = ticket;
            this.weapon = weapon;
            this.template = template;
            this.data = data;
            this.choices = choices;
        }

        boolean matches(ItemStack ticket, ItemStack weapon) {
            return this.ticket == ticket && this.weapon == weapon;
        }
    }

    private static class TicketAChoice {
        final ItemStack ticket;
        final ItemStack weapon;
        final CustomWeapon template;
        final WeaponInstanceData data;
        final List<String> availableStats;
        final int useCount;
        final int failStreak;
        final double successRate;
        final boolean guaranteed;

        TicketAChoice(ItemStack ticket, ItemStack weapon, CustomWeapon template, WeaponInstanceData data,
                      List<String> availableStats, int useCount, double successRate, boolean guaranteed) {
            this.ticket = ticket;
            this.weapon = weapon;
            this.template = template;
            this.data = data;
            this.availableStats = availableStats;
            this.useCount = useCount;
            this.failStreak = data.getTicketAFailStreak();
            this.successRate = successRate;
            this.guaranteed = guaranteed;
        }
    }

    private enum TicketGuiType {
        TICKET_A,
        TICKET_B
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

            TicketBChoice choice = pendingBChoices.get(uuid);
            if (choice == null) return;

            int index = -1;
            for (int i = 0; i < CHOICE_SLOTS.length; i++) {
                if (event.getSlot() == CHOICE_SLOTS[i]) {
                    index = i;
                    break;
                }
            }
            if (index < 0 || index >= choice.choices.size()) {
                return;
            }

            String selectedStat = choice.choices.get(index);
            double baseValue = choice.previewValues.getOrDefault(selectedStat, generateBaseValue(selectedStat));

            choice.data.setEffectBonus(selectedStat, baseValue);
            choice.data.incrementTicketBUses();
            choice.data.saveToItemStack(choice.weapon);
            WeaponManager.updateLore(choice.weapon, choice.template, choice.data);
            WeaponManager.clearAttributes(player);
            consumeTicket(choice.ticket);
            recordTicketUse(player, TicketType.TICKET_B);

            pendingBChoices.remove(uuid);
            player.closeInventory();
            DevLog.debug(player.getName() + " selected ticket_b stat " + selectedStat + "=" + baseValue + " for " + choice.template.getId());
            Message.send(player, "&a已添加词条: &f" + statName(selectedStat) + " &e" + format(baseValue, selectedStat));
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

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof TicketChoiceHolder holder)) return;
            if (holder.type == TicketGuiType.TICKET_A) {
                pendingAChoices.remove(holder.playerId);
            }
            // 开发券关闭界面不刷新候选项；重新右键会打开同一组三选一，直到选择或更换目标物品。
        }
    }

    private static double generateBaseValue(String stat) {
        return switch (stat) {
            default -> WeaponAffixManager.generateBaseValue(stat, RANDOM);
        };
    }
}
