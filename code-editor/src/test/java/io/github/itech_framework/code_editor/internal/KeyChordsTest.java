package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyChordsTest {

    @Test
    void typedTextAllowsPlainKeysAndAltGr() {
        assertTrue(KeyChords.isTypedText(false, false, false));
        assertTrue(KeyChords.isTypedText(true, true, false));
        assertTrue(KeyChords.isTypedText(false, true, false));
    }

    @Test
    void typedTextRejectsControlAndMeta() {
        assertFalse(KeyChords.isTypedText(true, false, false));
        assertFalse(KeyChords.isTypedText(false, false, true));
        assertFalse(KeyChords.isTypedText(true, true, true));
        assertFalse(KeyChords.isTypedText(false, true, true));
    }

    @Test
    void typingPressAllowsPlainKeysAndAltGr() {
        assertTrue(KeyChords.isTypingPress(false, false, false));
        assertTrue(KeyChords.isTypingPress(true, true, false));
    }

    @Test
    void typingPressRejectsShortcuts() {
        assertFalse(KeyChords.isTypingPress(true, false, false));
        assertFalse(KeyChords.isTypingPress(false, true, false));
        assertFalse(KeyChords.isTypingPress(false, false, true));
        assertFalse(KeyChords.isTypingPress(true, true, true));
    }
}