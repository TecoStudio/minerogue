package com.roguelike.forge;

import com.roguelike.RoguelikePlugin;
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
    private static final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 24;
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
        for (int slot : INPUT_SLOTS) {
            inventory.setItem(slot, null);
        }
        inventory.setItem(RESULT_SLOT, createGuiItem(Material.BARRIER, "&c无可用配方", List.of(
                "&7按工作台 3x3 形式摆放材料",
                "&7已加载配方: &f" + ForgeRecipeManager.count()
        )));
        inventory.setItem(INFO_SLOT, createGuiItem(Material.ANVIL, "&6铸造台", List.of(
                "&7铁砧下方放白色羊毛即可制成",
                "&7用于合成插件防具、工具和武器",
                "&7点击右侧结果槽完成铸造"
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

    private static void updateResult(Inventory inventory) {
        ForgeRecipeManager.ForgeRecipe recipe = ForgeRecipeManager.match(inventory, INPUT_SLOTS);
        if (recipe == null) {
            inventory.setItem(RESULT_SLOT, createGuiItem(Material.BARRIER, "&c无可用配方", List.of(
                    "&7按工作台 3x3 形式摆放材料",
                    "&7已加载配方: &f" + ForgeRecipeManager.count()
            )));
            return;
        }
        inventory.setItem(RESULT_SLOT, recipe.result().clone());
    }

    private static boolean isInputSlot(int slot) {
        for (int input : INPUT_SLOTS) {
            if (input == slot) return true;
        }
        return false;
    }

    private static void returnInputs(Player player, Inventory inventory) {
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            inventory.setItem(slot, null);
            player.getInventory().addItem(stack).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
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
            ForgeRecipeManager.ForgeRecipe recipe = ForgeRecipeManager.match(inventory, INPUT_SLOTS);
            if (recipe == null) return;
            recipe.consume(inventory, INPUT_SLOTS);
            ItemStack result = recipe.result().clone();
            player.getInventory().addItem(result).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            updateResult(inventory);
            Message.send(player, "&a铸造完成。 ");
        }
    }

    private static class ForgeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

}
