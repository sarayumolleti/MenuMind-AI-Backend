package com.menumind.backend.config;

import com.menumind.backend.security.CustomAccessDeniedHandler;
import com.menumind.backend.security.JwtAuthenticationEntryPoint;
import com.menumind.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 Configuration
 * 
 * Configures stateless JWT-based authentication, role-based authorization rules,
 * password hashing, and CORS settings for the React frontend.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF since REST APIs use stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)
            // Enable CORS for frontend cross-origin requests
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Exception handling for 401 Unauthorized and 403 Forbidden
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // Stateless sessions: server stores no HTTP session state
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Role-Based URL Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu-items/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/recommendations/cross-sell/**").permitAll()

                // User Management
                .requestMatchers("/api/users/profile").authenticated()
                .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                // Manager & Admin only: Menu modifications, Analytics, ML, Dashboard
                .requestMatchers("/api/categories/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                .requestMatchers("/api/menu-items/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                .requestMatchers("/api/analytics/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                .requestMatchers("/api/ml/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                .requestMatchers("/api/ai/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                .requestMatchers("/api/dashboard/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")

                // Staff, Manager & Admin: Offline/Counter billing
                .requestMatchers("/api/bills/**").hasAnyAuthority("ROLE_STAFF", "ROLE_MANAGER", "ROLE_ADMIN")

                // Customer, Staff, Manager & Admin: Orders
                .requestMatchers("/api/orders/**").authenticated()

                // Customer Feedback
                .requestMatchers(HttpMethod.POST, "/api/feedbacks").permitAll()
                .requestMatchers("/api/feedbacks/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")

                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                // Any other request requires valid authentication
                .anyRequest().authenticated()
            );

        // Register JWT authentication filter before the standard UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cross-Origin Resource Sharing (CORS) Configuration
     * Permits React frontend (running on localhost:3000, 5173, etc.) to call this backend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
