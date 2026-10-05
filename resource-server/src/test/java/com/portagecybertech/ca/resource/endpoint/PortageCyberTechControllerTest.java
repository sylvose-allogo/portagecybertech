package com.portagecybertech.ca.resource.endpoint;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
/**
 * 
 * HTTP-level Resource Server controller tests covering valid JWT access, JWKS-backed signatures, required scopes, anonymous or malformed bearer rejection, and the health endpoint.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class PortageCyberTechControllerTest {

    private static final String HOST_NAME = "http://localhost:";

    private static final String URI_RESOURCE_SERVER_ENDPOINT = "/api/hello";
    private static final String URI_ACTUATOR_ENDPOINT = "/actuator/health";

    private static final String SCOPE = "scope";
    private static final String USER_NAME = PortageCyberTechControllerTest.class.getSimpleName();
    private static final String API_READ = "api.read";
    private static final String API_WRITE = "api.write";

    private static final String ISSUER = "https://issuer.example.test";
    private static final String RESOURCE_SERVER_AUDIENCE = "https://api.example.test";
    private static final String KEY_ID = "controller-test-signing-key";
    private static final RSAKey SIGNING_KEY = generateSigningKey();
    private static final String HELLO_WORLD_USER = "{\"message\":\"Hello World " + USER_NAME + "\"}";

    private static HttpServer jwksServer;
    private static String jwksUri;


    @Autowired
    private MockMvc mvc;

    @AfterAll
        /**
         * 
         * Performs the 'stop jwks server' operation and enforces its documented contract.
         */
        static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @DynamicPropertySource
        /**
         * 
         * Performs the 'register jwt properties' operation and enforces its documented contract.
         * @param registry Spring dynamic-property registry populated with local test service settings.
         */
        static void registerJwtProperties(DynamicPropertyRegistry registry) {
        try {
            startJwksServer();
        } catch (IOException exception) {
                throw new UncheckedIOException("Unable to start test JWKS endpoint", exception);
        }
        registry.add("app.oauth.issuer", () -> ISSUER);
        registry.add("app.oauth.api-audience", () -> RESOURCE_SERVER_AUDIENCE);
        registry.add("app.oauth.jwks-uri", () -> jwksUri);
    }

    @Test
    /**
     * 
     * Performs the 'hello world with jwt token bearer authentication' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void helloWorldWithJwtTokenBearerAuthentication() throws Exception {
        String accessToken = signedToken(USER_NAME, API_READ);

        mvc.perform(get(URI_RESOURCE_SERVER_ENDPOINT)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(HELLO_WORLD_USER));
    }

    @Test
    /**
     * 
     * Performs the 'hello world with jwks token bearer authentication' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void helloWorldWithJwksTokenBearerAuthentication() throws Exception {
        String accessToken = signedToken(USER_NAME, API_READ);
        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        HttpResponse<String> jwksResponse = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(jwksUri)).GET().build(    ),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, jwksResponse.statusCode());
        JWKSet jwkSet = JWKSet.parse(jwksResponse.body());
        RSAKey signingKey = (RSAKey) jwkSet.getKeyByKeyId(signedJwt.getHeader().getKeyID());
        assertNotNull(signingKey, "The configured JWKS must publish the JWT signing key");

        assertTrue(signedJwt.verify(new RSASSAVerifier(signingKey.toRSAPublicKey())));

        mvc.perform(get(URI_RESOURCE_SERVER_ENDPOINT)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().json(HELLO_WORLD_USER));
    }

    @Test
    /**
     * 
     * Performs the 'accepts bearer jwt with required scope' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void acceptsBearerJwtWithRequiredScope() throws Exception {
        mvc.perform(get(URI_RESOURCE_SERVER_ENDPOINT).with(jwt().jwt(token -> token
                        .subject(USER_NAME)
                        .audience(java.util.List.of(RESOURCE_SERVER_AUDIENCE))
                        .claim(SCOPE, API_READ))))
                .andExpect(status().isOk())
                .andExpect(content().json(HELLO_WORLD_USER));
    }

    @Test
        /**
         * 
         * Performs the 'requires bearer authentication' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void requiresBearerAuthentication() throws Exception {
        mvc.perform(get(URI_RESOURCE_SERVER_ENDPOINT)).andExpect(status().isUnauthorized());
    }

    @Test
        /**
         * 
         * Performs the 'rejects malformed bearer token' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsMalformedBearerToken() throws Exception {
        mvc.perform(get(URI_RESOURCE_SERVER_ENDPOINT).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
        /**
         * 
         * Performs the 'rejects valid token without required scope' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsValidTokenWithoutRequiredScope() throws Exception     {
        mvc.perform(get(URI_RESOURCE_SERVER_ENDPOINT).with(jwt().jwt(token -> token
                        .subject(USER_NAME)
                        .claim(SCOPE, API_WRITE))))
                .andExpect(status().isForbidden());
    }

    @Test
        /**
         * 
         * Performs the 'exposes healthy endpoint' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void exposesHealthyEndpoint() throws Exception {
        mvc.perform(get(URI_ACTUATOR_ENDPOINT)).andExpect(status().isOk());
    }

    /**
     * 
     * Performs the 'signed token' operation and returns the corresponding result.
     * @param subject Subject identifier represented in the issued token; the demonstration grant does not authenticate this value.
     * @param scope Space-delimited OAuth scope requested for the token or encoded in the JWT.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private static String signedToken(String subject, String scope) throws Exception {
        Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(subject)
                .audience(RESOURCE_SERVER_AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(120)))
                .claim(SCOPE, scope)
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
                claims);
        jwt.sign(new RSASSASigner(SIGNING_KEY));
        return jwt.serialize();
    }

    /**
     * 
     * Performs the 'start jwks server' operation and enforces its documented contract.
     * @throws IOException when the operation cannot complete its contract
     */
    private static synchronized void startJwksServer() throws IOException {
        if (jwksServer != null) {
            return;
        }
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/oauth2/jwks", exchange -> {
            byte[] response = new JWKSet(SIGNING_KEY.toPublicJWK())
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        jwksServer.start();
        jwksUri = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/oauth2/jwks";
    }

    /**
     * 
     * Performs the 'generate signing key' operation required by this class.
     * @return the result described above.
     */
    private static RSAKey generateSigningKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        } catch (JOSEException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
