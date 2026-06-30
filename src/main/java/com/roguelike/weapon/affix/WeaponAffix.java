package com.roguelike.weapon.affix;

import com.roguelike.item.CustomWeapon;
import com.roguelike.item.WeaponInstanceData;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Random;

public interface WeaponAffix {
    String id();

    String displayName();

    boolean isStrengthenable(CustomWeapon template, WeaponInstanceData data);

    double generateBaseValue(Random random);

    double strengthen(double currentValue, int useCount, Random random);

    String format(double value);

    void appendLore(List<Component> lore, CustomWeapon template, WeaponInstanceData data);
}
