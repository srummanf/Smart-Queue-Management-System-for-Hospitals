package com.hospitalqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Route rules mirror PLAN.md §7's route table exactly. Patient-facing
 * {@code /patients/**} stays unauthenticated by design (PRD §5, CLAUDE.md) —
 * do not add a role requirement there. Uses Spring Security's built-in
 * default login page rather than a custom template, as an explicit
 * minimalist scope decision.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/webjars/**", "/ws/**", "/patients/**").permitAll()
                        .requestMatchers("/doctors/*/dashboard", "/doctors/*/call-next", "/tokens/*/complete")
                            .hasRole("DOCTOR")
                        .requestMatchers("/register", "/tokens/*/escalate", "/analytics").hasRole("STAFF")
                        .requestMatchers("/tokens/*/cancel").hasAnyRole("STAFF", "DOCTOR")
                        .anyRequest().authenticated())
                .formLogin(withDefaults -> {});

        return http.build();
    }
}
