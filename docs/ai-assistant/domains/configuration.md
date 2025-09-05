# Configuration & Lifecycle Domain

## Purpose
Manages server configuration parsing, component lifecycle coordination, and runtime management. This domain handles XML configuration processing, startup/shutdown sequences, component state management, and dynamic reconfiguration through a sophisticated event-driven architecture with built-in error handling and validation.

## Architecture Overview
```
Configuration Processing:
server.xml → Digester → Object Graph → Component Registration

Lifecycle Management:
Bootstrap → Catalina → Server → Service → Engine → Host → Context
     ↓
State Machine: NEW → INITIALIZING → INITIALIZED → STARTING_PREP → 
               STARTING → STARTED → STOPPING_PREP → STOPPING → 
               STOPPED → DESTROYING → DESTROYED

Event Flow:
LifecycleEvent → LifecycleListener → Component Action
```

## Key Components

### Configuration Processing
- **`Digester`** (`o.a.tomcat.util.digester`) - XML-to-object mapping framework
- **`Catalina`** (`o.a.catalina.startup`) - Main server configuration processor
- **`ContextConfig`** (`o.a.catalina.startup`) - Context configuration handler
- **`HostConfig`** (`o.a.catalina.startup`) - Host configuration manager
- **`WebRuleSet`** (`o.a.catalina.startup`) - web.xml parsing rules

### Lifecycle Management
- **`Lifecycle`** (`o.a.catalina`) - Core lifecycle interface
- **`LifecycleBase`** (`o.a.catalina.util`) - Base lifecycle implementation
- **`LifecycleListener`** (`o.a.catalina`) - Lifecycle event listener
- **`LifecycleEvent`** (`o.a.catalina`) - Lifecycle event representation
- **`LifecycleState`** (`o.a.catalina`) - Component state enumeration

### Bootstrap & Startup
- **`Bootstrap`** (`o.a.catalina.startup`) - JVM entry point and classloader setup
- **`Catalina`** (`o.a.catalina.startup`) - Server instance management
- **`Server`** (`o.a.catalina`) - Top-level server container
- **`StandardServer`** (`o.a.catalina.core`) - Default server implementation

### Rule Sets & Parsing
- **`ServerRuleSet`** (`o.a.catalina.startup`) - server.xml parsing rules
- **`ContextRuleSet`** (`o.a.catalina.startup`) - context.xml rules
- **`NamingRuleSet`** (`o.a.catalina.startup`) - JNDI resource rules
- **`ClusterRuleSet`** (`o.a.catalina.ha`) - Clustering configuration rules

## Entry Points

### Bootstrap Sequence
1. **`Bootstrap.main()`** - JVM entry point
2. **`Bootstrap.init()`** - Classloader initialization
3. **`Catalina.load()`** - Configuration loading
4. **`Catalina.start()`** - Component startup

### Configuration Loading
- `server.xml` → `Digester` → Server object graph
- `context.xml` → `ContextConfig` → Context configuration
- `web.xml` → `WebRuleSet` → Application configuration

## Digester Framework

### Core Concepts
```java
// Rule-based XML processing
Digester digester = new Digester();
digester.addObjectCreate("Server", StandardServer.class);
digester.addSetProperties("Server");
digester.addObjectCreate("Server/Service", StandardService.class);
digester.addSetNext("Server/Service", "addService", Service.class);
```

### Property Substitution
```java
// System property substitution in XML
// ${catalina.home}/conf/server.xml
// ${catalina.base}/webapps
// ${java.io.tmpdir}/tomcat-work

PropertySource[] sources = PropertySource.getPropertySources();
IntrospectionUtils.replaceProperties(value, sources, null);
```

### Rule Processing
```java
// Built-in rule types
ObjectCreateRule    // Create object instances
SetPropertiesRule   // Set bean properties
SetNextRule        // Add child to parent
CallMethodRule     // Invoke methods
CallParamRule      // Method parameters
```

## Lifecycle State Machine

