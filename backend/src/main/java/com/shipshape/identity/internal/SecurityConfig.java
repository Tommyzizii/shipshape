package com.shipshape.identity.internal;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Configuration
class SecurityConfig {

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper,
            SecurityContextRepository securityContextRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Spring forwards failed requests to /error; locking it would turn every error into a 401
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                // Cookie-based CSRF token (XSRF-TOKEN cookie, X-XSRF-TOKEN header) that a JavaScript SPA can read
                .csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                // No login page redirect and no browser basic-auth popup: API clients get Problem Details JSON
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, e) -> writeProblem(
                                jsonMapper, request, response, HttpStatus.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, e) -> writeProblem(
                                jsonMapper, request, response, HttpStatus.FORBIDDEN, "Access is denied")));
        return http.build();
    }

    private static void writeProblem(JsonMapper jsonMapper, HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String detail) throws IOException {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
