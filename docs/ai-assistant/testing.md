# Apache Tomcat Testing Guide

## Test Architecture Overview

Apache Tomcat uses a comprehensive testing strategy with multiple layers:

```
Unit Tests → Integration Tests → System Tests → Performance Tests
     ↓             ↓               ↓              ↓
  JUnit        TomcatBaseTest   Full Server    Load Testing
```

## Test Organization

### Test Directory Structure
```
test/
├── org/apache/catalina/          # Core container tests
├── org/apache/coyote/            # Protocol handler tests  
├── org/apache/jasper/            # JSP engine tests
├── org/apache/tomcat/            # Utility tests
├── org/apache/naming/            # JNDI tests
└── webapp/                       # Test web applications
    ├── WEB-INF/
    ├── jsp/
    └── static/
```

### Test Categories

#### Unit Tests (Fast)
- **Purpose**: Test individual components in isolation
- **Runtime**: < 1 second per test
- **Dependencies**: Minimal, mocked where possible
- **Coverage**: Core logic, edge cases, error conditions

#### Integration Tests (Medium)
- **Purpose**: Test component interactions
- **Runtime**: 1-10 seconds per test  
- **Dependencies**: Real Tomcat instances, databases
- **Coverage**: Request flows, configuration, deployment

#### System Tests (Slow)
- **Purpose**: End-to-end functionality testing
- **Runtime**: 10+ seconds per test
- **Dependencies**: Full server setup, external services
- **Coverage**: Complete scenarios, performance baselines

## Core Test Infrastructure

### TomcatBaseTest - Foundation Class
```java
public abstract class TomcatBaseTest {
    
    protected Tomcat tomcat;
    protected Context context;
    protected File tempDir;
    
    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("tomcat-test").toFile();
        
        tomcat = new Tomcat();
        tomcat.setBaseDir(tempDir.getAbsolutePath());
        tomcat.setPort(0); // Random free port
        tomcat.getHost().setAutoDeploy(false);
        
        context = tomcat.addContext("", tempDir.getAbsolutePath());
        
        tomcat.start();
    }
    
    @After
    public void tearDown() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
        
        if (tempDir != null) {
            ExpandWar.delete(tempDir);
        }
    }
    
    protected int getPort() {
        return tomcat.getConnector().getLocalPort();
    }
    
    protected String getUrl(String path) {
        return "http://localhost:" + getPort() + path;
    }
    
    protected ByteChunk getUrl(String url) throws IOException {
        return TomcatBaseTest.getUrl(url, null, null);
    }
}
```

### HTTP Test Utilities
```java
public class TomcatBaseTest {
    
    public static ByteChunk getUrl(String url, Map<String,List<String>> reqHead,
                                  Map<String,List<String>> resHead) throws IOException {
        
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        // Set request headers
        if (reqHead != null) {
            for (Map.Entry<String,List<String>> entry : reqHead.entrySet()) {
                StringBuilder valueList = new StringBuilder();
                for (String value : entry.getValue()) {
                    if (valueList.length() > 0) {
                        valueList.append(',');
                    }
                    valueList.append(value);
                }
                connection.setRequestProperty(entry.getKey(), valueList.toString());
            }
        }
        
        int rc = connection.getResponseCode();
        
        // Capture response headers
        if (resHead != null) {
            for (Map.Entry<String,List<String>> entry : connection.getHeaderFields().entrySet()) {
                resHead.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Read response
        InputStream is;
        if (rc < 400) {
            is = connection.getInputStream();
        } else {
            is = connection.getErrorStream();
        }
        
        ByteChunk result = new ByteChunk();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) {
            result.append(buffer, 0, read);
        }
        
        return result;
    }
}
```

## Testing Patterns by Component

### Container Testing
```java
@Test
public void testStandardContext() throws Exception {
    StandardContext context = new StandardContext();
    context.setPath("/test");
    context.setDocBase(tempDir.getAbsolutePath());
    
    // Add to host
    tomcat.getHost().addChild(context);
    
    // Verify context started
    assertTrue(context.getState().isAvailable());
    assertEquals("/test", context.getPath());
}

@Test
public void testContextClassLoader() throws Exception {
    Context context = tomcat.addContext("/test", tempDir.getAbsolutePath());
    
    // Create a simple servlet
    Tomcat.addServlet(context, "test", new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws IOException {
            resp.getWriter().write("ClassLoader: " + 
                Thread.currentThread().getContextClassLoader().getClass().getName());
        }
    });
    context.addServletMappingDecoded("/test", "test");
    
    tomcat.start();
    
    ByteChunk bc = getUrl(getUrl("/test/test"));
    assertTrue(bc.toString().contains("WebappClassLoader"));
}
```

