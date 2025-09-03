# Security & Authentication Domain

## Purpose
Handles authentication, authorization, SSL/TLS, and security constraints across the entire servlet container.

## Architecture Overview
```
Request → Security Constraint Check → Authentication → Authorization → Resource Access
    ↓              ↓                     ↓               ↓              ↓
  Filter      Valve Pipeline        Authenticator    Realm Check    Protected Resource
```

## Key Components

### Authentication Framework
- **`AuthenticatorBase`** - Abstract base for all authenticators
- **`BasicAuthenticator`** - HTTP Basic authentication (RFC 7617)
- **`DigestAuthenticator`** - HTTP Digest authentication (RFC 7616)
- **`FormAuthenticator`** - HTML form-based authentication
- **`SSLAuthenticator`** - SSL client certificate authentication
- **`SpnegoAuthenticator`** - Kerberos/SPNEGO authentication

### Authorization & Realms
- **`RealmBase`** - Abstract base for user/role repositories
- **`MemoryRealm`** - XML file-based user store
- **`JNDIRealm`** - LDAP/Active Directory integration
- **`DataSourceRealm`** - Database-backed user store
- **`JAASRealm`** - Java Authentication and Authorization Service
- **`CombinedRealm`** - Multiple realm chaining

### Security Constraints
- **`SecurityConstraint`** - URL pattern protection rules
- **`SecurityCollection`** - Resource collection definitions
- **`LoginConfig`** - Authentication method configuration

## Authentication Flow Detailed

### 1. Request Interception
```java
// In pipeline processing
if (hasSecurityConstraints) {
    if (!authenticate(request, response)) {
        return; // Challenge sent to client
    }
}
```

### 2. Authenticator Selection
Based on `<login-config>` in web.xml:
- **BASIC** → `BasicAuthenticator`
- **DIGEST** → `DigestAuthenticator`
- **FORM** → `FormAuthenticator`
- **CLIENT-CERT** → `SSLAuthenticator`

### 3. Credential Extraction
```java
// Basic authentication example
String authorization = request.getHeader("Authorization");
if (authorization != null && authorization.startsWith("Basic ")) {
    String credentials = new String(Base64.decode(
        authorization.substring(6).getBytes()));
    int colon = credentials.indexOf(':');
    username = credentials.substring(0, colon);
    password = credentials.substring(colon + 1);
}
```

### 4. Realm Validation
```java
Principal principal = realm.authenticate(username, password);
if (principal != null) {
    register(request, response, principal, authType, username);
}
```

## Security Configuration

### Web.xml Security Constraints
```xml
<security-constraint>
    <web-resource-collection>
        <web-resource-name>Admin Area</web-resource-name>
        <url-pattern>/admin/*</url-pattern>
        <http-method>GET</http-method>
        <http-method>POST</http-method>
    </web-resource-collection>
    <auth-constraint>
        <role-name>admin</role-name>
    </auth-constraint>
    <user-data-constraint>
        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
</security-constraint>

<login-config>
    <auth-method>FORM</auth-method>
    <form-login-config>
        <form-login-page>/login.html</form-login-page>
        <form-error-page>/login-error.html</form-error-page>
    </form-login-config>
</login-config>
```

### Server.xml Realm Configuration
```xml
<!-- Database realm -->
<Realm className="org.apache.catalina.realm.DataSourceRealm"
       dataSourceName="jdbc/MyDS"
       userTable="users" 
       userNameCol="username"
       userCredCol="password"
       userRoleTable="user_roles"
       roleNameCol="rolename"/>

<!-- LDAP realm -->
<Realm className="org.apache.catalina.realm.JNDIRealm"
       connectionURL="ldap://ldap.example.com:389"
       userPattern="uid={0},ou=people,dc=example,dc=com"
       roleBase="ou=groups,dc=example,dc=com"
       roleName="cn"
       roleSearch="(uniqueMember={0})"/>
```

## SSL/TLS Configuration

### Connector Configuration
```xml
<Connector port="8443" protocol="HTTP/1.1"
           SSLEnabled="true" scheme="https" secure="true">
    <SSLHostConfig>
        <Certificate certificateKeystoreFile="keystore.jks"
                     certificateKeystorePassword="password"
                     certificateKeyAlias="tomcat"
                     type="RSA"/>
    </SSLHostConfig>
</Connector>
```

### SSL Context Configuration
```java
// Programmatic SSL configuration
SSLHostConfig sslHostConfig = new SSLHostConfig();
SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(
    sslHostConfig, SSLHostConfigCertificate.Type.RSA);
certificate.setCertificateKeystoreFile("keystore.jks");
certificate.setCertificateKeystorePassword("password");
```

## Common Security Patterns

### Custom Authenticator Implementation
```java
public class CustomAuthenticator extends AuthenticatorBase {
    
    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response)
            throws IOException {
        
        // Extract custom credentials
        String token = request.getHeader("X-Auth-Token");
        if (token == null) {
            sendUnauthorized(request, response, "Missing token");
            return false;
        }
        
        // Validate token
        Principal principal = validateToken(token);
        if (principal == null) {
            sendUnauthorized(request, response, "Invalid token");
            return false;
        }
        
        // Register authenticated user
        register(request, response, principal, "TOKEN", token);
        return true;
    }
    
    @Override
    protected String getAuthMethod() {
        return "TOKEN";
    }
    
    private void sendUnauthorized(Request request, HttpServletResponse response, 
                                 String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Token realm=\"Protected\"");
        response.getWriter().println(message);
    }
}
```

