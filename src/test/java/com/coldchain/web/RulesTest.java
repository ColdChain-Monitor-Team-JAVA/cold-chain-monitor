package com.coldchain.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RulesTest {
    @Test void withinRangeIsNotABreach() { assertNull(Rules.evaluate(5, 2, 8, 2)); }
    @Test void smallExcursionIsWarning() { assertEquals("WARNING", Rules.evaluate(9, 2, 8, 2).severity()); }
    @Test void largeExcursionIsCritical() {
        assertEquals("CRITICAL", Rules.evaluate(11, 2, 8, 2).severity());
        assertEquals("LOW", Rules.evaluate(-1, 2, 8, 2).direction());
    }
    @Test void validAndInvalidTransitions() {
        assertTrue(Rules.allowed("CREATED", "IN_TRANSIT"));
        assertFalse(Rules.allowed("CREATED", "DELIVERED"));
        assertFalse(Rules.allowed("DELIVERED", "IN_TRANSIT"));
    }
}
