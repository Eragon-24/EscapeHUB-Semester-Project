package com.escapehub.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared application clock, so services can be tested with a fixed time.
 */
@Configuration
public class ClockConfig {

    /**
     * The system clock.
     *
     * @return the system {@link Clock}
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