### Servlet Testing
```java
public class TestServlet extends TomcatBaseTest {
    
    @Test
    public void testBasicServlet() throws Exception {
        Tomcat.addServlet(context, "test", new TestBasicServlet());
        context.addServletMappingDecoded("/test", "test");
        
        ByteChunk bc = getUrl(getUrl("/test"));
        assertEquals("Hello World", bc.toString());
    }
    
    @Test
    public void testServletParameters() throws Exception {
        Tomcat.addServlet(context, "echo", new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
                    throws IOException {
                String param = req.getParameter("message");
                resp.getWriter().write("Echo: " + param);
            }
        });
        context.addServletMappingDecoded("/echo", "echo");
        
        Map<String,List<String>> paramData = new HashMap<>();
        paramData.put("message", Arrays.asList("test message"));
        
        ByteChunk bc = postUrl(getUrl("/echo"), paramData);
        assertEquals("Echo: test message", bc.toString());
    }
    
    private static class TestBasicServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws IOException {
            resp.getWriter().write("Hello World");
        }
    }
}
```

### Session Testing
```java
@Test
public void testSessionCreation() throws Exception {
    Tomcat.addServlet(context, "session", new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws IOException {
            HttpSession session = req.getSession(true);
            resp.getWriter().write("Session ID: " + session.getId());
        }
    });
    context.addServletMappingDecoded("/session", "session");
    
    Map<String,List<String>> resHeaders = new HashMap<>();
    ByteChunk bc = TomcatBaseTest.getUrl(getUrl("/session"), null, resHeaders);
    
    assertTrue(bc.toString().startsWith("Session ID: "));
    
    // Verify Set-Cookie header
    List<String> cookies = resHeaders.get("Set-Cookie");
    assertNotNull(cookies);
    assertTrue(cookies.get(0).startsWith("JSESSIONID="));
}

@Test
public void testSessionPersistence() throws Exception {
    // Configure persistent manager
    PersistentManager manager = new PersistentManager();
    manager.setStore(new FileStore());
    context.setManager(manager);
    
    // Create session servlet
    Tomcat.addServlet(context, "session", new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws IOException {
            HttpSession session = req.getSession();
            String action = req.getParameter("action");
            
            if ("set".equals(action)) {
                session.setAttribute("test", "value");
                resp.getWriter().write("Set");
            } else {
                String value = (String) session.getAttribute("test");
                resp.getWriter().write("Value: " + value);
            }
        }
    });
    context.addServletMappingDecoded("/session", "session");
    
    tomcat.start();
    
    // Set session attribute
    Map<String,List<String>> resHeaders = new HashMap<>();
    TomcatBaseTest.getUrl(getUrl("/session?action=set"), null, resHeaders);
    
    String sessionCookie = extractSessionCookie(resHeaders);
    
    // Stop and restart context to trigger persistence
    context.stop();
    context.start();
    
    // Verify session restored
    Map<String,List<String>> reqHeaders = new HashMap<>();
    reqHeaders.put("Cookie", Arrays.asList(sessionCookie));
    
    ByteChunk bc = TomcatBaseTest.getUrl(getUrl("/session"), reqHeaders, null);
    assertEquals("Value: value", bc.toString());
}
```

### Security Testing
```java
@Test
public void testBasicAuthentication() throws Exception {
    // Configure Basic authentication
    LoginConfig loginConfig = new LoginConfig();
    loginConfig.setAuthMethod("BASIC");
    loginConfig.setRealmName("Test Realm");
    context.setLoginConfig(loginConfig);
    
    // Add security constraint
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addAuthRole("user");
    SecurityCollection collection = new SecurityCollection();
    collection.addPattern("/protected/*");
    constraint.addCollection(collection);
    context.addConstraint(constraint);
    
    // Add Basic authenticator
    context.getPipeline().addValve(new BasicAuthenticator());
    
    // Add test realm
    MemoryRealm realm = new MemoryRealm();
    realm.addUser("testuser", "testpass");
    realm.addRole("testuser", "user");
    context.setRealm(realm);
    
    // Add protected servlet
    Tomcat.addServlet(context, "protected", new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws IOException {
            resp.getWriter().write("Protected content");
        }
    });
    context.addServletMappingDecoded("/protected/*", "protected");
    
    tomcat.start();
    
    // Test without credentials - should get 401
    int rc = TomcatBaseTest.getUrl(getUrl("/protected/test"), new ByteChunk(), null);
    assertEquals(401, rc);
    
    // Test with valid credentials
    Map<String,List<String>> reqHeaders = new HashMap<>();
    String credentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes());
    reqHeaders.put("Authorization", Arrays.asList("Basic " + credentials));
    
    ByteChunk bc = TomcatBaseTest.getUrl(getUrl("/protected/test"), reqHeaders, null);
    assertEquals("Protected content", bc.toString());
}
```

