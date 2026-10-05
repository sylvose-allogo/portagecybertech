package com.portagecybertech.ca.authorization.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.portagecybertech.ca.authorization.oauth.SubjectGrantAuthenticationConverter;
import com.portagecybertech.ca.authorization.oauth.SubjectGrantAuthenticationProvider;
import com.portagecybertech.ca.authorization.oauth.TokenExchangeAuthenticationConverter;
import com.portagecybertech.ca.authorization.oauth.TokenExchangeAuthenticationProvider;
import com.portagecybertech.ca.authorization.security.secrets.SecretProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Configuration

/**
 * 
 * Defines the Authorization Server security boundary and its Spring beans: ephemeral RSA signing material, public JWKS publication, JWT encoding and validation, client registration, grant-specific token generation, and ordered HTTP filter chains. Runtime secrets and production-grade persistent key management must be supplied separately.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SecurityConfiguration {

    private static final String URI_ACTUATOR_ENDPOINT = "/actuator/health";
    private static final String API_READ = "api.read";

    @Bean

    /**
     * 
     * Generates a fresh 2048-bit RSA key pair and wraps it in a signing JWK with a unique key identifier. The key is held in memory and is not a persistent production key-management strategy.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    RSAKey signingKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("authorization-" + UUID.randomUUID())
                .build();
    }

    @Bean
    /**
     * 
     * Publishes the configured signing JWK through a selector-backed Nimbus JWK source so the token generator can sign JWTs and the authorization endpoint can expose the corresponding public key.
     * @param signingKey RSA signing key or key pair used to produce the JWT under test.
     * @return the result described above.
     */
    JWKSource<SecurityContext> jwkSource(RSAKey signingKey) {
        JWKSet jwkSet = new JWKSet(signingKey);
        return (selector, context) -> selector.select(jwkSet);
    }

    @Bean
    /**
     * 
     * Creates the Nimbus JWT encoder backed by the configured JWK source.
     * @param jwkSource JWK source containing signing material and exposing the corresponding public key.
     * @return the result described above.
     */
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(
            @Value("${app.oauth.api-audience:http://localhost:8080/api}") String apiAudience) {
        return context -> {
            Object audience = context.get("audience");
            context.getClaims().claim(JwtClaimNames.AUD, List.of(
                    audience == null ? apiAudience : audience.toString()));
        };
    }

    @Bean
    /**
     * 
     * Creates the JWT token generator and registers the project audience customizer.
     * @param jwtEncoder Nimbus encoder that signs JWTs using the configured JWK source.
     * @param jwtCustomizer JWT customizer that adds project-specific claims to generated access tokens.
     * @return the result described above.
     */
    OAuth2TokenGenerator<Jwt> tokenGenerator(JwtEncoder jwtEncoder,
            OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        JwtGenerator generator = new JwtGenerator(jwtEncoder);
        generator.setJwtCustomizer(jwtCustomizer);
        return generator;
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey signingKey,
            @Value("${app.oauth.subject-token-issuer:${app.oauth.issuer:http://localhost:9090}}")
            String subjectTokenIssuer,
            @Value("${app.oauth.api-audience:http://localhost:8080/api}") String apiAudience,
            @Value("${app.oauth.jwks-uri:}") String jwksUri,
            @Value("${app.oauth.authorization-public-key:}") String authorizationPublicKey,
            @Value("${app.oauth.additional-public-key:}") String additionalPublicKey,
            @Value("${app.oauth.additional-key-id:rotation-simulated}") String additionalKeyId) throws Exception {
        if (!jwksUri.isBlank()) {
            NimbusJwtDecoder remote = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
            configureValidators(remote, subjectTokenIssuer, apiAudience);
            return remote;
        }

        RSAPublicKey primaryPublicKey = authorizationPublicKey.isBlank()
                ? signingKey.toRSAPublicKey()
                : (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                        new X509EncodedKeySpec(Base64.getDecoder().decode(authorizationPublicKey)));
        NimbusJwtDecoder primary = decoder(primaryPublicKey, subjectTokenIssuer, apiAudience);
        if (additionalPublicKey.isBlank()) {
            return primary;
        }
        byte[] encodedKey = Base64.getDecoder().decode(additionalPublicKey);
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(encodedKey));
        NimbusJwtDecoder additional = decoder(publicKey, subjectTokenIssuer, apiAudience);
        return token -> {
            try {
                String keyId = com.nimbusds.jwt.SignedJWT.parse(token).getHeader().getKeyID();
                return additionalKeyId.equals(keyId) ? additional.decode(token) : primary.decode(token);
            } catch (java.text.ParseException exception) {
                return primary.decode(token);
            }
        };
    }


    /**
     * 
     * Creates an RSA public-key JWT decoder and attaches the shared issuer and audience validators.
     * @param publicKey RSA public key expected to verify the token signature.
     * @param issuer Expected JWT issuer identifier.
     * @param apiAudience Expected or default API audience URI embedded in, or validated against, the JWT.
     * @return the result described above.
     */
    private NimbusJwtDecoder decoder(RSAPublicKey publicKey, String issuer, String apiAudience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        configureValidators(decoder, issuer, apiAudience);
        return decoder;
    }


    /**
     * 
     * Combines Spring Security default issuer/time validators with a required API-audience validator on the supplied Nimbus decoder.
     * @param decoder JWT decoder configured for the trust policy being exercised.
     * @param issuer Expected JWT issuer identifier.
     * @param apiAudience Expected or default API audience URI embedded in, or validated against, the JWT.
     */
    private void configureValidators(NimbusJwtDecoder decoder, String issuer, String apiAudience) {
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(apiAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
    }

    @Bean

    /**
     * 
     * Creates Spring Security's delegating password encoder for registered OAuth client secrets.
     * @return the result described above.
     */
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(PasswordEncoder encoder,
            SecretProvider secretProvider,
            @Value("${app.oauth.subject-grant.enabled:false}") boolean subjectGrantEnabled) {
        String clientId = secretProvider.getRequiredSecret("client-id");
        String clientSecret = secretProvider.getRequiredSecret("client-secret");
        if (clientSecret.isBlank()) {
            throw new IllegalStateException("La propriété app.oauth.client-secret doit être configurée");
        }
        RegisteredClient.Builder agentBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(encoder.encode(clientSecret))
                .clientAuthenticationMethod(
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)

     .authorizationGrantType(TokenExchangeAuthenticationProvider.GRANT_TYPE)
                .scope(API_READ)
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(5)).build());

        if (subjectGrantEnabled) {
            agentBuilder.authorizationGrantType(SubjectGrantAuthenticationProvider.GRANT_TYPE);
        }
        RegisteredClient agent = agentBuilder.build();
        return new InMemoryRegisteredClientRepository(agent);
    }

    @Bean
    /**
     * 
     * Creates the in-memory authorization service used to retain issued access-token metadata during the process lifetime.
     * @return the result described above.
     */
    OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${app.oauth.issuer:http://localhost:9090}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    @Order(1)
    /**
     * 
     * Configures OAuth authorization endpoints, OIDC defaults, custom grant converters/providers, CSRF exclusions for protocol endpoints, authenticated-client enforcement, and the browser login entry point.
     * @param http HTTP client or server handle used by this operation.
     * @param subjectProvider Provider that resolves the subject principal represented by the issued token.
     * @param exchangeProvider Authentication provider that validates and processes token-exchange requests.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    SecurityFilterChain authorizationServerChain(HttpSecurity http,
            SubjectGrantAuthenticationProvider subjectProvider,
            TokenExchangeAuthenticationProvider exchangeProvider) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpoints = authorizationServer.getEndpointsMatcher();
        http.securityMatcher(endpoints)
                .with(authorizationServer, server -> server
                        .oidc(Customizer.withDefaults())
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenRequestConverter(new SubjectGrantAuthenticationConverter())
                                .accessTokenRequestConverter(new TokenExchangeAuthenticationConverter())
                                .authenticationProvider(subjectProvider)
                                .authenticationProvider(exchangeProvider)
                        ))
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpoints))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    @Bean
    @Order(2)
    /**
     * 
     * Defines the fallback Authorization Server web policy, including public health/API documentation and the form-login defaults.
     * @param http HTTP client or server handle used by this operation.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    SecurityFilterChain applicationChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(URI_ACTUATOR_ENDPOINT, "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
