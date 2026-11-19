package io.github.itech_framework.code_editor.language.template;

public record StateAction(Type type, String target) {

    public enum Type {
        NONE, NEXT, PUSH, POP
    }

    public static StateAction none() {
        return new StateAction(Type.NONE, null);
    }

    public static StateAction pop() {
        return new StateAction(Type.POP, null);
    }
}