### WebSocket Testing
```java
@Test
public void testWebSocketEcho() throws Exception {
    // Add WebSocket endpoint
    ServerEndpointConfig config = ServerEndpointConfig.Builder
        .create(TesterEchoServer.Basic.class, "/echo")
        .build();
    
    context.addApplicationListener(new ContextListener());
    tomcat.start();
    
    // Connect WebSocket client
    WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
    ClientEndpoint clientEndpoint = new TesterMessageCountClient.BasicText();
    
    URI uri = new URI("ws://localhost:" + getPort() + "/echo");
    Session wsSession = wsContainer.connectToServer(clientEndpoint, uri);
    
    // Send message
    wsSession.getBasicRemote().sendText("Hello WebSocket");
    
    // Wait for response
    ((TesterMessageCountClient.BasicText) clientEndpoint).waitForMessages(1);
    
    // Verify echo
    List<String> messages = ((TesterMessageCountClient.BasicText) clientEndpoint).getMessages();
    assertEquals(1, messages.size());
    assertEquals("Hello WebSocket", messages.get(0));
    
    wsSession.close();
}

public static class ContextListener implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ServerContainer sc = (ServerContainer) ctx.getAttribute(
            "jakarta.websocket.server.ServerContainer");
        try {
            sc.addEndpoint(TesterEchoServer.Basic.class);
        } catch (DeploymentException e) {
            throw new ServletException(e);
        }
    }
}
```

### Clustering Testing
```java
@Test
public void testSessionReplication() throws Exception {
    // Setup two Tomcat instances with clustering
    Tomcat tomcat1 = createClusteredTomcat();
    Tomcat tomcat2 = createClusteredTomcat();
    
    // Add session test servlet to both
    addSessionServlet(tomcat1);
    addSessionServlet(tomcat2);
    
    tomcat1.start();
    tomcat2.start();
    
    // Create session on node 1
    Map<String,List<String>> resHeaders = new HashMap<>();
    TomcatBaseTest.getUrl("http://localhost:" + tomcat1.getConnector().getLocalPort() + 
                         "/session?action=set&value=test", null, resHeaders);
    
    String sessionCookie = extractSessionCookie(resHeaders);
    
    // Access session from node 2
    Map<String,List<String>> reqHeaders = new HashMap<>();
    reqHeaders.put("Cookie", Arrays.asList(sessionCookie));
    
    ByteChunk bc = TomcatBaseTest.getUrl("http://localhost:" + 
        tomcat2.getConnector().getLocalPort() + "/session?action=get", 
        reqHeaders, null);
    
    assertEquals("test", bc.toString());
    
    tomcat1.stop();
    tomcat2.stop();
}

private Tomcat createClusteredTomcat() throws Exception {
    Tomcat tomcat = new Tomcat();
    tomcat.setPort(0);
    
    Context context = tomcat.addContext("", tempDir.getAbsolutePath());
    
    // Add clustering
    SimpleTcpCluster cluster = new SimpleTcpCluster();
    DeltaManager manager = new DeltaManager();
    context.setManager(manager);
    context.setCluster(cluster);
    
    return tomcat;
}
```

## Performance Testing

