package com.portagecybertech.ca.authorization.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;


/**
 * 
 * Recognizes the local demonstration subject grant on the token endpoint and converts its form parameters and current authenticated client into a Spring Security authentication request. This grant trusts a caller-supplied subject and is not an end-user authentication mechanism.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SubjectGrantAuthenticationConverter implements AuthenticationConverter {

    @Override

    /**
     * 
     * Returns null for unrelated OAuth grants; for the local subject grant, builds an authentication request from the current client principal and non-grant form parameters.
     * @param request Incoming HTTP request whose grant type and form parameters are inspected.
     * @return the result described above.
     */
    public Authentication convert(HttpServletRequest request) {
        if (!SubjectGrantAuthenticationToken.GRANT_TYPE_VALUE.equals(
                request.getParameter(OAuth2ParameterNames.GRANT_TYPE))) {
            return null;
        }

        return new SubjectGrantAuthenticationToken(
                SecurityContextHolder.getContext().getAuthentication(),
                OAuth2GrantRequestParameters.from(request));
    }
}