### Custom Realm Implementation
```java
public class CustomRealm extends RealmBase {
    
    @Override
    protected String getPassword(String username) {
        // Retrieve password hash from custom source
        return userService.getPasswordHash(username);
    }
    
    @Override
    protected Principal getPrincipal(String username) {
        User user = userService.findByUsername(username);
        if (user == null) return null;
        
        return new GenericPrincipal(username, user.getPassword(), 
                                   user.getRoles());
    }
    
    @Override
    public boolean hasRole(Wrapper wrapper, Principal principal, String role) {
        if (principal instanceof GenericPrincipal) {
            return ((GenericPrincipal) principal).hasRole(role);
        }
        return false;
    }
}
```

## Security Filters

### CSRF Prevention
```java
// Built-in CSRF filter
<filter>
    <filter-name>CsrfPreventionFilter</filter-name>
    <filter-class>org.apache.catalina.filters.CsrfPreventionFilter</filter-class>
    <init-param>
        <param-name>entryPoints</param-name>
        <param-value>/login,/logout</param-value>
    </init-param>
</filter>
```

### CORS Support
```java
<filter>
    <filter-name>CorsFilter</filter-name>
    <filter-class>org.apache.catalina.filters.CorsFilter</filter-class>
    <init-param>
        <param-name>cors.allowed.origins</param-name>
        <param-value>https://example.com</param-value>
    </init-param>
    <init-param>
        <param-name>cors.allowed.methods</param-name>
        <param-value>GET,POST,HEAD,OPTIONS,PUT</param-value>
    </init-param>
</filter>
```

## Security Best Practices

### Password Handling
```java
// Secure password comparison
public boolean authenticate(String username, String password) {
    String storedHash = getPassword(username);
    if (storedHash == null) return false;
    
    // Use time-constant comparison
    return MessageDigest.isEqual(
        hashPassword(password).getBytes(),
        storedHash.getBytes()
    );
}
```

### Session Security
```java
// Secure session configuration
<session-config>
    <session-timeout>30</session-timeout>
    <cookie-config>
        <http-only>true</http-only>
        <secure>true</secure>
        <same-site>strict</same-site>
    </cookie-config>
    <tracking-mode>COOKIE</tracking-mode>
</session-config>
```

## Performance Considerations

### Caching Authentication Results
```java
// Cache principal lookups
private final Map<String, CachedPrincipal> principalCache = 
    new ConcurrentHashMap<>();

@Override
protected Principal authenticate(String username, String credentials) {
    CachedPrincipal cached = principalCache.get(username);
    if (cached != null && !cached.isExpired()) {
        if (cached.validateCredentials(credentials)) {
            return cached.getPrincipal();
        }
    }
    
    // Perform expensive authentication
    Principal principal = super.authenticate(username, credentials);
    if (principal != null) {
        principalCache.put(username, new CachedPrincipal(principal, credentials));
    }
    
    return principal;
}
```

### SSL Performance
- Use SSL session caching
- Configure appropriate cipher suites
- Consider SSL termination at load balancer

## Testing Security Components

### Authentication Testing
```java
@Test
public void testBasicAuthentication() throws Exception {
    // Setup Basic auth
    context.setLoginConfig(new LoginConfig("BASIC", "Test", null, null));
    context.getPipeline().addValve(new BasicAuthenticator());
    
    // Add security constraint
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addAuthRole("user");
    SecurityCollection collection = new SecurityCollection();
    collection.addPattern("/protected/*");
    constraint.addCollection(collection);
    context.addConstraint(constraint);
    
    // Test without credentials
    int rc = getUrl("http://localhost:" + getPort() + "/protected/resource", 
                   new ByteChunk(), null);
    Assert.assertEquals(401, rc);
    
    // Test with valid credentials
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("Authorization", Arrays.asList("Basic " + 
        Base64.getEncoder().encodeToString("user:password".getBytes())));
    rc = getUrl("http://localhost:" + getPort() + "/protected/resource", 
               new ByteChunk(), headers);
    Assert.assertEquals(200, rc);
}
```

## Common Security Issues

### Issue: Session Fixation
**Problem**: Session ID not regenerated after authentication  
**Solution**: Call `request.getSession().invalidate()` and create new session

### Issue: Weak Cipher Suites
**Problem**: SSL configured with vulnerable ciphers  
**Solution**: Configure strong cipher suites in SSL connector

### Issue: Missing Security Headers
**Problem**: Responses lack security headers  
**Solution**: Use `HttpHeaderSecurityFilter` or custom valve

## Debugging Security

### Enable Security Logging
```properties
org.apache.catalina.authenticator.level = FINE
org.apache.catalina.realm.level = FINE
org.apache.catalina.security.level = FINE
```

### Common Debugging Points
- `AuthenticatorBase.authenticate()` - Authentication attempts
- `RealmBase.authenticate()` - User validation
- `SecurityConstraint.included()` - Constraint matching
- `SSLUtil.configureSSLEngine()` - SSL configuration

## Dependencies
- **Depends on**: Catalina core, Session management, JNDI
- **Used by**: All protected web applications, Management interfaces

## Related Domains
- **[Session Management](session-management.md)** - User session handling
- **[Request Processing](request-processing.md)** - Security valve integration
- **[Configuration](configuration.md)** - Security descriptor parsing