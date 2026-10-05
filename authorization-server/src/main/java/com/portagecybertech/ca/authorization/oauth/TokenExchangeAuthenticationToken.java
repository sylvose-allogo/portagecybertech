package com.portagecybertech.ca.authorization.oauth;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;


/**
 * 
 * Represents an unprocessed RFC 8693 token-exchange request, including the authenticated client and submitted subject-token parameters.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class TokenExchangeAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final String GRANT_TYPE_VALUE = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String SUBJECT_TOKEN_TYPE =
            "urn:ietf:params:oauth:token-type:access_token";


    /**
     * 
     * Creates an RFC 8693 grant authentication request carrying the authenticated OAuth client and submitted exchange parameters.
     * @param clientPrincipal Authenticated OAuth client principal associated with this grant request.
     * @param parameters Map of submitted OAuth grant parameters passed to the authentication request.
     */
    public TokenExchangeAuthenticationToken(Authentication clientPrincipal,
            Map<String, Object> parameters) {
        super(TokenExchangeAuthenticationProvider.GRANT_TYPE, clientPrincipal, parameters);
    }
}
