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

import java.io.IOException;
import java.security.Principal;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.MessageBytes;

/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of OAuth2 Bearer Token Authentication,
 * as described in RFC 6750: "The OAuth 2.0 Authorization Framework: Bearer Token Usage".
 * <p>
 * This authenticator validates JWT bearer tokens from the Authorization header and integrates
 * with Tomcat's existing Realm infrastructure. It supports:
 * <ul>
 * <li>JWT token validation with HMAC (HS256, HS384, HS512) and RSA (RS256, RS384, RS512) signatures</li>
 * <li>Token expiration and not-before validation with configurable clock skew</li>
 * <li>Issuer and audience validation</li>
 * <li>Role extraction from JWT claims</li>
 * <li>Token caching for improved performance</li>
 * <li>Integration with existing Realm for user lookup and authorization</li>
 * </ul>
 * <p>
 * Configuration example in context.xml:
 * <pre>
 * &lt;Valve className="org.apache.catalina.authenticator.OAuth2BearerAuthenticator"
 *        secretKey="base64-encoded-secret-key"
 *        issuer="https://your-issuer.com"
 *        audience="your-audience"
 *        clockSkewSeconds="60"
 *        tokenCacheSize="1000"
 *        realmAuthentication="true" /&gt;
 * </pre>
 *
 * @author Apache Tomcat Team
 */
public class OAuth2BearerAuthenticator extends AuthenticatorBase {