### Load Testing Framework
```java
@Test
public void testConcurrentRequests() throws Exception {
    int threadCount = 50;
    int requestsPerThread = 100;
    
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < threadCount; i++) {
        executor.execute(() -> {
            try {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        ByteChunk bc = getUrl(getUrl("/test"));
                        if (bc.toString().equals("Hello World")) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (IOException e) {
                        errorCount.incrementAndGet();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    long endTime = System.currentTimeMillis();
    
    int totalRequests = threadCount * requestsPerThread;
    assertEquals(totalRequests, successCount.get() + errorCount.get());
    assertTrue(errorCount.get() < totalRequests * 0.01); // < 1% error rate
    
    double requestsPerSecond = totalRequests / ((endTime - startTime) / 1000.0);
    System.out.println("Requests per second: " + requestsPerSecond);
    assertTrue(requestsPerSecond > 100); // Minimum performance threshold
}

@Test
public void testMemoryUsage() throws Exception {
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // Create many sessions
    for (int i = 0; i < 1000; i++) {
        Map<String,List<String>> resHeaders = new HashMap<>();
        TomcatBaseTest.getUrl(getUrl("/session?id=" + i), null, resHeaders);
    }
    
    // Force GC
    System.gc();
    Thread.sleep(100);
    
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = finalMemory - initialMemory;
    
    // Verify reasonable memory usage (< 50MB for 1000 sessions)
    assertTrue("Memory increase: " + memoryIncrease, memoryIncrease < 50 * 1024 * 1024);
}
```

## Running Tests

### Command Line Execution
```bash
# Run all tests
ant test

# Run specific test class
ant test -Dtest.name="**/TestStandardContext.java"

# Run tests with specific pattern
ant test -Dtest.name="**/Test*Session*.java"

# Run only fast tests (excluding integration)
ant test -Dtest.excludes="**/Test*Integration*.java"

# Run tests with detailed output
ant test -Dtest.verbose=true

# Run tests with code coverage
ant cobertura test

# Run performance tests only
ant test -Dtest.includes="**/TestPerformance*.java"
```

### Test Configuration
```properties
# In test.properties
test.temp.dir=${java.io.tmpdir}/tomcat-test
test.timeout=300
test.verbose=false
test.excludes=
test.includes=**/*Test*.java
```

## Test Utilities Reference

### Common Assertion Patterns
```java
// Response code checks
int rc = getUrl(url, bc, null);
assertEquals(HttpServletResponse.SC_OK, rc);

// Content verification
assertTrue(bc.toString().contains("expected"));
assertEquals("exact match", bc.toString().trim());

// Header verification
assertTrue(resHeaders.containsKey("Content-Type"));
assertEquals("text/html", resHeaders.get("Content-Type").get(0));

// Session cookie extraction
String sessionId = extractSessionId(resHeaders);
assertNotNull(sessionId);
assertTrue(sessionId.matches("[A-F0-9]{32}"));

// Timing assertions
long elapsed = System.currentTimeMillis() - start;
assertTrue("Request took too long: " + elapsed + "ms", elapsed < 5000);
```

### Mock Objects
```java
// Mock request
MockHttpServletRequest request = new MockHttpServletRequest();
request.setMethod("GET");
request.setRequestURI("/test");
request.addParameter("param", "value");

// Mock response
MockHttpServletResponse response = new MockHttpServletResponse();

// Mock session
MockHttpSession session = new MockHttpSession();
session.setAttribute("user", "testuser");
request.setSession(session);
```

## Debugging Tests

### Test Failure Investigation
```java
@Rule
public TestWatcher watcher = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
        System.out.println("Test failed: " + description.getMethodName());
        System.out.println("Tomcat port: " + getPort());
        System.out.println("Context path: " + context.getPath());
        
        // Dump thread state
        ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threads = threadMX.dumpAllThreads(true, true);
        for (ThreadInfo thread : threads) {
            System.out.println(thread.toString());
        }
    }
};
```

### Logging Configuration for Tests
```properties
# In test/logging.properties
handlers = java.util.logging.ConsoleHandler
.level = INFO

java.util.logging.ConsoleHandler.level = ALL
java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter

# Component-specific logging
org.apache.catalina.core.level = FINE
org.apache.catalina.session.level = FINE
org.apache.coyote.level = WARNING
```

## Best Practices

### Test Design
1. **Isolation**: Tests should not depend on each other
2. **Cleanup**: Always clean up resources in @After methods
3. **Timeouts**: Set reasonable timeouts for network operations
4. **Assertions**: Use specific assertions with meaningful messages
5. **Data**: Use test-specific data, avoid hardcoded values

### Performance
1. **Test Categories**: Separate fast unit tests from slow integration tests
2. **Resource Management**: Reuse test infrastructure where possible
3. **Parallel Execution**: Design tests to run safely in parallel
4. **Memory**: Monitor memory usage in long-running tests

### Maintenance
1. **Documentation**: Document complex test scenarios
2. **Refactoring**: Extract common test patterns to utilities
3. **Coverage**: Aim for high test coverage of critical paths
4. **Review**: Regular review of test effectiveness and maintenance burden