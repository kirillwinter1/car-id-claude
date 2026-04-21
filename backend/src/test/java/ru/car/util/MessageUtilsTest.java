package ru.car.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MessageUtilsTest {

    @Test
    void escapeHtml_escapesAngleBracketsAndAmpersand() {
        assertThat(MessageUtils.escapeHtml("<b>A & B</b>")).isEqualTo("&lt;b&gt;A &amp; B&lt;/b&gt;");
    }

    @Test
    void escapeHtml_null_returnsEmptyString() {
        assertThat(MessageUtils.escapeHtml(null)).isEqualTo("");
    }

    @Test
    void formatPhone_russianNumber() {
        assertThat(MessageUtils.formatPhone("79313178898")).isEqualTo("+7 931 317-88-98");
    }

    @Test
    void formatPhone_invalidLength_returnsOriginal() {
        assertThat(MessageUtils.formatPhone("abc")).isEqualTo("abc");
        assertThat(MessageUtils.formatPhone("12345")).isEqualTo("12345");
    }

    @Test
    void formatPhone_null_returnsEmptyString() {
        assertThat(MessageUtils.formatPhone(null)).isEqualTo("");
    }
}
