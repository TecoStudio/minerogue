package com.roguelike.gui;

import com.roguelike.util.Message;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiHelper {

    public static ItemStack createFiller() {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Message.toComponent(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
