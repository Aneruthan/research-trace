package com.researchtrace.backend.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextLimitsTest {

    @Test
    void keepsNullAndShortTitlesUnchanged() {
        assertThat(TextLimits.title(null)).isNull();
        assertThat(TextLimits.title("Keep the baseline")).isEqualTo("Keep the baseline");
        String exact = "a".repeat(TextLimits.TITLE_MAX_LENGTH);
        assertThat(TextLimits.title(exact)).isSameAs(exact);
    }

    @Test
    void shortensLongTitlesToTheColumnLimitWithAnEllipsis() {
        String shortened = TextLimits.title("b".repeat(300));

        assertThat(shortened).hasSize(TextLimits.TITLE_MAX_LENGTH);
        assertThat(shortened).startsWith("b".repeat(254));
        assertThat(shortened).endsWith("\u2026");
    }

    @Test
    void doesNotSplitASurrogatePair() {
        String emoji = new String(Character.toChars(0x1F600));
        String value = "c".repeat(253) + emoji + "d".repeat(10);

        String shortened = TextLimits.title(value);

        assertThat(shortened).isEqualTo("c".repeat(253) + "\u2026");
        assertThat(shortened.length()).isLessThanOrEqualTo(TextLimits.TITLE_MAX_LENGTH);
    }
}
