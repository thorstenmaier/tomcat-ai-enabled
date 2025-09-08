# Monitoring & Management Domain

## Purpose
Provides comprehensive monitoring, management, and diagnostic capabilities for Apache Tomcat through JMX integration, administrative interfaces, logging frameworks, and health monitoring. This domain exposes runtime metrics, enables remote management, handles operational logging, and integrates with external monitoring systems.

## Architecture Overview
```
Monitoring Architecture:
Application Components → JMX MBeans → Registry → MBeanServer → Management Clients
                     ↓
              JULI Logging → Handlers → Formatters → Log Outputs
                     ↓
              Access Logging → Valves → Pattern Formatting → Log Files
                     ↓
              Administrative Apps → Manager/Host-Manager → REST APIs
                     ↓
              Health Checks → HealthCheckValve → JSON Status
```

## Key Components

### JMX & MBean Management
- **`Registry`** (`o.a.tomcat.util.modeler`) - MBean registration and lifecycle
- **`ManagedBean`** (`o.a.tomcat.util.modeler`) - MBean metadata descriptor
- **`BaseModelMBean`** (`o.a.tomcat.util.modeler`) - Base MBean implementation
- **`JmxEnabled`** (`o.a.catalina`) - Interface for automatic JMX registration
- **`MBeanFactory`** (`o.a.catalina.mbeans`) - Runtime MBean creation

### System Metrics
- **`RequestInfo`** (`o.a.coyote`) - Individual request metrics
- **`RequestGroupInfo`** (`o.a.coyote`) - Aggregated request statistics
- **`ThreadPool`** MBeans - Thread pool monitoring
- **`GlobalRequestProcessor`** - Global request processing metrics
- **`DataSource`** MBeans - Connection pool monitoring

### Administrative Interfaces
- **`ManagerServlet`** (`o.a.catalina.manager`) - Application management
- **`HTMLManagerServlet`** (`o.a.catalina.manager`) - Web-based management
- **`HostManagerServlet`** (`o.a.catalina.manager`) - Host management
- **`StatusServlet`** (`o.a.catalina.manager`) - System status reporting
- **`JMXProxyServlet`** (`o.a.catalina.manager`) - JMX-over-HTTP bridge

### Logging Framework (JULI)
- **`ClassLoaderLogManager`** (`o.a.juli`) - Per-classloader logging isolation
- **`DirectJDKLog`** (`o.a.juli.logging`) - Direct JDK logging integration
- **`FileHandler`** (`o.a.juli`) - File-based log handler
- **`AsyncFileHandler`** (`o.a.juli`) - Asynchronous file logging
- **`OneLineFormatter`** (`o.a.juli`) - Single-line log formatting

### Access Logging
- **`AccessLogValve`** (`o.a.catalina.valves`) - Request access logging
- **`ExtendedAccessLogValve`** (`o.a.catalina.valves`) - Extended format logging
- **`JsonAccessLogValve`** (`o.a.catalina.valves`) - JSON format logging
- **`RemoteAddrValve`** (`o.a.catalina.valves`) - IP-based access control

### Health & Diagnostics
- **`HealthCheckValve`** (`o.a.catalina.valves`) - Health endpoint implementation
- **`CrawlerSessionManagerValve`** (`o.a.catalina.valves`) - Bot session management
- **`StuckThreadDetectionValve`** (`o.a.catalina.valves`) - Thread monitoring

## Entry Points

### JMX Management
- Remote JMX: `service:jmx:rmi:///jndi/rmi://host:port/jmxrmi`
- Local JMX: `MBeanServer.getPlatformMBeanServer()`
- HTTP JMX Proxy: `http://host:port/manager/jmxproxy`

### Administrative Access
- Manager App: `http://host:port/manager/`
- Host Manager: `http://host:port/host-manager/`
- Status Servlet: `http://host:port/manager/status`

### Health Monitoring
- Health Check: `http://host:port/health`
- Readiness: `http://host:port/ready`
- Custom health endpoints via HealthCheckValve

## JMX Integration

