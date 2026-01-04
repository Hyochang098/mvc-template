package com.example.template.global.config;

import com.example.template.global.security.handler.CustomAccessDeniedHandler;
import com.example.template.global.security.handler.JwtAuthenticationEntryPoint;
import com.example.template.global.security.service.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomAccessDeniedHandler accessDeniedHandler;
  private final JwtAuthenticationEntryPoint authenticationEntryPoint;

  @Value("${security.cors.allowed-origins:*}")
  private List<String> allowedOrigins;

  @Value("${security.cors.allowed-origin-patterns:*}")
  private List<String> allowedOriginPatterns;

  @Value("${security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
  private List<String> allowedMethods;

  @Value("${security.cors.allowed-headers:*}")
  private List<String> allowedHeaders;

  @Value("${security.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // credentials 허용 시 명시적 origin 또는 패턴 사용
    boolean hasWildcardOrigin = allowedOrigins.stream().anyMatch("*"::equals);
    if (allowCredentials && hasWildcardOrigin) {
      configuration.setAllowedOriginPatterns(Optional.ofNullable(allowedOriginPatterns)
          .filter(list -> !list.isEmpty())
          .orElse(List.of("*")));
    } else {
      configuration.setAllowedOrigins(allowedOrigins);
      configuration.setAllowedOriginPatterns(allowedOriginPatterns);
    }

    configuration.setAllowedMethods(Optional.ofNullable(allowedMethods).orElse(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")));
    configuration.setAllowedHeaders(Optional.ofNullable(allowedHeaders).orElse(List.of("*")));
    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }


  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/h2-console/**",
                "/api/auth/**"
            ).permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated() // hasRole("GENERAL")
        )
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(authenticationEntryPoint)  // 401 Unauthorized 처리
            .accessDeniedHandler(accessDeniedHandler)            // 403 Forbidden 처리
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
