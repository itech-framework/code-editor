package io.github.itech_framework.code_editor.theme;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

public record ThemeColor(int red, int green, int blue, double alpha) {

    private static final String FORMATS =
            "expected #rgb, #rrggbb, #rrggbbaa, rgb(r, g, b) or rgba(r, g, b, a)";

    private static final Pattern INTEGER = Pattern.compile("\\d{1,3}");

    private static final Pattern DECIMAL = Pattern.compile("\\d+(\\.\\d*)?|\\.\\d+");

    public static ThemeColor parse(String text) {
        String value = text.trim();
        if (value.startsWith("#")) {
            return hex(value.substring(1));
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("rgba(") && lower.endsWith(")")) {
            return function(lower.substring(5, lower.length() - 1), true);
        }
        if (lower.startsWith("rgb(") && lower.endsWith(")")) {
            return function(lower.substring(4, lower.length() - 1), false);
        }
        throw new IllegalArgumentException(FORMATS);
    }

    public String toCss() {
        BigDecimal rounded = BigDecimal.valueOf(Math.round(alpha * 1000) / 1000.0);
        if (rounded.compareTo(BigDecimal.ONE) >= 0) {
            return String.format(Locale.ROOT, "#%02x%02x%02x", red, green, blue);
        }
        return "rgba(" + red + ", " + green + ", " + blue + ", " + rounded.stripTrailingZeros().toPlainString() + ")";
    }

    private static ThemeColor hex(String digits) {
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            boolean valid = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!valid) {
                throw new IllegalArgumentException("'" + c + "' is not a hex digit");
            }
        }
        if (digits.length() == 3) {
            return new ThemeColor(pair(digits.charAt(0), digits.charAt(0)), pair(digits.charAt(1), digits.charAt(1)),
                    pair(digits.charAt(2), digits.charAt(2)), 1.0);
        }
        if (digits.length() == 6 || digits.length() == 8) {
            double alpha = digits.length() == 8 ? pair(digits.charAt(6), digits.charAt(7)) / 255.0 : 1.0;
            return new ThemeColor(pair(digits.charAt(0), digits.charAt(1)), pair(digits.charAt(2), digits.charAt(3)),
                    pair(digits.charAt(4), digits.charAt(5)), alpha);
        }
        throw new IllegalArgumentException("a hex color needs 3, 6 or 8 digits but has " + digits.length());
    }

    private static int pair(char high, char low) {
        return Character.digit(high, 16) * 16 + Character.digit(low, 16);
    }

    private static ThemeColor function(String arguments, boolean withAlpha) {
        String[] parts = arguments.split(",", -1);
        int expected = withAlpha ? 4 : 3;
        if (parts.length != expected) {
            throw new IllegalArgumentException((withAlpha ? "rgba" : "rgb") + " needs " + expected + " values");
        }
        int[] channels = new int[3];
        for (int i = 0; i < 3; i++) {
            String part = parts[i].trim();
            if (!INTEGER.matcher(part).matches() || Integer.parseInt(part) > 255) {
                throw new IllegalArgumentException("'" + part + "' is not a number from 0 to 255");
            }
            channels[i] = Integer.parseInt(part);
        }
        double alpha = 1.0;
        if (withAlpha) {
            String part = parts[3].trim();
            if (!DECIMAL.matcher(part).matches() || Double.parseDouble(part) > 1.0) {
                throw new IllegalArgumentException("'" + part + "' is not an alpha from 0 to 1");
            }
            alpha = Double.parseDouble(part);
        }
        return new ThemeColor(channels[0], channels[1], channels[2], alpha);
    }
}