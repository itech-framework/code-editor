package io.github.itech_framework.code_editor.internal;

public final class SingleLineText {

	private SingleLineText() {
	}

	public static String strip(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		if (text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
			return text;
		}
		StringBuilder out = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c != '\n' && c != '\r') {
				out.append(c);
			}
		}
		return out.toString();
	}
}