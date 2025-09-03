# Session Management Domain

## Purpose
Manages HTTP session lifecycle, persistence, clustering, and replication across the servlet container.

## Architecture Overview
```
Request → Session Manager → Session Store → Replication → Persistence
    ↓           ↓              ↓             ↓            ↓
 Session ID   Memory/DB    File/JDBC    Cluster Sync   Backup
```

## Key Components

### Session Managers
- **`StandardManager`** - In-memory session management (default)
- **`PersistentManager`** - Persistent session storage with swapping
- **`DeltaManager`** - Cluster-aware session replication (all-to-all)
- **`BackupManager`** - Cluster-aware session replication (primary-backup)

### Session Implementations
- **`StandardSession`** - Standard HTTP session implementation
- **`DeltaSession`** - Cluster-aware session with delta replication
- **`PersistentSession`** - Swappable persistent session

### Session Stores
- **`FileStore`** - File system persistence
- **`JDBCStore`** - Database persistence
- **`StoreBase`** - Abstract base for custom stores

## Session Lifecycle

### 1. Session Creation
```java
// Automatic creation when requested
HttpSession session = request.getSession(true);

// Internal creation flow
public Session createSession(String sessionId) {
    Session session = createEmptySession();
    session.setNew(true);
    session.setValid(true);
    session.setCreationTime(System.currentTimeMillis());
    session.setMaxInactiveInterval(getContext().getSessionTimeout() * 60);
    session.setId(sessionId);
    
    sessionCounter++;
    return session;
}
```

### 2. Session Tracking
Multiple tracking mechanisms supported:
- **Cookies** - Default mechanism (`JSESSIONID`)
- **URL Rewriting** - Fallback when cookies disabled
- **SSL Session ID** - For SSL-based tracking

### 3. Session Validation
```java
// Periodic background validation
public void processExpires() {
    long timeNow = System.currentTimeMillis();
    Session sessions[] = findSessions();
    
    for (Session session : sessions) {
        if (!session.isValid()) {
            continue;
        }
        
        int maxInactiveInterval = session.getMaxInactiveInterval();
        if (maxInactiveInterval < 0) {
            continue; // Never expires
        }
        
        int timeIdle = (int) ((timeNow - session.getThisAccessedTime()) / 1000L);
        if (timeIdle >= maxInactiveInterval) {
            session.expire();
        }
    }
}
```

### 4. Session Destruction
```java
public void expire() {
    // Notify listeners
    fireSessionEvent(Session.SESSION_DESTROYED_EVENT, null);
    
    // Remove from manager
    manager.remove(this);
    
    // Unbind all attributes
    for (String name : attributes.keySet()) {
        removeAttribute(name);
    }
}
```

## Session Configuration

### Basic Configuration (context.xml)
```xml
<Context>
    <!-- Standard in-memory manager -->
    <Manager className="org.apache.catalina.session.StandardManager"
             maxActiveSessions="1000"
             sessionIdLength="16"/>
             
    <!-- Persistent manager with file store -->
    <Manager className="org.apache.catalina.session.PersistentManager"
             maxActiveSessions="100"
             minIdleSwap="60"
             maxIdleSwap="300"
             maxIdleBackup="120">
        <Store className="org.apache.catalina.session.FileStore"
               directory="sessions"/>
    </Manager>
</Context>
```

### Clustering Configuration
```xml
<Context>
    <!-- Delta manager for all-to-all replication -->
    <Manager className="org.apache.catalina.ha.session.DeltaManager"
             expireSessionsOnShutdown="false"
             notifyListenersOnReplication="true"
             maxInactiveInterval="1800"/>
</Context>
```

## Session Persistence

### File-based Persistence
```java
public class FileStore extends StoreBase {
    
    @Override
    public void save(Session session) throws IOException {
        File file = file(session.getIdInternal());
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(
            new BufferedOutputStream(fos));
        
        try {
            ((StandardSession) session).writeObjectData(oos);
        } finally {
            oos.close();
        }
    }
    
    @Override
    public Session load(String id) throws ClassNotFoundException, IOException {
        File file = file(id);
        if (!file.exists()) {
            return null;
        }
        
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(
            new BufferedInputStream(fis));
        
        StandardSession session = (StandardSession) manager.createEmptySession();
        session.readObjectData(ois);
        session.setManager(manager);
        
        return session;
    }
}
```

