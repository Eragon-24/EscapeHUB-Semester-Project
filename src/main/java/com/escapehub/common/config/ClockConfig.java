package com.escapehub.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Κοινό ρολόι της εφαρμογής, ώστε οι υπηρεσίες να δοκιμάζονται με σταθερή ώρα.
 */
@Configuration
public class ClockConfig {

    /**
     * Το ρολόι συστήματος.
     *
     * @return το {@link Clock} του συστήματος
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