### MBean Registration
```java
// Automatic registration via JmxEnabled
public class StandardEngine extends ContainerBase implements Engine, JmxEnabled {
    @Override
    protected String getDomainInternal() {
        return MBeanUtils.getDomain(getParent());
    }
    
    @Override
    protected String getObjectNameKeyProperties() {
        return "type=Engine";
    }
}
```

### Registry Pattern
```java
// MBean lifecycle management
public class Registry {
    private static Registry registry = null;
    private MBeanServer server = null;
    private Map<String, ManagedBean> descriptors = new HashMap<>();
    
    public static Registry getRegistry() {
        if (registry == null) {
            registry = new Registry();
        }
        return registry;
    }
    
    public void registerComponent(Object bean, String oname, String type) 
            throws Exception {
        ManagedBean managed = findManagedBean(bean.getClass());
        DynamicMBean mbean = managed.createMBean(bean);
        
        ObjectName objectName = new ObjectName(oname);
        server.registerMBean(mbean, objectName);
    }
}
```

### MBean Descriptors
```xml
<!-- mbeans-descriptors.xml -->
<mbeans-descriptors>
    <mbean name="StandardContext"
           description="Standard Context Component"
           domain="Catalina"
           group="Context"
           type="org.apache.catalina.core.StandardContext">
           
        <attribute name="available"
                  description="Is context available"
                  type="boolean"
                  writeable="false"/>
                  
        <attribute name="path"
                  description="Context path"
                  type="java.lang.String"
                  writeable="false"/>
                  
        <operation name="reload"
                  description="Reload the Context"
                  impact="ACTION"
                  returnType="void"/>
    </mbean>
</mbeans-descriptors>
```

## Monitoring Components

### Request Processing Metrics
```java
public class RequestInfo {
    private long bytesReceived = 0;
    private long bytesSent = 0;
    private long processingTime = 0;
    private long requestCount = 0;
    private int maxTime = 0;
    private String maxRequestUri = null;
    private volatile String currentUri = null;
    private volatile String currentQueryString = null;
    private volatile String method = null;
    private volatile String contentType = null;
    private volatile int contentLength = -1;
}

// Aggregated statistics
public class RequestGroupInfo {
    private final List<RequestInfo> processors = new ArrayList<>();
    
    public synchronized long getBytesReceived() {
        long bytes = 0;
        for (RequestInfo processor : processors) {
            bytes += processor.getBytesReceived();
        }
        return bytes;
    }
    
    public synchronized long getProcessingTime() {
        long time = 0;
        for (RequestInfo processor : processors) {
            time += processor.getProcessingTime();
        }
        return time;
    }
}
```

### Thread Pool Monitoring
```java
// NioEndpoint MBean
public class NioEndpoint extends AbstractJsseEndpoint<NioChannel,SocketChannel> {
    public int getCurrentThreadCount() {
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getActiveCount();
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }
    
    public int getCurrentThreadsBusy() {
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
                return tpe.getActiveCount();
            }
        }
        return 0;
    }
}
```

## Administrative Interfaces

### Manager Application
```java
public class ManagerServlet extends HttpServlet implements ContainerServlet {
    // Application deployment
    protected void deploy(PrintWriter writer, String contextPath,
                         String war, String config, boolean update,
                         HttpServletRequest request) {
        // Deploy new application
        Context context = deployer.deploy(contextPath, war, config);
        if (context != null) {
            writer.println(sm.getString("managerServlet.deployed", contextPath));
        }
    }
    
    // List applications
    protected void list(PrintWriter writer, String filterBy, String statusLine) {
        Container[] contexts = host.findChildren();
        for (Container container : contexts) {
            Context context = (Context) container;
            if (context != null) {
                writer.println("OK - Listed applications for virtual host "
                             + host.getName());
                writer.print(context.getPath());
                writer.print(":");
                writer.print(context.getState());
                // ... more status info
            }
        }
    }
}
```

