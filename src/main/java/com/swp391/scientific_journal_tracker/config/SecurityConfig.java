// package com.swp391.scientific_journal_tracker.config;

// import com.swp391.scientific_journal_tracker.security.*;

// import jakarta.servlet.http.HttpServletResponse;
// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.*;
// import org.springframework.core.annotation.Order;
// import org.springframework.security.authentication.*;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.crypto.bcrypt.*;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration
// @EnableWebSecurity
// @RequiredArgsConstructor

// public class SecurityConfig {

//     private final JwtAuthenticationFilter jwtAuthFilter;
//     private final OAuth2SuccessHandler oAuth2SuccessHandler;

//     // Chain 1: dành riêng cho /api/auth/**, public hoàn toàn
//     @Bean
//     @Order(1)
//     public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
//         http
//                 .securityMatcher("/api/auth/**")
//                 .csrf(AbstractHttpConfigurer::disable)
//                 .authorizeHttpRequests(auth -> auth
//                         .anyRequest().permitAll())
//                 .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

//         return http.build();
//     }

//     // Chain 2: dành cho các API còn lại + Google OAuth2
//     @Bean
//     @Order(2)
//     public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
//         http
//                 .csrf(AbstractHttpConfigurer::disable)
//                 .authorizeHttpRequests(auth -> auth
//                         .requestMatchers(
//                                 "/login/oauth2/**",
//                                 "/oauth2/**")
//                         .permitAll()
//                         .anyRequest().authenticated())
//                 .exceptionHandling(ex -> ex
//                         .authenticationEntryPoint((request, response, authException) -> response
//                                 .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
//                 .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                 .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))
//                 .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

//         return http.build();
//     }

//     @Bean
//     public PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder();
//     }

//     @Bean
//     public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//         return config.getAuthenticationManager();
//     }
// }
package com.swp391.scientific_journal_tracker.config;

import com.swp391.scientific_journal_tracker.security.JwtAuthenticationFilter;
import com.swp391.scientific_journal_tracker.security.OAuth2SuccessHandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

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
                                                .anyRequest().permitAll())
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

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
                                                .requestMatchers(
                                                                "/login/oauth2/**",
                                                                "/oauth2/**",
                                                                "/error")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> response
                                                                .sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Unauthorized")))
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

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