package com.portagecybertech.ca.authorization.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.portagecybertech.ca.authorization.oauth.SubjectGrantAuthenticationProvider;
import com.portagecybertech.ca.authorization.oauth.TokenExchangeAuthenticationProvider;
import com.portagecybertech.ca.authorization.security.secrets.SecretProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

/**
 * 
 * Unit and Spring-context tests for Authorization Server beans and security policy: key publication, client grants and credentials, audience and issuer validation, token lifetime, and token generator wiring.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class SecurityConfigurationTest {

    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @LocalServerPort
    private int PORT_SPRING_BOOT_RANDOM;

    private static final String HOST_NAME = "http://localhost:";

    private static final String PORT_AUTHORIZATION_SERVER_JWT_ENDPOINT = "9090";
    private static final String PORT_RESOURCE_SERVER_ENDPOINT = "8080";

    private static final String URI_AUTHORIZATION_SERVER_JWT_ENDPOINT = "/oauth2/token";
    private static final String URI_RESOURCE_SERVER_API = "/api";

    private static final String URL_AUTHORIZATION_SERVER_JWT_ENDPOINT = HOST_NAME + PORT_AUTHORIZATION_SERVER_JWT_ENDPOINT + URI_AUTHORIZATION_SERVER_JWT_ENDPOINT;
    private static final String URL_RESOURCE_SERVER_API = HOST_NAME + PORT_RESOURCE_SERVER_ENDPOINT + URI_RESOURCE_SERVER_API;

    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_SECRET = "test-only-client-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String USER_NAME = SecurityConfigurationTest.class.getSimpleName();
    private static final String API_READ = "api.read";
    private static final String RESOURCE_SERVER_AUDIENCE = "http://localhost:8080/api";
    private static final SecretProvider TEST_SECRET_PROVIDER = name -> switch (name) {
        case "client-id" -> CLIENT_ID;
        case "client-secret" -> CLIENT_SECRET;
        default -> throw new IllegalArgumentException("Unexpected secret name: " + name);
    };

    private static final String SUBJECT_TOKEN = "subject_token";
    private static final String SUBJECT_TOKEN_TYPE = "subject_token_type";
    private static final String REQUESTED_TOKEN_TYPE = "requested_token_type";
    private static final String API_AUDIENCE = "audience";


    @Value("${app.oauth.api-audience:http://localhost:8080/api}")
    private String apiAudience;


    @Test

    /**
     * 
     * Performs the 'creates signing key and publishes it through jwk source' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void createsSigningKeyAndPublishesItThroughJwkSource() throws Exception {
        RSAKey signingKey = configuration.signingKey();
        JWKSource<SecurityContext> source = configuration.jwkSource(signingKey);
        JWKSelector selector = new JWKSelector(
                new JWKMatcher.Builder().keyID(signingKey.getKeyID()).build());

        List<com.nimbusds.jose.jwk.JWK> selected = source.get(selector, null);

        assertEquals(1, selected.size());
        assertEquals(signingKey.getKeyID(), selected.getFirst().getKeyID());
        assertNotNull(signingKey.toRSAPrivateKey());
    }

    @Test
    /**
     * 
     * Performs the 'builds client with configured credentials and grant types' operation and enforces its documented contract.
     */
    void buildsClientWithConfiguredCredentialsAndGrantTypes() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        RegisteredClientRepository clients =
                configuration.registeredClientRepository(encoder, TEST_SECRET_PROVIDER, true);
        RegisteredClient client = clients.findByClientId(CLIENT_ID);

        assertNotNull(client);
        assertEquals(Set.of(SubjectGrantAuthenticationProvider.GRANT_TYPE,
                TokenExchangeAuthenticationProvider.GRANT_TYPE), client.getAuthorizationGrantTypes());
        assertEquals(Set.of(API_READ), client.getScopes());
        assertEquals(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                client                .getClientAuthenticationMethods());
        assertTrue(encoder.matches(CLIENT_SECRET, client.getClientSecret()));
        assertNotNull(client.getClientSecret());
        assertFalse(client.getClientSecret().contains(CLIENT_SECRET));
        assertEquals(Duration.ofMinutes(5), client.getTokenSettings().getAccessTokenTimeToLive());
    }

    @Test
    /**
     * 
     * Performs the 'excludes unverified subject grant when disabled' operation required by this class.
     */
    void excludesUnverifiedSubjectGrantWhenDisabled() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        RegisteredClientRepository clients =
                configuration.registeredClientRepository(encoder, TEST_SECRET_PROVIDER, false);

        assertEquals(Set.of(TokenExchangeAuthenticationProvider.GRANT_TYPE),
                clients.findByClientId(CLIENT_ID).getAuthorizationGrantTypes());
    }

    @Test
    /**
     * 
     * Performs the 'requires explicit client secret configuration' operation and enforces its documented contract.
     */
    void requiresExplicitClientSecretConfiguration() {
        assertThrows(IllegalStateException.class,
                () -> configuration.registeredClientRepository(
                        configuration.passwordEncoder(), name -> " ", false));
    }

    @Test

    /**
     * 
     * Performs the 'configures issuer and in memory authorization service' operation and enforces its documented contract.
     */
    void configuresIssuerAndInMemoryAuthorizationService() {
        AuthorizationServerSettings settings =
                configuration.authorizationServerSettings(URL_AUTHORIZATION_SERVER_JWT_ENDPOINT);

        assertEquals(URL_AUTHORIZATION_SERVER_JWT_ENDPOINT, settings.getIssuer());
        assertNotNull(configuration.authorizationService());
        assertInstanceOf(InMemoryOAuth2AuthorizationService.class, configuration.authorizationService());
    }

    @Test

    /**
     * 
     * Performs the 'adds default and explicit audience to jwt claims' operation and enforces its documented contract.
     */
    void addsDefaultAndExplicitAudienceToJwtClaims() {
        String URL_OTHER_RESOURCE_SERVER_API = HOST_NAME + PORT_SPRING_BOOT_RANDOM + URI_RESOURCE_SERVER_API;
        JwtEncodingContext defaultContext = JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder())
                .build();
        configuration.jwtCustomizer(URL_OTHER_RESOURCE_SERVER_API).customize(defaultContext);
        assertEquals(List.of(URL_OTHER_RESOURCE_SERVER_API),
                defaultContext.getClaims().build().getAudience());

        JwtEncodingContext explicitContext = JwtEncodingContext.with(
                        JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder())
                .put(API_AUDIENCE, URL_RESOURCE_SERVER_API)
                .build();
        configuration.jwtCustomizer(URL_OTHER_RESOURCE_SERVER_API).customize(explicitContext);
        assertEquals(List.of(URL_RESOURCE_SERVER_API),
                explicitContext.getClaims().build().getAudience());
    }

    @Test
    /**
     * 
     * Performs the 'decoder requires configured issuer and api audience' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void decoderRequiresConfiguredIssuerAndApiAudience() throws Exception {
        String URL_OTHER_AUTHORIZATION_SERVER_JWT_ENDPOINT = HOST_NAME + PORT_SPRING_BOOT_RANDOM + URI_AUTHORIZATION_SERVER_JWT_ENDPOINT;
        String URL_OTHER_RESOURCE_SERVER_API = HOST_NAME + PORT_SPRING_BOOT_RANDOM + URI_RESOURCE_SERVER_API;

        RSAKey signingKey = configuration.signingKey();
        String issuer = URL_AUTHORIZATION_SERVER_JWT_ENDPOINT;
        String sub = USER_NAME;
        String audience = RESOURCE_SERVER_AUDIENCE;
        JwtDecoder decoder = configuration.jwtDecoder(
                signingKey, issuer, audience, "", "", "", "rotation-simulated");


       assertEquals(List.of(RESOURCE_SERVER_AUDIENCE),
                decoder.decode(signedToken(signingKey, issuer, sub, audience)).getAudience());
        assertThrows(BadJwtException.class,
                () -> decoder.decode(signedToken(signingKey, issuer, sub, URL_OTHER_RESOURCE_SERVER_API)));
        assertThrows(BadJwtException.class,
                () -> decoder.decode(signedToken(signingKey, URL_OTHER_AUTHORIZATION_SERVER_JWT_ENDPOINT, sub, audience)));
    }

    @Test
    /**
     * 
     * Performs the 'creates jwt encoder and token generator' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void createsJwtEncoderAndTokenGenerator() throws Exception {
        RSAKey signingKey = configuration.signingKey();
        JWKSource<SecurityContext> source = configuration.jwkSource(signingKey);
        JwtEncoder encoder = configuration.jwtEncoder(source);

        assertNotNull(encoder);
        assertNotNull(configuration.tokenGenerator(encoder,
                configuration.jwtCustomizer(apiAudience)));
    }

    /**
     * 
     * Performs the 'signed token' operation and returns the corresponding result.
     * @param signingKey RSA signing key or key pair used to produce the JWT under test.
     * @param issuer Expected JWT issuer identifier.
     * @param sub JWT subject claim identifying the principal represented by the token.
     * @param audience Requested resource audience; it must be present in the configured allow-list.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private String signedToken(RSAKey signingKey, String issuer, String sub, String audience) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(sub)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(120)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) signingKey.toRSAPrivateKey()));
        return jwt.serialize();
    }
}
