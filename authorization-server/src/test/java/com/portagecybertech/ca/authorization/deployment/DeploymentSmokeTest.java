package com.portagecybertech.ca.authorization.deployment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)

/**
 * 
 * Smoke tests for the deployed Authorization Server HTTP surface, checking health, availability of the OpenAPI document, and public-only JWKS publication.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class DeploymentSmokeTest {

    private static final String HOST_NAME = "http://localhost:";

    private static final String URI_AUTHORIZATION_SERVER_JWT_ENDPOINT = "/oauth2/token";
    private static final String URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "/oauth2/jwks";
    private static final String URI_ACTUATOR_ENDPOINT = "/actuator/health";

    private static final String OPENAPI_ENDPOINT = "/openapi-authorization.yaml";

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    /**
     * 
     * Performs the 'exposes healthy deployment endpoint' operation and enforces its documented contract.
     */
    void exposesHealthyDeploymentEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                HOST_NAME + port + URI_ACTUATOR_ENDPOINT, String.class);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test

    /**
     * 
     * Performs the 'serves open api specification' operation required by this class.
     */
    void servesOpenApiSpecification() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                HOST_NAME + port + OPENAPI_ENDPOINT, String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT));

   assertTrue(response.getBody().contains(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT));
    }

    @Test
    /**
     * 
     * Performs the 'publishes only public signing keys' operation and enforces its documented contract.
     */
    void publishesOnlyPublicSigningKeys() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                HOST_NAME + port + URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT, String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"keys\""));
        assertFalse(response.getBody().contains("\"d\""));
    }
}