### State Transitions
```java
public enum LifecycleState {
    NEW(false, null),
    INITIALIZING(false, Lifecycle.BEFORE_INIT_EVENT),
    INITIALIZED(false, Lifecycle.AFTER_INIT_EVENT),
    STARTING_PREP(false, Lifecycle.BEFORE_START_EVENT),
    STARTING(true, Lifecycle.START_EVENT),
    STARTED(true, Lifecycle.AFTER_START_EVENT),
    STOPPING_PREP(true, Lifecycle.BEFORE_STOP_EVENT),
    STOPPING(false, Lifecycle.STOP_EVENT),
    STOPPED(false, Lifecycle.AFTER_STOP_EVENT),
    DESTROYING(false, Lifecycle.BEFORE_DESTROY_EVENT),
    DESTROYED(false, Lifecycle.AFTER_DESTROY_EVENT),
    FAILED(false, null);
}
```

### State Management
```java
public abstract class LifecycleBase implements Lifecycle {
    private volatile LifecycleState state = LifecycleState.NEW;
    
    protected final void setState(LifecycleState state) {
        setStateInternal(state, null, true);
    }
    
    protected final void setState(LifecycleState state, Object data) {
        setStateInternal(state, data, true);
    }
    
    private synchronized void setStateInternal(LifecycleState state, 
                                             Object data, boolean check) {
        if (check) {
            // Validate state transitions
            if (!this.state.isAvailable() && state.isAvailable()) {
                // Invalid transition
                invalidTransition(state.name());
            }
        }
        
        this.state = state;
        String lifecycleEvent = state.getLifecycleEvent();
        if (lifecycleEvent != null) {
            fireLifecycleEvent(lifecycleEvent, data);
        }
    }
}
```

## Configuration Files Processing

### server.xml Structure
```xml
<!-- Server configuration hierarchy -->
<Server port="8005" shutdown="SHUTDOWN">
    <Listener className="org.apache.catalina.startup.VersionLoggerListener"/>
    <GlobalNamingResources>
        <!-- Global resources -->
    </GlobalNamingResources>
    
    <Service name="Catalina">
        <Connector port="8080" protocol="HTTP/1.1"/>
        <Engine name="Catalina" defaultHost="localhost">
            <Host name="localhost" appBase="webapps">
                <Context path="/myapp" docBase="myapp.war"/>
            </Host>
        </Engine>
    </Service>
</Server>
```

### Parsing Rules
```java
// ServerRuleSet registration
digester.addRuleSet(new ServerRuleSet(prefix));

public class ServerRuleSet extends RuleSetBase {
    public void addRuleInstances(Digester digester) {
        digester.addObjectCreate(prefix + "Server",
                               "org.apache.catalina.core.StandardServer",
                               "className");
        digester.addSetProperties(prefix + "Server");
        digester.addSetNext(prefix + "Server", "setServer", 
                          "org.apache.catalina.Server");
        
        digester.addObjectCreate(prefix + "Server/GlobalNamingResources",
                               "org.apache.catalina.deploy.NamingResourcesImpl");
        digester.addSetNext(prefix + "Server/GlobalNamingResources",
                          "setGlobalNamingResources",
                          "org.apache.catalina.deploy.NamingResourcesImpl");
        // ... more rules
    }
}
```

### Context Configuration
```java
public class ContextConfig implements LifecycleListener {
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
            configureStart();
        }
    }
    
    protected void configureStart() {
        // Process context.xml files
        processContextConfig();
        
        // Process web.xml
        processWebXml();
        
        // Process annotations
        processAnnotations();
    }
}
```

## Component Lifecycle Coordination

### Hierarchical Startup
```java
// Parent-to-child startup propagation
public abstract class ContainerBase extends LifecycleBase 
                                  implements Container {
    protected void startInternal() throws LifecycleException {
        // Start this container
        super.startInternal();
        
        // Start child containers
        for (Container child : findChildren()) {
            if (child.getState().isAvailable()) {
                continue; // Already started
            }
            child.start();
        }
    }
}
```

### Event Propagation
```java
// Lifecycle event broadcasting
protected void fireLifecycleEvent(String type, Object data) {
    LifecycleEvent event = new LifecycleEvent(this, type, data);
    
    for (LifecycleListener listener : lifecycleListeners) {
        try {
            listener.lifecycleEvent(event);
        } catch (Throwable t) {
            // Log error but continue processing
            log.error("Listener error", t);
        }
    }
}
```

