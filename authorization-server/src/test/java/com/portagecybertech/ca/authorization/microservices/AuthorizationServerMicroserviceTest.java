package com.portagecybertech.ca.authorization.microservices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/**
 * 
 * HTTP contract tests for the running Authorization Server, verifying JWKS exposure, signed JWT claims and signatures, client authentication, and scope rejection.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class AuthorizationServerMicroserviceTest {

    private static final String URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "/oauth2/jwks";
    private static final String URI_AUTHORIZATION_SERVER_JWT_ENDPOINT = "/oauth2/token";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_ID = "agent-client";
    private static final String CLIENT_SECRET = "test-only-client-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String API_READ = "api.read";
    private static final String API_WRITE = "api.write";
    private static final String RESOURCE_SERVER_AUDIENCE = "http://localhost:8080/api";


    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
        /**
         * 
         * Performs the 'publishes public jwks over http' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void publishesPublicJwksOverHttp() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                serviceUrl(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT), String.class);

        assertEquals(200, response.getStatusCode().value());
        JsonNode keys = objectMapper.readTree(response.getBody()).path("keys");
        assertTrue(keys.isArray());
        assertFalse(keys.isEmpty());
        JsonNode key = keys.get(0);
        assertEquals("RSA", key.path("kty").asText());
        assertFalse(key.path("kid").asText().isBlank());
        assertFalse(key.path("n").asText().isBlank());
        assertFalse(key.path("e").asText().isBlank());
        assertFalse(key.has("d"), "JWKS must not publish private key material");
    }

    @Test
    /**
     * 
     * Performs the 'issues jwt that can be verified by the published jwks' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void issuesJwtThatCanBeVerifiedByThePublishedJwks() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                serviceUrl(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT),
                HttpMethod.POST,
                tokenRequest("MicroserviceContractTest", API_READ, CLIENT_SECRET),
                String.class);

        assertEquals(200, response.getStatusCode().value(), response.getBody());
        JsonNode tokenResponse = objectMapper.readTree(response.getBody());
        assertEquals("Bearer", tokenResponse.path("token_type").asText());
        assertEquals(API_READ, tokenResponse.path(SCOPE).asText());

        SignedJWT jwt = SignedJWT.parse(tokenResponse.path("access_token").asText());
        assertEquals("MicroserviceContractTest", jwt.getJWTClaimsSet().getSubject());
        assertTrue(jwt.getJWTClaimsSet().getAudience().contains(RESOURCE_SERVER_AUDIENCE));
        assertEquals("RS256", jwt.getHeader().getAlgorithm().getName());

        JsonNode keys = objectMapper.readTree(restTemplate.getForObject(
                serviceUrl(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT), String.class)).path("keys");
        RSAKey publicKey = (RSAKey) JWKSet.parse(
                objectMapper.createObjectNode().set("keys", keys).toString())
                .getKeyByKeyId(jwt.getHeader().getKeyID());
        assertTrue(jwt.verify(new RSASSAVerifier(publicKey)));
    }

    @Test
    /**
     * 
     * Performs the 'rejects invalid client credentials at the token contract' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsInvalidClientCredentialsAtTheTokenContract() throws Exception {
        String credentials = java.util.Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + "wrong-secret").getBytes(StandardCharsets.UTF_8));
        String form = GRANT_TYPE + "=" + encode(SUBJECT_GRANT)
                + "&" + SUBJECT + "=" + encode("MicroserviceContractTest")
                + "&" + SCOPE + "=" + encode(API_READ);
        HttpRequest request = HttpRequest.newBuilder(URI.create(serviceUrl(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("invalid_client"));
    }

    @Test
        /**
         * 
         * Performs the 'rejects invalid scope credentials at the token contract' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsInvalidScopeCredentialsAtTheTokenContract() throws Exception {
        String credentials = java.util.Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        String form = GRANT_TYPE + "=" + encode(SUBJECT_GRANT)
                + "&" + SUBJECT + "=" + encode("MicroserviceContractTest")
                + "&" + SCOPE + "=" + encode(API_WRITE);
        HttpRequest request = HttpRequest.newBuilder(URI.create(serviceUrl(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("invalid_scope"));
    }

    @Test
        /**
         * 
         * Performs the 'retrieves access token valid client credentials at the token contract' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void retrievesAccessTokenValidClientCredentialsAtTheTokenContract() throws Exception {
        String credentials = java.util.Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        String form = GRANT_TYPE + "=" + encode(SUBJECT_GRANT)
                + "&" + SUBJECT + "=" + encode("MicroserviceContractTest")
                + "&" + SCOPE + "=" + encode(API_READ);
        HttpRequest request = HttpRequest.newBuilder(URI.create(serviceUrl(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("access_token"));
    }

        /**
         * 
         * Performs the 'token request' operation and returns the corresponding result.
         * @param subject Subject identifier represented in the issued token; the demonstration grant does not authenticate this value.
         * @param scope Space-delimited OAuth scope requested for the token or encoded in the JWT.
         * @param secret OAuth client secret supplied for credential verification.
         * @return the result described above.
         */
        private HttpEntity<MultiValueMap<String, String>> tokenRequest(
            String subject, String scope, String secret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CLIENT_ID, secret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(GRANT_TYPE, SUBJECT_GRANT);
        form.add(SUBJECT, subject);
        form.add(SCOPE, scope);
        return new HttpEntity<>(form, headers);
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
     * Performs the 'encode' operation and returns the corresponding result.
     * @param value Value being encoded, decoded, or asserted by the operation.
     * @return the result described above.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}