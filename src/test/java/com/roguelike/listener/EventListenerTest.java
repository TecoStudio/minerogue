package com.roguelike.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventListenerTest {
    @Test
    void customHealingIsClampedToMaximumHealth() {
        assertEquals(20.0, EventListener.healedHealth(10.0, 20.0, 15.0), 0.001);
        assertEquals(10.0, EventListener.healedHealth(10.0, 20.0, 0.0), 0.001);
    }

    @Test
    void deathExplosionChanceUsesClampedChanceRoll() {
        assertTrue(EventListener.shouldTriggerChance(1.5, 0.99));
        assertFalse(EventListener.shouldTriggerChance(-0.1, 0.0));
        assertTrue(EventListener.shouldTriggerChance(0.25, 0.24));
        assertFalse(EventListener.shouldTriggerChance(0.25, 0.25));
    }

    @Test
    void bleedingDamageBonusOnlyAppliesToBleedingTargets() {
        assertEquals(12.5, EventListener.damageWithBleedingBonus(10.0, true, 0.25), 0.001);
        assertEquals(10.0, EventListener.damageWithBleedingBonus(10.0, false, 0.25), 0.001);
        assertEquals(10.0, EventListener.damageWithBleedingBonus(10.0, true, -0.25), 0.001);
    }
}