### Failure Handling
```java
// Component failure isolation
protected final void handleSubClassException(Throwable t, 
                                           String key, Object... args) {
    ExceptionUtils.handleThrowable(t);
    
    setStateInternal(LifecycleState.FAILED, null, false);
    
    String msg = sm.getString(key, args);
    if (getThrowOnFailure()) {
        throw new LifecycleException(msg, t);
    } else {
        log.error(msg, t);
    }
}
```

## Bootstrap & Startup Sequence

### Bootstrap Process
```java
public class Bootstrap {
    public static void main(String[] args) {
        synchronized (daemonLock) {
            if (daemon == null) {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.init(); // Initialize classloaders
                daemon = bootstrap;
            }
        }
        
        // Process command
        daemon.load(args);  // Load configuration
        daemon.start();     // Start server
    }
    
    private void init() throws Exception {
        initClassLoaders(); // Set up classloader hierarchy
        
        Class<?> startupClass = catalinaLoader.loadClass("org.apache.catalina.startup.Catalina");
        Object startupInstance = startupClass.newInstance();
        
        // Set up reflection method calls
        String methodName = "setParentClassLoader";
        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Class.forName("java.lang.ClassLoader");
        Method method = startupInstance.getClass().getMethod(methodName, paramTypes);
        Object paramValues[] = new Object[1];
        paramValues[0] = sharedLoader;
        method.invoke(startupInstance, paramValues);
        
        catalinaDaemon = startupInstance;
    }
}
```

### Catalina Initialization
```java
public class Catalina {
    public void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        
        long t1 = System.nanoTime();
        
        // Parse server.xml
        Digester digester = createStartDigester();
        
        try (ConfigFileLoader.ConfigurationSource.Resource resource = 
             ConfigFileLoader.getSource().getServerXml()) {
            
            InputStream inputStream = resource.getInputStream();
            digester.push(this);
            digester.parse(inputStream);
        }
        
        getServer().setCatalina(this);
        getServer().setCatalinaHome(Bootstrap.getCatalinaHomeFile());
        getServer().setCatalinaBase(Bootstrap.getCatalinaBaseFile());
        
        // Initialize server
        getServer().init();
        
        long t2 = System.nanoTime();
        log.info("Server initialization in " + ((t2 - t1) / 1000000) + " ms");
    }
}
```

## Dynamic Reconfiguration

### JMX Integration
```java
// MBean registration for runtime management
public class MBeanFactory {
    public String createStandardContext(String parent, String path, 
                                      String docBase) {
        StandardContext context = new StandardContext();
        context.setPath(path);
        context.setDocBase(docBase);
        
        // Add to parent container
        Container host = getParentContainer(parent);
        host.addChild(context);
        
        return context.getObjectName().toString();
    }
}
```

### Configuration Reloading
```java
// Context reloading support
public class StandardContext extends ContainerBase implements Context {
    public synchronized void reload() {
        // Check for modifications
        if (!getReloadable()) {
            return;
        }
        
        // Stop current instance
        setPaused(true);
        
        try {
            stop();
        } catch (LifecycleException e) {
            log.error("Error stopping context", e);
            return;
        }
        
        try {
            start(); // Restart with new configuration
        } catch (LifecycleException e) {
            log.error("Error starting context", e);
        } finally {
            setPaused(false);
        }
    }
}
```

## Common Operations

### Add Lifecycle Listener
```java
public void addCustomListener() {
    LifecycleListener listener = new LifecycleListener() {
        public void lifecycleEvent(LifecycleEvent event) {
            String type = event.getType();
            if (Lifecycle.START_EVENT.equals(type)) {
                // Handle start event
                log.info("Component started: " + event.getSource());
            }
        }
    };
    
    // Add to component
    ((Lifecycle) component).addLifecycleListener(listener);
}
```

### Custom Digester Rules
```java
public class CustomRuleSet extends RuleSetBase {
    public void addRuleInstances(Digester digester) {
        digester.addObjectCreate("Server/CustomComponent",
                               "com.example.CustomComponent");
        digester.addSetProperties("Server/CustomComponent");
        digester.addSetNext("Server/CustomComponent", 
                          "addCustomComponent",
                          "com.example.CustomComponent");
    }
}
```