### JMX Proxy Servlet
```java
public class JMXProxyServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        response.setContentType("text/plain");
        
        String command = request.getParameter("cmd");
        String objectName = request.getParameter("objectname");
        
        if ("list".equals(command)) {
            listBeans(response.getWriter(), objectName);
        } else if ("get".equals(command)) {
            String attribute = request.getParameter("att");
            getAttribute(response.getWriter(), objectName, attribute);
        } else if ("set".equals(command)) {
            String attribute = request.getParameter("att");
            String value = request.getParameter("val");
            setAttribute(response.getWriter(), objectName, attribute, value);
        }
    }
}
```

### Status Reporting
```java
public class StatusServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        response.setContentType("text/xml");
        PrintWriter writer = response.getWriter();
        
        writer.println("<?xml version='1.0' encoding='utf-8'?>");
        writer.println("<status>");
        
        // Server information
        writer.println("<jvm>");
        Runtime runtime = Runtime.getRuntime();
        writer.println("  <memory free='" + runtime.freeMemory() + 
                      "' total='" + runtime.totalMemory() + 
                      "' max='" + runtime.maxMemory() + "'/>");
        writer.println("</jvm>");
        
        // Connector status
        Service[] services = server.findServices();
        for (Service service : services) {
            writer.println("<service name='" + service.getName() + "'>");
            Connector[] connectors = service.findConnectors();
            for (Connector connector : connectors) {
                writeConnectorState(writer, connector);
            }
            writer.println("</service>");
        }
        
        writer.println("</status>");
    }
}
```

## JULI Logging Framework

### ClassLoader Isolation
```java
public class ClassLoaderLogManager extends LogManager {
    protected final Map<ClassLoader, ClassLoaderLogInfo> classLoaderLoggers =
        new WeakHashMap<>();
        
    protected ClassLoaderLogInfo getClassLoaderInfo(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        
        ClassLoaderLogInfo info = classLoaderLoggers.get(classLoader);
        if (info == null) {
            final ClassLoader classLoaderParam = classLoader;
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    try {
                        readConfiguration(classLoaderParam);
                    } catch (IOException e) {
                        // Handle error
                    }
                    return null;
                }
            });
            info = classLoaderLoggers.get(classLoader);
        }
        return info;
    }
}
```

### Async Logging
```java
public class AsyncFileHandler extends FileHandler {
    private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>();
    private volatile Thread writerThread;
    private volatile boolean closed = false;
    
    @Override
    public void publish(LogRecord record) {
        if (!closed) {
            // Add to queue for async processing
            queue.offer(record);
        }
    }
    
    private void startWriterThread() {
        writerThread = new Thread(() -> {
            LogRecord record;
            while (!closed) {
                try {
                    record = queue.take();
                    super.publish(record); // Delegate to parent
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        writerThread.setDaemon(true);
        writerThread.start();
    }
}
```

### JSON Formatting
```java
public class JsonFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"timestamp\":\"").append(Instant.ofEpochMilli(record.getMillis()))
          .append("\",");
        sb.append("\"level\":\"").append(record.getLevel()).append("\",");
        sb.append("\"logger\":\"").append(record.getLoggerName()).append("\",");
        sb.append("\"message\":\"").append(escape(formatMessage(record)))
          .append("\",");
        sb.append("\"thread\":\"").append(record.getThreadID()).append("\"");
        
        if (record.getThrown() != null) {
            sb.append(",\"exception\":\"").append(escape(getStackTrace(record.getThrown())))
              .append("\"");
        }
        
        sb.append("}").append(System.lineSeparator());
        return sb.toString();
    }
}
```

## Access Logging

