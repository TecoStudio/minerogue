package com.roguelike.armor.affix;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.Random;

public interface ArmorAffix {
    String id();

    String displayName();

    Enchantment enchantment();

    int maxLevel();

    boolean isApplicable(Material material);

    int generateBaseLevel(Random random);


    int strengthen(int currentLevel);


    String format(int level);
}
