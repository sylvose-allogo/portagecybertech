package com.portagecybertech.ca.authorization.security.secrets;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

/**
 * Verifies KV v2 response handling, HTTPS enforcement, token use and failures
 * without requiring a running Vault server.
 *
 * <p>Portage CyberTech project version 1.0.0; documented 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
/**
 * 
 * Nested class 'VaultKv2SecretProviderTest' used by VaultKv2SecretProviderTest; it supports the containing class contract and is not a separate application service.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class VaultKv2SecretProviderTest {

    @Test
    /**
     * 
     * Performs the 'reads and caches kv v2 oauth values' operation and enforces its documented contract.
     */
    void readsAndCachesKvV2OAuthValues() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://vault.test/v1/secret/data/portagecybertech/authorization-server"))
                .andExpect(method(GET))
                .andExpect(header("X-Vault-Token", "agent-token"))
                .andRespond(withSuccess("""
                        {"data":{"data":{"client-id":"agent-client","client-secret":"vault-secret"}}}
                        """, org.springframework.http.MediaType.APPLICATION_JSON));
        VaultKv2SecretProvider provider = new VaultKv2SecretProvider(builder,
                "https://vault.test", "secret", "portagecybertech/authorization-server",
                "agent-token", "");

        assertEquals("agent-client", provider.getRequiredSecret("client-id"));
        assertEquals("vault-secret", provider.getRequiredSecret("client-secret"));
        server.verify();
    }

    @Test
    /**
     * 
     * Performs the 'rejects missing values and invalid paths' operation and enforces its documented contract.
     */
    void rejectsMissingValuesAndInvalidPaths() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://vault.test/v1/secret/data/config"))
                .andRespond(withSuccess("""
                        {"data":{"data":{"client-id":"agent-client"}}}
                        """, org.springframework.http.MediaType.APPLICATION_JSON));
        VaultKv2SecretProvider provider = new VaultKv2SecretProvider(builder,
                "https://vault.test", "secret", "config", "agent-token", "");

        assertThrows(IllegalStateException.class, () -> provider.getRequiredSecret("client-secret"));
        assertThrows(IllegalArgumentException.class, () -> provider.getRequiredSecret("../client-secret"));
        server.verify();
        assertThrows(IllegalArgumentException.class, () -> new VaultKv2SecretProvider(
                RestClient.builder(), "https://vault.test",
                "secret", "../config", "agent-token", ""));
        assertThrows(IllegalArgumentException.class, () -> new VaultKv2SecretProvider(
                RestClient.builder(), "http://vault.test",
                "secret", "config", "agent-token", ""));
    }

    @Test
    /**
     * 
     * Performs the 'requires an agent token when vault is enabled' operation and enforces its documented contract.
     */
    void requiresAnAgentTokenWhenVaultIsEnabled() {
        VaultKv2SecretProvider provider = new VaultKv2SecretProvider(RestClient.builder(),
                "https://vault.test", "secret", "config", "", "");

        assertThrows(IllegalStateException.class, () -> provider.getRequiredSecret("client-secret"));
    }
}
