package lumi.insert.app.config.security;

import java.nio.file.AccessDeniedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler; 
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Main security configuration for the application.
 * Enables method-level security (e.g., @PreAuthorize) and defines 
 * the HTTP filter chain, CORS/CSRF settings, and error handling.
 * @author KelvinKhodes
 * @since 1.0.0 
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Bean
    BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * Custom handler for 403 Forbidden errors.
     * Instead of a default Spring error page, it delegates the exception 
     * to the global HandlerExceptionResolver, allowing for consistent 
     * JSON error responses across the API.
     */
    @Bean
    AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            resolver.resolveException(request, response, null, new AccessDeniedException("Access denied, require an authority"));
        };
    }

    /**
     * Configures the HTTP security filter chain.
     * Note: Current configuration permits all requests at the HTTP level, 
     * relying on Method Security (@PreAuthorize) for specific endpoint protection.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/**").permitAll()
                    .anyRequest().authenticated())
            // Link the custom 403 error handler defined above.
            .exceptionHandling(exc -> exc.accessDeniedHandler(customAccessDeniedHandler()));
        
        return http.build();
    }

    
}