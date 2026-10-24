package com.foodrescue.config;

import com.foodrescue.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // ── Public UI routes (Thymeleaf views — auth enforced client-side via JWT) ──
                .requestMatchers(
                    "/", "/login", "/register",
                    "/donor/**", "/ngo/**", "/admin/**",
                    "/chat", "/notifications", "/profile", "/settings", "/help",
                    "/ai/**", "/error/**", "/error", "/actuator/health"
                ).permitAll()
                // ── Static assets & uploads ──
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**", "/fonts/**",
                    "/favicon.ico", "/webjars/**", "/uploads/**"
                ).permitAll()
                // ── WebSocket ──
                .requestMatchers("/ws-notifications/**").permitAll()
                // ── Swagger / OpenAPI ──
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // ── Public API auth endpoints ──
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                // ── Protected API endpoints ──
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/food/**").hasAnyRole("DONOR", "ADMIN", "NGO")
                .requestMatchers("/api/ai/**").hasAnyRole("DONOR", "ADMIN", "NGO")
                .requestMatchers("/api/notifications/**").hasAnyRole("DONOR", "ADMIN", "NGO")
                // BUG FIX: /api/ngo/available must be accessible by DONOR (for NGO selection after upload)
                // This rule MUST come BEFORE the broader /api/ngo/** rule — Spring processes in order
                .requestMatchers("/api/ngo/available").hasAnyRole("DONOR", "NGO", "ADMIN")
                .requestMatchers("/api/ngo/**", "/api/request/**").hasAnyRole("NGO", "ADMIN")
                // ── Donation Request Workflow (donors, NGOs, admins) ──
                .requestMatchers("/api/donor/**").hasAnyRole("DONOR", "ADMIN")
                .requestMatchers("/api/chat/**").authenticated()
                .requestMatchers("/api/requests/**").hasAnyRole("DONOR", "NGO", "ADMIN")
                .requestMatchers("/api/auth/me", "/api/auth/logout",
                                 "/api/auth/profile", "/api/auth/password").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}