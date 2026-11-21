package io.github.itech_framework.code_editor.theme;

public final class ThemeException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ThemeException(String source, String field, String reason) {
        super(source + ": " + field + ": " + reason);
    }

    public ThemeException(String source, String field, String reason, Throwable cause) {
        super(source + ": " + field + ": " + reason, cause);
    }
}