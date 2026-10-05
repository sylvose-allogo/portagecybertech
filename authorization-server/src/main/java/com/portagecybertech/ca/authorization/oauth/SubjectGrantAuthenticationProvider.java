package com.portagecybertech.ca.authorization.oauth;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component

/**
 * 
 * Authenticates and authorizes requests for the local subject grant. It checks the authenticated and registered client, validates the caller-supplied subject and scopes, then delegates JWT creation to the shared access-token issuer; enable this grant only in an isolated demonstration.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SubjectGrantAuthenticationProvider implements AuthenticationProvider {

    public static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType(SubjectGrantAuthenticationToken.GRANT_TYPE_VALUE);

    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private final RegisteredClientRepository clients;
    private final AccessTokenIssuer tokenIssuer;
    private final String apiAudience;

    public SubjectGrantAuthenticationProvider(RegisteredClientRepository clients,
            AccessTokenIssuer tokenIssuer,
            @Value("${app.oauth.api-audience:http://localhost:8080/api}") String apiAudience) {
        this.clients = clients;
        this.tokenIssuer = tokenIssuer;
        this.apiAudience = apiAudience;
    }

    @Override

    /**
     * 
     * Validates the authenticated client, registered grant permission, bounded non-empty caller-supplied subject, and allowed scopes, then delegates token signing. The subject value is not authenticated as a user identity.
     * @param authentication Spring Security authentication object supplied to the provider for validation and processing.
     * @return the result described above.
     */
    public Authentication authenticate(Authentication authentication) {
        SubjectGrantAuthenticationToken grant = (SubjectGrantAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken client = OAuth2GrantRequestSupport.requireAuthenticatedClient(grant);
        RegisteredClient registeredClient = clients.findByClientId(client.getName());
        if (registeredClient == null || !registeredClient.getAuthorizationGrantTypes().contains(GRANT_TYPE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        Object rawSubject = grant.getAdditionalParameters().get(SUBJECT);
        if (!(rawSubject instanceof String subject) || subject.isBlank() || subject.length() > 200) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }
        Set<String> scopes = OAuth2GrantRequestSupport.requestedScopes(
                grant.getAdditionalParameters().get(SCOPE), registeredClient.getScopes());
        Authentication subjectPrincipal = UsernamePasswordAuthenticationToken.authenticated(
                subject, "N/A", AuthorityUtils.NO_AUTHORITIES);
        return tokenIssuer.issue(registeredClient, client, subjectPrincipal, GRANT_TYPE,
                grant, scopes, apiAudience);
    }

    @Override

    /**
     * 
     * Reports whether Spring Security can route the supplied authentication class to this subject-grant provider.
     * @param authentication Spring Security authentication object supplied to the provider for validation and processing.
     * @return the result described above.
     */
    public boolean supports(Class<?> authentication) {
        return SubjectGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
