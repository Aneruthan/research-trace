package com.researchtrace.backend.model;

public final class TextLimits {

    /** Title columns are varchar(255) in the entities and in docker/initdb/01-init.sql. */
    public static final int TITLE_MAX_LENGTH = 255;

    private static final String ELLIPSIS = "\u2026";

    private TextLimits() {
    }

    public static String title(String value) {
        if (value == null || value.length() <= TITLE_MAX_LENGTH) {
            return value;
        }
        int end = TITLE_MAX_LENGTH - ELLIPSIS.length();
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end) + ELLIPSIS;
    }
}
