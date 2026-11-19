package io.github.itech_framework.code_editor.internal.json;

public final class JsonException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6097897104624969253L;
	private final int line;
    private final int column;

    public JsonException(String message, int line, int column) {
        super(message + " (line " + line + ", column " + column + ")");
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}