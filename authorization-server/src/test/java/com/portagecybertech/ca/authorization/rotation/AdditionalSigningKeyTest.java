package com.portagecybertech.ca.authorization.rotation;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.portagecybertech.ca.authorization.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
/**
 * 
 * Tests the Authorization Server decoder configuration with an additional public key identified by kid. This verifies source-token decoding at the Authorization Server and is not a Resource Server additional-key end-to-end test.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class AdditionalSigningKeyTest {

    private static final String HOST_NAME = "http://localhost:";
    private static final String PORT_RESOURCE_SERVER_ENDPOINT = "8181";
    private static final String URI_RESOURCE_SERVER_API = "/api";

    @Value("${OAUTH_ISSUER:http://localhost:9090}")
    private String issuer;

    @Value("${app.oauth.api-audience:http://localhost:9090/api}")
    private String apiAudience;


    @Test
        /**
         * 
         * Performs the 'accepts jwt signed by configured replacement public key' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void acceptsJwtSignedByConfiguredReplacementPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair primaryPair = generator.generateKeyPair();
        KeyPair replacementPair = generator.generateKeyPair();
        String replacementKid = "rotation-simulated";
        String publicKey = Base64.getEncoder().encodeToString(replacementPair.getPublic().getEncoded());

        SecurityConfiguration configuration = new SecurityConfiguration();
        RSAKey primaryJwk = new RSAKey.Builder(
                (java.security.interfaces.RSAPublicKey) primaryPair.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey) primaryPair.getPrivate())
                .keyID("primary").build();
        JwtDecoder decoder = configuration.jwtDecoder(
                primaryJwk, issuer, apiAudience, "", "", publicKey, replacementKid);
        String compact = signedToken(replacementPair, replacementKid, issuer, apiAudience);

        Jwt decoded = decoder.decode(compact);

        String URL_OTHER_RESOURCE_SERVER_API = HOST_NAME + PORT_RESOURCE_SERVER_ENDPOINT + URI_RESOURCE_SERVER_API;

        assertEquals("rotation-user", decoded.getSubject());
        assertEquals(List.of(apiAudience), decoded.getAudience());
        assertThrows(BadJwtException.class, () -> decoder.decode(signedToken(
                replacementPair, replacementKid, issuer, URL_OTHER_RESOURCE_SERVER_API)));
    }

        /**
         * 
         * Performs the 'signed token' operation and returns the corresponding result.
         * @param signingPair Key pair whose private key signs the JWT and whose public key is configured for verification.
         * @param kid JWT key identifier (kid) used to select the matching public JWKS key.
         * @param issuer Expected JWT issuer identifier.
         * @param audience Requested resource audience; it must be present in the configured allow-list.
         * @return the result described above.
         * @throws Exception when the operation cannot complete its contract
         */
        private String signedToken(KeyPair signingPair, String kid, String issuer, String audience) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("rotation-user")
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(120)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) signingPair.getPrivate()));
        return jwt.serialize();
    }
}