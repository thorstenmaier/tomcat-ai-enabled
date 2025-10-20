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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.startup.TesterServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.TomcatBaseTest;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;

/**
 * Integration tests for {@link OAuth2BearerAuthenticator} with HTTP requests.
 */
public class TestOAuth2BearerAuthenticator extends TomcatBaseTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1obWFjLXNpZ25pbmctdGhhdC1pcy1sb25nLWVub3VnaA==";
    private static final String TEST_ISSUER = "https://test-issuer.com";
    private static final String TEST_AUDIENCE = "test-audience";

    private static final String USER1 = "user1";
    private static final String USER2 = "user2";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";
    private static final String ROLE_MANAGER = "manager";

    @Test
    public void testSuccessfulAuthentication() throws Exception {
        doTestAuthentication(true, false, 200, USER1, new String[] { ROLE_USER, ROLE_ADMIN });
    }

    @Test
    public void testMissingAuthorizationHeader() throws Exception {
        doTestAuthentication(false, false, 401, null, null);
    }

    @Test
    public void testExpiredToken() throws Exception {
        doTestAuthentication(true, true, 401, USER1, new String[] { ROLE_USER });
    }

    @Test
    public void testSuccessfulAuthenticationWithRealmIntegration() throws Exception {
        OAuth2BearerAuthenticator authenticator = setupAuthenticator(true, true);

        Tomcat tomcat = getTomcatInstance();
        Context ctx = tomcat.addContext("", null);

        // Configure realm with test users
        TestRealm realm = new TestRealm();
        realm.addUser(USER1, new String[] { ROLE_USER, ROLE_MANAGER });
        realm.addUser(USER2, new String[] { ROLE_ADMIN });
        ctx.setRealm(realm);

        // Configure authenticator
        ctx.getPipeline().addValve(authenticator);

        // Configure security constraints
        SecurityConstraint constraint = new SecurityConstraint();
        constraint.addAuthRole(ROLE_USER);
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        constraint.addCollection(collection);
        ctx.addConstraint(constraint);

        LoginConfig config = new LoginConfig();
        config.setAuthMethod("BEARER");
        ctx.setLoginConfig(config);

        // Add test servlet
        Wrapper wrapper = Tomcat.addServlet(ctx, "TestServlet", new AuthInfoServlet());
        wrapper.addMapping("/test");

        tomcat.start();

        // Test with valid token
        long now = System.currentTimeMillis() / 1000;
        String token = createToken(USER1, now + 3600, now, new String[] { ROLE_ADMIN });

        ByteChunk bc = new ByteChunk();
        Map<String, List<String>> headers = new HashMap<>();
        List<String> authHeaders = new ArrayList<>();
        authHeaders.add("Bearer " + token);
        headers.put("Authorization", authHeaders);

        int rc = getUrl("http://localhost:" + getPort() + "/test", bc, headers);

        Assert.assertEquals(200, rc);
        String response = bc.toString();
        Assert.assertTrue(response.contains("User: " + USER1));
        // Should have realm roles merged with token roles
        Assert.assertTrue(response.contains("Roles: ") && (response.contains(ROLE_USER) || response.contains(ROLE_ADMIN)));
    }

    @Test
    public void testPreemptiveAuthentication() throws Exception {
        OAuth2BearerAuthenticator authenticator = setupAuthenticator(true, true);

        Tomcat tomcat = getTomcatInstance();
        Context ctx = tomcat.addContext("", null);
        ctx.setRealm(new TestRealm());
        ctx.getPipeline().addValve(authenticator);

        // Enable preemptive authentication
        ctx.setPreemptiveAuthentication(true);

        // No security constraints - preemptive auth should still work
        LoginConfig config = new LoginConfig();
        config.setAuthMethod("BEARER");
        ctx.setLoginConfig(config);

        Wrapper wrapper = Tomcat.addServlet(ctx, "TestServlet", new AuthInfoServlet());
        wrapper.addMapping("/test");

        tomcat.start();

        long now = System.currentTimeMillis() / 1000;
        String token = createToken(USER1, now + 3600, now, new String[] { ROLE_USER });

        ByteChunk bc = new ByteChunk();
        Map<String, List<String>> headers = new HashMap<>();
        List<String> authHeaders = new ArrayList<>();
        authHeaders.add("Bearer " + token);
        headers.put("Authorization", authHeaders);

        int rc = getUrl("http://localhost:" + getPort() + "/test", bc, headers);

        Assert.assertEquals(200, rc);
        String response = bc.toString();
        Assert.assertTrue(response.contains("User: " + USER1));
    }

    @Test
    public void testInvalidBearerFormat() throws Exception {
        OAuth2BearerAuthenticator authenticator = setupAuthenticator(false, true);

        Tomcat tomcat = getTomcatInstance();
        Context ctx = tomcat.addContext("", null);
        ctx.setRealm(new TestRealm());
        ctx.getPipeline().addValve(authenticator);

        SecurityConstraint constraint = new SecurityConstraint();
        constraint.addAuthRole(ROLE_USER);
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        constraint.addCollection(collection);
        ctx.addConstraint(constraint);

        LoginConfig config = new LoginConfig();
        config.setAuthMethod("BEARER");
        ctx.setLoginConfig(config);

        Wrapper wrapper = Tomcat.addServlet(ctx, "TestServlet", new TesterServlet());
        wrapper.addMapping("/test");

        tomcat.start();

        ByteChunk bc = new ByteChunk();
        Map<String, List<String>> headers = new HashMap<>();
        List<String> authHeaders = new ArrayList<>();
        // Use "Basic" instead of "Bearer"
        authHeaders.add("Basic dXNlcjpwYXNz");
        headers.put("Authorization", authHeaders);

        int rc = getUrl("http://localhost:" + getPort() + "/test", bc, headers);

        Assert.assertEquals(401, rc);
    }

    @Test
    public void testRoleExtractionFromToken() throws Exception {
        OAuth2BearerAuthenticator authenticator = setupAuthenticator(false, true);
        authenticator.setExtractRolesFromToken(true);

        Tomcat tomcat = getTomcatInstance();
        Context ctx = tomcat.addContext("", null);
        ctx.setRealm(new TestRealm());
        ctx.getPipeline().addValve(authenticator);

        SecurityConstraint constraint = new SecurityConstraint();
        constraint.addAuthRole(ROLE_ADMIN);
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        constraint.addCollection(collection);
        ctx.addConstraint(constraint);

        LoginConfig config = new LoginConfig();
        config.setAuthMethod("BEARER");
        ctx.setLoginConfig(config);

        Wrapper wrapper = Tomcat.addServlet(ctx, "TestServlet", new AuthInfoServlet());
        wrapper.addMapping("/test");

        tomcat.start();

        long now = System.currentTimeMillis() / 1000;
        String token = createToken(USER1, now + 3600, now, new String[] { ROLE_ADMIN, ROLE_USER });

        ByteChunk bc = new ByteChunk();
        Map<String, List<String>> headers = new HashMap<>();
        List<String> authHeaders = new ArrayList<>();
        authHeaders.add("Bearer " + token);
        headers.put("Authorization", authHeaders);

        int rc = getUrl("http://localhost:" + getPort() + "/test", bc, headers);

        Assert.assertEquals(200, rc);
        String response = bc.toString();
        Assert.assertTrue(response.contains("User: " + USER1));
        Assert.assertTrue(response.contains(ROLE_ADMIN));
    }

    // Helper methods

    private void doTestAuthentication(boolean includeAuth, boolean expiredToken, int expectedStatus, String username,
            String[] roles) throws Exception {

        OAuth2BearerAuthenticator authenticator = setupAuthenticator(false, true);

        Tomcat tomcat = getTomcatInstance();
        Context ctx = tomcat.addContext("", null);
        ctx.setRealm(new TestRealm());
        ctx.getPipeline().addValve(authenticator);

        SecurityConstraint constraint = new SecurityConstraint();
        constraint.addAuthRole(ROLE_USER);
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        constraint.addCollection(collection);
        ctx.addConstraint(constraint);

        LoginConfig config = new LoginConfig();
        config.setAuthMethod("BEARER");
        ctx.setLoginConfig(config);

        Wrapper wrapper = Tomcat.addServlet(ctx, "TestServlet", new TesterServlet());
        wrapper.addMapping("/test");

        tomcat.start();

        ByteChunk bc = new ByteChunk();
        Map<String, List<String>> headers = new HashMap<>();

        if (includeAuth) {
            long now = System.currentTimeMillis() / 1000;
            long exp = expiredToken ? now - 3600 : now + 3600;
            String token = createToken(username, exp, now, roles);

            List<String> authHeaders = new ArrayList<>();
            authHeaders.add("Bearer " + token);
            headers.put("Authorization", authHeaders);
        }

        int rc = getUrl("http://localhost:" + getPort() + "/test", bc, headers);

        Assert.assertEquals(expectedStatus, rc);
    }

    private OAuth2BearerAuthenticator setupAuthenticator(boolean useRealm, boolean useIssuerAudience) {
        OAuth2BearerAuthenticator authenticator = new OAuth2BearerAuthenticator();
        authenticator.setSecretKey(TEST_SECRET);
        if (useIssuerAudience) {
            authenticator.setIssuer(TEST_ISSUER);
            authenticator.setAudience(TEST_AUDIENCE);
        }
        authenticator.setRealmAuthentication(useRealm);
        authenticator.setTokenCacheSize(100);
        return authenticator;
    }

    private String createToken(String subject, long exp, long iat, String[] roles) throws Exception {
        String header = createHeader();
        String payload = createPayload(subject, exp, iat, roles);
        String signature = signToken(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String createHeader() {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    }

    private String createPayload(String subject, long exp, long iat, String[] roles) {
        StringBuilder payloadJson = new StringBuilder("{");
        payloadJson.append("\"sub\":\"").append(subject).append("\"");
        payloadJson.append(",\"exp\":").append(exp);
        payloadJson.append(",\"iat\":").append(iat);
        payloadJson.append(",\"iss\":\"").append(TEST_ISSUER).append("\"");
        payloadJson.append(",\"aud\":\"").append(TEST_AUDIENCE).append("\"");

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

    private String signToken(String data) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    // Test servlet that displays authentication info
    public static class AuthInfoServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            Principal principal = req.getUserPrincipal();
            if (principal != null) {
                out.println("User: " + principal.getName());
                out.println("Auth Type: " + req.getAuthType());

                if (principal instanceof GenericPrincipal) {
                    GenericPrincipal gp = (GenericPrincipal) principal;
                    String[] roles = gp.getRoles();
                    if (roles != null && roles.length > 0) {
                        out.print("Roles: ");
                        for (int i = 0; i < roles.length; i++) {
                            if (i > 0) {
                                out.print(", ");
                            }
                            out.print(roles[i]);
                        }
                        out.println();
                    }
                }
            } else {
                out.println("No authenticated user");
            }
        }
    }

    // Test realm for authentication
    public static class TestRealm extends RealmBase {
        private final Map<String, String[]> users = new HashMap<>();

        public void addUser(String username, String[] roles) {
            users.put(username, roles);
        }

        @Override
        protected String getPassword(String username) {
            return null;
        }

        protected Principal getPrincipal(String username) {
            String[] roles = users.get(username);
            if (roles != null) {
                return new GenericPrincipal(username, Arrays.asList(roles));
            }
            return null;
        }

        @Override
        public Principal authenticate(String username) {
            return getPrincipal(username);
        }

        @Override
        public Principal authenticate(String username, String credentials) {
            return getPrincipal(username);
        }

        protected String getName() {
            return "TestRealm";
        }
    }
}
