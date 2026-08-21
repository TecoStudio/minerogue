package com.roguelike.ticket;

import com.roguelike.equipment.EquipmentTypeResolver;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketManagerWearableRulesTest {
    @Test
    void wearableBoundaryIncludesArmorPiecesAndElytraButNotShield() {
        assertTrue(EquipmentTypeResolver.isWearable(Material.DIAMOND_HELMET));
        assertTrue(EquipmentTypeResolver.isWearable(Material.DIAMOND_CHESTPLATE));
        assertTrue(EquipmentTypeResolver.isWearable(Material.DIAMOND_LEGGINGS));
        assertTrue(EquipmentTypeResolver.isWearable(Material.DIAMOND_BOOTS));
        assertTrue(EquipmentTypeResolver.isWearable(Material.ELYTRA));
        assertFalse(EquipmentTypeResolver.isWearable(Material.SHIELD));
    }

    @Test
    void superDevelopmentTicketDoesNotTargetTools() {
        assertFalse(TicketManager.isSuperDevelopmentAllowed(Material.DIAMOND_PICKAXE));
        assertFalse(TicketManager.isSuperDevelopmentAllowed(Material.DIAMOND_AXE));
        assertTrue(TicketManager.isSuperDevelopmentAllowed(Material.DIAMOND_SWORD));
        assertTrue(TicketManager.isSuperDevelopmentAllowed(Material.DIAMOND_HELMET));
        assertTrue(TicketManager.isSuperDevelopmentAllowed(Material.ELYTRA));
    }
}
