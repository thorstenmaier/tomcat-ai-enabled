# Apache Tomcat Code Patterns Library

## Overview
This document catalogs common coding patterns used throughout the Tomcat codebase, enabling consistent code generation and modifications.

## Lifecycle Management Pattern

### Standard Lifecycle Implementation
```java
public class MyComponent extends LifecycleBase {
    
    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        // Initialize resources
    }
    
    @Override
    protected void startInternal() throws LifecycleException {
        setState(LifecycleState.STARTING);
        // Start processing
        setState(LifecycleState.STARTED);
    }
    
    @Override
    protected void stopInternal() throws LifecycleException {
        setState(LifecycleState.STOPPING);
        // Stop processing
        setState(LifecycleState.STOPPED);
    }
    
    @Override
    protected void destroyInternal() throws LifecycleException {
        // Cleanup resources
        super.destroyInternal();
    }
}
```

**Usage**: All major components (Connector, Container, etc.)  
**Files**: `StandardServer.java`, `StandardService.java`, `StandardEngine.java`

## Container Pattern

### Container with Pipeline
```java
public class MyContainer extends ContainerBase {
    
    public MyContainer() {
        pipeline.setBasic(new MyValve());
    }
    
    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        // Container-specific initialization
    }
}
```

**Pipeline Valve Pattern**:
```java
public class MyValve extends ValveBase {
    
    @Override
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        
        // Pre-processing
        try {
            // Pass to next valve
            getNext().invoke(request, response);
        } finally {
            // Post-processing (always executes)
        }
    }
}
```

**Usage**: Engine, Host, Context, Wrapper implementations

## Error Handling Pattern

### Exception Handling in Valves
```java
public class MyValve extends ValveBase {
    
    @Override
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        
        try {
            // Processing logic
            getNext().invoke(request, response);
            
        } catch (IOException ioe) {
            container.getLogger().error("IO error", ioe);
            throw ioe;
            
        } catch (ServletException se) {
            container.getLogger().error("Servlet error", se);
            throw se;
            
        } catch (RuntimeException re) {
            container.getLogger().error("Runtime error", re);
            throw new ServletException(re);
        }
    }
}
```

### Resource Cleanup Pattern
```java
public void process() throws Exception {
    Resource resource = null;
    try {
        resource = acquireResource();
        // Use resource
    } catch (Exception e) {
        // Handle exception
        throw e;
    } finally {
        if (resource != null) {
            try {
                releaseResource(resource);
            } catch (Exception e) {
                log.warn("Failed to release resource", e);
            }
        }
    }
}
```

## Logging Pattern

### Standard Logging
```java
public class MyClass {
    
    private static final Log log = LogFactory.getLog(MyClass.class);
    
    public void process() {
        if (log.isDebugEnabled()) {
            log.debug("Processing started for: " + identifier);
        }
        
        try {
            // Processing
            if (log.isInfoEnabled()) {
                log.info("Successfully processed: " + identifier);
            }
        } catch (Exception e) {
            log.error("Failed to process: " + identifier, e);
        }
    }
}
```

### Performance Logging
```java
long start = System.currentTimeMillis();
try {
    // Operation
} finally {
    if (log.isDebugEnabled()) {
        log.debug("Operation took: " + 
                  (System.currentTimeMillis() - start) + "ms");
    }
}
```

**Usage**: Consistent across all packages

## Authentication Pattern

### Authenticator Implementation
```java
public class MyAuthenticator extends AuthenticatorBase {
    
    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response)
            throws IOException {
        
        // Extract credentials
        String credentials = extractCredentials(request);
        if (credentials == null) {
            // Challenge for credentials
            sendUnauthorized(request, response);
            return false;
        }
        
        // Validate credentials
        Principal principal = validate(credentials);
        if (principal == null) {
            sendUnauthorized(request, response);
            return false;
        }
        
        // Register authenticated principal
        register(request, response, principal, 
                 getAuthMethod(), credentials);
        return true;
    }
    
    @Override
    protected String getAuthMethod() {
        return "MY-AUTH";
    }
}
```

**Files**: `BasicAuthenticator.java`, `DigestAuthenticator.java`, `FormAuthenticator.java`

## Session Management Pattern

### Session Store Implementation
```java
public class MyStore extends StoreBase {
    
    @Override
    public void save(Session session) throws IOException {
        try {
            // Serialize session
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            ((StandardSession) session).writeObjectData(oos);
            oos.close();
            
            // Store bytes
            storeSessionData(session.getId(), baos.toByteArray());
            
        } catch (IOException e) {
            manager.getContext().getLogger().error(
                "Failed to save session " + session.getId(), e);
            throw e;
        }
    }
    
    @Override
    public Session load(String id) throws IOException, ClassNotFoundException {
        byte[] data = loadSessionData(id);
        if (data == null) {
            return null;
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        StandardSession session = (StandardSession) manager.createEmptySession();
        session.readObjectData(ois);
        session.setManager(manager);
        
        return session;
    }
}
```

## Threading Pattern

