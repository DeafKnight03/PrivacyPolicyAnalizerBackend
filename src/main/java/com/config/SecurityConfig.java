package com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity   // <-- abilita @PreAuthorize
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // endpoint pubblici
                        .requestMatchers("/api/auth/**").permitAll()
                        // endpoint protetti per ruolo
                        .requestMatchers("/api/policies/**").hasRole("USER")
                        .requestMatchers("/api/policies/**").hasRole("USERPREMIUM")

                        // tutto il resto
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
