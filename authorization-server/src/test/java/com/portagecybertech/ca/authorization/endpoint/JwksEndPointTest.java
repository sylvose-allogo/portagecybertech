package com.portagecybertech.ca.authorization.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
/**
 * 
 * Contract tests for the public JWKS endpoint, including unauthenticated access, JSON media type, correspondence to the active RSA signing key, and absence of private key parameters.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class JwksEndPointTest {

    private static final String URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "/oauth2/jwks";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RSAKey signingKey;

    
    @Test
    /**
     * 
     * Performs the 'exposes jwks without authentication' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void exposesJwksWithoutAuthentication() throws Exception {
        mvc.perform(get(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 
     * Performs the 'responds with json content type' operation required by this class.
     * @throws Exception when the operation cannot complete its contract
     */
    void respondsWithJsonContentType() throws Exception {
        mvc.perform(get(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
        /**
         * 
         * Performs the 'returns public rsa key matching the signing key' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void returnsPublicRsaKeyMatchingTheSigningKey() throws Exception {
        JsonNode jwks = readJwks();
        JsonNode keys = jwks.path("keys");

        assertTrue(keys.isArray());
        assertEquals(1, keys.size());
        JsonNode key = keys.get(0);
        assertEquals("RSA", key.path("kty").asText());
        assertEquals(signingKey.getKeyID(), key.path("kid").asText());
        assertTrue(key.path("n").isTextual() && !key.path("n").asText().isBlank());
        assertTrue(key.path("e").isTextual() && !key.path("e").asText().isBlank());
    }

    @Test
    /**
     * 
     * Performs the 'never exposes private rsa key parameters' operation required by this class.
     * @throws Exception when the operation cannot complete its contract
     */
    void neverExposesPrivateRsaKeyParameters() throws Exception {
        JsonNode key = readJwks().path("keys").get(0);

        assertNotNull(key);
        for (String privateParameter : new String[] {"d", "p", "q", "dp", "dq", "qi", "oth"}) {
            assertFalse(key.has(privateParameter), "JWKS must not expose " + privateParameter);
        }
    }

    /**
     * 
     * Performs the 'read jwks' operation and returns the corresponding result.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private JsonNode readJwks() throws Exception {
        MvcResult result = mvc.perform(get(URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}