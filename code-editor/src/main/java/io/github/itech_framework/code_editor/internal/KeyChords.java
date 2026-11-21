package io.github.itech_framework.code_editor.internal;

public final class KeyChords {

    private KeyChords() {
    }

    public static boolean isTypedText(boolean control, boolean alt, boolean meta) {
        return !meta && (!control || alt);
    }

    public static boolean isTypingPress(boolean control, boolean alt, boolean meta) {
        if (meta) {
            return false;
        }
        return control ? alt : !alt;
    }
}