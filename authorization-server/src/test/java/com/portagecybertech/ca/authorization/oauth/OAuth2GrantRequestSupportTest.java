package com.portagecybertech.ca.authorization.oauth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 
 * Focused tests for default audience selection and rejection of requested or configured audiences that fall outside the configured allow-list.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class OAuth2GrantRequestSupportTest {

    @Test
    /**
     * 
     * Performs the 'defaults to configured audience when none is requested' operation and enforces its documented contract.
     */
    void defaultsToConfiguredAudienceWhenNoneIsRequested() {
        assertEquals("https://api.example.test",
                OAuth2GrantRequestSupport.requestedAudience(
                        null, Set.of("https://api.example.test"), "https://api.example.test"));
    }

    @Test
        /**
         * 
         * Performs the 'rejects audience outside configured allow list' operation and enforces its documented contract.
         */
        void rejectsAudienceOutsideConfiguredAllowList() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> OAuth2GrantRequestSupport.requestedAudience(
                        "https://untrusted.example.test",
                            Set.of("https://api.example.test"),
                        "https://api.example.test"));
    }

    @Test
    /**
     * 
     * Performs the 'rejects default audience when configuration does not allow it' operation and enforces its documented contract.
     */
    void rejectsDefaultAudienceWhenConfigurationDoesNotAllowIt() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> OAuth2GrantRequestSupport.requestedAudience(
                        null,
                        Set.of("https://another-api.example.test"),
                        "https://api.example.test"));
    }
}
