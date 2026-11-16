package com.changeflow.domain;

/** FR-022/024: Java strip semantics and Unicode code-point limits. */
public final class ContentRules {
    private ContentRules() { }
    public static String title(String value) { return normalize(value, 200); }
    public static String description(String value) { return normalize(value, 5000); }
    public static String reason(String value) { return normalize(value, 2000); }
    public static String explanation(String value) { return normalize(value, 2000); }
    public static String normalize(String value, int maximum) {
        if (value == null) throw new IllegalArgumentException("Text is required.");
        String normalized = value.strip();
        if (normalized.isBlank() || normalized.codePointCount(0, normalized.length()) > maximum) {
            throw new IllegalArgumentException("Text must be nonblank and within its code-point limit.");
        }
        return normalized;
    }
}
