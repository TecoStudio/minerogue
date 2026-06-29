package com.roguelike.ticket;

import com.roguelike.RoguelikePlugin;
import com.roguelike.config.ConfigManager;
import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import com.roguelike.util.Message;
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
    private static final long CONFIRM_TIMEOUT_MS = 10_000L;
    private static final int[] CHOICE_SLOTS = {11, 13, 15};
    private static final Map<UUID, TicketAConfirmation> pendingAConfirmations = new HashMap<>();
    private static final Map<UUID, TicketBChoice> pendingBChoices = new HashMap<>();

    private static final String[] ALL_EFFECT_KEYS = {
            "lifesteal_percent", "chain_targets", "chain_range", "chain_damage_percent",
            "crit_chance", "crit_damage", "bleed_chance", "bleed_damage", "bleed_duration",
            "fire_damage", "fire_duration", "lightning_chance", "stun_duration",
            "slow_duration", "slow_level", "damage_store_percent", "damage_store_max"
    };

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

    public static void giveLevelUpTickets(Player player, int levelsGained) {
        for (int i = 0; i < levelsGained; i++) {
            player.getInventory().addItem(createTicket(TicketType.TICKET_A));
            player.getInventory().addItem(createTicket(TicketType.TICKET_B));
            player.getInventory().addItem(createTicket(TicketType.TICKET_C));
        }
    }

    public static boolean applyTicket(Player player, ItemStack ticketStack, ItemStack weaponStack) {
        TicketType type = getTicketType(ticketStack);
        if (type == null) return false;
        if (type == TicketType.WEAPON_DEVELOPMENT) {
            return applyWeaponDevelopment(player, weaponStack);
        }

        CustomWeapon template = WeaponManager.getTemplate(weaponStack);
        WeaponInstanceData data = WeaponInstanceData.fromItemStack(weaponStack);
        if (type == TicketType.TICKET_B && (template == null || data == null)) {
            return applyWeaponDevelopment(player, weaponStack);
        }
        if (template == null || data == null) {
            Message.send(player, "&c目标物品不是 Roguelike 武器！");
            return false;
        }

        switch (type) {
            case TICKET_A -> {
                return applyTicketA(player, ticketStack, template, data, weaponStack);
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

    private static boolean applyWeaponDevelopment(Player player, ItemStack targetStack) {
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
        Message.send(player, "&d开发成功！目标物品已成为特殊品质武器，可继续使用开发券添加词条。");
        return true;
    }

    private static boolean applyTicketA(Player player, ItemStack ticketStack, CustomWeapon template, WeaponInstanceData data, ItemStack weaponStack) {
        List<String> availableStats = getNonZeroStats(template, data);
        if (availableStats.isEmpty()) {
            Message.send(player, "&c武器没有可强化的词条！");
            return false;
        }

        int useCount = data.getTicketAUses();
        double successRate = calculateSuccessRate(useCount);
        TicketAConfirmation pending = pendingAConfirmations.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (pending == null || !pending.matches(ticketStack, weaponStack, useCount) || now > pending.expiresAt) {
            pendingAConfirmations.put(player.getUniqueId(), new TicketAConfirmation(ticketStack, weaponStack, useCount, now + CONFIRM_TIMEOUT_MS));
            Message.send(player, "&e强化成功率: &f" + String.format("%.1f%%", successRate * 100));
            Message.send(player, "&7保持两手物品不变，10秒内再次右键确认强化。成功或失败都会消耗强化券。");
            return false;
        }
        pendingAConfirmations.remove(player.getUniqueId());

        String stat = availableStats.get(RANDOM.nextInt(availableStats.size()));

        if (RANDOM.nextDouble() > successRate) {
            data.incrementTicketAUses();
            data.saveToItemStack(weaponStack);
            WeaponManager.updateLore(weaponStack, template, data);
            Message.send(player, "&c强化失败！");
            Message.send(player, "&7当前成功率: " + String.format("%.1f%%", successRate * 100));
            return false;
        }

        double baseValue = getStatBaseValue(template, data, stat);
        double multiplierRange = 0.35 / Math.pow(2, useCount);
        double multiplier = 0.35 + RANDOM.nextDouble() * multiplierRange;
        double newValue = baseValue * (1 + multiplier);

        setStatValue(template, data, stat, newValue);
        data.incrementTicketAUses();
        data.saveToItemStack(weaponStack);
        WeaponManager.updateLore(weaponStack, template, data);
        WeaponManager.clearAttributes(player);

        Message.send(player, "&a强化成功！ &f" + statName(stat) + " &7" + format(baseValue, stat) + " &f-> &e" + format(newValue, stat));
        Message.send(player, "&7成功率: " + String.format("%.1f%%", successRate * 100));
        return true;
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
        }
        openTicketBChoiceGui(player, choice);

        return false;
    }

    private static void openTicketBChoiceGui(Player player, TicketBChoice choice) {
        Inventory inventory = Bukkit.createInventory(new TicketChoiceHolder(player.getUniqueId()), 27, Message.toComponent("&6选择开发词条"));
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

    private static List<String> getNonZeroStats(CustomWeapon template, WeaponInstanceData data) {
        List<String> stats = new ArrayList<>();
        if (data.getTotalDamage(template) != template.getBaseDamage()) stats.add("damage");
        if (data.getTotalAttackSpeed(template) != template.getAttackSpeed()) stats.add("attack_speed");
        if (data.getTotalEffect(template, "attack_range", 3.0) != template.getEffect("attack_range", 3.0)) stats.add("attack_range");
        for (String key : ALL_EFFECT_KEYS) {
            if (data.getTotalEffect(template, key, 0.0) != template.getEffect(key, 0.0)) {
                stats.add(key);
            }
        }
        return stats;
    }

    private static List<String> getAvailableEffects(CustomWeapon template, WeaponInstanceData data) {
        List<String> available = new ArrayList<>();
        for (String key : ALL_EFFECT_KEYS) {
            if (template.getEffect(key, 0.0) == 0 && data.getEffectBonus(key) == 0) {
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

    private static String statName(String stat) {
        return switch (stat) {
            case "damage" -> "基础伤害";
            case "attack_speed" -> "攻击速度";
            case "attack_range" -> "攻击距离";
            case "lifesteal_percent" -> "吸血百分比";
            case "lifesteal_flat" -> "吸血固定值";
            case "slow_duration" -> "减速时长";
            case "slow_level" -> "减速等级";
            case "chain_targets" -> "连锁目标";
            case "chain_range" -> "连锁范围";
            case "chain_damage_percent" -> "连锁伤害";
            case "damage_store_percent" -> "伤害存储率";
            case "damage_store_max" -> "伤害存储上限";
            case "crit_chance" -> "暴击率";
            case "crit_damage" -> "暴击倍率";
            case "bleed_chance" -> "流血概率";
            case "bleed_damage" -> "流血伤害";
            case "bleed_duration" -> "流血时长";
            case "fire_damage" -> "火焰伤害";
            case "fire_duration" -> "燃烧时长";
            case "lightning_chance" -> "雷电概率";
            case "stun_duration" -> "眩晕时长";
            default -> stat;
        };
    }

    private static String format(double value, String stat) {
        if (stat.contains("chance") || stat.equals("lifesteal_percent") || stat.equals("chain_damage_percent") || stat.equals("damage_store_percent")) {
            return WeaponManager.format(value * 100, 1) + "%";
        }
        if (stat.equals("chain_targets") || stat.equals("slow_level")) return String.valueOf((int) value);
        return WeaponManager.format(value, 2);
    }

    private static void consumeTicket(ItemStack ticketStack) {
        ticketStack.setAmount(ticketStack.getAmount() - 1);
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

    private static class TicketAConfirmation {
        final ItemStack ticket;
        final ItemStack weapon;
        final int useCount;
        final long expiresAt;

        TicketAConfirmation(ItemStack ticket, ItemStack weapon, int useCount, long expiresAt) {
            this.ticket = ticket;
            this.weapon = weapon;
            this.useCount = useCount;
            this.expiresAt = expiresAt;
        }

        boolean matches(ItemStack ticket, ItemStack weapon, int useCount) {
            return this.ticket == ticket && this.weapon == weapon && this.useCount == useCount;
        }
    }

    private static class TicketChoiceHolder implements InventoryHolder {
        final UUID playerId;

        TicketChoiceHolder(UUID playerId) {
            this.playerId = playerId;
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

            pendingBChoices.remove(uuid);
            player.closeInventory();
            Message.send(player, "&a已添加词条: &f" + statName(selectedStat) + " &e" + format(baseValue, selectedStat));
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof TicketChoiceHolder holder)) return;
            // 关闭界面不刷新候选项；重新右键会打开同一组三选一，直到选择或更换目标物品。
        }
    }

    private static double generateBaseValue(String stat) {
        return switch (stat) {
            case "lifesteal_percent" -> 0.10 + RANDOM.nextDouble() * 0.10;
            case "crit_chance" -> 0.05 + RANDOM.nextDouble() * 0.10;
            case "crit_damage" -> 1.5 + RANDOM.nextDouble() * 0.5;
            case "chain_targets" -> RANDOM.nextInt(3) + 1;
            case "chain_range" -> 2.0 + RANDOM.nextDouble() * 2.0;
            case "chain_damage_percent" -> 0.30 + RANDOM.nextDouble() * 0.20;
            case "bleed_chance" -> 0.10 + RANDOM.nextDouble() * 0.15;
            case "bleed_damage" -> 2.0 + RANDOM.nextDouble() * 4.0;
            case "bleed_duration" -> 2.0 + RANDOM.nextDouble() * 3.0;
            case "fire_damage" -> 2.0 + RANDOM.nextDouble() * 3.0;
            case "fire_duration" -> 2.0 + RANDOM.nextDouble() * 3.0;
            case "lightning_chance" -> 0.05 + RANDOM.nextDouble() * 0.10;
            case "stun_duration" -> 0.5 + RANDOM.nextDouble() * 1.0;
            case "slow_duration" -> 1.0 + RANDOM.nextDouble() * 2.0;
            case "slow_level" -> RANDOM.nextInt(2) + 1;
            case "damage_store_percent" -> 0.10 + RANDOM.nextDouble() * 0.15;
            case "damage_store_max" -> 20.0 + RANDOM.nextDouble() * 30.0;
            default -> RANDOM.nextDouble() * 0.2;
        };
    }
}
