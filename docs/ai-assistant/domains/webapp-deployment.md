# Web Application Deployment Domain

## Purpose
Manages the complete lifecycle of web application deployment in Tomcat, from WAR file processing and context configuration to classloader management and hot deployment. This domain orchestrates how applications are discovered, loaded, configured, started, and monitored for changes.

## Architecture Overview
```
HostConfig (Deployment Orchestrator)
├── DeployDescriptors (XML context files)
├── DeployWARs (WAR archives)
├── DeployDirectories (Exploded WARs)
└── Background Processing (Change monitoring)
    
ContextConfig (Application Configurator)
├── Parse web.xml/web-fragment.xml
├── Process annotations (@WebServlet, etc.)
├── Configure ServletContainerInitializers
└── Apply default configurations

WebappClassLoader (Resource Loading)
├── Parent-last delegation model
├── Resource caching
├── Leak prevention
└── Instrumentation support
```

## Key Components

### Core Classes
- **`HostConfig`** (`o.a.catalina.startup`) - Main deployment orchestrator, monitors deployment directories
- **`ContextConfig`** (`o.a.catalina.startup`) - Configures individual contexts during startup
- **`StandardContext`** (`o.a.catalina.core`) - Context implementation managing application lifecycle
- **`WebappClassLoaderBase`** (`o.a.catalina.loader`) - Custom classloader with leak prevention
- **`WebappLoader`** (`o.a.catalina.loader`) - Manages classloader lifecycle
- **`Catalina`** (`o.a.catalina.startup`) - Bootstrap class for server startup
- **`ContextRuleSet`** (`o.a.catalina.startup`) - Digester rules for context.xml parsing
- **`WebXmlParser`** (`o.a.tomcat.util.descriptor.web`) - Parses deployment descriptors

### Key Interfaces
- **`Loader`** - Abstracts classloader management
- **`Lifecycle`** - Standard lifecycle management
- **`Container`** - Base container interface
- **`WebResourceRoot`** - Resource access abstraction

## Entry Points

### Primary Entry Points
1. **`HostConfig.lifecycleEvent()`** - Receives lifecycle events from Host
2. **`HostConfig.deployApps()`** - Main deployment orchestration
3. **`ContextConfig.lifecycleEvent()`** - Context configuration trigger
4. **`StandardContext.startInternal()`** - Context startup sequence

### Configuration Entry Points
- `server.xml` → `<Host>` attributes (appBase, autoDeploy, deployOnStartup)
- `context.xml` → Context-specific configuration
- `web.xml` → Application deployment descriptor
- Manager webapp → REST/HTML deployment interface

## Deployment Flow

### 1. Discovery Phase
```java
// HostConfig.deployApps() orchestrates three parallel phases:
deployDescriptors() → XML files in configBase
deployWARs()        → WAR files in appBase  
deployDirectories() → Directories in appBase
```

### 2. Context Creation
```java
// For each discovered application:
1. Create StandardContext instance
2. Set docBase (WAR/directory path)
3. Configure from context.xml if present
4. Add to Host container
```

### 3. Configuration Phase (ContextConfig)
```java
1. beforeStart() → Process context.xml
2. configureStart() → 
   - Parse web.xml
   - Scan for annotations
   - Process web-fragment.xml files
   - Apply ServletContainerInitializers
3. configureStop() → Cleanup
```

### 4. ClassLoader Setup
```java
// WebappLoader creates WebappClassLoaderBase:
- Sets up parent-last delegation
- Configures delegate patterns
- Initializes resource roots
- Enables instrumentation if needed
```

### 5. Application Start
```java
// StandardContext.startInternal():
1. Fire BEFORE_START events
2. Initialize resources
3. Create wrapper instances
4. Load servlets on startup
5. Fire START events
```

## Hot Deployment & Monitoring

### Change Detection
```java
// Background thread checks every 10 seconds:
- Modified WAR files → Redeploy
- New WAR/directory → Deploy
- Deleted resources → Undeploy
- Modified context.xml → Reload or redeploy
```

### Resource Tracking
```java
class DeployedApplication {
    LinkedHashMap<String, Long> redeployResources;
    LinkedHashMap<String, Long> reloadResources;
    // Tracks timestamps with 1-second resolution
}
```

## ClassLoader Architecture

### Hierarchy
```
Bootstrap ClassLoader (JVM)
└── System ClassLoader (CLASSPATH)
    └── Common ClassLoader ($CATALINA_HOME/lib)
        └── WebappClassLoader (WEB-INF/lib, WEB-INF/classes)
```

