package com.portagecybertech.ca.resource.validation;

import com.portagecybertech.ca.resource.decoder.JwtTokenDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
/**
 * 
 * Coordinates access-token checks and returns a structured validation report containing selected JWT metadata. Its boolean helpers convert expected token, configuration, or network validation failures into a negative result for demonstration and tests.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class JwtTokenValidation {

    private final JwtTokenDecoder tokenDecoder;

    /**
     * 
     * Injects the token-decoding collaborator used to validate and describe access tokens.
     * @param tokenDecoder Collaborator that decodes JWTs and retrieves their validated metadata.
     */
    public JwtTokenValidation(JwtTokenDecoder tokenDecoder) {
        this.tokenDecoder = tokenDecoder;
    }

        /**
         * 
         * Removes an optional Bearer prefix, validates the signature and expiration, collects header/claims/algorithm/key/audience metadata, and returns an immutable validation report; invalid input is reported as an exception.
         * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
         * @return the result described above.
         */
        public ValidatedToken validate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

        String token = accessToken.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                ? accessToken.substring("Bearer ".length()).trim()
                : accessToken;
        if (token.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

        boolean signatureValid = tokenDecoder.hasValidSignature(token);
        if (!signatureValid) {
            throw new BadJwtException("La signature du jeton JWT est invalide");
        }

        Instant expiresAt = tokenDecoder.getExpiresAt(token);
        if (!expiresAt.isAfter(Instant.now())) {
            throw new BadJwtException("Le jeton JWT est expiré");
        }

        Map<String, Object> jwtHeader = tokenDecoder.getJwtHeader(token);
        String claimsJson = tokenDecoder.getClaimsJson(token);
        String bearerAuthorizationValue = tokenDecoder.toBearerAuthorizationValue(token);
        String signingAlgorithm = tokenDecoder.getSigningAlgorithm(token);
        RSAPublicKey signingPublicKey = tokenDecoder.getSigningPublicKey(token);
        List<String> audiences = tokenDecoder.getAudiences(token);

        return new ValidatedToken(
                jwtHeader,
                claimsJson,
                bearerAuthorizationValue,
                expiresAt,
                signingAlgorithm,
                signatureValid,
                signingPublicKey,
                audiences);
    }

    /**
     * 
     * Returns true only when the configured validation workflow succeeds; expected JWT, input, key-service, and HTTP-client failures produce false.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public boolean isValid(String accessToken) {
        try {
            validate(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException | IllegalStateException | RestClientException exception) {
            return false;
        }
    }

    /**
     * 
     * Builds a Spring Security decoder for the supplied JWKS URI and returns whether it accepts the normalized token, returning false for invalid inputs or expected decoder/network failures.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @param jwksUri URI of the JSON Web Key Set used to resolve and verify public signing keys.
     * @return the result described above.
     */
    public boolean isValid(String accessToken, String jwksUri) {
        if (accessToken == null || accessToken.isBlank() || jwksUri == null || jwksUri.isBlank()) {
            return false;
        }

        String token = accessToken.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                ? accessToken.substring("Bearer ".length()).trim()
                : accessToken;
        if (token.isBlank()) {
            return false;
        }

        try {
            NimbusJwtDecoder.withJwkSetUri(jwksUri)
                    .build()
                    .decode(token);
            return true;
        } catch (JwtException | IllegalArgumentException | RestClientException exception) {
            return false;
        }
    }

    /**
     * 
     * Immutable report of validated JWT metadata: decoded header, JSON claims, bearer-header form, expiration, signing algorithm, signature result, selected RSA public key, and audiences.
     *
     * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
     * @param jwtHeader Decoded JOSE header returned as a map of header names and values.
     * @param claimsJson JSON representation of the validated JWT claims returned by the test double.
     * @param bearerAuthorizationValue Expected Authorization header value formatted with the standard Bearer authorization scheme.
     * @param expiresAt JWT expiration instant used to determine whether the token remains valid.
     * @param signingAlgorithm JWS signing-algorithm name expected in the validated token report.
     * @param signatureValid Expected signature-validation result supplied to the test double.
     * @param signingPublicKey RSA public key corresponding to the signing key used to verify the token.
     * @param audiences Audience URIs expected in the validated-token result.
     * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
     * @version 1.0.0
     * @since 1.0.0
     */
    public record ValidatedToken(
            Map<String, Object> jwtHeader,
            String claimsJson,
            String bearerAuthorizationValue,
            Instant expiresAt,
            String signingAlgorithm,
            boolean signatureValid,
            RSAPublicKey signingPublicKey,
            List<String> audiences) {
    }
}