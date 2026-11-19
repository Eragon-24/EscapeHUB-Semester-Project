package com.escapehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    /**
     * Κωδικοποιητής κωδικών (BCrypt μέσω delegating encoder).
     *
     * <p>Κοινός για την εγγραφή (US-007) και τη σύνδεση (US-008): ο hash αποθηκεύεται με πρόθεμα
     * ({@code {bcrypt}...}) και η σύνδεση τον επαληθεύει με τον ίδιο encoder.
     *
     * @return ο κωδικοποιητής κωδικών
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
