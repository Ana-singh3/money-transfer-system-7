package com.moneytransfersystem.controller.support;

import com.moneytransfersystem.config.JwtTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Minimal security config for @WebMvcTest slices:
 * - Enables @PreAuthorize on controller methods
 * - Permits /api/v1/auth/** without authentication
 * - Requires authentication for /api/**
 * - Does NOT register JWT filters (unit tests should not rely on JWT plumbing)
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class WebMvcTestSecurityConfig {

    /**
     * Provide JwtTokenProvider bean because the application has a JwtAuthenticationFilter component
     * that depends on it. In @WebMvcTest slices, we don't want to mock the filter (can short-circuit
     * the chain); instead we give it a harmless provider so it becomes effectively a no-op when
     * no Authorization header is present.
     */
    @Bean
    JwtTokenProvider jwtTokenProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret",
                "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong");
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", 86400000L);
        return provider;
    }

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .anonymous(anon -> anon.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase())
                        )
                )
                // Provide a simple auth mechanism for the framework; @WithMockUser supplies Authentication.
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }
}


