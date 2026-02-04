package ru.car.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import ru.car.filter.RequestURIOverriderServletFilter;
import ru.car.service.security.SecurityUserService;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final RequestURIOverriderServletFilter requestURIOverriderServletFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityUserService userService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    private static final String[] AUTH_WHITELIST = {
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/api/public/**",
            "/api/public/authenticate",
            "/actuator/**",
            "/swagger-ui/**"
    };

    private static final String[] AUTH_URL_WHITELIST = {
            "/api/user.login_oauth_mobile",
            "/api/user.login_oauth_code",
            "/api/feedback/send",
            "/api/version_control.get",
            "/api/marketplaces.get",
            "/api/report/send",
            "/api/report/get_all_reasons",
            "/api/report/createDraft",
            "/api/report/updateDraft",
            "/api/report/sendDraft",
            "/api/report/send",
            "/api/notification/*/status/isRead",
            "/api/notification/*/status",
            "/api/qr/*",
            "/api/zvonok/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Своего рода отключение CORS (разрешение запросов со всех доменов)
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOriginPatterns(List.of("*"));
                    corsConfiguration.setAllowedMethods(List.of("*"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
                // Настройка доступа к конечным точкам
                .authorizeHttpRequests(request -> request
                    // Можно указать конкретный путь, * - 1 уровень вложенности, ** - любое количество уровней вложенности
                    .requestMatchers(AUTH_URL_WHITELIST).permitAll()
                    .requestMatchers(AUTH_WHITELIST).permitAll()
                    .requestMatchers("/endpoint", "/admin/**").hasRole("ADMIN")
//                    .requestMatchers(EndpointRequest.to(ShutdownEndpoint.class)).permitAll()
//                    .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
//                    .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
                    .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                    .anyRequest().authenticated())
                .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestURIOverriderServletFilter, JwtAuthenticationFilter.class)
                .httpBasic(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
}