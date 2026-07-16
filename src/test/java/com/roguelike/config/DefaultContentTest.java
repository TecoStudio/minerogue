package com.roguelike.config;

import com.roguelike.item.CustomItem;
import com.roguelike.item.CustomWeapon;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultContentTest {
    private static final Set<String> EXISTING_WEAPON_IDS = Set.of(
            "wooden_sword",
            "flame_sword",
            "vampire_dagger",
            "thunder_axe",
            "whirlwind_blade",
            "inferno_greatsword",
            "phase_scythe",
            "special_weapon",
            "rusty_iron_sword",
            "excited_stone_sword"
    );

    private static final Set<String> EXPANSION_WEAPON_IDS = Set.of(
            "ember_knife",
            "frost_cleaver",
            "storm_spear",
            "plague_saber",
            "echo_blade",
            "glass_cannon_hammer"
    );

    @Test
    void defaultWeaponsKeepExistingTemplatesAndExposeExpansionTemplates() {
        Map<String, CustomWeapon> weapons = DefaultWeapons.create();

        assertAll(
                () -> EXISTING_WEAPON_IDS.forEach(id -> assertTrue(weapons.containsKey(id), "missing existing weapon " + id)),
                () -> EXPANSION_WEAPON_IDS.forEach(id -> assertTrue(weapons.containsKey(id), "missing expansion weapon " + id)),
                () -> assertEquals(5.0, weapons.get("ember_knife").getBaseDamage(), 0.001),
                () -> assertEquals(20.0, weapons.get("glass_cannon_hammer").getBaseDamage(), 0.001)
        );
    }

    @Test
    void defaultWeaponsUseDropEligibleRaritiesExceptSpecialTemplate() {
        Map<String, CustomWeapon> weapons = DefaultWeapons.create();
        Set<String> dropRarities = Set.of("common", "rare", "epic", "legendary");

        assertEquals("special", weapons.get("special_weapon").getRarity());
        for (String id : EXPANSION_WEAPON_IDS) {
            CustomWeapon weapon = weapons.get(id);
            assertNotNull(weapon, "missing expansion weapon " + id);
            String rarity = weapon.getRarity();
            assertTrue(dropRarities.contains(rarity), id + " has non-drop rarity " + rarity);
        }
    }

    @Test
    void specialWeaponCanBreakNormalAffixSlotLimit() {
        CustomWeapon special = DefaultWeapons.create().get("special_weapon");

        assertTrue(special.allowsOverflowAffixes());
        assertEquals(0, special.getBonusAffixSlots());
    }

    @Test
    void defaultWeaponEffectsAreDefensiveCopies() {
        CustomWeapon weapon = DefaultWeapons.create().get("ember_knife");
        assertNotNull(weapon, "missing expansion weapon ember_knife");

        weapon.getEffects().put("attack_range", 99.0);

        assertEquals(2.7, weapon.getAttackRange(), 0.001);
    }

    @Test
    void defaultItemsKeepHealingPotionAndExposeDistinctExpansionItems() {
        Map<String, CustomItem> items = DefaultItems.create();
        Set<String> expansionItemIds = Set.of("greater_healing_potion", "swift_tonic", "iron_skin_tonic");

        assertAll(
                () -> assertTrue(items.containsKey("healing_potion"), "missing existing healing_potion"),
                () -> assertEquals("minecraft:potion", items.get("healing_potion").getItem()),
                () -> assertEquals(10.0, items.get("healing_potion").getEffect("heal_amount"), 0.001),
                () -> expansionItemIds.forEach(id -> assertTrue(items.containsKey(id), "missing expansion item " + id)),
                () -> expansionItemIds.forEach(id -> assertEquals("minecraft:potion", items.get(id).getItem(), id + " should use potion material")),
                () -> items.forEach((key, item) -> assertEquals(item.getId().toLowerCase(), key))
        );
    }

    @Test
    void defaultItemEffectsAreDefensiveCopies() {
        CustomItem item = DefaultItems.create().get("greater_healing_potion");
        assertNotNull(item, "missing expansion item greater_healing_potion");

        item.getEffects().put("heal_amount", 99.0);

        assertEquals(20.0, item.getEffect("heal_amount"), 0.001);
    }

    @Test
    void defaultMobModifiersExposeExistingSchemaOnly() {
        Map<String, ConfigManager.MobConfig> modifiers = DefaultMobs.modifiers();
        Map<String, CustomWeapon> weapons = DefaultWeapons.create();

        assertAll(
                () -> assertValidModifier(modifiers, weapons, "husk"),
                () -> assertValidModifier(modifiers, weapons, "drowned"),
                () -> assertValidModifier(modifiers, weapons, "pillager"),
                () -> assertValidModifier(modifiers, weapons, "zombified_piglin")
        );
    }

    @Test
    void defaultMobExperienceStillContainsExistingDefaults() {
        Map<String, Integer> experience = DefaultMobs.experience();

        assertAll(
                () -> assertEquals(15, experience.get("zombie")),
                () -> assertEquals(15, experience.get("skeleton")),
                () -> assertEquals(20, experience.get("creeper")),
                () -> assertEquals(12, experience.get("spider")),
                () -> assertEquals(30, experience.get("enderman")),
                () -> assertEquals(22, experience.get("blaze")),
                () -> assertEquals(350, experience.get("warden")),
                () -> assertEquals(500, experience.get("ender_dragon")),
                () -> assertEquals(300, experience.get("wither"))
        );
    }

    private static void assertValidModifier(Map<String, ConfigManager.MobConfig> modifiers,
                                            Map<String, CustomWeapon> weapons,
                                            String id) {
        ConfigManager.MobConfig modifier = modifiers.get(id);
        assertNotNull(modifier, "missing mob modifier " + id);
        assertTrue(modifier.healthMultiplier() > 0.0, id + " health multiplier must be positive");
        assertTrue(modifier.damageMultiplier() > 0.0, id + " damage multiplier must be positive");
        assertTrue(modifier.speedMultiplier() > 0.0, id + " speed multiplier must be positive");
        assertTrue(weapons.containsKey(modifier.weaponTemplate()), id + " references missing weapon " + modifier.weaponTemplate());
    }
}
