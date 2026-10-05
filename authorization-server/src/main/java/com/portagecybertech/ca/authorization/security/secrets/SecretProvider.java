package com.portagecybertech.ca.authorization.security.secrets;

/**
 * Supplies runtime OAuth client configuration without coupling the authorization
 * service to a particular secret-storage implementation.
 *
 * <p>Portage CyberTech project version 1.0.0; documented 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
@FunctionalInterface
/**
 * 
 * Nested interface 'SecretProvider' used by SecretProvider; it supports the containing class contract and is not a separate application service.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface SecretProvider {

    /**
     * Resolves a required OAuth client value.
     *
     * @param name logical value name, such as {@code client-id} or {@code client-secret}
     * @return the non-blank configured value
     * @throws IllegalStateException if the requested value is not available
     */
    String getRequiredSecret(String name);
}
