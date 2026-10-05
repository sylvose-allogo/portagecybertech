package com.portagecybertech.ca.authorization.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(properties = {
        "app.oauth.issuer=http://localhost:9090",
        "app.oauth.api-audience=http://localhost:8080/api"
})
@AutoConfigureMockMvc
/**
 * 
 * System-level tests for the Authorization Server OAuth flow: local source-token creation, RFC 8693 exchange, output claims and JWKS signature verification, plus rejection of untrusted audiences, insufficient scopes, and tampered source tokens.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class OAuthSystemTest {

    private static final String HOST_NAME = "http://localhost:";

    private static final String PORT_RESOURCE_SERVER_ENDPOINT = "8080";

    private static final String URI_AUTHORIZATION_SERVER_JWT_ENDPOINT = "/oauth2/token";
    private static final String URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "/oauth2/jwks";
    private static final String URI_RESOURCE_SERVER_API = "/api";

    private static final String URL_RESOURCE_SERVER_API = HOST_NAME + PORT_RESOURCE_SERVER_ENDPOINT + URI_RESOURCE_SERVER_API;

    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_SECRET = "test-only-client-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String USER_NAME = OAuthSystemTest.class.getSimpleName();
    private static final String API_READ = "api.read";
    private static final String API_WRITE = "api.write";

    private static final String SUBJECT_TOKEN = "subject_token";
    private static final String SUBJECT_TOKEN_TYPE = "subject_token_type";
    private static final String REQUESTED_TOKEN_TYPE = "requested_token_type";
    private static final String API_AUDIENCE = "audience";

    private static final String SUBJECT_TOKEN_EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String SUBJECT_TOKEN_TYPE_GRANT = "urn:ietf:params:oauth:token-type:access_token";
    private static final String REQUESTED_TOKEN_TYPE_GRANT = "urn:ietf:params:oauth:token-type:access_token";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
        /**
         * 
         * Performs the 'exchanges verified subject token for configured resource audience' operation required by this class.
         * @throws Exception when the operation cannot complete its contract
         */
        void exchangesVerifiedSubjectTokenForConfiguredResourceAudience() throws Exception {
        MvcResult tokenResult = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME)
                        .param(SCOPE, API_READ))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(tokenResult);

        String accessToken = readToken(tokenResult);
        assertNotNull(accessToken);

        MvcResult jwtResult = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE,SUBJECT_TOKEN_EXCHANGE_GRANT)
                        .param(SUBJECT_TOKEN, accessToken)
                        .param(SUBJECT_TOKEN_TYPE,SUBJECT_TOKEN_TYPE_GRANT)
                        .param(REQUESTED_TOKEN_TYPE, REQUESTED_TOKEN_TYPE_GRANT)
                        .param(API_AUDIENCE, URL_RESOURCE_SERVER_API)
                            .param(SCOPE,API_READ))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(jwtResult);

        String jwtToken = readToken(jwtResult);
        assertNotNull(jwtToken);

        JsonNode exchangeBody = objectMapper.readTree(jwtResult.getResponse().getContentAsString());
        assertNotNull(exchangeBody);

        assertEquals(SUBJECT_TOKEN_TYPE_GRANT, exchangeBody.get("issued_token_type").asText());

        Jwt decoded = jwtDecoder.decode(jwtToken);
        assertEquals(USER_NAME, decoded.getSubject());
        assertEquals(List.of(URL_RESOURCE_SERVER_API), decoded.getAudience());
    }

    @Test
    /**
     * 
     * Performs the 'publishes jwks for issued jwt' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void publishesJwksForIssuedJwt() throws Exception {
        MvcResult tokenResult = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME)
                        .param(SCOPE, API_READ))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(tokenResult);

        String accessToken = readToken(tokenResult);
        assertNotNull(accessToken);

        MvcResult jwksResult = mvc.perform(get(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(jwksResult);

        JWKSet jwkSet = JWKSet.parse(jwksResult.getResponse().getContentAsString());
        assertNotNull(jwkSet);

        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        assertNotNull(signedJwt);

        RSAKey signingKey = (RSAKey) jwkSet.getKeyByKeyId(signedJwt.getHeader().getKeyID());
        assertNotNull(signingKey);

        assertTrue(signedJwt.verify(new RSASSAVerifier(signingKey.toRSAPublicKey())));

        Jwt decoded = jwtDecoder.decode(accessToken);
        assertEquals(USER_NAME, decoded.getSubject());
        assertEquals(List.of(URL_RESOURCE_SERVER_API), decoded.getAudience());
    }

    @Test
    /**
     * 
     * Performs the 'rejects tampered subject token with invalid grant' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void rejectsTamperedSubjectTokenWithInvalidGrant() throws Exception {
        String subjectToken = issueSubjectToken();
        String[] segments = subjectToken.split("\\.");
        char replacement = segments[2].charAt(0) == 'A' ? 'B' : 'A';
        String tamperedToken = segments[0] + "." + segments[1] + "."
                + replacement + segments[2].substring(1);

        mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_TOKEN_EXCHANGE_GRANT)
                        .param(SUBJECT_TOKEN, tamperedToken)
                        .param(SUBJECT_TOKEN_TYPE, SUBJECT_TOKEN_TYPE_GRANT)
                        .param(SCOPE, API_READ))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.error").value("invalid_grant"));
    }

    @Test
        /**
         * 
         * Performs the 'rejects token exchange audience outside configured allow list' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsTokenExchangeAudienceOutsideConfiguredAllowList() throws Exception {
        mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_TOKEN_EXCHANGE_GRANT)
                        .param(SUBJECT_TOKEN, issueSubjectToken())
                        .param(SUBJECT_TOKEN_TYPE, SUBJECT_TOKEN_TYPE_GRANT)
                        .param(API_AUDIENCE, "https://untrusted.example.test")
                        .param(SCOPE, API_READ))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.error").value("invalid_request"));
    }

    @Test
        /**
         * 
         * Performs the 'rejects token exchange scope missing from source token' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void rejectsTokenExchangeScopeMissingFromSourceToken() throws Exception {
        mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_TOKEN_EXCHANGE_GRANT)
                        .param(SUBJECT_TOKEN, issueSubjectToken())
                        .param(SUBJECT_TOKEN_TYPE, SUBJECT_TOKEN_TYPE_GRANT)
                        .param(SCOPE, API_WRITE))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.error").value("invalid_scope"));
    }

    /**
     * 
     * Performs the 'issue subject token' operation and returns the corresponding result.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private String issueSubjectToken() throws Exception {
        MvcResult result = mvc.perform(post(URI_AUTHORIZATION_SERVER_JWT_ENDPOINT)
                        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(GRANT_TYPE, SUBJECT_GRANT)
                        .param(SUBJECT, USER_NAME)
                        .param(SCOPE, API_READ))
                .andExpect(status().isOk())
                .andReturn();
        return readToken(result);
    }

    /**
     * 
     * Performs the 'read token' operation and returns the corresponding result.
     * @param result HTTP exchange result whose response body contains the token or JSON document.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private String readToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("access_token").asText();
        assertNotNull(token);
        return token;
    }
}