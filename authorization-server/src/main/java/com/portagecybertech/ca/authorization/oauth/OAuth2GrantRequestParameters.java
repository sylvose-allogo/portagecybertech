package com.portagecybertech.ca.authorization.oauth;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;


/**
 * 
 * Copies non-grant-type form parameters from an HTTP token request into the parameter map consumed by custom OAuth grant authentication tokens. It preserves the first value of each submitted parameter and omits empty values.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
final class OAuth2GrantRequestParameters {


    /**
     * 
     * Initializes the OAuth2GrantRequestParameters instance with the supplied collaborators and configuration.
     */
    private OAuth2GrantRequestParameters() {
    }


    /**
     * 
     * Extracts the first non-empty submitted value for each form parameter other than grant_type and returns those values as the custom grant parameter map.
     * @param request Incoming HTTP request whose grant type and form parameters are inspected.
     * @return the result described above.
     */
    static Map<String, Object> from(HttpServletRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if (!OAuth2ParameterNames.GRANT_TYPE.equals(name) && values.length > 0) {
                parameters.put(name, values[0]);
            }
        });
        return parameters;
    }
}
