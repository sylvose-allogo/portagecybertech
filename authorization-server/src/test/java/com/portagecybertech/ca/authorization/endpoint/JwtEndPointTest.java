package com.portagecybertech.ca.authorization.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
/**
 * 
 * Contract tests for OAuth access-token issuance, including a valid local subject-grant request and rejection of missing or invalid client credentials and subject input.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class JwtEndPointTest {

    private static final String URI_AUTHORIZATION_SERVER_JWT_ENDPOINT = "/oauth2/token";

    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_SECRET = "test-only-client-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String USER_NAME = JwtEndPointTest.class.getSimpleName();
    private static final String API_READ = "api.read";

    private static final String RESOURCE_SERVER_AUDIENCE = "http://localhost:8080/api";


    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    /**
     * 
     * Performs the 'issues signed jwt for valid subject grant' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void issuesSignedJwtForValidSubjectGrant() throws Exception {
        MvcResult result = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME)
                        .param(SCOPE, API_READ))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = response.path("access_token").asText();
        assertFalse(accessToken.isBlank());
        assertEquals("Bearer", response.path("token_type").asText());
        assertEquals(API_READ, response.path(SCOPE).asText());

        SignedJWT jwt = SignedJWT.parse(accessToken);
        assertEquals(this.getClass().getSimpleName(), jwt.getJWTClaimsSet().getSubject());
        assertTrue(jwt.getJWTClaimsSet().getAudience().contains(RESOURCE_SERVER_AUDIENCE));
        assertNotNull(jwt.getHeader().getKeyID());
        assertEquals("RS256", jwt.getHeader().getAlgorithm().getName());
    }

    @Test
        /**
         * 
         * Performs the 'rejects request without client authentication' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsRequestWithoutClientAuthentication() throws Exception {
        mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME))
                .andExpect(status().isUnauthorized());
    }

    @Test
        /**
         * 
         * Performs the 'rejects invalid client credentials' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsInvalidClientCredentials() throws Exception {
        mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, "wrong-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                            .param(SUBJECT, USER_NAME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    /**
     * 
     * Performs the 'rejects subject grant without subject' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsSubjectGrantWithoutSubject() throws Exception {
        mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }
}