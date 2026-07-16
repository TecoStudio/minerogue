package com.roguelike.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiveCommandTest {
    @Test
    void invalidAmountsAreRejectedAndValidAmountsAreClampedToPositiveValues() {
        assertEquals(1, GiveCommand.parseAmount(null));
        assertEquals(1, GiveCommand.parseAmount("not-a-number"));
        assertEquals(1, GiveCommand.parseAmount("0"));
        assertEquals(1, GiveCommand.parseAmount("-2"));
        assertEquals(32, GiveCommand.parseAmount("32"));
        assertEquals(64, GiveCommand.parseAmount("100"));
    }

    @Test
    void noArgumentGiveUsesGuiMode() {
        assertTrue(GiveCommand.isGuiMode(new String[]{"give"}));
        assertFalse(GiveCommand.isGuiMode(new String[]{"give", "weapon", "wooden_sword"}));
    }

    @Test
    void giveGuiPagesAreClampedToAvailableRange() {
        assertEquals(0, GiveCommand.clampPage(-1, 16));
        assertEquals(0, GiveCommand.clampPage(0, 16));
        assertEquals(1, GiveCommand.clampPage(99, 46));
        assertEquals(0, GiveCommand.clampPage(5, 0));
    }
}