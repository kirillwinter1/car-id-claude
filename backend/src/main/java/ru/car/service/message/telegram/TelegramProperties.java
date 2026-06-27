package ru.car.service.message.telegram;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("telegram")
public class TelegramProperties {
    private String bot;
    private String token;
    private Long feedbackChannelId;
    private Boolean enable;

    /** Прокси для исходящих к api.telegram.org. Нужен на проде в РФ, где Telegram заблокирован
     *  (см. TECH_DEBT P17): локальный SOCKS5 от sing-box. По умолчанию выключен — dev/тесты ходят напрямую. */
    private Proxy proxy = new Proxy();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Proxy {
        private boolean enabled = false;
        /** HTTP | SOCKS4 | SOCKS5 */
        private String type = "SOCKS5";
        private String host = "127.0.0.1";
        private int port = 10808;
    }
}
