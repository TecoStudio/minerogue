package com.roguelike.mana;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManaManagerTest {
    @Test
    void progressClampsToExperienceBarRange() {
        assertEquals(0.0f, ManaManager.progress(-1.0, 100.0), 0.001f);
        assertEquals(0.5f, ManaManager.progress(50.0, 100.0), 0.001f);
        assertEquals(1.0f, ManaManager.progress(150.0, 100.0), 0.001f);
        assertEquals(0.0f, ManaManager.progress(50.0, 0.0), 0.001f);
    }
}