### Background Processing
```java
public class MyBackgroundProcessor implements Runnable {
    
    private volatile boolean stopRequested = false;
    private Thread backgroundThread = null;
    
    public void start() {
        if (backgroundThread == null) {
            backgroundThread = new Thread(this, "MyProcessor");
            backgroundThread.setDaemon(true);
            backgroundThread.start();
        }
    }
    
    public void stop() {
        stopRequested = true;
        if (backgroundThread != null) {
            backgroundThread.interrupt();
            try {
                backgroundThread.join(5000); // 5 second timeout
            } catch (InterruptedException e) {
                // Ignore
            }
            backgroundThread = null;
        }
    }
    
    @Override
    public void run() {
        while (!stopRequested) {
            try {
                Thread.sleep(processInterval);
                if (!stopRequested) {
                    process();
                }
            } catch (InterruptedException e) {
                // Expected on shutdown
                break;
            } catch (Exception e) {
                log.error("Background processing error", e);
            }
        }
    }
}
```

**Usage**: `ContainerBackgroundProcessor`, `StandardManager.backgroundProcess()`

## Testing Patterns

### Base Test Class Pattern
```java
public abstract class TomcatBaseTest {
    
    protected Tomcat tomcat;
    protected Context context;
    
    @Before
    public void setUp() throws Exception {
        tomcat = new Tomcat();
        tomcat.setBaseDir("target/test-tomcat");
        tomcat.setPort(0); // Random free port
        
        context = tomcat.addContext("", 
                new File("target/test-classes").getAbsolutePath());
        
        tomcat.start();
    }
    
    @After
    public void tearDown() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }
    
    protected int getPort() {
        return tomcat.getConnector().getLocalPort();
    }
    
    protected String getUrl(String path) {
        return "http://localhost:" + getPort() + path;
    }
}
```

### Servlet Test Pattern
```java
@Test
public void testServletResponse() throws Exception {
    // Add servlet
    Tomcat.addServlet(context, "test", new TestServlet());
    context.addServletMappingDecoded("/test", "test");
    
    // Make request
    ByteChunk bc = new ByteChunk();
    int rc = getUrl("http://localhost:" + getPort() + "/test", bc, null);
    
    // Assert response
    Assert.assertEquals(HttpServletResponse.SC_OK, rc);
    Assert.assertTrue(bc.toString().contains("expected content"));
}
```

### WebSocket Test Pattern
```java
@Test
public void testWebSocket() throws Exception {
    Context ctx = tomcat.addContext("", null);
    ctx.addApplicationListener(TesterEchoServer.Config.class.getName());
    
    tomcat.start();
    
    WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
    ClientEndpoint endpoint = new TesterMessageCountClient.BasicText();
    
    URI uri = new URI("ws://localhost:" + getPort() + "/echo");
    Session session = wsContainer.connectToServer(endpoint, uri);
    
    session.getBasicRemote().sendText("test message");
    // Verify echo response
}
```

## Configuration Pattern

### Digester Rules Pattern
```java
public class MyConfigurationDigester {
    
    public Digester createDigester() {
        Digester digester = new Digester();
        
        // Server element
        digester.addObjectCreate("Server", 
                                "org.apache.catalina.core.StandardServer",
                                "className");
        digester.addSetProperties("Server");
        digester.addSetNext("Server", "setServer");
        
        // Service element
        digester.addObjectCreate("Server/Service",
                                "org.apache.catalina.core.StandardService",
                                "className");
        digester.addSetProperties("Server/Service");
        digester.addSetNext("Server/Service", "addService");
        
        return digester;
    }
}
```

**Usage**: `Catalina.createStartDigester()`, `ContextConfig.createWebXmlDigester()`

## Resource Management Pattern

### JNDI Resource Factory
```java
public class MyResourceFactory implements ObjectFactory {
    
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                   Hashtable<?,?> environment) throws Exception {
        
        Reference ref = (Reference) obj;
        
        // Extract configuration
        String param1 = getStringRefAddr(ref, "param1");
        String param2 = getStringRefAddr(ref, "param2");
        
        // Create and configure resource
        MyResource resource = new MyResource();
        resource.setParam1(param1);
        resource.setParam2(param2);
        
        return resource;
    }
    
    private String getStringRefAddr(Reference ref, String addrType) {
        RefAddr addr = ref.get(addrType);
        return addr == null ? null : addr.getContent().toString();
    }
}
```

## Protocol Handler Pattern

### Custom Protocol Implementation
```java
public class MyProtocolHandler extends AbstractProtocol<MySocketWrapper> {
    
    @Override
    protected Processor createProcessor() {
        MyProcessor processor = new MyProcessor();
        processor.setAdapter(getAdapter());
        return processor;
    }
    
    @Override
    protected SocketWrapperBase<MySocket> createSocketWrapper(MySocket socket) {
        return new MySocketWrapper(socket, this);
    }
}

public class MyProcessor extends AbstractProcessor {
    
    @Override
    public SocketState service(SocketWrapperBase<?> socketWrapper)
            throws IOException {
        
        // Parse protocol
        parseRequest();
        
        // Prepare response
        prepareResponse();
        
        // Process via adapter
        getAdapter().service(request, response);
        
        // Finalize response
        finishResponse();
        
        return SocketState.CLOSED;
    }
}
```

**Usage**: `Http11Processor`, `AjpProcessor`, `Http2UpgradeHandler`