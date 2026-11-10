package com.escapehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Temporary scaffold configuration: routes are open while the project skeleton is prepared.
     * Authentication, authorization rules and login flow must be implemented before real use.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(Customizer.withDefaults());
        return http.build();
    }
}
