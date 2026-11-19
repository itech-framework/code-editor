package io.github.itech_framework.code_editor.language.template;

public final class TemplateException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 786866947381404915L;

	public TemplateException(String source, String field, String reason) {
        super(source + ": " + field + ": " + reason);
    }

    public TemplateException(String source, String field, String reason, Throwable cause) {
        super(source + ": " + field + ": " + reason, cause);
    }
}