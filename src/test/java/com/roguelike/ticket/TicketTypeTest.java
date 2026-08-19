package com.roguelike.ticket;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketTypeTest {
    @Test
    void superDevelopmentAndRemoveTicketsAreAvailable() {
        assertTrue(TicketType.fromId("super_ticket_b") != null);
        assertTrue(TicketType.fromId("super_ticket_c") != null);
        assertNull(TicketType.fromId("tool_ticket_b"));
    }

    @Test
    void superTicketsUseDistinctMaterials() {
        assertTrue(TicketType.fromId("super_ticket_b").getMaterial() != Material.PAPER);
        assertTrue(TicketType.fromId("super_ticket_c").getMaterial() != Material.PAPER);
    }
}
