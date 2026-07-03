package com.swp391.scientific_journal_tracker.config;

import java.util.List;

import com.swp391.scientific_journal_tracker.security.OAuth2RedirectOriginFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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

import com.swp391.scientific_journal_tracker.security.JwtAuthenticationFilter;
import com.swp391.scientific_journal_tracker.security.OAuth2SuccessHandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final OAuth2RedirectOriginFilter oAuth2RedirectOriginFilter;

        @Value("${app.frontend-url:http://localhost:5173/}")
        private String frontendUrl;

        // Chain 1: dành riêng cho /api/auth/**, public hoàn toàn
        @Bean
        @Order(1)
        public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/auth/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/reset-password",
                                                                "/api/auth/refresh-token",
                                                                "/api/auth/logout")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                                                .anyRequest().authenticated())
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .logout(logout -> logout
                                                .logoutUrl("/api/auth/logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)));
                return http.build();
        }

        // Chain 2: dành cho các API còn lại + Google OAuth2
        @Bean
        @Order(2)
        public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                .requestMatchers(
                                                                "/",
                                                                "/favicon.ico",
                                                                "/api/health",
                                                                "/login",
                                                                "/login/**",
                                                                "/login/oauth2/**",
                                                                "/oauth2/**",
                                                                "/error")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/papers/**",
                                                                "/api/keywords/**",
                                                                "/api/dashboard/**",
                                                                "/api/trends/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/journals/following",
                                                                "/api/journals/*/follow",
                                                                "/api/journals/*/follow/check",
                                                                "/api/topics/following",
                                                                "/api/topics/*/follow",
                                                                "/api/topics/*/follow/check")
                                                .authenticated()

                                                .requestMatchers(HttpMethod.GET, "/api/topics/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/journals/**").permitAll()
                                                .requestMatchers("/api/bookmarks/**").authenticated()
                                                .requestMatchers("/api/notifications/**").authenticated()

                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> response
                                                                .sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Unauthorized")))
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))
                                .addFilterBefore(oAuth2RedirectOriginFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .logoutSuccessHandler((req, res, auth) -> {
                                                        res.setStatus(HttpServletResponse.SC_OK);
                                                }));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOriginPatterns(List.of(
                                "http://localhost:5173",
                                "http://localhost:*",
                                "https://*.vercel.app",
                                "https://su26swp06-fe.vercel.app",
                                frontendUrl.replaceAll("/$", "")));

                config.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE",
                                "OPTIONS"));

                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}