### Database Persistence
```java
public class JDBCStore extends StoreBase {
    
    public void save(Session session) throws IOException {
        String sql = "INSERT INTO tomcat_sessions (session_id, session_data, " +
                    "valid_session, max_inactive, last_access) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, session.getIdInternal());
            stmt.setBytes(2, serialize(session));
            stmt.setString(3, session.isValid() ? "1" : "0");
            stmt.setInt(4, session.getMaxInactiveInterval());
            stmt.setLong(5, session.getLastAccessedTime());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to save session", e);
        }
    }
}
```

## Session Clustering

### Cluster Communication Architecture
```
Node A (Primary)     Node B (Backup)     Node C (Backup)
     │                      │                  │
     ├─ Session Create ────→│                  │
     ├─ Delta Change ──────→├─────────────────→│
     └─ Session Expire ────→└─────────────────→│
```

### Delta Replication
```java
public class DeltaSession extends StandardSession {
    
    // Track attribute changes
    private DeltaRequest deltaRequest = new DeltaRequest(getIdInternal(), false);
    
    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        
        // Record delta change
        deltaRequest.setAttribute(name, value);
        setDeltaRequest(deltaRequest);
    }
    
    public void applyDelta(byte[] delta) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(delta);
        ReplicationStream ois = new ReplicationStream(bis, 
            manager.getContext().getLoader().getClassLoader());
        
        DeltaRequest request = (DeltaRequest) ois.readObject();
        request.execute(this);
    }
}
```

### Cluster Manager
```java
public class DeltaManager extends ClusterManagerBase {
    
    public void sessionAttributeChanged(DeltaSession session, 
                                       String name, Object value) {
        // Send delta to all cluster nodes
        DeltaRequest deltaRequest = session.getDeltaRequest();
        SessionMessage msg = new SessionMessageImpl(getName(),
            SessionMessage.EVT_SESSION_DELTA,
            deltaRequest.serialize(),
            session.getIdInternal(),
            session.getIdInternal());
            
        cluster.send(msg);
    }
}
```

## Session Security

### Session ID Generation
```java
public String generateSessionId() {
    byte[] buffer = new byte[getSessionIdLength()];
    
    // Use secure random
    getRandom().nextBytes(buffer);
    
    // Convert to hex string
    StringBuilder reply = new StringBuilder();
    for (byte b : buffer) {
        reply.append(String.format("%02x", b & 0xff));
    }
    
    return reply.toString();
}
```

### Session Hijacking Prevention
```java
// Regenerate session ID after authentication
public void login(String username, String password) {
    if (authenticate(username, password)) {
        // Invalidate old session
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        
        // Create new session
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("user", username);
    }
}
```

### Cookie Security
```xml
<session-config>
    <session-timeout>30</session-timeout>
    <cookie-config>
        <name>JSESSIONID</name>
        <http-only>true</http-only>
        <secure>true</secure>
        <max-age>1800</max-age>
        <same-site>Strict</same-site>
    </cookie-config>
    <tracking-mode>COOKIE</tracking-mode>
</session-config>
```

## Performance Optimization

### Session Attribute Guidelines
```java
// Good: Lightweight, serializable objects
session.setAttribute("userId", userId);
session.setAttribute("preferences", new UserPreferences());

// Bad: Heavy objects, non-serializable
session.setAttribute("database", dataSource); // Non-serializable
session.setAttribute("results", largeResultSet); // Memory intensive
```

### Session Passivation
```java
// Configure session swapping
<Manager className="org.apache.catalina.session.PersistentManager"
         minIdleSwap="60"      <!-- Swap after 1 minute idle -->
         maxIdleSwap="300"     <!-- Force swap after 5 minutes -->
         maxActiveSessions="100" <!-- Keep max 100 in memory -->
         saveOnRestart="true">
    <Store className="org.apache.catalina.session.FileStore"/>
</Manager>
```

