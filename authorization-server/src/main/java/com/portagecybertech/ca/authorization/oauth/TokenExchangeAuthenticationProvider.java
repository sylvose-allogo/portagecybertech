package com.portagecybertech.ca.authorization.oauth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component

/**
 * 
 * Implements the RFC 8693 access-token exchange contract. It validates the client, source-token type and signature/claims, intersects requested scopes with source and client permissions, enforces the audience allow-list, and delegates new-token issuance.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class TokenExchangeAuthenticationProvider implements AuthenticationProvider {

    public static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType(TokenExchangeAuthenticationToken.GRANT_TYPE_VALUE);
    private static final String SCOPE = "scope";

    private final RegisteredClientRepository clients;
    private final JwtDecoder jwtDecoder;
    private final AccessTokenIssuer tokenIssuer;
    private final Set<String> allowedAudiences;
    private final String defaultAudience;

    public TokenExchangeAuthenticationProvider(RegisteredClientRepository clients,
            JwtDecoder jwtDecoder, AccessTokenIssuer tokenIssuer,
            @Value("${app.oauth.allowed-audiences:${app.oauth.api-audience:http://localhost:8080/api}}")
            String allowedAudiences,
            @Value("${app.oauth.api-audience:http://localhost:8080/api}") String defaultAudience) {
        this.clients = clients;
        this.jwtDecoder = jwtDecoder;
        this.tokenIssuer = tokenIssuer;
        this.allowedAudiences = Arrays.stream(allowedAudiences.split(","))
                .map(String::trim)
                .filter(audience -> !audience.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.defaultAudience = defaultAudience;
    }

    @Override

    /**
     * 
     * Validates the registered client and RFC 8693 token-type parameters, decodes the trusted source JWT, intersects its scopes with client permissions, validates the requested audience, and delegates issuance of the exchanged access token.
     * @param authentication Spring Security authentication object supplied to the provider for validation and processing.
     * @return the result described above.
     */
    public Authentication authenticate(Authentication authentication) {
        TokenExchangeAuthenticationToken grant = (TokenExchangeAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken client = OAuth2GrantRequestSupport.requireAuthenticatedClient(grant);
        RegisteredClient registeredClient = clients.findByClientId(client.getName());
        if (registeredClient == null || !registeredClient.getAuthorizationGrantTypes().contains(GRANT_TYPE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        Object rawToken = grant.getAdditionalParameters().get("subject_token");
        Object rawTokenType = grant.getAdditionalParameters().get("subject_token_type");
        Object requestedTokenType = grant.getAdditionalParameters().get("requested_token_type");
        if (!(rawToken instanceof String subjectToken) || subjectToken.isBlank()
                || !TokenExchangeAuthenticationToken.SUBJECT_TOKEN_TYPE.equals(rawTokenType)
                || (requestedTokenType != null
                        && !TokenExchangeAuthenticationToken.SUBJECT_TOKEN_TYPE.equals(requestedTokenType))) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        Jwt sourceToken;
        try {
            sourceToken = jwtDecoder.decode(subjectToken);
        } catch (JwtException exception) {
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            OAuth2ErrorCodes.INVALID_GRANT, "The subject token is invalid", null),
                    exception);
        }

        Set<String> sourceScopes = sourceToken.getClaimAsStringList(SCOPE) == null
                ? Set.of() : Set.copyOf(sourceToken.getClaimAsStringList(SCOPE));
        Set<String> requestedScopes = OAuth2GrantRequestSupport.requestedScopes(
                grant.getAdditionalParameters().get(SCOPE), sourceScopes);
        if (!registeredClient.getScopes().containsAll(requestedScopes)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
        }

        String audience = OAuth2GrantRequestSupport.requestedAudience(
                grant.getAdditionalParameters().get("audience"), allowedAudiences, defaultAudience);
        Authentication subjectPrincipal = UsernamePasswordAuthenticationToken.authenticated(
                sourceToken.getSubject(), "N/A", AuthorityUtils.NO_AUTHORITIES);
        return tokenIssuer.issue(registeredClient, client, subjectPrincipal, GRANT_TYPE,
                grant, requestedScopes, audience);
    }

    @Override

    /**
     * 
     * Reports whether Spring Security can route the supplied authentication class to the RFC 8693 exchange provider.
     * @param authentication Spring Security authentication object supplied to the provider for validation and processing.
     * @return the result described above.
     */
    public boolean supports(Class<?> authentication) {
        return TokenExchangeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
