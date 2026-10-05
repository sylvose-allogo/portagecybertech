package com.portagecybertech.ca.authorization.security.secrets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

/**
 * Selects the configured secret source for the Authorization Server.
 *
 * <p>The default is the pre-existing Spring environment/property source.
 * Vault integration is activated explicitly with
 * {@code app.security.secrets.provider=vault}.</p>
 *
 * <p>Portage CyberTech project version 1.0.0; documented 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
/**
 * 
 * Nested class 'SecretProviderConfiguration' used by SecretProviderConfiguration; it supports the containing class contract and is not a separate application service.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SecretProviderConfiguration {

    /**
     * Creates the compatibility provider for the current Spring property sources.
     *
     * @param environment Spring environment containing OAuth configuration
     * @return the provider used when Vault is disabled
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "app.security.secrets",
            name = "provider",
            havingValue = "environment",
            matchIfMissing = true)
    /**
     * 
     * Performs the 'environment secret provider' operation required by this class.
     * @param environment Parameter used by this operation; its expected form and validation are described in the method contract.
     * @return the result described above.
     */
    SecretProvider environmentSecretProvider(Environment environment) {
        return new EnvironmentSecretProvider(environment);
    }

    /**
     * Creates the Vault KV v2 provider when Vault is explicitly selected.
     *
     * @param restClientBuilder configured Spring HTTP client builder
     * @param vaultAddress configured Vault base URL
     * @param kvMount Vault KV v2 mount name
     * @param secretPath relative secret path inside the KV v2 mount
     * @param vaultToken optional Vault Agent token from the process environment
     * @param vaultTokenFile optional Vault Agent token sink file
     * @return Vault-backed secret provider
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "app.security.secrets",
            name = "provider",
            havingValue = "vault")
    SecretProvider vaultKv2SecretProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.vault.address:http://127.0.0.1:8200}") String vaultAddress,
            @Value("${app.vault.kv.mount:secret}") String kvMount,
            @Value("${app.vault.kv.path:portagecybertech/authorization-server}") String secretPath,
            @Value("${VAULT_TOKEN:}") String vaultToken,
            @Value("${VAULT_TOKEN_FILE:}") String vaultTokenFile) {
        return new VaultKv2SecretProvider(restClientBuilder, vaultAddress, kvMount,
                secretPath, vaultToken, vaultTokenFile);
    }
}
