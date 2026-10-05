package com.portagecybertech.ca.resource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
/**
 * 
 * Configures JWT trust for the Resource Server and the ordered HTTP security chains. API requests require a JWT whose issuer and audience match configuration and whose authorities include SCOPE_api.read; health and API-documentation routes remain separately accessible.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class ResourceSecurityConfiguration {

    private static final String URI_ACTUATOR_ENDPOINT = "/actuator/health";

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.oauth.jwks-uri:http://localhost:9090/oauth2/jwks}") String jwksUri,
            @Value("${app.oauth.issuer:http://localhost:9090}") String issuer,
            @Value("${app.oauth.api-audience:http://localhost:8080/api}") String apiAudience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(apiAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
        return decoder;
    }

    @Bean
    @Order(1)
        /**
         * 
         * Protects every /api/** request with JWT bearer authentication and requires the SCOPE_api.read authority.
         * @param http HTTP client or server handle used by this operation.
         * @return the result described above.
         * @throws Exception when the operation cannot complete its contract
         */
        SecurityFilterChain resourceServerChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().hasAuthority("SCOPE_api.read"))
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Order(2)
        /**
         * 
         * Configures the fallback web chain, allowing health/API documentation routes and keeping non-API application routes outside the protected resource matcher.
         * @param http HTTP client or server handle used by this operation.
         * @return the result described above.
         * @throws Exception when the operation cannot complete its contract
         */
        SecurityFilterChain applicationChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(URI_ACTUATOR_ENDPOINT, "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }
}