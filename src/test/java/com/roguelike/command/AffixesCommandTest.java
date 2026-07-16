package com.roguelike.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffixesCommandTest {
    @Test
    void affixesSubcommandSupportsHeldWeaponInspectionMode() {
        assertTrue(AffixesCommand.isHeldMode(new String[]{"affixes", "held"}));
        assertTrue(AffixesCommand.isHeldMode(new String[]{"affixes", "hand"}));
        assertTrue(AffixesCommand.isHeldMode(new String[]{"affixes", "held", "hermesbot"}));
        assertFalse(AffixesCommand.isHeldMode(new String[]{"affixes"}));
        assertFalse(AffixesCommand.isHeldMode(new String[]{"affixes", "unknown"}));
    }

    @Test
    void heldModeOptionalTargetNameIsOnlyAcceptedForInspectionMode() {
        assertEquals("hermesbot", AffixesCommand.heldTargetName(new String[]{"affixes", "held", "hermesbot"}));
        assertEquals("hermesbot", AffixesCommand.heldTargetName(new String[]{"affixes", "hand", "hermesbot"}));
        assertEquals(null, AffixesCommand.heldTargetName(new String[]{"affixes", "held"}));
        assertEquals(null, AffixesCommand.heldTargetName(new String[]{"affixes", "unknown", "hermesbot"}));
    }
}
