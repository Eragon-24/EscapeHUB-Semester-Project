package com.escapehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * US-008: σύνδεση/αποσύνδεση με form login.
 *
 * <p>Οι χρήστες προέρχονται από το {@code UserRepository} του module {@code user} (US-007), μέσω του
 * {@code UserAccountDetailsService}. Έτσι μόνο εγγεγραμμένοι λογαριασμοί μπορούν να συνδεθούν.
 */
@Configuration
public class SecurityConfig {

    /**
     * Ορίζει ποιες σελίδες είναι ανοιχτές και πώς γίνεται login/logout.
     * Η σελίδα εγγραφής {@code /register} είναι δημόσια, ώστε ένας ανώνυμος επισκέπτης να μπορεί να
     * δημιουργήσει λογαριασμό.
     *
     * @param http η ρύθμιση ασφάλειας HTTP
     * @return η αλυσίδα φίλτρων ασφάλειας
     * @throws Exception αν αποτύχει η δημιουργία της αλυσίδας
     */
    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/register", "/room", "/error",
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
