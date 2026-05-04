package com.reports.api.config;

import com.reports.api.security.JwtAuthenticationFilter;
import com.reports.api.security.OAuth2LoginFailureHandler;
import com.reports.api.security.OAuth2LoginSuccessHandler;
import com.reports.api.security.UnauthorizedEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UnauthorizedEntryPoint unauthorizedEntryPoint,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oauth2LoginFailureHandler,
            @Value("${spring.security.oauth2.client.registration.azure.client-id:}") String azureClientId
    ) throws Exception {
        boolean microsoftLoginEnabled = StringUtils.hasText(azureClientId);
        SessionCreationPolicy sessionPolicy = microsoftLoginEnabled
                ? SessionCreationPolicy.IF_REQUIRED
                : SessionCreationPolicy.STATELESS;

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(sessionPolicy))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/auth/login", "/auth/oauth2/azure").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (microsoftLoginEnabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureHandler(oauth2LoginFailureHandler));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
