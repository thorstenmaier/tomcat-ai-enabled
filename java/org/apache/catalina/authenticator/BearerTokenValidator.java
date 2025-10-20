/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.authenticator;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.apache.tomcat.util.res.StringManager;

/**
 * Validates JWT (JSON Web Token) bearer tokens for OAuth2 authentication.
 * Supports HS256, HS384, HS512, RS256, RS384, and RS512 signing algorithms.
 * Validates token structure, signature, expiration, and not-before claims.
 */
public class BearerTokenValidator {

    protected static final StringManager sm = StringManager.getManager(BearerTokenValidator.class);

    private final String secretKey;
    private final PublicKey publicKey;
    private final String issuer;
    private final String audience;
    private final int clockSkewSeconds;

    // Cache for parsed and validated tokens to improve performance
    private final ConcurrentMap<String, TokenClaims> tokenCache = new ConcurrentHashMap<>();
    private final int maxCacheSize;

    /**
     * Creates a validator for HMAC-based JWT tokens (HS256, HS384, HS512).
     *
     * @param secretKey          The secret key for HMAC signature validation (Base64-encoded)
     * @param issuer             Expected token issuer (null to skip validation)
     * @param audience           Expected token audience (null to skip validation)
     * @param clockSkewSeconds   Allowed clock skew in seconds for time-based validations
     * @param maxCacheSize       Maximum number of tokens to cache (0 to disable caching)
     */
    public BearerTokenValidator(String secretKey, String issuer, String audience, int clockSkewSeconds,
            int maxCacheSize) {
        this.secretKey = secretKey;
        this.publicKey = null;
        this.issuer = issuer;
        this.audience = audience;
        this.clockSkewSeconds = clockSkewSeconds;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Creates a validator for RSA-based JWT tokens (RS256, RS384, RS512).
     *
     * @param publicKey          The RSA public key for signature validation
     * @param issuer             Expected token issuer (null to skip validation)
     * @param audience           Expected token audience (null to skip validation)
     * @param clockSkewSeconds   Allowed clock skew in seconds for time-based validations
     * @param maxCacheSize       Maximum number of tokens to cache (0 to disable caching)
     */
    public BearerTokenValidator(PublicKey publicKey, String issuer, String audience, int clockSkewSeconds,
            int maxCacheSize) {
        this.secretKey = null;
        this.publicKey = publicKey;
        this.issuer = issuer;
        this.audience = audience;
        this.clockSkewSeconds = clockSkewSeconds;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Validates a JWT bearer token and returns the claims if valid.
     *
     * @param token The JWT token string
     * @return TokenClaims object containing the validated claims
     * @throws TokenValidationException if the token is invalid or expired
     */
    public TokenClaims validate(String token) throws TokenValidationException {
        if (token == null || token.isEmpty()) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.tokenEmpty"));
        }

        // Check cache first
        if (maxCacheSize > 0) {
            TokenClaims cached = tokenCache.get(token);
            if (cached != null && !cached.isExpired(clockSkewSeconds)) {
                return cached;
            }
            // Remove expired token from cache
            if (cached != null) {
                tokenCache.remove(token);
            }
        }

        // Parse JWT structure
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidStructure"));
        }

        String headerEncoded = parts[0];
        String payloadEncoded = parts[1];
        String signatureEncoded = parts[2];

        // Decode and parse header
        String headerJson;
        try {
            headerJson = new String(Base64.getUrlDecoder().decode(headerEncoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidBase64Header"), e);
        }

        JSONParser headerParser = new JSONParser(headerJson);
        Object headerObj;
        try {
            headerObj = headerParser.parse();
        } catch (ParseException e) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidJsonHeader"), e);
        }

        if (!(headerObj instanceof java.util.LinkedHashMap)) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidHeaderType"));
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> header = (java.util.Map<String, Object>) headerObj;
        String algorithm = (String) header.get("alg");

        if (algorithm == null) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.missingAlgorithm"));
        }

        // Validate signature
        String signatureInput = headerEncoded + "." + payloadEncoded;
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getUrlDecoder().decode(signatureEncoded);
        } catch (IllegalArgumentException e) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidBase64Signature"), e);
        }

        if (!verifySignature(algorithm, signatureInput, signatureBytes)) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidSignature"));
        }

        // Decode and parse payload
        String payloadJson;
        try {
            payloadJson = new String(Base64.getUrlDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidBase64Payload"), e);
        }

        JSONParser payloadParser = new JSONParser(payloadJson);
        Object payloadObj;
        try {
            payloadObj = payloadParser.parse();
        } catch (ParseException e) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidJsonPayload"), e);
        }

        if (!(payloadObj instanceof java.util.LinkedHashMap)) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.invalidPayloadType"));
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadObj;

        // Create TokenClaims object
        TokenClaims claims = new TokenClaims(payload);

        // Validate claims
        validateClaims(claims);

        // Cache the validated token
        if (maxCacheSize > 0) {
            // Simple cache eviction: remove oldest entry if cache is full
            if (tokenCache.size() >= maxCacheSize) {
                // Remove first entry (simple FIFO)
                java.util.Iterator<String> keys = tokenCache.keySet().iterator();
                if (keys.hasNext()) {
                    tokenCache.remove(keys.next());
                }
            }
            tokenCache.put(token, claims);
        }

        return claims;
    }

    /**
     * Verifies the JWT signature using the configured key and algorithm.
     */
    private boolean verifySignature(String algorithm, String signatureInput, byte[] signature)
            throws TokenValidationException {
        try {
            if (algorithm.startsWith("HS")) {
                return verifyHmacSignature(algorithm, signatureInput, signature);
            } else if (algorithm.startsWith("RS")) {
                return verifyRsaSignature(algorithm, signatureInput, signature);
            } else {
                throw new TokenValidationException(
                        sm.getString("bearerTokenValidator.unsupportedAlgorithm", algorithm));
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.signatureVerificationFailed"), e);
        }
    }

    /**
     * Verifies HMAC-based signatures (HS256, HS384, HS512).
     */
    private boolean verifyHmacSignature(String algorithm, String signatureInput, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (secretKey == null) {
            throw new IllegalStateException(sm.getString("bearerTokenValidator.secretKeyRequired"));
        }

        String javaAlgorithm;
        switch (algorithm) {
            case "HS256":
                javaAlgorithm = "HmacSHA256";
                break;
            case "HS384":
                javaAlgorithm = "HmacSHA384";
                break;
            case "HS512":
                javaAlgorithm = "HmacSHA512";
                break;
            default:
                throw new NoSuchAlgorithmException(sm.getString("bearerTokenValidator.unsupportedAlgorithm", algorithm));
        }

        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        Key key = new SecretKeySpec(keyBytes, javaAlgorithm);
        Mac mac = Mac.getInstance(javaAlgorithm);
        mac.init(key);
        byte[] calculatedSignature = mac.doFinal(signatureInput.getBytes(StandardCharsets.UTF_8));

        return java.security.MessageDigest.isEqual(signature, calculatedSignature);
    }

    /**
     * Verifies RSA-based signatures (RS256, RS384, RS512).
     */
    private boolean verifyRsaSignature(String algorithm, String signatureInput, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (publicKey == null) {
            throw new IllegalStateException(sm.getString("bearerTokenValidator.publicKeyRequired"));
        }

        String javaAlgorithm;
        switch (algorithm) {
            case "RS256":
                javaAlgorithm = "SHA256withRSA";
                break;
            case "RS384":
                javaAlgorithm = "SHA384withRSA";
                break;
            case "RS512":
                javaAlgorithm = "SHA512withRSA";
                break;
            default:
                throw new NoSuchAlgorithmException(sm.getString("bearerTokenValidator.unsupportedAlgorithm", algorithm));
        }

        Signature sig = Signature.getInstance(javaAlgorithm);
        sig.initVerify(publicKey);
        sig.update(signatureInput.getBytes(StandardCharsets.UTF_8));
        return sig.verify(signature);
    }

    /**
     * Validates standard JWT claims (exp, nbf, iss, aud).
     */
    private void validateClaims(TokenClaims claims) throws TokenValidationException {
        long now = System.currentTimeMillis() / 1000;

        // Validate expiration
        if (claims.isExpired(clockSkewSeconds)) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.tokenExpired"));
        }

        // Validate not-before
        Long nbf = claims.getNotBefore();
        if (nbf != null && now + clockSkewSeconds < nbf) {
            throw new TokenValidationException(sm.getString("bearerTokenValidator.tokenNotYetValid"));
        }

        // Validate issuer
        if (issuer != null && !issuer.isEmpty()) {
            String tokenIssuer = claims.getIssuer();
            if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
                throw new TokenValidationException(
                        sm.getString("bearerTokenValidator.invalidIssuer", tokenIssuer, issuer));
            }
        }

        // Validate audience
        if (audience != null && !audience.isEmpty()) {
            String tokenAudience = claims.getAudience();
            if (tokenAudience == null || !audience.equals(tokenAudience)) {
                throw new TokenValidationException(
                        sm.getString("bearerTokenValidator.invalidAudience", tokenAudience, audience));
            }
        }
    }

    /**
     * Clears the token cache.
     */
    public void clearCache() {
        tokenCache.clear();
    }

    /**
     * Represents the claims contained in a validated JWT token.
     */
    public static class TokenClaims {
        private final java.util.Map<String, Object> claims;

        public TokenClaims(java.util.Map<String, Object> claims) {
            this.claims = claims;
        }

        public String getSubject() {
            return (String) claims.get("sub");
        }

        public String getIssuer() {
            return (String) claims.get("iss");
        }

        public String getAudience() {
            return (String) claims.get("aud");
        }

        public Long getExpiration() {
            Object exp = claims.get("exp");
            if (exp instanceof Number) {
                return ((Number) exp).longValue();
            }
            return null;
        }

        public Long getNotBefore() {
            Object nbf = claims.get("nbf");
            if (nbf instanceof Number) {
                return ((Number) nbf).longValue();
            }
            return null;
        }

        public Long getIssuedAt() {
            Object iat = claims.get("iat");
            if (iat instanceof Number) {
                return ((Number) iat).longValue();
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public String[] getRoles() {
            Object roles = claims.get("roles");
            if (roles instanceof java.util.List) {
                java.util.List<String> roleList = (java.util.List<String>) roles;
                return roleList.toArray(new String[0]);
            } else if (roles instanceof String) {
                // Support comma-separated or space-separated roles
                String rolesStr = (String) roles;
                if (rolesStr.contains(",")) {
                    return rolesStr.split(",\\s*");
                } else {
                    return rolesStr.split("\\s+");
                }
            }
            return new String[0];
        }

        public Object getClaim(String name) {
            return claims.get(name);
        }

        public boolean isExpired(int clockSkewSeconds) {
            Long exp = getExpiration();
            if (exp == null) {
                return false; // No expiration claim
            }
            long now = System.currentTimeMillis() / 1000;
            return now - clockSkewSeconds > exp;
        }

        public java.util.Map<String, Object> getAllClaims() {
            return new java.util.LinkedHashMap<>(claims);
        }
    }

    /**
     * Exception thrown when token validation fails.
     */
    public static class TokenValidationException extends Exception {
        private static final long serialVersionUID = 1L;

        public TokenValidationException(String message) {
            super(message);
        }

        public TokenValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Creates a PublicKey from a Base64-encoded X.509 key specification.
     *
     * @param base64EncodedKey The Base64-encoded public key
     * @param algorithm        The key algorithm (e.g., "RSA")
     * @return The PublicKey object
     * @throws InvalidKeySpecException  if the key spec is invalid
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public static PublicKey createPublicKey(String base64EncodedKey, String algorithm)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePublic(spec);
    }
}