    private final Log log = LogFactory.getLog(OAuth2BearerAuthenticator.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_METHOD = "BEARER";

    // Configuration properties
    private String secretKey = null;
    private String publicKeyEncoded = null;
    private String publicKeyAlgorithm = "RSA";
    private String issuer = null;
    private String audience = null;
    private int clockSkewSeconds = 60;
    private int tokenCacheSize = 1000;
    private String rolesClaimName = "roles";
    private boolean realmAuthentication = true;
    private boolean extractRolesFromToken = true;

    // Runtime components
    private BearerTokenValidator tokenValidator = null;

    // Getters and setters for configuration

    /**
     * Sets the secret key for HMAC-based JWT validation (Base64-encoded).
     * Use this for HS256, HS384, or HS512 algorithms.
     *
     * @param secretKey The Base64-encoded secret key
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Sets the public key for RSA-based JWT validation (Base64-encoded X.509 format).
     * Use this for RS256, RS384, or RS512 algorithms.
     *
     * @param publicKeyEncoded The Base64-encoded public key
     */
    public void setPublicKey(String publicKeyEncoded) {
        this.publicKeyEncoded = publicKeyEncoded;
    }

    public String getPublicKey() {
        return publicKeyEncoded;
    }

    /**
     * Sets the algorithm for the public key (default: RSA).
     *
     * @param publicKeyAlgorithm The public key algorithm
     */
    public void setPublicKeyAlgorithm(String publicKeyAlgorithm) {
        this.publicKeyAlgorithm = publicKeyAlgorithm;
    }

    public String getPublicKeyAlgorithm() {
        return publicKeyAlgorithm;
    }

    /**
     * Sets the expected token issuer for validation.
     * If null or empty, issuer validation is skipped.
     *
     * @param issuer The expected issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getIssuer() {
        return issuer;
    }

    /**
     * Sets the expected token audience for validation.
     * If null or empty, audience validation is skipped.
     *
     * @param audience The expected audience
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getAudience() {
        return audience;
    }

    /**
     * Sets the allowed clock skew in seconds for time-based validations.
     * Default is 60 seconds.
     *
     * @param clockSkewSeconds The clock skew in seconds
     */
    public void setClockSkewSeconds(int clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public int getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    /**
     * Sets the maximum number of tokens to cache for improved performance.
     * Set to 0 to disable caching. Default is 1000.
     *
     * @param tokenCacheSize The maximum cache size
     */
    public void setTokenCacheSize(int tokenCacheSize) {
        this.tokenCacheSize = tokenCacheSize;
    }

    public int getTokenCacheSize() {
        return tokenCacheSize;
    }

    /**
     * Sets the name of the JWT claim containing user roles.
     * Default is "roles".
     *
     * @param rolesClaimName The roles claim name
     */
    public void setRolesClaimName(String rolesClaimName) {
        this.rolesClaimName = rolesClaimName;
    }

    public String getRolesClaimName() {
        return rolesClaimName;
    }

    /**
     * Enables or disables realm-based authentication after token validation.
     * If true, the username from the token will be authenticated against the realm.
     * If false, a GenericPrincipal will be created directly from the token claims.
     * Default is true.
     *
     * @param realmAuthentication Whether to use realm authentication
     */
    public void setRealmAuthentication(boolean realmAuthentication) {
        this.realmAuthentication = realmAuthentication;
    }

    public boolean getRealmAuthentication() {
        return realmAuthentication;
    }

    /**
     * Enables or disables role extraction from the JWT token.
     * If true, roles are extracted from the token's roles claim.
     * If false, only realm-provided roles are used (requires realmAuthentication=true).
     * Default is true.
     *
     * @param extractRolesFromToken Whether to extract roles from token
     */
    public void setExtractRolesFromToken(boolean extractRolesFromToken) {
        this.extractRolesFromToken = extractRolesFromToken;
    }

    public boolean getExtractRolesFromToken() {
        return extractRolesFromToken;
    }

    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();

        // Initialize token validator based on configuration
        try {
            if (secretKey != null && !secretKey.isEmpty()) {
                // HMAC-based validation
                tokenValidator = new BearerTokenValidator(secretKey, issuer, audience, clockSkewSeconds, tokenCacheSize);
                log.info(sm.getString("oauth2BearerAuthenticator.hmacValidatorConfigured"));
            } else if (publicKeyEncoded != null && !publicKeyEncoded.isEmpty()) {
                // RSA-based validation
                PublicKey publicKey = BearerTokenValidator.createPublicKey(publicKeyEncoded, publicKeyAlgorithm);
                tokenValidator =
                        new BearerTokenValidator(publicKey, issuer, audience, clockSkewSeconds, tokenCacheSize);
                log.info(sm.getString("oauth2BearerAuthenticator.rsaValidatorConfigured"));
            } else {
                throw new LifecycleException(sm.getString("oauth2BearerAuthenticator.noKeyConfigured"));
            }
        } catch (Exception e) {
            throw new LifecycleException(sm.getString("oauth2BearerAuthenticator.validatorInitFailed"), e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        if (tokenValidator != null) {
            tokenValidator.clearCache();
        }
        super.stopInternal();
    }

    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response) throws IOException {

        // Check for cached authentication
        if (checkForCachedAuthentication(request, response, true)) {
            return true;
        }

        // Extract Authorization header
        MessageBytes authorization = request.getCoyoteRequest().getMimeHeaders().getValue("authorization");

        if (authorization == null) {
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("oauth2BearerAuthenticator.missingAuthHeader"));
            }
            sendUnauthorizedResponse(response, "Bearer realm=\"" + getRealmName(context) + "\"");
            return false;
        }

        authorization.toBytes();
        String authHeader = authorization.toString();

        // Check for Bearer prefix
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("oauth2BearerAuthenticator.notBearerToken"));
            }
            sendUnauthorizedResponse(response, "Bearer realm=\"" + getRealmName(context) + "\"");
            return false;
        }

        // Extract token
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("oauth2BearerAuthenticator.emptyToken"));
            }
            sendUnauthorizedResponse(response, "Bearer realm=\"" + getRealmName(context) + "\", " +
                    "error=\"invalid_token\", error_description=\"The access token is empty\"");
            return false;
        }

        // Validate token
        BearerTokenValidator.TokenClaims claims;
        try {
            claims = tokenValidator.validate(token);
        } catch (BearerTokenValidator.TokenValidationException e) {
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("oauth2BearerAuthenticator.tokenValidationFailed"), e);
            }
            sendUnauthorizedResponse(response, "Bearer realm=\"" + getRealmName(context) + "\", " +
                    "error=\"invalid_token\", error_description=\"" + escapeQuotes(e.getMessage()) + "\"");
            return false;
        }

        // Extract username from token
        String username = claims.getSubject();
        if (username == null || username.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(sm.getString("oauth2BearerAuthenticator.missingSubject"));
            }
            sendUnauthorizedResponse(response, "Bearer realm=\"" + getRealmName(context) + "\", " +
                    "error=\"invalid_token\", error_description=\"Token missing 'sub' claim\"");
            return false;
        }

        // Authenticate user
        Principal principal;
        if (realmAuthentication) {
            // Authenticate against realm
            principal = context.getRealm().authenticate(username);
            if (principal == null) {
                if (log.isDebugEnabled()) {
                    log.debug(sm.getString("oauth2BearerAuthenticator.realmAuthFailed", username));
                }
                sendUnauthorizedResponse(response, "Bearer realm=\"" + getRealmName(context) + "\", " +
                        "error=\"invalid_token\", error_description=\"User not found in realm\"");
                return false;
            }

            // If extractRolesFromToken is true and principal is GenericPrincipal,
            // merge token roles with realm roles
            if (extractRolesFromToken && principal instanceof GenericPrincipal) {
                GenericPrincipal gp = (GenericPrincipal) principal;
                String[] tokenRoles = getRolesFromClaims(claims);
                List<String> mergedRoles = mergeRoles(gp.getRoles(), tokenRoles);
                principal = new GenericPrincipal(username, mergedRoles);
            }
        } else {
            // Create principal directly from token claims
            List<String> roles = new ArrayList<>();
            if (extractRolesFromToken) {
                String[] tokenRoles = getRolesFromClaims(claims);
                for (String role : tokenRoles) {
                    roles.add(role);
                }
            }
            principal = new GenericPrincipal(username, roles);
        }

        // Register authentication
        register(request, response, principal, AUTH_METHOD, username, null);

        if (log.isDebugEnabled()) {
            log.debug(sm.getString("oauth2BearerAuthenticator.authenticated", username));
        }

        return true;
    }

    /**
     * Extracts roles from token claims based on the configured roles claim name.
     */
    private String[] getRolesFromClaims(BearerTokenValidator.TokenClaims claims) {
        // First try the configured roles claim name
        Object rolesObj = claims.getClaim(rolesClaimName);

        if (rolesObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> roleList = (java.util.List<String>) rolesObj;
            return roleList.toArray(new String[0]);
        } else if (rolesObj instanceof String) {
            String rolesStr = (String) rolesObj;
            if (rolesStr.contains(",")) {
                return rolesStr.split(",\\s*");
            } else {
                return rolesStr.split("\\s+");
            }
        }

        // Fall back to standard 'roles' claim if different claim name was used
        if (!"roles".equals(rolesClaimName)) {
            return claims.getRoles();
        }

        return new String[0];
    }

    /**
     * Merges roles from two arrays, removing duplicates.
     */
    private List<String> mergeRoles(String[] roles1, String[] roles2) {
        List<String> merged = new ArrayList<>();

        if (roles1 != null) {
            for (String role : roles1) {
                if (!merged.contains(role)) {
                    merged.add(role);
                }
            }
        }

        if (roles2 != null) {
            for (String role : roles2) {
                if (!merged.contains(role)) {
                    merged.add(role);
                }
            }
        }

        return merged;
    }

    /**
     * Sends an HTTP 401 Unauthorized response with the specified WWW-Authenticate header.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String authenticateHeader) throws IOException {
        response.setHeader(AUTH_HEADER_NAME, authenticateHeader);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Escapes double quotes in a string for use in HTTP header values.
     */
    private String escapeQuotes(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\"", "\\\"");
    }

    @Override
    protected String getAuthMethod() {
        return AUTH_METHOD;
    }

    @Override
    protected boolean isPreemptiveAuthPossible(Request request) {
        MessageBytes authorizationHeader = request.getCoyoteRequest().getMimeHeaders().getValue("authorization");
        return authorizationHeader != null && authorizationHeader.startsWithIgnoreCase(BEARER_PREFIX, 0);
    }
}
