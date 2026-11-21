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
    
    public static boolean isCommentToggle(String codeName, String text, boolean shift) {
        if ("/".equals(text)) {
            return true;
        }
        if ("SLASH".equals(codeName) || "DIVIDE".equals(codeName)) {
            return !shift;
        }
        return "DIGIT7".equals(codeName) && shift && (text == null || text.isEmpty());
    }
}