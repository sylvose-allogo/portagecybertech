package com.portagecybertech.ca.authorization.security.secrets;

import org.springframework.core.env.Environment;

/**
 * Resolves OAuth client configuration from Spring's property sources.
 *
 * <p>This provider preserves the existing local-development and test behavior.
 * Environment variables, mounted configuration and application properties are
 * resolved using Spring's normal property precedence.</p>
 *
 * <p>Portage CyberTech project version 1.0.0; documented 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
/**
 * 
 * Nested class 'EnvironmentSecretProvider' used by EnvironmentSecretProvider; it supports the containing class contract and is not a separate application service.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class EnvironmentSecretProvider implements SecretProvider {

    private final Environment environment;

    /**
     * Creates a provider backed by Spring's environment.
     *
     * @param environment Spring property environment
     */
    /**
     * 
     * Initializes the EnvironmentSecretProvider instance with the supplied collaborators and configuration.
     * @param environment Parameter used by this operation; its expected form and validation are described in the method contract.
     */
    public EnvironmentSecretProvider(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns a required OAuth client property and retains the historic
     * {@code agent-client} default for the public client identifier only.
     *
     * @param name logical value name mapped to {@code app.oauth.<name>}
     * @return a non-blank property value
     * @throws IllegalStateException if the required property is absent or blank
     */
    @Override
    /**
     * 
     * Performs the 'get required secret' operation and returns the corresponding result.
     * @param name Parameter used by this operation; its expected form and validation are described in the method contract.
     * @return the result described above.
     */
    public String getRequiredSecret(String name) {
        String propertyName = "app.oauth." + name;
        String value = environment.getProperty(propertyName);
        if (value == null && "client-id".equals(name)) {
            value = "agent-client";
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required OAuth configuration is missing: " + propertyName);
        }
        return value;
    }
}
