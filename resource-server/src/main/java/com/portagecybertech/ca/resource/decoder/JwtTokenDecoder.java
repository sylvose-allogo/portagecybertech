package com.portagecybertech.ca.resource.decoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.web.client.RestClient.Builder;

@Component
/**
 * 
 * Provides validated access-token inspection utilities. Spring Security performs the configured JWT validation; Nimbus JOSE/JWT is used to inspect the compact token, retrieve the matching public RSA key from JWKS, and independently verify the RS256 signature.
 *
 * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
 * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
 * @version 1.0.0
 * @since 1.0.0
 */
public class JwtTokenDecoder {

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String jwksUri;

    public JwtTokenDecoder(JwtDecoder jwtDecoder, ObjectMapper objectMapper, Builder restClientBuilder,
            @Value("${app.oauth.jwks-uri:http://localhost:9090/oauth2/jwks}") String jwksUri) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.jwksUri = jwksUri;
    }

    /**
     * 
     * Validates the supplied compact JWT through Spring Security before extracting its Nimbus header, claims, and encoded signature; malformed or untrusted tokens fail with a JWT exception.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public DecodedToken decode(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

            Jwt validatedJwt = jwtDecoder.decode(accessToken);
        try {
            SignedJWT signedJwt = SignedJWT.parse(accessToken);
            return new DecodedToken(
                    validatedJwt.getHeaders(),
                    validatedJwt.getClaims(),
                    signedJwt.getSignature().toString());
        } catch (ParseException exception) {
            throw new BadJwtException("Impossible de lire la structure du jeton JWT", exception);
        }
    }

        /**
         * 
         * Immutable view of a successfully decoded JWT containing its JOSE header, validated claims, and encoded signature.
         *
         * <p>Project demonstration metadata: Portage CyberTech, release 1.0.0, documentation date 2026-10-04.</p>
         * @param header JOSE header extracted from the compact JWT.
         * @param claims Validated JWT claim set represented as name/value pairs.
         * @param signature Encoded JWS signature component extracted from the compact token.
         * @author Sylvose Allogo <sylvose.allogo@yahoo.com>
         * @version 1.0.0
         * @since 1.0.0
         */
        public record DecodedToken(
            Map<String, Object> header,
            Map<String, Object> claims,
            String signature) {
    }

    /**
     * 
     * Performs the 'get jwt header' operation and returns the corresponding result.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public Map<String, Object> getJwtHeader(String accessToken) {
        return decode(accessToken).header();
    }

    /**
     * 
     * Performs the 'get claims json' operation and returns the corresponding result.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public String getClaimsJson(String accessToken) {
        try {
            return objectMapper.writeValueAsString(decode(accessToken).claims());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de sérialiser le corps du jeton JWT", exception);
        }
    }

        /**
         * 
         * Performs the 'to bearer authorization value' operation and returns the corresponding result.
         * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
         * @return the result described above.
         */
        public String toBearerAuthorizationValue(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }
        if (accessToken.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return accessToken;
        }
        return "Bearer " + accessToken;
    }

        /**
         * 
         * Performs the 'get expires at' operation and returns the corresponding result.
         * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
         * @return the result described above.
         */
        public Instant getExpiresAt(String accessToken) {
        Instant expiresAt = jwtDecoder.decode(accessToken).getExpiresAt();
        if (expiresAt == null) {
            throw new BadJwtException("Le jeton JWT ne contient pas de date d'expiration (exp)");
        }
        if (!expiresAt.isAfter(Instant.now())) {
            throw new BadJwtException("Le jeton JWT est expiré");
        }
        return expiresAt;
    }

        /**
         * 
         * Performs the 'get signing algorithm' operation and returns the corresponding result.
         * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
         * @return the result described above.
         */
        public String getSigningAlgorithm(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

        try {
            JWSAlgorithm algorithm = SignedJWT.parse(accessToken).getHeader().getAlgorithm();
            if (!JWSAlgorithm.Family.SIGNATURE.contains(algorithm)) {
                throw new BadJwtException("L'algorithme du JWT n'est pas un algorithme de signature");
            }
            return algorithm.getName();
        } catch (ParseException exception) {
            throw new BadJwtException("Impossible de lire l'en-tête du jeton JWT", exception);
        }
    }

    /**
     * 
     * Performs the 'get signing public key' operation and returns the corresponding result.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public RSAPublicKey getSigningPublicKey(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

        try {
            String keyId = SignedJWT.parse(accessToken).getHeader().getKeyID();
            if (keyId == null || keyId.isBlank()) {
                throw new BadJwtException("L'en-tête du JWT ne contient pas de kid");
            }

            String jwksJson = restClient.get().uri(jwksUri).retrieve().body(String.class);
            if (jwksJson == null || jwksJson.isBlank()) {
                throw new IllegalStateException("Le serveur d'autorisation a retourné un JWKS vide");
            }

            JWK jwk = JWKSet.parse(jwksJson).getKeyByKeyId(keyId);
            if (!(jwk instanceof RSAKey rsaKey)) {
                throw new BadJwtException("Aucune clé publique RSA trouvée pour le kid du JWT");
            }
            return rsaKey.toRSAPublicKey();
        } catch (ParseException | JOSEException exception) {
            throw new BadJwtException("Impossible de récupérer la clé publique du JWT", exception);
        }
    }

    /**
     * 
     * Performs the 'has valid signature' operation and returns the corresponding result.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public boolean hasValidSignature(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

        try {
            SignedJWT signedJwt = SignedJWT.parse(accessToken);
            if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())) {
                throw new BadJwtException("Seuls les jetons signés avec RS256 sont pris en charge");
            }
            return signedJwt.verify(new RSASSAVerifier(getSigningPublicKey(accessToken)));
        } catch (ParseException | JOSEException exception) {
            throw new BadJwtException("Impossible de vérifier la signature du jeton JWT", exception);
        }
    }

    /**
     * 
     * Performs the 'get audiences' operation and returns the corresponding result.
     * @param accessToken Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.
     * @return the result described above.
     */
    public List<String> getAudiences(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton JWT ne peut pas être vide");
        }

        List<String> audiences = jwtDecoder.decode(accessToken).getAudience();
        return audiences == null ? List.of() : audiences;
    }
}