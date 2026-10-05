package com.portagecybertech.ca.authorization.oauth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;


/**
 * 
 * Centralizes validation shared by custom grants: authenticated OAuth client enforcement, requested-scope subset checking, and requested-audience allow-list enforcement. It raises OAuth errors rather than silently accepting invalid grant input.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
final class OAuth2GrantRequestSupport {

    /**
     * 
     * Initializes the OAuth2GrantRequestSupport instance with the supplied collaborators and configuration.
     */
    private OAuth2GrantRequestSupport() {
    }


    /**
     * 
     * Requires the grant principal to be an authenticated OAuth client and raises invalid_client when the precondition is not met.
     * @param grant Custom OAuth grant authentication request being validated and processed.
     * @return the result described above.
     */
    static OAuth2ClientAuthenticationToken requireAuthenticatedClient(
            OAuth2AuthorizationGrantAuthenticationToken grant) {
        if (!(grant.getPrincipal() instanceof OAuth2ClientAuthenticationToken client)
                || !client.isAuthenticated()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }
        return client;
    }


    /**
     * 
     * Uses the complete allowed scope set when scope is omitted; otherwise parses whitespace-separated requested values and rejects empty or unauthorized scopes with invalid_scope.
     * @param rawScope Unparsed scope form parameter; whitespace-separated values are checked against permitted scopes.
     * @param allowedScopes Scopes registered as available to the OAuth client.
     * @return the result described above.
     */
    static Set<String> requestedScopes(Object rawScope, Set<String> allowedScopes) {
        if (rawScope == null || rawScope.toString().isBlank()) {

return allowedScopes;
        }
        Set<String> requested = Arrays.stream(rawScope.toString().trim().split("\\s+"))
                .collect(Collectors.toSet());
        if (requested.isEmpty() || !allowedScopes.containsAll(requested)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
        }
        return requested;
    }

    /**
     * 
     * Selects the configured default audience when none is supplied and rejects any selected URI outside the explicit audience allow-list.
     * @param rawAudience Unparsed audience form parameter submitted to the token endpoint.
     * @param allowedAudiences Configured set of permitted audience URIs.
     * @param defaultAudience Audience selected when the request does not explicitly provide one.
     * @return the result described above.
     */
    static String requestedAudience(Object rawAudience, Set<String> allowedAudiences,
            String defaultAudience) {
        String audience = rawAudience == null || rawAudience.toString().isBlank()
                ? defaultAudience : rawAudience.toString().trim();
        if (!allowedAudiences.contains(audience)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }
        return audience;
    }
}
