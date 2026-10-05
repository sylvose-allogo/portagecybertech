package com.portagecybertech.ca.resource.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.portagecybertech.ca.resource.decoder.JwtTokenDecoder;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 
 * Unit tests for validation-report construction, invalid and expired JWT handling, boolean validation results, and verification through a local JWKS endpoint.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class JwtTokenValidationTest {

    private static final String LOCALHOST = "localhost";
    private static final String HOST_NAME = "http://localhost:";
    private static final String JWT_TOKEN = "signed.jwt.token";

    @Test
        /**
         * 
         * Performs the 'returns details after validating token' operation and enforces its documented contract.
         */
        void returnsDetailsAfterValidatingToken() {
        Instant expiration = Instant.now().plusSeconds(60);
        RSAPublicKey publicKey = new TestRsaPublicKey();
        List<String> audiences = List.of("https://api.example.test");
        JwtTokenDecoder decoder = new StubTokenDecoder(
                "{\"sub\":\"alice\"}", "Bearer " + JWT_TOKEN, expiration,
                "RS256", true, publicKey, audiences);
        JwtTokenValidation validation = new JwtTokenValidation(decoder);

        JwtTokenValidation.ValidatedToken result = validation.validate("Bearer " + JWT_TOKEN);

        assertEquals(Map.of("alg", "RS256", "typ", "JWT"), result.jwtHeader());
        assertEquals("{\"sub\":\"alice\"}", result.claimsJson());
        assertEquals("Bearer " + JWT_TOKEN, result.bearerAuthorizationValue());
        assertEquals(expiration, result.expiresAt());
        assertEquals("RS256", result.signingAlgorithm());
        assertTrue(result.signatureValid());
        assertEquals(publicKey, result.signingPublicKey());
        assertEquals(audiences, result.audiences());
    }

    @Test
        /**
         * 
         * Performs the 'rejects token with invalid signature' operation and enforces its documented contract.
         */
        void rejectsTokenWithInvalidSignature() {
        JwtTokenDecoder decoder = stubDecoder(false, Instant.now().plusSeconds(60), null);
        JwtTokenValidation validation = new JwtTokenValidation(decoder);

        assertThrows(BadJwtException.class, () -> validation.validate(JWT_TOKEN));
    }

        @Test
    /**
     * 
     * Performs the 'rejects expired token' operation and enforces its documented contract.
     */
    void rejectsExpiredToken() {
        JwtTokenDecoder decoder =     stubDecoder(true, Instant.now().minusSeconds(1), null);
        JwtTokenValidation validation = new JwtTokenValidation(decoder);

        assertThrows(BadJwtException.class, () -> validation.validate(JWT_TOKEN));
    }

    @Test
        /**
         * 
         * Performs the 'rejects empty token' operation and enforces its documented contract.
         */
        void rejectsEmptyToken() {
        JwtTokenValidation service = new JwtTokenValidation(
                stubDecoder(true, Instant.now().plusSeconds(60), null));

        assertThrows(IllegalArgumentException.class, () -> service.validate(" "));
    }

    @Test
    /**
     * 
     * Performs the 'is valid returns true when validation succeeds' operation and enforces its documented contract.
     */
    void isValidReturnsTrueWhenValidationSucceeds() {
        JwtTokenValidation validation = new JwtTokenValidation(
                stubDecoder(true, Instant.now().plusSeconds(60), new TestRsaPublicKey()));

        assertTrue(validation.isValid(JWT_TOKEN));
    }

    @Test
    /**
     * 
     * Performs the 'is valid returns false when validation fails' operation and enforces its documented contract.
     */
    void isValidReturnsFalseWhenValidationFails() {
        JwtTokenValidation invalidSignatureService = new JwtTokenValidation(
                stubDecoder(false, Instant.now().plusSeconds(60), null));
        JwtTokenValidation expiredTokenService = new JwtTokenValidation(
                stubDecoder(true, Instant.now().minusSeconds(1), null));

        assertFalse(invalidSignatureService.isValid(JWT_TOKEN));
        assertFalse(expiredTokenService.isValid(JWT_TOKEN));
        assertFalse(expiredTokenService.isValid(" "));
    }

    @Test
    /**
     * 
     * Performs the 'is valid uses spring security jwks decoder' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void isValidUsesSpringSecurityJwksDecoder() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String keyId = "test-jwks-key";
        RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).keyID(keyId).build();
        HttpServer jwksServer = HttpServer.create(new InetSocketAddress(LOCALHOST, 0), 0);
        jwksServer.createContext("/jwks", exchange -> {
            byte[] response = new JWKSet(publicJwk).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        jwksServer.start();
        try {
            String jwksUri = HOST_NAME + jwksServer.getAddress().getPort() + "/jwks";
            String validToken = signedToken(keyPair, keyId, Instant.now().plusSeconds(60));
            String expiredToken = signedToken(keyPair, keyId, Instant.now().minusSeconds(60));
            JwtTokenValidation service = new JwtTokenValidation(new StubTokenDecoder(
                    "{\"sub\":\"alice\"}", "Bearer " +     JWT_TOKEN, Instant.now().plusSeconds(60),
                    "RS256", true, new TestRsaPublicKey(), List.of("https://api.example.test")));

            assertTrue(service.isValid("Bearer " + validToken, jwksUri));
            assertFalse(service.isValid(expiredToken, jwksUri));
            assertFalse(service.isValid(validToken, " "));
        } finally {
            jwksServer.stop(0);
        }
    }

    @Test
    /**
     * 
     * Performs the 'validate returns jwt audience values' operation and enforces its documented contract.
     */
    void validateReturnsJwtAudienceValues() {
        List<String> expectedAudience = List.of("https://api.example.test", "https://reports.example.test");
        JwtTokenValidation validation = new JwtTokenValidation(
                new StubTokenDecoder("{\"sub\":\"alice\"}", "Bearer " + JWT_TOKEN,
                        Instant.now().plusSeconds(60), "RS256", true, new TestRsaPublicKey(),
                        expectedAudience));

        assertEquals(expectedAudience, validation.validate(JWT_TOKEN).audiences());
    }

    @Test
        /**
         * 
         * Performs the 'validate returns empty list when jwt has no audience' operation and enforces its documented contract.
         */
        void validateReturnsEmptyListWhenJwtHasNoAudience() {
        JwtTokenValidation validation = new JwtTokenValidation(
                new StubTokenDecoder("{\"sub\":\"alice\"}", "Bearer " + JWT_TOKEN,
                        Instant.now().plusSeconds(60), "RS256", true, new TestRsaPublicKey(),
                        List.of()));

        assertTrue(validation.validate(JWT_TOKEN).audiences().isEmpty());
    }

    @Test
    /**
     * 
     * Performs the 'is valid returns false when audience cannot be read' operation and enforces its documented contract.
     */
    void isValidReturnsFalseWhenAudienceCannotBeRead() {
        JwtTokenValidation validation = new JwtTokenValidation(
                new StubTokenDecoder("{\"sub\":\"alice\"}", "Bearer " + JWT_TOKEN,
                        Instant.now().plusSeconds(60), "RS256", true, new TestRsaPublicKey(),
                        List.of(), true));

        assertFalse(validation.isValid(JWT_TOKEN));
    }

    /**
     * 
     * Test-only JwtTokenDecoder substitute that returns controlled metadata or validation failures so validation-service behaviour can be tested without network calls.
     *
     * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
     * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
     * @version 1.0.0
     * @since 1.0.0
     */
    private static final class StubTokenDecoder extends JwtTokenDecoder {

        private final String claimsJson;
        private final String bearerAuthorizationValue;
        private final Instant expiresAt;
        private final String signingAlgorithm;
        private final boolean signatureValid;
        private final RSAPublicKey signingPublicKey;
        private final List<String> audiences;
        private final boolean audienceReadFails;

        /**
         * 
         * Initializes the StubTokenDecoder instance with the supplied collaborators and configuration.
         * @param claimsJson JSON representation of the validated JWT claims returned by the test double.
         * @param bearerAuthorizationValue Expected Authorization header value formatted with the standard Bearer authorization scheme.
         * @param expiresAt JWT expiration instant used to determine whether the token remains valid.
         * @param signingAlgorithm JWS signing-algorithm name expected in the validated token report.
         * @param signatureValid Expected signature-validation result supplied to the test double.
         * @param signingPublicKey RSA public key corresponding to the signing key used to verify the token.
         * @param audiences Audience URIs expected in the validated-token result.
         */
        private StubTokenDecoder(String claimsJson, String bearerAuthorizationValue, Instant expiresAt,
                String signingAlgorithm, boolean signatureValid, RSAPublicKey signingPublicKey,
                List<String> audiences) {
            this(claimsJson, bearerAuthorizationValue, expiresAt, signingAlgorithm,
                    signatureValid, signingPublicKey, audiences, false);
        }

        /**
         * 
         * Initializes the StubTokenDecoder instance with the supplied collaborators and configuration.
         * @param claimsJson JSON representation of the validated JWT claims returned by the test double.
         * @param bearerAuthorizationValue Expected Authorization header value formatted with the standard Bearer authorization scheme.
         * @param expiresAt JWT expiration instant used to determine whether the token remains valid.
         * @param signingAlgorithm JWS signing-algorithm name expected in the validated token report.
         * @param signatureValid Expected signature-validation result supplied to the test double.
         * @param signingPublicKey RSA public key corresponding to the signing key used to verify the token.
         * @param audiences Audience URIs expected in the validated-token result.
         * @param audienceReadFails Whether the test double simulates a failure while reading JWT audiences.
         */
        private StubTokenDecoder(String claimsJson, String bearerAuthorizationValue, Instant expiresAt,
                String signingAlgorithm, boolean signatureValid, RSAPublicKey signingPublicKey,
                List<String> audiences, boolean audienceReadFails) {
            super((JwtDecoder) token -> {
                throw new UnsupportedOperationException("Not used by this service test");
            }, new ObjectMapper(), RestClient.builder(), "http://localhost/jwks");

            this.claimsJson = claimsJson;
            this.bearerAuthorizationValue = bearerAuthorizationValue;
            this.expiresAt = expiresAt;
            this.signingAlgorithm = signingAlgorithm;
            this.signatureValid = signatureValid;
            this.signingPublicKey = signingPublicKey;
            this.audiences = audiences;
            this.audienceReadFails = audienceReadFails;
        }

        @Override
                /**
                 * 
                 * Performs the 'get jwt header' operation and returns the corresponding result.
                 * @param token Compact JWT or token value being processed by the current operation.
                 * @return the result described above.
                 */
                public Map<String, Object> getJwtHeader(String token) {
            return Map.of("alg", "RS256", "typ", "JWT");
        }

        @Override
                /**
                 * 
                 * Performs the 'get claims json' operation and returns the corresponding result.
                 * @param token Compact JWT or token value being processed by the current operation.
                 * @return the result described above.
                 */
                public String getClaimsJson(String token) {
            return claimsJson;
        }

        @Override
        /**
         * 
         * Performs the 'to bearer authorization value' operation and returns the corresponding result.
         * @param token Compact JWT or token value being processed by the current operation.
         * @return the result described above.
         */
        public String toBearerAuthorizationValue(String token) {
            return bearerAuthorizationValue;
        }

        @Override
        /**
         * 
         * Performs the 'get expires at' operation and returns the corresponding result.
         * @param token Compact JWT or token value being processed by the current operation.
         * @return the result described above.
         */
        public Instant getExpiresAt(String token) {
            return expiresAt;
        }

        @Override
                /**
                 * 
                 * Performs the 'get signing algorithm' operation and returns the corresponding result.
                 * @param token Compact JWT or token value being processed by the current operation.
                 * @return the result described above.
                 */
                public String getSigningAlgorithm(String token) {
            return signingAlgorithm;
        }

        @Override
                /**
                 * 
                 * Performs the 'has valid signature' operation and returns the corresponding result.
                 * @param token Compact JWT or token value being processed by the current operation.
                 * @return the result described above.
                 */
                public boolean hasValidSignature(String token) {
            return signatureValid;
        }

        @Override
        /**
         * 
         * Performs the 'get signing public key' operation and returns the corresponding result.
         * @param token Compact JWT or token value being processed by the current operation.
         * @return the result described above.
         */
        public RSAPublicKey getSigningPublicKey(String token) {
            return signingPublicKey;
        }

        @Override
        /**
         * 
         * Performs the 'get audiences' operation and returns the corresponding result.
         * @param token Compact JWT or token value being processed by the current operation.
         * @return the result described above.
         */
        public List<String> getAudiences(String token) {
            if (audienceReadFails) {
                throw new BadJwtException("Unable to read JWT audience");
            }
            return audiences;
        }
    }

    /**
     * 
     * Performs the 'stub decoder' operation required by this class.
     * @param signatureValid Expected signature-validation result supplied to the test double.
     * @param expiration JWT expiration instant used to test time-based validation.
     * @param publicKey RSA public key expected to verify the token signature.
     * @return the result described above.
     */
    private StubTokenDecoder stubDecoder(boolean signatureValid, Instant expiration, RSAPublicKey publicKey) {
        return new StubTokenDecoder(
                "{\"sub\":\"alice\"}", "Bearer " + JWT_TOKEN, expiration,
                "RS256", signatureValid, publicKey, List.of("https://api.example.test"));
    }

    /**
     * 
     * Minimal test RSA public-key value used to verify that validated-token reports preserve the selected public-key reference.
     *
     * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
     * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
     * @version 1.0.0
     * @since 1.0.0
     */
    private static final class TestRsaPublicKey implements RSAPublicKey {

        @Override
        /**
         * 
         * Performs the 'get modulus' operation and returns the corresponding result.
         * @return the result described above.
         */
        public BigInteger getModulus() {
            return BigInteger.ONE;
        }

        @Override
        /**
         * 
         * Performs the 'get public exponent' operation and returns the corresponding result.
         * @return the result described above.
         */
        public BigInteger getPublicExponent() {
            return BigInteger.valueOf(65537);
        }

        @Override
        /**
         * 
         * Performs the 'get algorithm' operation and returns the corresponding result.
         * @return the result described above.
         */
        public String getAlgorithm() {
            return "RSA";
        }

        @Override
        /**
         * 
         * Performs the 'get format' operation and returns the corresponding result.
         * @return the result described above.
         */
        public String getFormat() {
            return "X.509";
        }

        @Override
        /**
         * 
         * Performs the 'get encoded' operation and returns the corresponding result.
         * @return the result described above.
         */
        public byte[] getEncoded() {
            return new byte[0];
        }
    }

    /**
     * 
     * Performs the 'generate rsa key pair' operation required by this class.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 
     * Performs the 'signed token' operation and returns the corresponding result.
     * @param keyPair RSA key pair used to sign or validate a test JWT.
     * @param keyId JWT key identifier (kid) used to select the matching public JWKS key.
     * @param expiration JWT expiration instant used to test time-based validation.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private String signedToken(KeyPair keyPair, String keyId, Instant expiration) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                new JWTClaimsSet.Builder()
                        .subject("alice")
                        .expirationTime(java.util.Date.from(expiration))
                        .build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }
}