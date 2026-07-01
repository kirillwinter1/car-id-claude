package ru.car.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContactLinks Tests")
class ContactLinksTest {

    @Test
    void telegram() {
        assertThat(ContactLinks.telegram("@ivan")).isEqualTo("https://t.me/ivan");
        assertThat(ContactLinks.telegram("ivan")).isEqualTo("https://t.me/ivan");
        assertThat(ContactLinks.telegram("https://t.me/ivan")).isEqualTo("https://t.me/ivan");
        assertThat(ContactLinks.telegram("  ")).isNull();
        assertThat(ContactLinks.telegram(null)).isNull();
    }

    @Test
    void vk() {
        assertThat(ContactLinks.vk("ivan_vk")).isEqualTo("https://vk.me/ivan_vk");
        assertThat(ContactLinks.vk("https://vk.com/ivan_vk")).isEqualTo("https://vk.me/ivan_vk");
        assertThat(ContactLinks.vk("id12345")).isEqualTo("https://vk.me/id12345");
    }

    @Test
    void max() {
        assertThat(ContactLinks.max("max.ru/u/abc")).isEqualTo("https://max.ru/u/abc");
        assertThat(ContactLinks.max("https://max.ru/u/abc")).isEqualTo("https://max.ru/u/abc");
        assertThat(ContactLinks.max("")).isNull();
    }
}
