package io.notifyhub.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    @Test
    void exactMatch() {
        assertTrue(Router.matches("alert", "alert"));
        assertFalse(Router.matches("alert.db", "alert"));
        assertFalse(Router.matches("alert", "alert.db"));
    }

    @Test
    void starMatchesExactlyOneSegment() {
        assertTrue(Router.matches("alert.db", "alert.*"));
        assertTrue(Router.matches("alert.db", "*.db"));
        assertTrue(Router.matches("alert.db.down", "*.*.down"));
        assertFalse(Router.matches("alert", "alert.*"));
        assertFalse(Router.matches("alert.db.down", "alert.*"));
    }

    @Test
    void hashMatchesZeroOrMoreSegments() {
        assertTrue(Router.matches("logs", "logs.#"));
        assertTrue(Router.matches("logs.a", "logs.#"));
        assertTrue(Router.matches("logs.a.b.c", "logs.#"));
        assertFalse(Router.matches("app.logs.a", "logs.#"));
        // '#' 仅允许作为末段
        assertFalse(Router.matches("logs.a.b", "logs.#.b"));
    }

    @Test
    void hashAloneMatchesEverything() {
        assertTrue(Router.matches("anything", "#"));
        assertTrue(Router.matches("a.b.c", "#"));
    }

    @Test
    void blankNeverMatches() {
        assertFalse(Router.matches("", "*"));
        assertFalse(Router.matches("alert", ""));
        assertFalse(Router.matches(null, "*"));
    }
}