### Programmatic Configuration
```java
// Create server programmatically
StandardServer server = new StandardServer();
server.setPort(8005);
server.setShutdown("SHUTDOWN");

StandardService service = new StandardService();
service.setName("Catalina");
server.addService(service);

Connector connector = new Connector();
connector.setPort(8080);
service.addConnector(connector);

StandardEngine engine = new StandardEngine();
engine.setName("Catalina");
engine.setDefaultHost("localhost");
service.setContainer(engine);

StandardHost host = new StandardHost();
host.setName("localhost");
host.setAppBase("webapps");
engine.addChild(host);
```

## Performance Considerations

### Configuration Caching
```java
// Digester instance caching
private static final Map<String, Digester> digesterCache = 
    new ConcurrentHashMap<>();
    
private Digester getDigester(String ruleSetKey) {
    return digesterCache.computeIfAbsent(ruleSetKey, key -> {
        Digester digester = new Digester();
        // Configure rules...
        return digester;
    });
}
```

### Lifecycle Event Optimization
```java
// Efficient listener management
private final List<LifecycleListener> lifecycleListeners = 
    new CopyOnWriteArrayList<>(); // Thread-safe, optimized for reads
```

### Startup Time Optimization
```java
// Parallel container startup
public void startInternal() throws LifecycleException {
    // Start children in parallel
    if (getStartStopThreads() > 1) {
        startStopExecutor = new ThreadPoolExecutor(
            getStartStopThreads(), getStartStopThreads(),
            10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new StartStopThreadFactory(getName() + "-startStop-"));
    }
    
    Container[] children = findChildren();
    List<Future<Void>> results = new ArrayList<>();
    
    for (Container child : children) {
        Future<Void> result = startStopExecutor.submit(
            new StartChild(child));
        results.add(result);
    }
}
```

## Testing Strategies

### Unit Tests
```java
// Test lifecycle state transitions
@Test
public void testLifecycleTransitions() throws Exception {
    TestComponent component = new TestComponent();
    assertEquals(LifecycleState.NEW, component.getState());
    
    component.init();
    assertEquals(LifecycleState.INITIALIZED, component.getState());
    
    component.start();
    assertEquals(LifecycleState.STARTED, component.getState());
    
    component.stop();
    assertEquals(LifecycleState.STOPPED, component.getState());
    
    component.destroy();
    assertEquals(LifecycleState.DESTROYED, component.getState());
}
```

### Configuration Tests
```java
// Test Digester parsing
@Test
public void testServerConfiguration() throws Exception {
    String serverXml = 
        "<Server port='8005' shutdown='SHUTDOWN'>" +
        "  <Service name='Catalina'>" +
        "    <Connector port='8080'/>" +
        "  </Service>" +
        "</Server>";
        
    Digester digester = createServerDigester();
    StandardServer server = (StandardServer) digester.parse(
        new StringReader(serverXml));
        
    assertEquals(8005, server.getPort());
    assertEquals("SHUTDOWN", server.getShutdown());
    assertEquals(1, server.findServices().length);
}
```

## Common Issues & Solutions

### Issue: Configuration Parse Errors
- **Solution**: Validate XML syntax and schema
- Use proper DOCTYPE declarations
- Check for property substitution issues
- Enable debug logging for Digester

### Issue: Lifecycle State Conflicts
- **Solution**: Ensure proper state transition order
- Handle concurrent lifecycle operations
- Implement proper exception handling
- Use lifecycle event listeners for debugging

### Issue: Memory Leaks on Reload
- **Solution**: Properly implement stop() and destroy()
- Clear references to external resources
- Stop background threads
- Remove event listeners

### Issue: Slow Startup Times
- **Solution**: Enable parallel startup
- Optimize configuration parsing
- Reduce unnecessary initialization
- Profile startup bottlenecks

## Security Considerations

### Configuration Security
- Restrict access to configuration files
- Validate XML input to prevent XXE attacks
- Use secure property substitution
- Encrypt sensitive configuration values

### Runtime Security
- Validate MBean operations
- Secure JMX access
- Audit configuration changes
- Implement proper access controls

## Related Documentation
- [Web Application Deployment](webapp-deployment.md) - Context configuration integration
- [Resource Management](resource-management.md) - JNDI configuration
- [Monitoring Domain](monitoring.md) - JMX and lifecycle monitoring
- [Security Domain](security.md) - Security configuration parsing