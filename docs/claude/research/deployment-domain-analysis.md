# Web Application Deployment Domain Research Report

## Research Summary
Apache Tomcat's web application deployment domain is a sophisticated system centered around `HostConfig` and `ContextConfig` lifecycle listeners that manage WAR expansion, context initialization, classloader setup, and hot deployment through file monitoring and background processes.

## Key Findings

### 1. Core Deployment Architecture
- **HostConfig** (`o.a.c.startup.HostConfig`): Primary deployment controller implementing `LifecycleListener`
  - Manages three deployment types: XML descriptors, WAR files, and directories
  - Controls auto-deployment and hot-reload through background monitoring
  - Tracks deployed applications via `DeployedApplication` inner class with resource timestamps

- **ContextConfig** (`o.a.c.startup.ContextConfig`): Context initialization orchestrator
  - Parses web.xml and context.xml using Apache Commons Digester
  - Processes servlet annotations and ServletContainerInitializers
  - Configures security constraints, filters, servlets, and listeners
  - Manages JSP configuration and tag library scanning

### 2. Deployment Flow Sequence

#### Initial Deployment (deployOnStartup)
```
HostConfig.lifecycleEvent(START_EVENT)
  → deployApps()
    → deployDescriptors() - XML files from conf/Catalina/[host]
    → deployWARs() - WAR files from webapps
    → deployDirectories() - Expanded directories from webapps
      → Each creates Context and triggers ContextConfig
        → ContextConfig.configureStart()
          → webConfig() - Parse descriptors
          → processAnnotations() - Scan for @WebServlet, @WebFilter
          → configureContext() - Apply configuration to StandardContext
```

#### Hot Deployment (autoDeploy)
```
HostConfig.lifecycleEvent(PERIODIC_EVENT)
  → check()
    → checkResources() - Monitor file modifications
      → If changed: undeploy() → deployApps()
    → deployApps() - Deploy new applications
```

### 3. Classloader Hierarchy and Implementation

#### Classloader Structure
- **WebappLoader** (`o.a.c.loader.WebappLoader`): Manages lifecycle of web app classloader
- **WebappClassLoaderBase**: Abstract base implementing parent-last delegation
  - **WebappClassLoader**: Standard implementation
  - **ParallelWebappClassLoader**: Parallel-capable variant (default in Tomcat 8+)

#### Classloader Delegation Model
```
Bootstrap ClassLoader
  ↓
System ClassLoader  
  ↓
Common ClassLoader (shared/lib)
  ↓
WebappClassLoaderBase (WEB-INF/classes, WEB-INF/lib)
```

Parent-last delegation ensures webapp classes override container classes (except java.* and javax.*).

### 4. WAR File Deployment Mechanisms

#### ExpandWar Utility (`o.a.c.startup.ExpandWar`)
- Static methods for WAR expansion and deletion
- Handles anti-locking by copying WARs to temp directory
- Preserves timestamps for incremental deployment

#### Deployment Modes
1. **Packed WAR**: Runs directly from WAR (if unpackWARs=false)
2. **Expanded WAR**: Extracts to directory (default, unpackWARs=true)
3. **External WAR**: Deploys from location outside appBase

### 5. Context Configuration Processing

#### Descriptor Hierarchy (Merged in order)
1. Global web.xml (`conf/web.xml`)
2. Host web.xml defaults (`conf/[engine]/[host]/web.xml.default`)
3. Application web.xml (`WEB-INF/web.xml`)
4. Web fragments (`META-INF/web-fragment.xml` in JARs)
5. Annotations (`@WebServlet`, `@WebFilter`, `@WebListener`)
6. Programmatic configuration (ServletContainerInitializers)

#### Digester-based Parsing
- Uses Apache Commons Digester for XML→Object mapping
- Rule sets defined in `o.a.t.util.descriptor.web.WebRuleSet`
- Supports both interpreted and generated (compiled) parsing

### 6. Hot Deployment and Reload Implementation

#### Resource Monitoring
- `DeployedApplication.redeployResources`: LinkedHashMap<String, Long> tracking file→timestamp
- Monitored resources:
  - WAR files
  - Expanded directories  
  - context.xml descriptors
  - Global configuration files

