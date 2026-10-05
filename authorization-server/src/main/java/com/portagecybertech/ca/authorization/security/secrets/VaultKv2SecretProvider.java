package com.portagecybertech.ca.authorization.security.secrets;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads OAuth client values from a HashiCorp Vault KV v2 secret.
 *
 * <p>Authentication is supplied by Vault Agent through a short-lived token in
 * an environment variable or a protected sink file. This class never logs the
 * token or returned secret values. Values are read once at service startup; a
 * rotation requires restarting the authorization service.</p>
 *
 * <p>Portage CyberTech project version 1.0.0; documented 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
/**
 * 
 * Nested class 'VaultKv2SecretProvider' used by VaultKv2SecretProvider; it supports the containing class contract and is not a separate application service.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class VaultKv2SecretProvider implements SecretProvider {

    private final RestClient restClient;
    private final String kvMount;
    private final String secretPath;
    private final String vaultToken;
    private final Path vaultTokenFile;
    private volatile Map<String, String> cachedSecrets;

    /**
     * Creates the adapter for one Vault KV v2 entry.
     *
     * @param restClientBuilder Spring-configured HTTP client builder
     * @param vaultAddress Vault base URL; remote hosts must use HTTPS
     * @param kvMount KV v2 mount name, without a slash
     * @param secretPath relative KV v2 secret path
     * @param vaultToken optional short-lived Vault Agent token
     * @param vaultTokenFile optional protected Vault Agent token sink path
     * @throws IllegalArgumentException if the URL, mount or secret path is invalid
     */
    /**
     * 
     * Initializes the VaultKv2SecretProvider instance with the supplied collaborators and configuration.
     * @param restClientBuilder Spring RestClient builder used to create the HTTP client for JWKS retrieval.
     * @param vaultAddress Parameter used by this operation; its expected form and validation are described in the method contract.
     * @param kvMount Parameter used by this operation; its expected form and validation are described in the method contract.
     * @param secretPath Parameter used by this operation; its expected form and validation are described in the method contract.
     * @param vaultToken Parameter used by this operation; its expected form and validation are described in the method contract.
     * @param vaultTokenFile Parameter used by this operation; its expected form and validation are described in the method contract.
     */
    public VaultKv2SecretProvider(
            RestClient.Builder restClientBuilder,
            String vaultAddress,
            String kvMount,
            String secretPath,
            String vaultToken,
            String vaultTokenFile) {
        if (vaultAddress == null || vaultAddress.isBlank()) {
            throw new IllegalArgumentException("Vault address must be configured when Vault is enabled");
        }
        if (kvMount == null || !kvMount.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Vault KV mount must be a single path segment");
        }
        if (secretPath == null || !secretPath.matches("[A-Za-z0-9_/-]+")
                || secretPath.startsWith("/") || secretPath.endsWith("/")
                || secretPath.contains("..")) {
            throw new IllegalArgumentException("Vault KV path must be a relative path");
        }
        URI vaultUri = URI.create(vaultAddress);
        boolean localHttp = "http".equalsIgnoreCase(vaultUri.getScheme())
                && ("localhost".equalsIgnoreCase(vaultUri.getHost())
                || "127.0.0.1".equals(vaultUri.getHost())
                || "[::1]".equals(vaultUri.getHost()));
        if (!"https".equalsIgnoreCase(vaultUri.getScheme()) && !localHttp
                || vaultUri.getHost() == null || vaultUri.getUserInfo() != null
                || vaultUri.getQuery() != null || vaultUri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Vault must use HTTPS unless it is a local loopback development instance");
        }
        this.restClient = restClientBuilder.clone().baseUrl(vaultUri.toString()).build();
        this.kvMount = kvMount;
        this.secretPath = secretPath;
        this.vaultToken = vaultToken == null ? "" : vaultToken.trim();
        this.vaultTokenFile = vaultTokenFile == null || vaultTokenFile.isBlank()
                ? null
                : Path.of(vaultTokenFile);
    }

    /**
     * Returns a required non-blank value from the cached KV v2 entry.
     *
     * @param name secret key stored in the KV entry
     * @return secret value; callers must not log or expose it
     * @throws IllegalArgumentException if the key name is not a simple key
     * @throws IllegalStateException if authentication or the required value is unavailable
     */
    @Override
    /**
     * 
     * Performs the 'get required secret' operation and returns the corresponding result.
     * @param name Parameter used by this operation; its expected form and validation are described in the method contract.
     * @return the result described above.
     */
    public String getRequiredSecret(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Vault secret key must be a simple key name");
        }
        String value = loadSecrets().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required OAuth secret is missing from the configured Vault KV entry");
        }
        return value;
    }

    /**
     * 
     * Performs the 'load secrets' operation required by this class.
     * @return the result described above.
     */
    private Map<String, String> loadSecrets() {
        Map<String, String> current = cachedSecrets;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedSecrets == null) {
                cachedSecrets = fetchSecrets();
            }
            return cachedSecrets;
        }
    }

    /**
     * Fetches, validates and converts the configured KV v2 response to strings.
     *
     * @return immutable view of textual secret values
     */
    /**
     * 
     * Performs the 'fetch secrets' operation required by this class.
     * @return the result described above.
     */
    private Map<String, String> fetchSecrets() {
        String token = resolveVaultToken();
        String endpoint = "/v1/" + kvMount + "/data/" + secretPath;
        JsonNode response = restClient.get()
                .uri(endpoint)
                .header("X-Vault-Token", token)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(JsonNode.class);
        JsonNode data = response == null ? null : response.path("data").path("data");
        if (data == null || !data.isObject()) {
            throw new IllegalStateException("Vault returned an invalid KV v2 response");
        }
        Map<String, String> values = new HashMap<>();
        data.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                values.put(entry.getKey(), entry.getValue().textValue());
            }
        });
        return Map.copyOf(values);
    }

    /**
     * Obtains the short-lived Vault token supplied by the Vault Agent.
     *
     * @return non-blank Vault token
     * @throws IllegalStateException if no usable token source is configured
     */
    /**
     * 
     * Performs the 'resolve vault token' operation required by this class.
     * @return the result described above.
     */
    private String resolveVaultToken() {
        if (!vaultToken.isBlank()) {
            return vaultToken;
        }
        if (vaultTokenFile != null) {
            try {
                String token = Files.readString(vaultTokenFile).trim();
                if (!token.isBlank()) {
                    return token;
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read the Vault Agent token file", exception);
            }
        }
        throw new IllegalStateException(
                "Vault is enabled but neither VAULT_TOKEN nor VAULT_TOKEN_FILE provides a token");
    }
}