### Access Log Valve
```java
public class AccessLogValve extends ValveBase implements AccessLog {
    protected String pattern = "common";
    protected String prefix = "localhost_access_log";
    protected String suffix = ".txt";
    
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        getNext().invoke(request, response);
        
        // Log after processing
        CharArrayWriter result = new CharArrayWriter(128);
        
        for (AccessLogElement element : logElements) {
            element.addElement(result, request, response, time);
        }
        
        log(result.toString());
    }
    
    // Pattern elements
    protected static final Map<String, Class<? extends AccessLogElement>> elementMap 
        = new HashMap<>();
    static {
        elementMap.put("a", RemoteAddrElement.class);
        elementMap.put("A", LocalAddrElement.class);
        elementMap.put("b", BytesSentElement.class);
        elementMap.put("B", BytesSentElement.class);
        elementMap.put("h", HostElement.class);
        elementMap.put("H", ProtocolElement.class);
        elementMap.put("l", LogicalUserNameElement.class);
        elementMap.put("m", MethodElement.class);
        elementMap.put("p", PortElement.class);
        elementMap.put("q", QueryElement.class);
        elementMap.put("r", RequestElement.class);
        elementMap.put("s", HttpStatusCodeElement.class);
        elementMap.put("S", SessionIdElement.class);
        elementMap.put("t", DateTimeElement.class);
        elementMap.put("T", ElapsedTimeElement.class);
        elementMap.put("u", UserElement.class);
        elementMap.put("U", RequestURIElement.class);
        elementMap.put("v", LocalServerNameElement.class);
        elementMap.put("D", ElapsedTimeElement.class);
        elementMap.put("F", FirstByteTimeElement.class);
        elementMap.put("I", ThreadNameElement.class);
    }
}
```

### JSON Access Logging
```java
public class JsonAccessLogValve extends AccessLogValve {
    @Override
    protected AccessLogElement createAccessLogElement(String name, char pattern) {
        switch (pattern) {
            case 'a':
                return new JsonRemoteAddrElement();
            case 'A':
                return new JsonLocalAddrElement();
            case 'b':
                return new JsonBytesSentElement();
            case 't':
                return new JsonDateTimeElement();
            // ... more JSON elements
            default:
                return super.createAccessLogElement(name, pattern);
        }
    }
    
    private class JsonAccessLogElement implements AccessLogElement {
        @Override
        public void addElement(CharArrayWriter buf, Request request, 
                             Response response, long time) {
            buf.append("{");
            addJsonContent(buf, request, response, time);
            buf.append("}");
        }
    }
}
```

## Health Monitoring

### Health Check Valve
```java
public class HealthCheckValve extends ValveBase {
    private String healthCheckPath = "/health";
    private boolean allowHealthCheckFromRemote = false;
    
    @Override
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        
        if (healthCheckPath.equals(request.getRequestURI())) {
            if (!allowHealthCheckFromRemote) {
                String addr = request.getRemoteAddr();
                if (!"127.0.0.1".equals(addr) && !"::1".equals(addr) && 
                    !"0:0:0:0:0:0:0:1".equals(addr)) {
                    response.setStatus(403);
                    return;
                }
            }
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Check container health
            Container container = getContainer();
            boolean healthy = checkContainerHealth(container);
            
            response.setStatus(healthy ? 200 : 503);
            PrintWriter writer = response.getWriter();
            writer.print("{\"status\":\"");
            writer.print(healthy ? "UP" : "DOWN");
            writer.print("\",\"checks\":[");
            writeHealthDetails(writer, container);
            writer.print("]}");
            writer.flush();
            
            return;
        }
        
        getNext().invoke(request, response);
    }
    
    private boolean checkContainerHealth(Container container) {
        if (container == null) return false;
        
        LifecycleState state = container.getState();
        if (!state.isAvailable()) {
            return false;
        }
        
        // Check children recursively
        Container[] children = container.findChildren();
        for (Container child : children) {
            if (!checkContainerHealth(child)) {
                return false;
            }
        }
        
        return true;
    }
}
```

## External System Integration

### Prometheus Integration Example
```java
public class PrometheusMetricsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        resp.setContentType("text/plain; version=0.0.4; charset=utf-8");
        PrintWriter writer = resp.getWriter();
        
        // Export JMX metrics in Prometheus format
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        
        // Thread pool metrics
        ObjectName threadPoolName = new ObjectName("Catalina:type=ThreadPool,name=*");
        Set<ObjectName> threadPools = mbs.queryNames(threadPoolName, null);
        
        for (ObjectName name : threadPools) {
            String poolName = name.getKeyProperty("name");
            
            Integer currentThreadCount = (Integer) mbs.getAttribute(name, "currentThreadCount");
            Integer currentThreadsBusy = (Integer) mbs.getAttribute(name, "currentThreadsBusy");
            
            writer.println("# HELP tomcat_threads_current Current thread count");
            writer.println("# TYPE tomcat_threads_current gauge");
            writer.println("tomcat_threads_current{pool=\"" + poolName + "\"} " + currentThreadCount);
            
            writer.println("# HELP tomcat_threads_busy Current busy thread count");
            writer.println("# TYPE tomcat_threads_busy gauge");
            writer.println("tomcat_threads_busy{pool=\"" + poolName + "\"} " + currentThreadsBusy);
        }
        
        // Request processing metrics
        exportRequestMetrics(writer, mbs);
        
        writer.flush();
    }
}
```

