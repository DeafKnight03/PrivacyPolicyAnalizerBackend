package com.example.myapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(c -> {}) // optional: if you have CORS needs
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/policies/analyze1").hasAnyRole("USER","USERPREMIUM")
                                .requestMatchers(HttpMethod.POST, "/api/policies/analyze2").hasRole("USERPREMIUM")
                                .requestMatchers(HttpMethod.POST, "/api/policies/save").hasAnyRole("USER","USERPREMIUM")
                                .requestMatchers(HttpMethod.POST, "/api/policies/count").hasAnyRole("USER","USERPREMIUM")


                                .anyRequest().authenticated()
                        // .requestMatchers("/policies/**").hasAnyRole("USER","USERPREMIUM") // protected (when ready)

                )
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                );
        // .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // when JWT is ready

        return http.build();
    }

    // Mappa il claim "roles" in authorities Spring: ROLE_USER/ROLE_ADMIN...
    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
        return jwt -> {
            Object raw = jwt.getClaims().getOrDefault("roles", jwt.getClaims().get("role"));
            java.util.List<String> roles = switch (raw) {
                case String s -> java.util.Arrays.asList(s.split("\\s+")); // es. "USER ADMIN"
                case java.util.Collection<?> c -> c.stream().map(String::valueOf).toList();
                case null -> java.util.List.of();
                default -> java.util.List.of(String.valueOf(raw));
            };
            var auths = roles.stream()
                    .filter(r -> !r.isBlank())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .toList();
            return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, auths, jwt.getSubject());
        };
    }




    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
    }

}