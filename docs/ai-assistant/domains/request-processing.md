# Request Processing Domain

## Purpose
Handles the complete HTTP request lifecycle from network reception through servlet execution and response generation.

## Architecture Overview
```
Network Socket → Coyote Protocol → Adapter → Catalina Pipeline → Servlet
                     ↓                ↓            ↓
              HTTP Parsing    Protocol Bridge  Valve Chain
```

## Key Components

### Primary Classes
- **`CoyoteAdapter`** - Bridge between protocol layer and servlet container
- **`Request`** (2 versions):
  - `org.apache.coyote.Request` - Protocol-level request
  - `org.apache.catalina.connector.Request` - Servlet API request
- **`Response`** (2 versions):
  - `org.apache.coyote.Response` - Protocol-level response  
  - `org.apache.catalina.connector.Response` - Servlet API response
- **`StandardWrapperValve`** - Final valve that invokes servlet

### Valve Pipeline
Standard processing chain:
```
StandardEngineValve
  → StandardHostValve
    → StandardContextValve  
      → StandardWrapperValve
        → FilterChain
          → Servlet
```

### Important Packages
- `org.apache.catalina.connector` - Request/response facades
- `org.apache.catalina.core` - Container implementations
- `org.apache.catalina.valves` - Request interceptors
- `org.apache.coyote` - Protocol handling

## Request Flow Detailed

### 1. Protocol Reception
```java
// NioEndpoint accepts socket
SocketChannel socket = serverSocket.accept();

// Processor handles protocol
Http11Processor.service(socketWrapper);
  → parseRequestLine()
  → parseHeaders()
```

### 2. Adapter Bridge
```java
CoyoteAdapter.service(request, response)
  → Creates Catalina Request/Response
  → Finds matching Host/Context
  → Invokes Pipeline
```

### 3. Pipeline Processing
```java
Pipeline.invoke(request, response)
  → Each Valve processes and calls next
  → Valves can modify request/response
  → Final valve invokes servlet
```

### 4. Servlet Invocation
```java
StandardWrapperValve.invoke()
  → Allocates servlet instance
  → Creates filter chain
  → Calls servlet.service()
```

## Key Interfaces

### Request Processing
- **`Valve`** - Pipeline component interface
- **`Pipeline`** - Valve container
- **`Wrapper`** - Servlet container
- **`FilterChain`** - Servlet filter chain

### Async Support
- **`AsyncContext`** - Async request handling
- **`AsyncStateMachine`** - Async state transitions
- **`AsyncListener`** - Async event callbacks

## Common Modifications

### Adding Custom Valve
```java
public class CustomValve extends ValveBase {
    @Override
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        // Pre-processing
        
        // Pass to next valve
        getNext().invoke(request, response);
        
        // Post-processing
    }
}
```

### Request Attribute Access
```java
// In CoyoteAdapter or Valve
request.setAttribute("custom.attribute", value);

// In Servlet
Object value = request.getAttribute("custom.attribute");
```

### Header Manipulation
```java
// Add request header (before servlet)
request.getCoyoteRequest().getMimeHeaders()
    .addValue("Custom-Header").setString("value");

// Add response header
response.addHeader("Custom-Response", "value");
```

## Performance Considerations

### Hotspots
- `CoyoteAdapter.service()` - Called for every request
- `ApplicationFilterChain.doFilter()` - Filter processing
- `StandardWrapperValve.invoke()` - Servlet allocation

### Optimization Tips
1. Minimize valve chain length
2. Cache request attributes when possible
3. Avoid synchronization in valves
4. Use direct ByteBuffer operations
5. Implement efficient async handling

## Testing This Domain

### Unit Tests
- `TestCoyoteAdapter` - Adapter functionality
- `TestRequest` - Request object behavior
- `TestStandardWrapper` - Servlet management

### Integration Tests  
- `TestAsync` - Async request handling
- `TestNonBlockingAPI` - Non-blocking I/O
- `TestRequestFilter` - Filter chain

### Performance Tests
```bash
# Specific request processing tests
ant test -Dtest.name="**/TestCoyoteAdapter.java"
ant test -Dtest.name="**/TestRequestPerformance.java"
```

## Debugging Tips

### Key Breakpoints
- `CoyoteAdapter.service()` line 300 - Request start
- `StandardWrapperValve.invoke()` line 200 - Servlet invocation
- `ApplicationFilterChain.internalDoFilter()` - Filter processing

### Useful Logging
```properties
# In logging.properties
org.apache.catalina.connector.level = FINE
org.apache.catalina.core.level = FINE
org.apache.coyote.level = FINE
```

### Request Tracking
```java
// Add to custom valve for request tracking
log.info("Request: " + request.getRequestURI() + 
         " Thread: " + Thread.currentThread().getName());
```

## Common Issues

### Issue: Request attributes lost
**Cause**: Recycling between requests  
**Solution**: Don't cache request/response objects

### Issue: Async timeout
**Cause**: Default timeout too short  
**Solution**: Set `asyncTimeout` on Context

### Issue: Character encoding
**Cause**: Late `setCharacterEncoding()`  
**Solution**: Set encoding before reading parameters

## Dependencies
- **Depends on**: Coyote (protocol), Juli (logging)
- **Used by**: All web applications, Jasper (JSP)

## Related Domains
- **[Network I/O](network-io.md)** - Socket handling
- **[Security](security.md)** - Authentication valves
- **[Session Management](session-management.md)** - Session creation