## Common Operations

### Enable JMX Remote Access
```bash
# Add to CATALINA_OPTS
export CATALINA_OPTS="$CATALINA_OPTS -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false"
```

### Configure Access Logging
```xml
<!-- In server.xml or context.xml -->
<Valve className="org.apache.catalina.valves.AccessLogValve"
       directory="logs"
       prefix="access_log"
       suffix=".txt"
       pattern="%h %l %u %t &quot;%r&quot; %s %b %D"
       rotatable="true"/>
       
<!-- JSON access logging -->
<Valve className="org.apache.catalina.valves.JsonAccessLogValve"
       directory="logs"
       prefix="access_log"
       suffix=".json"/>
```

### Custom Health Check
```xml
<Valve className="org.apache.catalina.valves.HealthCheckValve"
       healthCheckPath="/health"
       allowHealthCheckFromRemote="false"/>
```

### JULI Configuration
```properties
# logging.properties
handlers = org.apache.juli.AsyncFileHandler
org.apache.juli.AsyncFileHandler.level = INFO
org.apache.juli.AsyncFileHandler.directory = logs
org.apache.juli.AsyncFileHandler.prefix = catalina.
org.apache.juli.AsyncFileHandler.suffix = .log
org.apache.juli.AsyncFileHandler.formatter = org.apache.juli.JsonFormatter

# Logger levels
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].level = INFO
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].handlers = org.apache.juli.FileHandler
```

## Performance Considerations

### JMX Performance
- Use MBean attribute caching for frequently accessed metrics
- Implement lazy loading for expensive calculations
- Consider MBean proxy patterns for remote access
- Monitor MBean registration/deregistration overhead

### Logging Performance  
- Use asynchronous handlers for high-traffic logging
- Configure appropriate buffer sizes
- Implement log rotation and compression
- Consider structured logging for better parsing performance

### Monitoring Overhead
- Sample metrics rather than continuous collection
- Use push vs pull strategies appropriately  
- Implement circuit breakers for external monitoring
- Cache expensive metric calculations

## Testing Strategies

### JMX Testing
```java
@Test
public void testMBeanRegistration() throws Exception {
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName("Catalina:type=Engine");
    
    assertTrue(mbs.isRegistered(name));
    
    String state = (String) mbs.getAttribute(name, "stateName");
    assertEquals("STARTED", state);
}
```

### Health Check Testing
```java
@Test
public void testHealthEndpoint() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/health");
    
    MockHttpServletResponse response = new MockHttpServletResponse();
    
    HealthCheckValve valve = new HealthCheckValve();
    valve.invoke(request, response);
    
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"status\":\"UP\""));
}
```

## Security Considerations

### JMX Security
- Enable JMX authentication and SSL
- Restrict JMX access to authorized networks
- Use role-based access control
- Monitor JMX access patterns

### Administrative Interface Security
- Secure manager applications with strong credentials
- Use HTTPS for administrative access
- Implement IP-based access restrictions
- Audit administrative operations

### Logging Security
- Sanitize log outputs to prevent injection
- Secure log file access and rotation
- Implement log integrity checking
- Consider log encryption for sensitive data

## Related Documentation
- [Configuration Domain](configuration.md) - MBean configuration and lifecycle
- [Security Domain](security.md) - Administrative security integration
- [Web Application Deployment](webapp-deployment.md) - Deployment monitoring
- [Network I/O Domain](network-io.md) - Connection and request monitoring