package com.portagecybertech.ca.resource.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.web.client.RestClient.Builder;


/**
 * 
 * Unit tests for validated JWT inspection, bearer formatting, claims and audience extraction, expiration handling, JWKS key lookup, and RS256 signature verification.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
class JwtTokenDecoderTest {

    private static final String HOST_NAME = "http://localhost:";

    private static final String PORT_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "9090";

    private static final String URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT = "/oauth2/jwks";

    private static final String URL_AUTHORIZATION_SERVER_JWKS_ENDPOINT = HOST_NAME + PORT_AUTHORIZATION_SERVER_JWKS_ENDPOINT + URI_AUTHORIZATION_SERVER_JWKS_ENDPOINT;
    private static final String RESOURCE_SERVER_AUDIENCE = "http://localhost:8080/api";

    private static final String SCOPE = "scope";
    private static final String API_READ = "api.read";
    private static final String JWT_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.c2lnbmF0dXJl";


    @Test
    /**
     * 
     * Performs the 'propagates jwt validation failures' operation required by this class.
     */
    void propagatesJwtValidationFailures() {
        JwtDecoder jwtDecoder = token -> {
            throw new BadJwtException("JWT signature validation failed");
        };
        Builder restClientBuilder = RestClient.builder();
        JwtTokenDecoder decoder = getJwtTokenDecoder(jwtDecoder, restClientBuilder);

        assertThrows(BadJwtException.class, () -> decoder.decode(JWT_TOKEN));
    }

    @Test
        /**
         * 
         * Performs the 'rejects empty tokens' operation and enforces its documented contract.
         */
        void rejectsEmptyTokens() {
        Builder restClientBuilder = RestClient.builder();
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> {
            throw new AssertionError("Decoder should not receive an empty token");
        }, restClientBuilder);

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(" "));
    }

    @Test
        /**
         * 
         * Performs the 'returns jwt header claims and signature only after decoder validation' operation and enforces its documented contract.
         */
        void returnsJwtHeaderClaimsAndSignatureOnlyAfterDecoderValidation() {
        Builder restClientBuilder = RestClient.builder();
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .claim("sub", "alice")
                .claim(SCOPE, API_READ)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build(), restClientBuilder);

        JwtTokenDecoder.DecodedToken decodedToken = decoder.decode(JWT_TOKEN);

        assertEquals("RS256", decodedToken.header().get("alg"));
        assertEquals("JWT", decoder.getJwtHeader(JWT_TOKEN).get("typ"));
        assertEquals("alice", decodedToken.claims().get("sub"));
        assertEquals(API_READ, decodedToken.claims().get(SCOPE));
        assertEquals(    "c2lnbmF0dXJl", decodedToken.signature());
    }

    @Test
    /**
     * 
     * Performs the 'returns validated claims payload as json' operation and enforces its documented contract.
     */
    void returnsValidatedClaimsPayloadAsJson() {
        Builder restClientBuilder = RestClient.builder();
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .claim("sub", "alice")
                .claim(SCOPE,     API_READ)
                .build(), restClientBuilder);

        String payload = decoder.getClaimsJson(JWT_TOKEN);

        assertTrue(payload.startsWith("{"));
        assertFalse(payload.contains("\"alg\":\"RS256\""));
        assertFalse(payload.contains("\"typ\":\"JWT\""));
        assertTrue(payload.contains("\"sub\":\"alice\""));
        assertTrue(payload.contains("\"scope\":\"api.read\""));
        assertTrue(payload.endsWith("}"));
    }

    @Test
    /**
     * 
     * Performs the 'formats access token as bearer authorization header' operation and enforces its documented contract.
     */
    void formatsAccessTokenAsBearerAuthorizationHeader() {
        Builder restClientBuilder =     RestClient.builder();
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> null, restClientBuilder);
        assertEquals("Bearer signed.jwt.token", decoder.toBearerAuthorizationValue("signed.jwt.token"));
        assertEquals("Bearer signed.jwt.token", decoder.toBearerAuthorizationValue("Bearer signed.jwt.token"));
        assertThrows(IllegalArgumentException.class, () -> decoder.toBearerAuthorizationValue(" "));
    }
    
    @Test
    /**
     * 
     * Performs the 'returns expiration date from validated jwt' operation and enforces its documented contract.
     */
    void returnsExpirationDateFromValidatedJwt() {
        Instant expiration = Instant.parse("2030-01-01T00:00:00Z");
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .expiresAt(expiration)
                .build(), RestClient.builder());

        assertEquals(expiration, decoder.getExpiresAt(JWT_TOKEN));
    }

    @Test
    /**
     * 
     * Performs the 'rejects jwt without expiration claim' operation and enforces its documented contract.
     */
    void rejectsJwtWithoutExpirationClaim() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", "alice")
                .build(), RestClient.builder());

        assertThrows(BadJwtException.class, () -> decoder.getExpiresAt(JWT_TOKEN));
    }

    @Test
    /**
     * 
     * Performs the 'returns signing algorithm from jwt header' operation and enforces its documented contract.
     */
    void returnsSigningAlgorithmFromJwtHeader() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> null, RestClient.builder());
        assertEquals("RS256", decoder.getSigningAlgorithm(JWT_TOKEN));
    }

    @Test
    /**
     * 
     * Performs the 'rejects empty token when reading signing algorithm' operation and enforces its documented contract.
     */
    void rejectsEmptyTokenWhenReadingSigningAlgorithm() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> null, RestClient.builder());
        assertThrows(IllegalArgumentException.class, () -> decoder.getSigningAlgorithm(" "));
    }

    @Test
        /**
         * 
         * Performs the 'retrieves public key matching jwt key id from authorization server jwks' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void retrievesPublicKeyMatchingJwtKeyIdFromAuthorizationServerJwks() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .keyID("auth-key-1")
                .build();
        Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        mockServer.expect(requestTo(URL_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andRespond(withSuccess(new JWKSet(jwk).toString(), MediaType.APPLICATION_JSON));
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> null, restClientBuilder);

        java.security.interfaces.RSAPublicKey publicKey = decoder.getSigningPublicKey(jwtWithKeyId("auth-key-1"));

        assertEquals(keyPair.getPublic(), publicKey);
        mockServer.verify();
    }

    @Test
        /**
         * 
         * Performs the 'verifies signature with authorization server public key' operation and enforces its documented contract.
         * @throws Exception when the operation cannot complete its contract
         */
        void verifiesSignatureWithAuthorizationServerPublicKey() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String keyId = "signature-test-key";
        RSAKey publicJwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .keyID(keyId)
                .build();
        Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        mockServer.expect(requestTo(URL_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andRespond(withSuccess(new JWKSet(publicJwk).toString(), MediaType.APPLICATION_JSON))    ;
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> null, restClientBuilder);

        assertTrue(decoder.hasValidSignature(signedJwt(keyPair, keyId)));
        mockServer.verify();
    }

    @Test
    /**
     * 
     * Performs the 'returns false when jwt signature does not match authorization server public key' operation and enforces its documented contract.
     * @throws Exception when the operation cannot complete its contract
     */
    void returnsFalseWhenJwtSignatureDoesNotMatchAuthorizationServerPublicKey() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String keyId = "signature-test-key";
        RSAKey publicJwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .keyID(keyId)
                .build();
        Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        mockServer.expect(requestTo(URL_AUTHORIZATION_SERVER_JWKS_ENDPOINT))
                .andRespond(withSuccess(new JWKSet(publicJwk).toString(), MediaType.APPLICATION_JSON));
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> null, restClientBuilder);
        String signedJwt = signedJwt(keyPair, keyId);
        int signatureStart = signedJwt.lastIndexOf('.') + 1;
        char replacement = signedJwt.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedJwt = signedJwt.substring(0, signatureStart) + replacement
                + signedJwt.substring(signatureStart + 1);

        assertFalse(decoder.hasValidSignature(tamperedJwt));
        mockServer.verify();
    }

    @Test
        /**
         * 
         * Performs the 'returns audience from decoded jwt' operation and enforces its documented contract.
         */
        void returnsAudienceFromDecodedJwt() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .audience(List.of(RESOURCE_SERVER_AUDIENCE, "https://another-api.example.test"))
                .build(), RestClient.builder());

        assertEquals(List.of(RESOURCE_SERVER_AUDIENCE, "https://another-api.example.test"),
                decoder.getAudiences(JWT_TOKEN));
    }

    @Test
        /**
         * 
         * Performs the 'returns empty list when jwt has no audience' operation and enforces its documented contract.
         */
        void returnsEmptyListWhenJwtHasNoAudience() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("alice")
                    .build(), RestClient.builder());

        assertTrue(decoder.getAudiences(JWT_TOKEN).isEmpty());
    }

    @Test
    /**
     * 
     * Performs the 'rejects empty token when reading audience' operation and enforces its documented contract.
     */
    void rejectsEmptyTokenWhenReadingAudience() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> Jwt.withTokenValue(token).build(),
                RestClient.builder());

        assertThrows(IllegalArgumentException.class,
                () -> decoder.getAudiences(" "));
    }

    @Test
    /**
     * 
     * Performs the 'propagates signature validation failures when verifying audience' operation required by this class.
     */
    void propagatesSignatureValidationFailuresWhenVerifyingAudience() {
        JwtTokenDecoder decoder = getJwtTokenDecoder(token -> {
            throw new BadJwtException("JWT signature validation failed");
        }, RestClient.builder());

        assertThrows(BadJwtException.class,
                () -> decoder.getAudiences(JWT_TOKEN));
    }


    /**
     * 
     * Performs the 'get jwt token decoder' operation and returns the corresponding result.
     * @param jwtDecoder Spring JWT decoder used to validate signature, issuer, audience, and standard claims.
     * @param restClientBuilder Spring RestClient builder used to create the HTTP client for JWKS retrieval.
     * @return the result described above.
     */
    private JwtTokenDecoder getJwtTokenDecoder(JwtDecoder jwtDecoder, Builder restClientBuilder) {
        return new JwtTokenDecoder(
                jwtDecoder, new ObjectMapper(), restClientBuilder,
                URL_AUTHORIZATION_SERVER_JWKS_ENDPOINT);
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
     * Performs the 'signed jwt' operation and returns the corresponding result.
     * @param keyPair RSA key pair used to sign or validate a test JWT.
     * @param keyId JWT key identifier (kid) used to select the matching public JWKS key.
     * @return the result described above.
     * @throws Exception when the operation cannot complete its contract
     */
    private String signedJwt(KeyPair keyPair, String keyId) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                new JWTClaimsSet.Builder().subject("alice").build());
        jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }

    /**
     * 
     * Performs the 'jwt with key id' operation required by this class.
     * @param keyId JWT key identifier (kid) used to select the matching public JWKS key.
     * @return the result described above.
     */
    private String jwtWithKeyId(String keyId) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"alg\":\"RS256\",\"kid\":\"" + keyId + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".c2ln";
    }

}