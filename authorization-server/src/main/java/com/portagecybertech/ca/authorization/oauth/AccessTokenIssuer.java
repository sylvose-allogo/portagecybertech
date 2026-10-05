package com.portagecybertech.ca.authorization.oauth;

import java.util.Set;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.stereotype.Component;

@Component

/**
 * 
 * Builds, signs, records, and returns short-lived OAuth access tokens for the configured grant providers. It binds the registered client, authenticated client principal, subject principal, allowed scopes, requested audience, grant type, and grant request into Spring Authorization Server token and authorization objects.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class AccessTokenIssuer {

    private final OAuth2TokenGenerator<Jwt> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;


    /**
     * 
     * Initializes the AccessTokenIssuer instance with the supplied collaborators and configuration.
     * @param tokenGenerator OAuth token generator that creates and signs access tokens.
     * @param authorizationService Authorization service that stores issued token and grant metadata.
     */
    public AccessTokenIssuer(OAuth2TokenGenerator<Jwt> tokenGenerator,
            OAuth2AuthorizationService authorizationService) {
        this.tokenGenerator = tokenGenerator;
        this.authorizationService = authorizationService;
    }


    /**
     * 
     * Creates an OAuth token context containing the client, subject principal, authorized scopes, audience, and grant; generates the signed JWT; persists its authorization metadata; and returns the protocol access-token response.
     * @param client Registered OAuth client whose grants, scopes, and credentials govern token issuance.
     * @param clientPrincipal Authenticated OAuth client principal associated with this grant request.
     * @param subjectPrincipal Authentication principal whose subject and authorities are used when generating the access token.
     * @param grantType OAuth grant type associated with the token request.
     * @param grantAuthentication Authentication token containing the custom grant request and its parameters.
     * @param scopes Authorized scopes included in the resulting access token.
     * @param audience Requested resource audience; it must be present in the configured allow-list.
     * @return the result described above.
     */
    public OAuth2AccessTokenAuthenticationToken issue(RegisteredClient client,
            Authentication clientPrincipal, Authentication subjectPrincipal,
            org.springframework.security.oauth2.core.AuthorizationGrantType grantType,
            OAuth2AuthorizationGrantAuthenticationToken grantAuthentication,
            Set<String> scopes, String audience) {
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .principal(subjectPrincipal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(scopes)
                .tokenType(org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(grantType)
                .authorizationGrant(grantAuthentication)
                .put("audience", audience)
                .build();

        Jwt jwt = tokenGenerator.generate(tokenContext);
        if (jwt == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER,
                jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), scopes);
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName(subjectPrincipal.getName())
                .authorizationGrantType(grantType)
                .authorizedScopes(scopes)
                .attribute(Authentication.class.getName(), subjectPrincipal)
                .token(accessToken, metadata -> metadata.put(
                        OAuth2Authorization.Token.CLAIMS_METADATA_NAME, jwt.getClaims()))
                .build();
        authorizationService.save(authorization);

        if (TokenExchangeAuthenticationProvider.GRANT_TYPE.equals(grantType)) {
            return new OAuth2AccessTokenAuthenticationToken(client, clientPrincipal, accessToken,
                    null, Map.of("issued_token_type", TokenExchangeAuthenticationToken.SUBJECT_TOKEN_TYPE));
        }
        return new OAuth2AccessTokenAuthenticationToken(client, clientPrincipal, accessToken);
    }
}
