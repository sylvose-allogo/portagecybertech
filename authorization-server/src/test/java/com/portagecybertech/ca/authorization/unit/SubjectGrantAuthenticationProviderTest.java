package com.portagecybertech.ca.authorization.unit;

import com.portagecybertech.ca.authorization.oauth.AccessTokenIssuer;
import com.portagecybertech.ca.authorization.oauth.SubjectGrantAuthenticationProvider;
import com.portagecybertech.ca.authorization.oauth.SubjectGrantAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * 
 * Unit tests for subject-grant handling, verifying that an authenticated registered client and allowed subject/scope cause delegation to the shared token issuer.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class SubjectGrantAuthenticationProviderTest {

   private static final String HOST_NAME = "http://localhost:";

    private static final String PORT_RESOURCE_SERVER_ENDPOINT = "8080";

    private static final String URI_RESOURCE_SERVER_API = "/api";

    private static final String URL_RESOURCE_SERVER_API = HOST_NAME + PORT_RESOURCE_SERVER_ENDPOINT + URI_RESOURCE_SERVER_API;

    private static final String CLIENT_ID = "agent-client";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SUBJECT = "subject";
    private static final String SCOPE = "scope";

    private static final String CLIENT_SECRET = "test-only-client-secret";
    private static final String SUBJECT_GRANT = "urn:portagecybertech:oauth:grant-type:subject";
    private static final String USER_NAME = SubjectGrantAuthenticationProviderTest.class.getSimpleName();
    private static final String API_READ = "api.read";


    @Mock
    private RegisteredClientRepository clients;

    @Mock
    private AccessTokenIssuer tokenIssuer;

    @Test
        /**
         * 
         * Performs the 'issues token for authenticated client and requested subject' operation and enforces its documented contract.
         */
        void issuesTokenForAuthenticatedClientAndRequestedSubject() {
        RegisteredClient registeredClient = RegisteredClient.withId("agent")
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(SubjectGrantAuthenticationProvider.GRANT_TYPE)
                .scope(API_READ)
                .build();
        OAuth2ClientAuthenticationToken clientPrincipal =
                new OAuth2ClientAuthenticationToken(registeredClient,
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC, null);
        SubjectGrantAuthenticationToken grant = new SubjectGrantAuthenticationToken(
                clientPrincipal, Map.of(SUBJECT, USER_NAME, SCOPE, API_READ));
        OAuth2AccessTokenAuthenticationToken expected = mock(OAuth2AccessTokenAuthenticationToken.class);

        when(clients.findByClientId(CLIENT_ID)).thenReturn(registeredClient);
        when(tokenIssuer.issue(eq(registeredClient), eq(clientPrincipal), any(Authentication.class),
                eq(SubjectGrantAuthenticationProvider.GRANT_TYPE),
                eq(grant), eq(Set.of(API_READ)), eq(URL_RESOURCE_SERVER_API)))
                .thenReturn(expected);

        Authentication result = new SubjectGrantAuthenticationProvider(
                clients, tokenIssuer, URL_RESOURCE_SERVER_API)
                .authenticate(grant);

        assertSame(expected, result);
        verify(tokenIssuer).issue(eq(registeredClient), eq(clientPrincipal), any(Authentication.class),
                eq(SubjectGrantAuthenticationProvider.GRANT_TYPE), eq(grant),
                eq(Set.of(API_READ)), eq(URL_RESOURCE_SERVER_API));
    }
}