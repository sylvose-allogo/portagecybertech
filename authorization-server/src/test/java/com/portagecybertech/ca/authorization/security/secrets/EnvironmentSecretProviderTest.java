package com.portagecybertech.ca.authorization.security.secrets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies compatibility with Spring property sources and required-value handling.
 *
 * <p>Portage CyberTech project version 1.0.0; documented 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
/**
 * 
 * Nested class 'EnvironmentSecretProviderTest' used by EnvironmentSecretProviderTest; it supports the containing class contract and is not a separate application service.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class EnvironmentSecretProviderTest {

    @Test
    /**
     * 
     * Performs the 'resolves required oauth properties' operation required by this class.
     */
    void resolvesRequiredOAuthProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.oauth.client-id", "agent-client")
                .withProperty("app.oauth.client-secret", "test-secret");
        EnvironmentSecretProvider provider = new EnvironmentSecretProvider(environment);

        assertEquals("agent-client", provider.getRequiredSecret("client-id"));
        assertEquals("test-secret", provider.getRequiredSecret("client-secret"));
    }

    @Test
    /**
     * 
     * Performs the 'rejects missing or blank oauth properties' operation and enforces its documented contract.
     */
    void rejectsMissingOrBlankOAuthProperties() {
        EnvironmentSecretProvider provider = new EnvironmentSecretProvider(new MockEnvironment()
                .withProperty("app.oauth.client-secret", " "));

        assertEquals("agent-client", provider.getRequiredSecret("client-id"));
        assertThrows(IllegalStateException.class, () -> provider.getRequiredSecret("client-secret"));
    }
}