## Testing Session Management

### Unit Tests
```java
@Test
public void testSessionCreation() {
    StandardManager manager = new StandardManager();
    manager.setContext(context);
    
    Session session = manager.createSession(null);
    assertNotNull(session);
    assertNotNull(session.getId());
    assertTrue(session.isNew());
    assertTrue(session.isValid());
}

@Test
public void testSessionExpiration() throws InterruptedException {
    StandardManager manager = new StandardManager();
    Session session = manager.createSession(null);
    session.setMaxInactiveInterval(1); // 1 second
    
    Thread.sleep(2000);
    manager.processExpires();
    
    assertFalse(session.isValid());
}
```

### Integration Tests
```java
@Test
public void testSessionPersistence() throws Exception {
    // Create session with data
    Map<String, List<String>> sessionData = new HashMap<>();
    sessionData.put("user", Arrays.asList("testuser"));
    
    ByteChunk bc = new ByteChunk();
    int rc = postUrl("http://localhost:" + getPort() + "/test", bc, sessionData);
    assertEquals(200, rc);
    
    // Extract session cookie
    String cookie = extractSessionCookie(bc);
    
    // Restart context to trigger persistence
    context.stop();
    context.start();
    
    // Verify session restored
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("Cookie", Arrays.asList(cookie));
    rc = getUrl("http://localhost:" + getPort() + "/test", bc, headers);
    assertEquals(200, rc);
    assertTrue(bc.toString().contains("testuser"));
}
```

## Common Issues & Solutions

### Issue: Session Memory Leaks
**Cause**: Large objects stored in session, not cleaned up  
**Solution**: 
```java
// Implement session listener for cleanup
public class SessionCleanupListener implements HttpSessionListener {
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        // Clean up resources
        Object resource = session.getAttribute("resource");
        if (resource instanceof Closeable) {
            try {
                ((Closeable) resource).close();
            } catch (IOException e) {
                log.warn("Failed to close resource", e);
            }
        }
    }
}
```

### Issue: Session Replication Failures
**Cause**: Non-serializable attributes  
**Solution**: Validate serialization
```java
@Override
public void setAttribute(String name, Object value) {
    if (value != null && !(value instanceof Serializable)) {
        throw new IllegalArgumentException(
            "Attribute " + name + " is not serializable");
    }
    super.setAttribute(name, value);
}
```

### Issue: Session Timeout Not Working
**Cause**: Background processing disabled  
**Solution**: Enable background processing
```xml
<Context backgroundProcessorDelay="10"> <!-- Process every 10 seconds -->
    <Manager className="org.apache.catalina.session.StandardManager"
             processExpiresFrequency="6"/> <!-- Check every 60 seconds -->
</Context>
```

## Monitoring Sessions

### JMX Metrics
```java
// Session manager MBean exposes:
// - activeSessions: Current active session count
// - maxActive: Maximum concurrent sessions
// - sessionCounter: Total sessions created
// - expiredSessions: Total expired sessions
// - rejectedSessions: Sessions rejected due to maxActive limit
```

### Custom Session Monitoring
```java
public class SessionMonitorValve extends ValveBase {
    
    @Override
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            long lastAccess = session.getLastAccessedTime();
            long now = System.currentTimeMillis();
            long idle = (now - lastAccess) / 1000;
            
            if (idle > WARN_THRESHOLD) {
                log.warn("Long idle session: {} ({}s)", 
                        session.getId(), idle);
            }
        }
        
        getNext().invoke(request, response);
    }
}
```

## Dependencies
- **Depends on**: Context lifecycle, Background processing, Clustering (optional)
- **Used by**: All web applications, Security authentication, User state management

## Related Domains
- **[Security](security.md)** - Authentication state storage
- **[Clustering](clustering.md)** - Session replication
- **[Request Processing](request-processing.md)** - Session access per request