package com.portagecybertech.ca.integration.microservices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portagecybertech.ca.authorization.AuthorizationServerApplication;
import com.portagecybertech.ca.resource.ResourceServerApplication;
import com.portagecybertech.ca.resource.validation.JwtTokenValidation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(
        classes = ResourceServerApplication.class,
        properties = {
                "app.oauth.issuer=http://localhost:9090",
                "app.oauth.api-audience=http://localhost:8080/api",
                "app.oauth.jwks-uri=http://localhost:9090/oauth2/jwks"
        })
@Import(MicroservicesTest.HttpClientTestConfiguration.class)
/**
 * 
 * Starts the Authorization Server and Resource Server as separate local Spring Boot processes and exercises their HTTP/JWT/JWKS contracts end to end, including valid access, tampered signatures, and public-key discovery.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class MicroservicesTest {

    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_SECRET = "test-only-client-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String USER_NAME = MicroservicesTest.class.getSimpleName();
    private static final String API_READ = "api.read";

    private static final String ACCESS_TOKEN_ENDPOINT = "http://localhost:9090/oauth2/token";
    private static final String JWKS_ENDPOINT = "http://localhost:9090/oauth2/jwks";
    private static final String HELLO_ENDPOINT = "http://localhost:8080/api/hello";

    private static ConfigurableApplicationContext authorizationServer;
    private static ConfigurableApplicationContext resourceServer;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenValidation validation;

    @TestConfiguration(proxyBeanMethods = false)
    /**
     * 
     * Provides the Java HTTP client used by the integration tests to call the separately running local microservices.
     *
     * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
     * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
     * @version 1.0.0
     * @since 1.0.0
     */
    static class HttpClientTestConfiguration {

        @Bean
                /**
                 * 
                 * Performs the 'http client' operation required by this class.
                 * @return the result described above.
                 */
                HttpClient httpClient() {
            return HttpClient.newHttpClient();
        }
    }

    @BeforeAll
        /**
         * 
         * Performs the 'start microservices' operation and enforces its documented contract.
         */
        static void startMicroservices() {
        authorizationServer = new SpringApplicationBuilder(
                AuthorizationServerApplication.class)
                .run(
                        "--server.port=9090",
                        "--app.oauth.issuer=http://localhost:9090",
                        "--app.oauth.api-audience=http://localhost:8080/api",
                        "--app.oauth.client-secret=" + CLIENT_SECRET,
                        "--app.oauth.subject-grant.enabled=true");

        resourceServer = new SpringApplicationBuilder(
                ResourceServerApplication.class)
                .run(
                        "--server.port=8080",
                        "--app.oauth.issuer=http://localhost:9090",
                        "--app.oauth.api-audience=http://localhost:8080/api",
                        "--app.oauth.jwks-uri=http://localhost:9090/oauth2/jwks");
    }

    @AfterAll
    /**
     * 
     * Performs the 'stop microservices' operation and enforces its documented contract.
     */
    static void stopMicroservices()     {
        if (resourceServer != null) {
            resourceServer.close();
        }
        if (authorizationServer != null) {
            authorizationServer.close();
        }
    }

    @Test
    /**
     * 
     * Performs the 'retrieves hello world from resource server with valid jwt' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void retrievesHelloWorldFromResourceServerWithValidJwt() throws Exception {
        String accessToken = requestAccessToken();
        assertTrue(validation.isValid(accessToken),
                "Le JWT doit être valide selon le JWKS distant");

        HttpRequest helloRequest = HttpRequest.newBuilder(URI.create(HELLO_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> helloResponse = httpClient.send(
                helloRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, helloResponse.statusCode(), helloResponse.body());
        assertEquals("Hello World " + USER_NAME, objectMapper.readTree(helloResponse.body()).path("message").asText());
    }


    @Test
    /**
     * 
     * Performs the 'retrieves hello world from resource server using jwks' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void retrievesHelloWorldFromResourceServerUsingJwks() throws Exception {
        String accessToken = requestAccessToken();
        assertTrue(validation.isValid(accessToken, JWKS_ENDPOINT),
                "Le JWT doit être valide selon le JWKS distant");

        HttpRequest helloRequest = HttpRequest.newBuilder(URI.create(HELLO_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> helloResponse = httpClient.send(
                helloRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, helloResponse.statusCode(), helloResponse.body());
        assertEquals("Hello World " + USER_NAME, objectMapper.readTree(helloResponse.body()).path("message").asText());
    }

    @Test
    /**
     * 
     * Performs the 'rejects access token with invalid signature at hello world endpoint' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsAccessTokenWithInvalidSignatureAtHelloWorldEndpoint() throws Exception {
        String invalidAccessToken = tamperSignature(requestAccessToken());

        assertFalse(validation.isValid(invalidAccessToken),
                "Le JWT dont la signature est altérée doit échouer à la validation");

        HttpRequest helloRequest = HttpRequest.newBuilder(URI.create(HELLO_ENDPOINT))
                .header("Authorization", "Bearer " + invalidAccessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> helloResponse = httpClient.send(
                helloRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, helloResponse.statusCode(), helloResponse.body());
    }

    @Test
        /**
         * 
         * Performs the 'rejects access token with invalid signature at hello world using jwks' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsAccessTokenWithInvalidSignatureAtHelloWorldUsingJwks() throws Exception {
        String invalidAccessToken = tamperSignature(requestAccessToken());

        assertFalse(validation.isValid(invalidAccessToken, JWKS_ENDPOINT),
                "Le JWT dont la signature est altérée doit échouer avec le JWKS distant");

        HttpRequest helloRequest = HttpRequest.newBuilder(URI.create(HELLO_ENDPOINT))
                .header("Authorization", "Bearer " + invalidAccessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> helloResponse = httpClient.send(
                helloRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, helloResponse.statusCode(), helloResponse.body());
    }

    @Test
        /**
         * 
         * Performs the 'retrieves access token from authorization server' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void retrievesAccessTokenFromAuthorizationServer() throws Exception {
        JsonNode tokenResponse = requestTokenResponse();
        String accessToken = tokenResponse.path("access_token").asText();
        assertTrue(validation.isValid(accessToken),
                "Le JWT doit être valide selon le JWKS distant");
        assertEquals("Bearer", tokenResponse.path("token_type").asText());
        assertEquals(API_READ, tokenResponse.path(SCOPE).asText());
        assertEquals(3, accessToken.split("\\.").length, "L'access_token doit être un JWT compact");
    }

    @Test
    /**
     * 
     * Performs the 'rejects tampered access token from authorization server' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsTamperedAccessTokenFromAuthorizationServer() throws Exception {
        String accessToken = requestAccessToken();
        assertTrue(validation.isValid(accessToken),
                "Le jeton d'origine doit être accepté par isValid(String)");

        String tamperedAccessToken = tamperSignature(accessToken);

        assertFalse(validation.isValid(tamperedAccessToken),
                "Le jeton altéré récupéré depuis /oauth2/token doit échouer à la validation");
    }

    @Test
        /**
         * 
         * Performs the 'retrieves public signing keys from authorization server jwks' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void retrievesPublicSigningKeysFromAuthorizationServerJwks() throws Exception {
        String accessToken = requestAccessToken();

        HttpRequest request = HttpRequest.newBuilder(URI.create(JWKS_ENDPOINT)).header("Accept", "application/json").GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        assertTrue(validation.isValid(accessToken, JWKS_ENDPOINT),
                "Le JWKS doit permettre de valider le JWT récupéré");
        JsonNode jwks = objectMapper.readTree(response.body());
        JsonNode keys = jwks.path("keys");
        assertTrue(keys.isArray(), "JWKS doit contenir un tableau keys");
        assertFalse(keys.isEmpty(), "JWKS doit contenir au moins une clé");
        JsonNode signingKey = keys.get(0);
        assertEquals("RSA", signingKey.path("kty").asText());
        assertFalse(signingKey.path("kid").asText().isBlank(), "La clé doit avoir un kid");
        assertFalse(signingKey.path("n").asText().isBlank(), "La clé doit exposer son module public")    ;
        assertFalse(signingKey.path("e").asText().isBlank(), "La clé doit exposer son exposant public    ");
        assertFalse(signingKey.has("d"), "JWKS ne doit jamais exposer le paramètre privé d");
    }

    @Test
    /**
     * 
     * Performs the 'rejects tampered access token using authorization server jwks' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsTamperedAccessTokenUsingAuthorizationServerJwks() throws Exception {
        String invalidAccessToken = tamperSignature(requestAccessToken());

        HttpRequest jwksRequest = HttpRequest.newBuilder(URI.create(JWKS_ENDPOINT))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> jwksResponse = httpClient.send(
                jwksRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, jwksResponse.statusCode(), jwksResponse.body());
        assertFalse(validation.isValid(invalidAccessToken, JWKS_ENDPOINT),
                "    La signature altérée doit être rejetée lors de la validation avec le JWKS de /oauth2/jwks");
    }

    /**
     * 
     * Performs the 'request token response' operation and returns the corresponding result.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private JsonNode requestTokenResponse() throws Exception {
        String form = GRANT_TYPE + "=" + encode(SUBJECT_GRANT)
                + "&" + SUBJECT + "=" + encode(USER_NAME)
                + "&" + SCOPE + "=" + encode(API_READ);
        String credentials = Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        HttpRequest request =     HttpRequest.newBuilder(URI.create(ACCESS_TOKEN_ENDPOINT))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        return objectMapper.readTree(response.body());
    }

    /**
     * 
     * Performs the 'request access token' operation and returns the corresponding result.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private String requestAccessToken() throws Exception {
        String accessToken = requestTokenResponse().path("access_token").asText();
        assertFalse(accessToken.isBlank(), "La réponse doit contenir un access_token");
        return accessToken;
    }

    /**
     * 
     * Performs the 'tamper signature' operation and returns the corresponding result.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    private String tamperSignature(String accessToken) {
        String[] tokenParts = accessToken.split("\\.");
        assertEquals(3, tokenParts.length, "L'access_token doit être un JWT compact");
        char changedFirstCharacter = tokenParts[2].charAt(0) == 'A' ? 'B' : 'A';
        return tokenParts[0] + "." + tokenParts[1] + "."
                + changedFirstCharacter + tokenParts[2].substring(1);
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