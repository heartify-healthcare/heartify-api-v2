package com.heartify.configserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()  // Allow health checks without auth
                .anyRequest().authenticated()  // All other requests require authentication
            )
            .httpBasic(Customizer.withDefaults())  // Enable HTTP Basic Authentication
            .csrf(csrf -> csrf.disable());  // Disable CSRF for simplicity in local dev

        return http.build();
    }
}
