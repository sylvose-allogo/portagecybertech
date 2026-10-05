package com.portagecybertech.ca.integration.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


/**
 * 
 * Implements Cucumber acceptance steps for authenticated subject-grant token issuance, anonymous-client rejection, public JWKS retrieval, and response assertions against the Authorization Server contract.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class MiniPlateformeOAuthSteps {

    private static final String URI_AUTHORIZATION_SERVER_JWT_ENDPOINT = "/oauth2/token";
    private static final String URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "/oauth2/jwks";

    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_SECRET = "integration-test-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String USER_NAME = MiniPlateformeOAuthSteps.class.getSimpleName();
    private static final String API_READ = "api.read";


    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private int responseStatus;
    private JsonNode tokenResponse;
    private JsonNode jwksResponse;


    @When("an authenticated client requests a JWT from the authorization endpoint")
    /**
     * 
     * Performs the 'request authorization server jwt' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    public void requestAuthorizationServerJwt() throws Exception {
        MvcResult result = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME)
                        .param(SCOPE, API_READ))
                .andReturn();
        responseStatus = result.getResponse().getStatus();
        tokenResponse = result.getResponse().getContentAsString().isBlank()
                ? null
                : objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @When("an anonymous client requests a token")
    /**
     * 
     * Performs the 'request hello anonymously' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    public void requestHelloAnonymously() throws Exception {
        MvcResult result = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME)
                        .param(SCOPE, API_READ))
                .andReturn();
        responseStatus = result.getResponse().getStatus();
    }

    @When("a client requests the authorization server JWKS endpoint")
        /**
         * 
         * Performs the 'request authorization server jwks' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        public void requestAuthorizationServerJwks() throws Exception {
        MvcResult result = mvc.perform(get(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT)).andReturn();
        responseStatus = result.getResponse().getStatus();
        jwksResponse = result.getResponse().getContentAsString().isBlank()
                ? null
                : objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Then("the token response contains a signed bearer JWT")
        /**
         * 
         * Performs the 'check token response contains signed bearer jwt' operation and enforces its documented contract.
         */
        public void checkTokenResponseContainsSignedBearerJwt() {
        assertEquals("Bearer", tokenResponse.path("token_type").asText());
        assertEquals(API_READ, tokenResponse.path(SCOPE).asText());
        String accessToken = tokenResponse.path("access_token").asText();

        assertFalse(accessToken.isBlank())    ;
        assertEquals(3, accessToken.split("\\.").length);
    }

    @Then("the JWKS response contains a public RSA signing key")
    /**
     * 
     * Performs the 'check jwks contains public rsa signing key' operation and enforces its documented contract.
     */
    public void checkJwksContainsPublicRsaSigningKey() {
        JsonNode keys = jwksResponse.path("keys");
        assertTrue(keys.isArray());
        assertFalse(keys.isEmpty());
        JsonNode key = keys.get(0);

        assertEquals("RSA", key.path("kty").asText());
        assertFalse(key.path("kid").asText().isBlank());
        assertFalse(key.path("n").asText().isBlank());
        assertFalse(key.path("e").asText().isBlank());
        assertFalse(key.has("d"));
    }

    @Then("the response status is {int}")
    /**
     * 
     * Performs the 'check response status' operation and enforces its documented contract.
     * @param expectedStatus Expected HTTP status code asserted by the Cucumber step.
     */
    public void checkResponseStatus(int expectedStatus) {
        assertEquals(expectedStatus, responseStatus);
    }
}