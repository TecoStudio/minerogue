package com.roguelike.weapon;

import com.roguelike.item.CustomWeapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponManagerTest {

    @BeforeEach
    void resetResourcePackModels() {
        // The jar scan never runs in unit tests; keep the registry empty by default
        // so each test opts in to the ids it needs.
        WeaponManager.setResourcePackModelIdsForTest(Set.of());
    }

    @Test
    void specialWeaponKeepsOriginalDisplayAndDoesNotUseResourcePackModel() {
        CustomWeapon template = weapon("special_weapon", "special");

        assertFalse(WeaponManager.usesTemplateDisplay(template));
        assertFalse(WeaponManager.usesResourcePackModel(template));
    }

    @Test
    void vanillaMainHandDamageUsesMaterialDefaults() {
        assertEquals(4.0, WeaponManager.getVanillaMainHandDamage(org.bukkit.Material.WOODEN_SWORD), 0.001);
        assertEquals(8.0, WeaponManager.getVanillaMainHandDamage(org.bukkit.Material.NETHERITE_SWORD), 0.001);
    }

    @Test
    void yamlWeaponUsesTemplateDisplayAndResourcePackModel() {
        WeaponManager.setResourcePackModelIdsForTest(Set.of("flame_sword"));
        CustomWeapon template = weapon("flame_sword", "epic");

        assertTrue(WeaponManager.usesTemplateDisplay(template));
        assertTrue(WeaponManager.usesResourcePackModel(template));
    }

    @Test
    void weaponWithoutTextureFallsBackToVanilla() {
        // No texture registered for flame_sword -> must not bind the resource-pack model.
        CustomWeapon template = weapon("flame_sword", "epic");

        assertTrue(WeaponManager.usesTemplateDisplay(template));
        assertFalse(WeaponManager.usesResourcePackModel(template));
    }

    @Test
    void specialWeaponExcludedEvenWhenTexturePresent() {
        // special_weapon is gated by usesTemplateDisplay, so even with a texture
        // registered it must keep its vanilla look.
        WeaponManager.setResourcePackModelIdsForTest(Set.of("special_weapon"));
        CustomWeapon template = weapon("special_weapon", "special");

        assertFalse(WeaponManager.usesResourcePackModel(template));
    }

    @Test
    void collectResourcePackModelIdsReturnsIntersectionOfItemsAndTextures() {
        List<String> entries = List.of(
                "resourcepack/assets/minerogue/items/flame_sword.json",
                "resourcepack/assets/minerogue/textures/item/flame_sword.png",
                "resourcepack/assets/minerogue/items/ember_knife.json",
                "resourcepack/assets/minerogue/models/item/ember_knife.json",
                "resourcepack/assets/minerogue/items/wooden_sword.json",
                "resourcepack/assets/minerogue/textures/item/wooden_sword.png",
                "resourcepack/pack.mcmeta");

        Set<String> ids = WeaponManager.collectResourcePackModelIds(entries);

        assertTrue(ids.contains("flame_sword"));
        assertTrue(ids.contains("wooden_sword"));
        // ember_knife has an item-model json but no texture png -> excluded.
        assertFalse(ids.contains("ember_knife"));
        assertEquals(2, ids.size());
    }

    private static CustomWeapon weapon(String id, String rarity) {
        return new CustomWeapon(id, id, "", "minecraft:wooden_sword", 4, 1.6, 59, rarity,
                Map.of("attack_range", 3.0), 0, false, "");
    }
}
