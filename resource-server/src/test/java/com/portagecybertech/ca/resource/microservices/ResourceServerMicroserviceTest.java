package com.portagecybertech.ca.resource.microservices;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/**
 * 
 * Starts the Resource Server with a local test JWKS endpoint and verifies its HTTP contract for valid signatures, invalid signatures, required scopes, and missing bearer tokens.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class ResourceServerMicroserviceTest {

    private static final String URI_RESOURCE_SERVER_ENDPOINT = "/api/hello";
    private static final String API_READ = "api.read";
    private static final String API_WRITE = "api.write";
    private static final String SCOPE = "scope";

    private static final String ISSUER = "https://issuer.example.test";
    private static final String API_AUDIENCE = "https://api.example.test";
    private static final String KEY_ID = "resource-server-contract-test-key";
    private static final RSAKey SIGNING_KEY = generateSigningKey();

    private static HttpServer jwksServer;
    private static String jwksUri;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
        /**
         * 
         * Performs the 'register jwt contract properties' operation and enforces its documented contract.
         * @param registry Spring dynamic-property registry populated with local test service settings.
         */
        static void registerJwtContractProperties(DynamicPropertyRegistry registry) {
        try {
            startJwksServer();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to start local test JWKS endpoint", exception);
        }
        registry.add("app.oauth.issuer", () -> ISSUER);
        registry.add("app.oauth.api-audience", () -> API_AUDIENCE);
        registry.add("app.oauth.jwks-uri", () -> jwksUri);
    }

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

    @Test
    /**
     * 
     * Performs the 'accepts token verified through the configured remote jwks contract' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void acceptsTokenVerifiedThroughTheConfiguredRemoteJwksContract() throws Exception {
        ResponseEntity<String> response = getHello(signedToken("alice", API_READ));

        assertEquals(200, response.getStatusCode().value(), response.getBody());
        assertTrue(response.getBody().contains("Hello World alice"));
    }

    @Test
    /**
     * 
     * Performs the 'rejects tampered jwt signature' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsTamperedJwtSignature() throws Exception {
        String validToken = signedToken("alice", API_READ);
        ResponseEntity<String> response = getHello(tamperSignature(validToken));

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    /**
     * 
     * Performs the 'rejects valid jwt without required api scope' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsValidJwtWithoutRequiredApiScope() throws Exception {
        ResponseEntity<String> response = getHello(signedToken("alice",API_WRITE));

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
        /**
         * 
         * Performs the 'rejects requests without bearer token' operation and enforces its documented contract.
         */
        void rejectsRequestsWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                serviceUrl(URI_RESOURCE_SERVER_ENDPOINT), String.class);

        assertEquals(401, response.getStatusCode().value());
    }

        /**
         * 
         * Performs the 'get hello' operation and returns the corresponding result.
         * @param token Compact JWT or token value being processed by the current operation.
         * @return the result described above.
         */
        private ResponseEntity<String> getHello(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                serviceUrl(URI_RESOURCE_SERVER_ENDPOINT),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

        /**
         * 
         * Performs the 'signed token' operation and returns the corresponding result.
         * @param subject Subject identifier represented in the issued token; the demonstration grant does not authenticate this value.
         * @param scope Space-delimited OAuth scope requested for the token or encoded in the JWT.
         * @return the result described above.
         * @throws Exception when the operation cannot complete its contract
         */
        private String signedToken(String subject, String scope) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(subject)
                .audience(API_AUDIENCE)
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
         * Performs the 'tamper signature' operation and returns the corresponding result.
         * @param token Compact JWT or token value being processed by the current operation.
         * @return the result described above.
         */
        private String tamperSignature(String token) {
        String[] parts = token.split("\\.");
        String signature = parts[2];
        char replacement = signature.charAt(0) == 'A' ? 'B' : 'A';
        parts[2] = replacement + signature.substring(1);
        return String.join(".", parts);
    }

    /**
     * 
     * Performs the 'service url' operation and returns the corresponding result.
     * @param path Relative HTTP endpoint path appended to the local service base URL.
     * @return the result described above.
     */
    private String serviceUrl(String path) {
        return "http://localhost:" + port + path;
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
        jwksServer.createContext("/jwks", exchange -> {
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
        jwksUri = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks";
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