### Parent-Last Delegation
```java
// WebappClassLoaderBase.loadClass() logic:
1. Check cached classes
2. Try system/delegate classes
3. Search local repositories first (parent-last)
4. Delegate to parent if not found
5. Throw ClassNotFoundException
```

### Leak Prevention
- Clears ThreadLocal variables
- Stops application threads
- Unregisters JDBC drivers
- Clears static references
- Monitors for memory leaks

## Common Operations

### Deploy WAR Programmatically
```java
Context context = new StandardContext();
context.setDocBase("/path/to/app.war");
context.setPath("/myapp");
host.addChild(context);
```

### Configure Auto-Deployment
```xml
<Host appBase="webapps" 
      unpackWARs="true"
      autoDeploy="true"
      deployOnStartup="true">
```

### Custom ClassLoader
```java
WebappLoader loader = new WebappLoader();
loader.setDelegate(true); // Parent-first
loader.setReloadable(true);
context.setLoader(loader);
```

### Monitor Deployment
```java
// JMX MBean: Catalina:type=Deployer,host=localhost
- deploy(String contextPath, String war)
- undeploy(String contextPath)
- reload(String contextPath)
- findDeployedApps()
```

## Dependencies

### Direct Dependencies
- **Lifecycle Management** → `o.a.catalina.Lifecycle`
- **Resource Access** → `o.a.catalina.WebResourceRoot`
- **Descriptor Parsing** → `o.a.tomcat.util.descriptor`
- **Digester** → `o.a.tomcat.util.digester`
- **File I/O** → `java.nio.file`

### Integration Points
- **Security Domain** → Authenticator configuration
- **Session Management** → Manager configuration
- **Request Processing** → Wrapper registration
- **JNDI/Resources** → Resource injection

## Testing Strategies

### Unit Tests
```java
// Test context configuration
@Test
public void testContextConfig() {
    ContextConfig config = new ContextConfig();
    StandardContext context = new StandardContext();
    // Test annotation scanning, web.xml parsing
}
```

### Integration Tests
```java
// Test full deployment
Tomcat tomcat = getTomcatInstance();
Context ctx = tomcat.addWebapp("/test", warFile);
tomcat.start();
// Verify deployment success
```

### Performance Tests
- Measure deployment time for large WARs
- Test parallel deployment scaling
- Benchmark annotation scanning
- Profile classloader performance

## Performance Considerations

### Optimization Points
1. **Parallel Deployment** - Use StartStopExecutor for concurrent deployment
2. **Annotation Scanning** - Cache scanning results, use metadata-complete
3. **JAR Scanning** - Configure jarsToSkip for faster startup
4. **ClassLoader Caching** - Resource caching reduces I/O

### Configuration Tuning
```xml
<!-- Disable JAR scanning for faster startup -->
<Context>
    <JarScanner scanManifest="false"
                scanClassPath="false"
                scanAllFiles="false"/>
</Context>
```

## Common Issues & Solutions

### Issue: Slow Deployment
- Enable parallel deployment: `startStopThreads > 1`
- Skip unnecessary JAR scanning
- Use `metadata-complete="true"` in web.xml

### Issue: Memory Leaks on Redeploy
- Enable leak prevention: `clearReferencesStopThreads="true"`
- Check for ThreadLocal cleanup
- Monitor with JVM profiler

### Issue: ClassNotFoundException
- Check classloader delegation settings
- Verify JAR placement (WEB-INF/lib vs shared/lib)
- Review parent-first/parent-last configuration

### Issue: Context Won't Deploy
- Check logs for parsing errors
- Verify file permissions
- Ensure valid context path
- Check for port conflicts

## Domain Expert Knowledge

### Design Patterns
- **Observer Pattern** - Lifecycle listeners for deployment events
- **Template Method** - ContextConfig configuration phases
- **Strategy Pattern** - Different deployment strategies (WAR/dir/XML)
- **Chain of Responsibility** - ClassLoader delegation

### Best Practices
1. Use context.xml for environment-specific configuration
2. Set `unpackWARs="false"` in production for security
3. Disable autoDeploy in production environments
4. Configure appropriate session timeout values
5. Use parallel deployment for faster startup

### Security Considerations
- Restrict deployment directories permissions
- Disable manager webapp in production
- Use SecurityManager for untrusted applications
- Configure proper umask for unpacked files
- Validate deployment descriptors against XXE attacks

## Related Documentation
- [Configuration Domain](configuration.md) - Server configuration management
- [Request Processing Domain](request-processing.md) - Request routing to contexts
- [Security Domain](security.md) - Authentication configuration
- [Resource Management Domain](resource-management.md) - JNDI resource binding