#### Reload Triggers
- WAR modification → Reload context (preserves XML descriptor)
- Directory timestamp change → Full redeploy
- context.xml change → Full redeploy
- WEB-INF/web.xml change → Handled by StandardContext.reload()

#### Background Processing
- `HostConfig.check()` called via Host.backgroundProcess()
- Default interval: 10 seconds (configurable via backgroundProcessorDelay)
- File modification resolution: 1000ms (prevents same-second modification misses)

### 7. StandardContext Lifecycle Integration

#### Key Lifecycle Methods
- `startInternal()`: Main initialization sequence
  - Creates WebappLoader
  - Initializes resources
  - Fires `CONFIGURE_START_EVENT` → triggers ContextConfig
  - Loads servlets marked load-on-startup
  - Starts container pipeline

#### Context States
- STARTING_PREP → STARTING → STARTED
- STOPPING_PREP → STOPPING → STOPPED
- Each transition fires lifecycle events consumed by listeners

### 8. Parallel Deployment Support

#### Version Management
- `ContextName` class handles versioning (format: `/context##version`)
- Supports multiple versions running simultaneously
- Automatic undeployment of old versions (undeployOldVersions=true)
- Session migration between versions

### 9. Security and Isolation

#### WebappClassLoader Security
- Implements `InstrumentableClassLoader` for instrumentation support
- Prevents loading of system classes from webapp
- Clears references on stop to prevent memory leaks
- ThreadLocal and static field cleanup

#### Anti-Locking Features (Windows)
- antiResourceLocking: Copies resources to temp directory
- antiJARLocking: Disables JAR file locking (deprecated)

### 10. ServletContainerInitializer Processing

#### Discovery and Invocation
- `WebappServiceLoader` discovers SCIs via META-INF/services
- `@HandlesTypes` annotation processing for type interest
- Invoked before context startup with discovered classes
- Supports programmatic servlet/filter/listener registration

## Relevant Code Locations

### Core Deployment Classes
- `/java/org/apache/catalina/startup/HostConfig.java`: Main deployment controller
- `/java/org/apache/catalina/startup/ContextConfig.java`: Context configuration
- `/java/org/apache/catalina/startup/ExpandWar.java`: WAR expansion utilities
- `/java/org/apache/catalina/startup/ContextName.java`: Context name/version parsing
- `/java/org/apache/catalina/startup/WebappServiceLoader.java`: SCI discovery

### Classloader Implementation
- `/java/org/apache/catalina/loader/WebappLoader.java`: Loader lifecycle management
- `/java/org/apache/catalina/loader/WebappClassLoaderBase.java`: Core classloader logic
- `/java/org/apache/catalina/loader/ParallelWebappClassLoader.java`: Default implementation

### Context Implementation
- `/java/org/apache/catalina/core/StandardContext.java`: Main context implementation
- `/java/org/apache/catalina/core/StandardHost.java`: Host container with deployment flags

### Descriptor Parsing
- `/java/org/apache/tomcat/util/descriptor/web/WebXml.java`: Web.xml model
- `/java/org/apache/tomcat/util/descriptor/web/WebXmlParser.java`: Parser implementation
- `/java/org/apache/tomcat/util/descriptor/web/WebRuleSet.java`: Digester rules

## Technical Details

### Deployment Properties
```java
// HostConfig
autoDeploy = true          // Enable hot deployment
deployOnStartup = true     // Deploy on server start
unpackWARs = true         // Extract WAR files
deployXML = true          // Process context.xml
copyXML = false           // Copy context.xml to config base

// StandardContext  
reloadable = false        // Monitor WEB-INF/classes and WEB-INF/lib
unpackWAR = true         // Context-specific override
antiResourceLocking = false  // Windows file locking workaround
```

