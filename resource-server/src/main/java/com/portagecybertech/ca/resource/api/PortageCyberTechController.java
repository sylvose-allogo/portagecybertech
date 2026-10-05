package com.portagecybertech.ca.resource.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
/**
 * 
 * Exposes the protected Hello World resource API. The endpoint uses the authenticated JWT principal as the greeting subject and relies on the Resource Server security chain to enforce bearer-token validity and the api.read authority.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class PortageCyberTechController {

    @GetMapping("/hello")
        /**
         * 
         * Returns the JSON greeting for the authenticated bearer-token subject. Access control and api.read scope enforcement occur in the Resource Server security filter chain before this controller runs.
         * @param authentication Spring Security authentication object supplied to the provider for validation and processing.
         * @return the result described above.
         */
        public Map<String, String> hello(Authentication authentication) {
        return Map.of("message", "Hello World " + authentication.getName());
    }
}