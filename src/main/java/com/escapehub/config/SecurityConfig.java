package com.escapehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * US-008: απλή σύνδεση/αποσύνδεση με form login.
 *
 * <p>Προσωρινή υλοποίηση για το Demo v0.5: ένας in-memory χρήστης. Θα αντικατασταθεί
 * από χρήστες στη βάση δεδομένων όταν υλοποιηθεί η εγγραφή (module {@code user}).
 */
@Configuration
public class SecurityConfig {

    /**
     * Ορίζει ποιες σελίδες είναι ανοιχτές και πώς γίνεται login/logout.
     *
     * @param http η ρύθμιση ασφάλειας HTTP
     * @return η αλυσίδα φίλτρων ασφάλειας
     * @throws Exception αν αποτύχει η δημιουργία της αλυσίδας
     */
    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/room", "/error",
                                "/css/**", "/js/**", "/api/v1/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());
        return http.build();
    }

    /**
     * Κωδικοποιητής κωδικών (BCrypt μέσω delegating encoder).
     *
     * @return ο κωδικοποιητής κωδικών
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Προσωρινός demo χρήστης. Τα στοιχεία διαβάζονται από το application.yml
     * ({@code escapehub.auth.*}) και ΔΕΝ προορίζονται για παραγωγή.
     *
     * @param username το όνομα χρήστη του demo
     * @param password ο κωδικός του demo
     * @param encoder ο κωδικοποιητής κωδικών
     * @return η υπηρεσία χρηστών
     */
    @Bean
    UserDetailsService userDetailsService(
            @Value("${escapehub.auth.demo-username}") final String username,
            @Value("${escapehub.auth.demo-password}") final String password,
            final PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(password))
                        .roles("PLAYER")
                        .build());
    }
}