### Resource Monitoring Algorithm
```java
// Simplified checkResources logic
for (String resource : app.redeployResources.keySet()) {
    File file = new File(resource);
    long lastModified = app.redeployResources.get(resource);
    
    if (file.lastModified() != lastModified) {
        if (file.isDirectory()) {
            // Update timestamp only
            app.redeployResources.put(resource, file.lastModified());
        } else if (resource.endsWith(".war") && app.hasDescriptor) {
            // Reload context for WAR with XML
            reloadContext(app);
        } else {
            // Full redeploy for other changes
            undeploy(app);
            deploy(app);
        }
    }
}
```

### Annotation Processing Flow
```java
// Parallel annotation scanning (Tomcat 8.5+)
if (context.getParallelAnnotationScanning()) {
    ExecutorService executor = server.getUtilityExecutor();
    List<Future<?>> futures = new ArrayList<>();
    
    for (WebXml fragment : fragments) {
        futures.add(executor.submit(() -> 
            scanWebXmlFragment(fragment, javaClassCache)));
    }
    
    // Wait for completion
    for (Future<?> future : futures) {
        future.get();
    }
}
```

## Integration Points with Other Domains

### Network I/O Domain
- Connector configuration affects webapp request handling
- Protocol handlers determine HTTP features available to apps

### Request Processing Domain  
- CoyoteAdapter bridges to StandardContext
- Pipeline valves process requests through context

### Session Management Domain
- StandardManager created per context
- Session persistence across reloads
- Distributable session support

### Security Domain
- Authenticator valves configured by ContextConfig
- Security constraints from web.xml
- JAAS realm integration

### Lifecycle Domain
- All deployment components implement Lifecycle
- Event-driven coordination between components
- Graceful shutdown and cleanup

## Common Patterns

### Lifecycle Listener Pattern
All major deployment components use lifecycle listeners for loose coupling:
- HostConfig listens to Host events
- ContextConfig listens to Context events
- Clean separation of concerns

### Resource Tracking Pattern
LinkedHashMap<String, Long> for ordered resource→timestamp mapping:
- Maintains deployment order
- Enables incremental checking
- Supports rollback on failure

### Parallel Processing Pattern
ExecutorService usage for concurrent operations:
- Parallel deployment of multiple contexts
- Parallel annotation scanning
- Parallel JSP compilation

### Anti-Pattern Avoidance
- Never modify generated parser code directly
- Avoid classloader leaks through proper cleanup
- Prevent file locking on Windows platforms

## Performance Considerations

### Startup Optimization
- Parallel deployment via StartStopExecutor
- Lazy servlet initialization (load-on-startup)
- Bytecode scanning caching with JavaClassCacheEntry
- Generated (compiled) Digester rules

### Runtime Optimization  
- Background thread for deployment checks
- Modification time resolution awareness (1000ms)
- Incremental resource checking
- WAR expansion avoidance option

### Memory Management
- WebappClassLoader leak prevention
- ThreadLocal cleanup
- Static field clearing
- Weak reference usage for caching

## Recommendations

### For Development
1. Enable `reloadable=true` for development contexts
2. Use `antiResourceLocking=true` on Windows
3. Leverage parallel deployment for zero-downtime updates
4. Monitor logs for deployment timing and issues

### For Production
1. Disable `autoDeploy` and `deployOnStartup` for stability
2. Use external deployment tools (e.g., Manager app)
3. Pre-compile JSPs to reduce startup time
4. Configure appropriate backgroundProcessorDelay

### For Debugging Deployment Issues
1. Enable debug logging for `org.apache.catalina.startup`
2. Check expanded WAR permissions and ownership
3. Verify context.xml syntax and placement
4. Monitor file timestamps with trace logging
5. Use JMX to inspect deployed applications

## Key Interfaces and Extension Points

### Custom Deployment
- Extend HostConfig for custom deployment logic
- Implement LifecycleListener for deployment hooks
- Create custom WebappClassLoader for special loading needs

### ServletContainerInitializer
- Programmatic configuration without web.xml
- Framework integration point (Spring, CDI)
- Dynamic servlet registration

### Custom Context Implementation
- Extend StandardContext for specialized behavior
- Override reload() for custom reload logic
- Implement custom resource loading

This comprehensive analysis provides a complete understanding of Tomcat's web application deployment mechanisms, from WAR file processing through context initialization to runtime hot deployment.