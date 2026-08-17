package com.roguelike.forge;

import com.roguelike.RoguelikePlugin;
import com.roguelike.ticket.TicketManager;
import com.roguelike.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ForgeTableManager {
    private static final int EQUIPMENT_SLOT = 10;
    private static final int[] MATERIAL_SLOTS = {12, 13, 14, 15};
    private static final int RESULT_SLOT = 22;
    private static final int INFO_SLOT = 4;
    private ForgeTableManager() {
    }

    public static void init(RoguelikePlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ForgeListener(), plugin);
    }

    public static boolean isForgeTable(Block block) {
        if (block == null || !isAnvil(block.getType())) return false;
        Block below = block.getRelative(0, -1, 0);
        return below.getType() == Material.WHITE_WOOL;
    }

    public static void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new ForgeHolder(), 45, Message.toComponent("&6铸造台"));
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(EQUIPMENT_SLOT, null);
        for (int slot : MATERIAL_SLOTS) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT_SLOT, createGuiItem(Material.BARRIER, "&7等待放入物品", List.of(
                "&7左侧放入装备，右侧放入券或材料",
                "&7放券: 强化/开发/移除词条",
                "&7放材料: 按配方合成",
                "&7点击成品槽完成加工"
        )));
        inventory.setItem(INFO_SLOT, createGuiItem(Material.ANVIL, "&6铸造台", List.of(
                "&7铁砧下方放白色羊毛即可制成",
                "&7左侧格子放装备",
                "&7右侧四个格子放券或材料",
                "&7成品槽会显示可执行的操作"
        )));
        player.openInventory(inventory);
    }

    private static boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
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

    private static ItemStack findTicket(Inventory inventory) {
        for (int slot : MATERIAL_SLOTS) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && TicketManager.getTicketType(stack) != null) return stack;
        }
        return null;
    }

    private static void updateResult(Inventory inventory) {
        ItemStack ticket = findTicket(inventory);
        if (ticket != null) {
            ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
            if (equipment == null || equipment.getType().isAir()) {
                inventory.setItem(RESULT_SLOT, createGuiItem(Material.BARRIER, "&c请先在左侧放入装备", List.of(
                        "&7右侧已放入券，左侧需要装备",
                        "&7点击右侧格子可移除券"
                )));
                return;
            }
            ItemStack preview = TicketManager.previewForge(ticket, equipment);
            if (preview == null) {
                inventory.setItem(RESULT_SLOT, createGuiItem(Material.BARRIER, "&c无法使用此券", List.of(
                        "&7请检查券与左侧装备是否匹配"
                )));
            } else {
                inventory.setItem(RESULT_SLOT, preview);
            }
            return;
        }

        ForgeRecipeManager.ForgeRecipe recipe = ForgeRecipeManager.match(inventory, MATERIAL_SLOTS);
        if (recipe == null) {
            inventory.setItem(RESULT_SLOT, createGuiItem(Material.BARRIER, "&c无可用配方", List.of(
                    "&7右侧四个格子摆放材料",
                    "&7或放入券加工装备",
                    "&7已加载配方: &f" + ForgeRecipeManager.count()
            )));
            return;
        }
        inventory.setItem(RESULT_SLOT, recipe.result().clone());
    }

    private static boolean isInputSlot(int slot) {
        if (slot == EQUIPMENT_SLOT) return true;
        for (int input : MATERIAL_SLOTS) {
            if (input == slot) return true;
        }
        return false;
    }

    private static void returnInputs(Player player, Inventory inventory) {
        int[] slots = new int[MATERIAL_SLOTS.length + 1];
        slots[0] = EQUIPMENT_SLOT;
        System.arraycopy(MATERIAL_SLOTS, 0, slots, 1, MATERIAL_SLOTS.length);
        for (int slot : slots) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            inventory.setItem(slot, null);
            player.getInventory().addItem(stack).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    private static void clearDepletedSlots(Inventory inventory) {
        int[] slots = new int[MATERIAL_SLOTS.length + 1];
        slots[0] = EQUIPMENT_SLOT;
        System.arraycopy(MATERIAL_SLOTS, 0, slots, 1, MATERIAL_SLOTS.length);
        for (int slot : slots) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType().isAir()) inventory.setItem(slot, null);
        }
    }

    private static class ForgeListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getView().getTopInventory().getHolder() instanceof ForgeHolder)) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            Inventory top = event.getView().getTopInventory();
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }
            int rawSlot = event.getRawSlot();
            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                if (rawSlot == RESULT_SLOT) {
                    event.setCancelled(true);
                    craft(player, top);
                    return;
                }
                if (!isInputSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }

            Bukkit.getScheduler().runTask(RoguelikePlugin.getInstance(), () -> updateResult(top));
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof ForgeHolder)) return;
            if (event.getPlayer() instanceof Player player) {
                returnInputs(player, event.getInventory());
            }
        }

        private void craft(Player player, Inventory inventory) {
            ItemStack ticket = findTicket(inventory);
            if (ticket != null) {
                ItemStack equipment = inventory.getItem(EQUIPMENT_SLOT);
                if (equipment == null || equipment.getType().isAir()) {
                    Message.send(player, "&c请先在铸造台左侧放入装备。");
                    return;
                }
                boolean accepted = TicketManager.applyForgeTicket(player, ticket, equipment);
                if (accepted) {
                    Message.send(player, "&a加工完成。");
                }
                Bukkit.getScheduler().runTask(RoguelikePlugin.getInstance(), () -> {
                    clearDepletedSlots(inventory);
                    updateResult(inventory);
                });
                return;
            }

            ForgeRecipeManager.ForgeRecipe recipe = ForgeRecipeManager.match(inventory, MATERIAL_SLOTS);
            if (recipe == null) return;
            recipe.consume(inventory, MATERIAL_SLOTS);
            ItemStack result = recipe.result().clone();
            player.getInventory().addItem(result).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            updateResult(inventory);
            Message.send(player, "&a铸造完成。");
        }
    }

    private static class ForgeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

}
