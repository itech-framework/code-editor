package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyChordsCommentTest {

    @Test
    void slashKeyWithoutShiftToggles() {
        assertTrue(KeyChords.isCommentToggle("SLASH", "/", false));
        assertTrue(KeyChords.isCommentToggle("DIVIDE", "", false));
    }

    @Test
    void slashKeyWithShiftDoesNotToggle() {
        assertFalse(KeyChords.isCommentToggle("SLASH", "?", true));
    }

    @Test
    void slashTextOnAnyKeyToggles() {
        assertTrue(KeyChords.isCommentToggle("DIGIT7", "/", true));
    }

    @Test
    void shiftedSevenWithNoTextToggles() {
        assertTrue(KeyChords.isCommentToggle("DIGIT7", "", true));
        assertTrue(KeyChords.isCommentToggle("DIGIT7", null, true));
    }

    @Test
    void shiftedSevenOnUsLayoutDoesNotToggle() {
        assertFalse(KeyChords.isCommentToggle("DIGIT7", "&", true));
    }

    @Test
    void unrelatedKeysDoNotToggle() {
        assertFalse(KeyChords.isCommentToggle("A", "a", false));
        assertFalse(KeyChords.isCommentToggle("DIGIT7", "7", false));
    }
}