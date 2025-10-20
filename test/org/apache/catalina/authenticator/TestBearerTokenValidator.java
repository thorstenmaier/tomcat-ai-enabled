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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.catalina.authenticator.BearerTokenValidator.TokenClaims;
import org.apache.catalina.authenticator.BearerTokenValidator.TokenValidationException;

/**
 * Unit tests for {@link BearerTokenValidator}.
 */
public class TestBearerTokenValidator {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1obWFjLXNpZ25pbmctdGhhdC1pcy1sb25nLWVub3VnaA==";
    private static final String TEST_ISSUER = "https://test-issuer.com";
    private static final String TEST_AUDIENCE = "test-audience";

    private KeyPair rsaKeyPair;

    @Before
    public void setUp() throws Exception {
        // Generate RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        rsaKeyPair = keyGen.generateKeyPair();
    }

    @Test
    public void testValidHS256Token() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 60, 0);

        long now = System.currentTimeMillis() / 1000;
        String token = createHmacToken("HS256", "testuser", now + 3600, now, TEST_ISSUER, TEST_AUDIENCE,
                new String[] { "user", "admin" });

        TokenClaims claims = validator.validate(token);
        Assert.assertNotNull(claims);
        Assert.assertEquals("testuser", claims.getSubject());
        Assert.assertEquals(TEST_ISSUER, claims.getIssuer());
        Assert.assertEquals(TEST_AUDIENCE, claims.getAudience());
        Assert.assertArrayEquals(new String[] { "user", "admin" }, claims.getRoles());
    }

    @Test
    public void testValidRS256Token() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(rsaKeyPair.getPublic(), null, null, 60, 0);

        long now = System.currentTimeMillis() / 1000;
        String token = createRsaToken("RS256", "testuser", now + 3600, now, TEST_ISSUER, TEST_AUDIENCE,
                new String[] { "user" }, rsaKeyPair.getPrivate());

        TokenClaims claims = validator.validate(token);
        Assert.assertNotNull(claims);
        Assert.assertEquals("testuser", claims.getSubject());
    }

    @Test
    public void testExpiredToken() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 0, 0);

        long pastTime = (System.currentTimeMillis() / 1000) - 3600; // 1 hour ago
        String token = createHmacToken("HS256", "testuser", pastTime, pastTime - 7200, null, null, null);

        try {
            validator.validate(token);
            Assert.fail("Should have thrown TokenValidationException for expired token");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("expired"));
        }
    }

    @Test
    public void testNotYetValidToken() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 0, 0);

        long now = System.currentTimeMillis() / 1000;
        long futureTime = now + 3600; // 1 hour from now
        // Create a token with nbf in the future
        String token = createTokenWithNbf("HS256", "testuser", futureTime + 3600, now, futureTime, null, null, null);

        try {
            validator.validate(token);
            Assert.fail("Should have thrown TokenValidationException for not-yet-valid token");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("not yet valid"));
        }
    }

    @Test
    public void testInvalidSignature() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 60, 0);

        long now = System.currentTimeMillis() / 1000;
        String token = createHmacToken("HS256", "testuser", now + 3600, now, null, null, null);

        // Tamper with the payload (change username) without recalculating signature
        String[] parts = token.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"hacker\"}".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        try {
            validator.validate(tamperedToken);
            Assert.fail("Should have thrown TokenValidationException for invalid signature");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("signature") || e.getMessage().contains("Base64") ||
                    e.getMessage().contains("JSON"));
        }
    }

    @Test
    public void testInvalidIssuer() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, TEST_ISSUER, null, 60, 0);

        long now = System.currentTimeMillis() / 1000;
        String token = createHmacToken("HS256", "testuser", now + 3600, now, "https://wrong-issuer.com", null, null);

        try {
            validator.validate(token);
            Assert.fail("Should have thrown TokenValidationException for invalid issuer");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("issuer"));
        }
    }

    @Test
    public void testInvalidAudience() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, TEST_AUDIENCE, 60, 0);

        long now = System.currentTimeMillis() / 1000;
        String token = createHmacToken("HS256", "testuser", now + 3600, now, null, "wrong-audience", null);

        try {
            validator.validate(token);
            Assert.fail("Should have thrown TokenValidationException for invalid audience");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("audience"));
        }
    }

    @Test
    public void testTokenCaching() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 60, 100);

        long now = System.currentTimeMillis() / 1000;
        String token = createHmacToken("HS256", "testuser", now + 3600, now, null, null, null);

        // First validation
        TokenClaims claims1 = validator.validate(token);
        Assert.assertNotNull(claims1);

        // Second validation should return cached result
        TokenClaims claims2 = validator.validate(token);
        Assert.assertNotNull(claims2);
        Assert.assertEquals(claims1.getSubject(), claims2.getSubject());
    }

    @Test
    public void testClockSkew() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 120, 0);

        long now = System.currentTimeMillis() / 1000;
        // Token expired 60 seconds ago, but with 120 seconds clock skew it should still be valid
        String token = createHmacToken("HS256", "testuser", now - 60, now - 3600, null, null, null);

        TokenClaims claims = validator.validate(token);
        Assert.assertNotNull(claims);
        Assert.assertEquals("testuser", claims.getSubject());
    }

    @Test
    public void testRolesParsing() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 60, 0);

        long now = System.currentTimeMillis() / 1000;

        // Test array of roles
        String token1 = createHmacToken("HS256", "user1", now + 3600, now, null, null,
                new String[] { "admin", "user", "manager" });
        TokenClaims claims1 = validator.validate(token1);
        Assert.assertArrayEquals(new String[] { "admin", "user", "manager" }, claims1.getRoles());

        // Test comma-separated roles
        String token2 = createTokenWithCustomRoles("HS256", "user2", now + 3600, now, "admin,user,manager");
        TokenClaims claims2 = validator.validate(token2);
        Assert.assertArrayEquals(new String[] { "admin", "user", "manager" }, claims2.getRoles());

        // Test space-separated roles
        String token3 = createTokenWithCustomRoles("HS256", "user3", now + 3600, now, "admin user manager");
        TokenClaims claims3 = validator.validate(token3);
        Assert.assertArrayEquals(new String[] { "admin", "user", "manager" }, claims3.getRoles());
    }

    @Test
    public void testInvalidTokenStructure() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 60, 0);

        // Token with only 2 parts
        try {
            validator.validate("header.payload");
            Assert.fail("Should have thrown TokenValidationException for invalid structure");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("three parts"));
        }

        // Token with 4 parts
        try {
            validator.validate("header.payload.signature.extra");
            Assert.fail("Should have thrown TokenValidationException for invalid structure");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("three parts"));
        }
    }

    @Test
    public void testNullToken() throws Exception {
        BearerTokenValidator validator = new BearerTokenValidator(TEST_SECRET, null, null, 60, 0);

        try {
            validator.validate(null);
            Assert.fail("Should have thrown TokenValidationException for null token");
        } catch (TokenValidationException e) {
            Assert.assertTrue(e.getMessage().contains("empty"));
        }
    }

    // Helper methods to create test tokens

    private String createHmacToken(String alg, String subject, long exp, long iat, String issuer, String audience,
            String[] roles) throws Exception {
        return createTokenWithNbf(alg, subject, exp, iat, 0, issuer, audience, roles);
    }

    private String createTokenWithNbf(String alg, String subject, long exp, long iat, long nbf, String issuer,
            String audience, String[] roles) throws Exception {
        String header = createHeader(alg);
        String payload = createPayloadWithNbf(subject, exp, iat, nbf, issuer, audience, roles);
        String signature = signHmac(alg, header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String createRsaToken(String alg, String subject, long exp, long iat, String issuer, String audience,
            String[] roles, PrivateKey privateKey) throws Exception {
        String header = createHeader(alg);
        String payload = createPayload(subject, exp, iat, issuer, audience, roles);
        String signature = signRsa(alg, header + "." + payload, privateKey);
        return header + "." + payload + "." + signature;
    }

    private String createTokenWithCustomRoles(String alg, String subject, long exp, long iat, String rolesString)
            throws Exception {
        String header = createHeader(alg);
        StringBuilder payloadJson = new StringBuilder("{");
        payloadJson.append("\"sub\":\"").append(subject).append("\"");
        if (exp > 0) {
            payloadJson.append(",\"exp\":").append(exp);
        }
        if (iat > 0) {
            payloadJson.append(",\"iat\":").append(iat);
        }
        if (rolesString != null) {
            payloadJson.append(",\"roles\":\"").append(rolesString).append("\"");
        }
        payloadJson.append("}");
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.toString().getBytes(StandardCharsets.UTF_8));
        String signature = signHmac(alg, header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String createHeader(String alg) {
        String headerJson = "{\"alg\":\"" + alg + "\",\"typ\":\"JWT\"}";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    }

    private String createPayload(String subject, long exp, long iat, String issuer, String audience, String[] roles) {
        return createPayloadWithNbf(subject, exp, iat, 0, issuer, audience, roles);
    }

    private String createPayloadWithNbf(String subject, long exp, long iat, long nbf, String issuer, String audience,
            String[] roles) {
        StringBuilder payloadJson = new StringBuilder("{");
        payloadJson.append("\"sub\":\"").append(subject).append("\"");
        if (exp > 0) {
            payloadJson.append(",\"exp\":").append(exp);
        }
        if (iat > 0) {
            payloadJson.append(",\"iat\":").append(iat);
        }
        if (nbf > 0) {
            payloadJson.append(",\"nbf\":").append(nbf);
        }
        if (issuer != null) {
            payloadJson.append(",\"iss\":\"").append(issuer).append("\"");
        }
        if (audience != null) {
            payloadJson.append(",\"aud\":\"").append(audience).append("\"");
        }
        if (roles != null && roles.length > 0) {
            payloadJson.append(",\"roles\":[");
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) {
                    payloadJson.append(",");
                }
                payloadJson.append("\"").append(roles[i]).append("\"");
            }
            payloadJson.append("]");
        }
        payloadJson.append("}");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String signHmac(String alg, String data) throws Exception {
        String javaAlg = alg.replace("HS", "HmacSHA");
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, javaAlg);
        Mac mac = Mac.getInstance(javaAlg);
        mac.init(keySpec);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private String signRsa(String alg, String data, PrivateKey privateKey) throws Exception {
        String javaAlg = alg.replace("RS", "SHA") + "withRSA";
        Signature signature = Signature.getInstance(javaAlg);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    }
}
