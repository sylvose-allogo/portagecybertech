package com.portagecybertech.ca.authorization.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;


/**
 * 
 * Recognizes RFC 8693 token-exchange requests on the token endpoint and converts their form fields and current authenticated client into a Spring Security grant authentication token.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class TokenExchangeAuthenticationConverter implements AuthenticationConverter {

    @Override

    /**
     * 
     * Returns null for unrelated OAuth grants; for RFC 8693, builds an exchange authentication request from the current client principal and submitted source-token parameters.
     * @param request Incoming HTTP request whose grant type and form parameters are inspected.
     * @return the result described above.
     */
    public Authentication convert(HttpServletRequest request) {
        if (!TokenExchangeAuthenticationToken.GRANT_TYPE_VALUE.equals(
                request.getParameter(OAuth2ParameterNames.GRANT_TYPE))) {
            return null;
        }

        return new TokenExchangeAuthenticationToken(
                SecurityContextHolder.getContext().getAuthentication(),
                OAuth2GrantRequestParameters.from(request));
    }
}
