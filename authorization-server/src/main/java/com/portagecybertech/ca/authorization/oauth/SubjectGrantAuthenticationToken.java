package com.portagecybertech.ca.authorization.oauth;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;


/**
 * 
 * Represents an unprocessed request for the local caller-supplied-subject OAuth grant. It carries the authenticated client principal and grant form parameters to the corresponding authentication provider.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class SubjectGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final String GRANT_TYPE_VALUE = "urn:portagecybertech:oauth:grant-type:subject";


    /**
     * 
     * Creates a subject-grant authentication request carrying the authenticated client principal and submitted grant parameters.
     * @param clientPrincipal Authenticated OAuth client principal associated with this grant request.
     * @param parameters Map of submitted OAuth grant parameters passed to the authentication request.
     */
    public SubjectGrantAuthenticationToken(Authentication clientPrincipal, Map<String, Object> parameters) {
        super(SubjectGrantAuthenticationProvider.GRANT_TYPE, clientPrincipal, parameters);
    }
}
