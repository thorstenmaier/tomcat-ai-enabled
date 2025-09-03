# Apache Tomcat API Endpoints Index

## Core Servlets

### DefaultServlet
**Location**: `java/org/apache/catalina/servlets/DefaultServlet.java`  
**Mapping**: Default (serves static content)  
**Methods**: GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE  
**Purpose**: Serves static files, directory listings, handles WebDAV

### JspServlet  
**Location**: `java/org/apache/jasper/servlet/JspServlet.java`  
**Mapping**: `*.jsp`, `*.jspx`  
**Methods**: GET, POST  
**Purpose**: Compiles and executes JSP pages

### CGIServlet
**Location**: `java/org/apache/catalina/servlets/CGIServlet.java`  
**Mapping**: `/cgi-bin/*` (when enabled)  
**Methods**: GET, POST  
**Purpose**: Executes CGI scripts

### WebdavServlet
**Location**: `java/org/apache/catalina/servlets/WebdavServlet.java`  
**Mapping**: Configured per deployment  
**Methods**: PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK  
**Purpose**: WebDAV protocol support

## Manager Application Endpoints

### HTMLManagerServlet
**Context**: `/manager`  
**Endpoints**:
- `/html/*` - HTML interface
- `/status/*` - Server status
- `/list` - List applications

### ManagerServlet
**Context**: `/manager`  
**Endpoints**:
- `/text/list` - List apps (text)
- `/text/deploy` - Deploy app
- `/text/undeploy` - Undeploy app
- `/text/reload` - Reload app
- `/text/start` - Start app
- `/text/stop` - Stop app
- `/text/sessions` - Session info

## Host Manager Endpoints

### HostManagerServlet
**Context**: `/host-manager`  
**Endpoints**:
- `/text/add` - Add virtual host
- `/text/remove` - Remove host
- `/text/list` - List hosts
- `/text/start` - Start host
- `/text/stop` - Stop host

## WebSocket Endpoints

### WsServerContainer
**Location**: `java/org/apache/tomcat/websocket/server/WsServerContainer.java`  
**Protocol**: WebSocket upgrade from HTTP  
**Endpoints**: Programmatically registered via:
```java
@ServerEndpoint("/websocket/endpoint")
ServerEndpointConfig.Builder.create(EndpointClass.class, "/path")
```

## REST API Patterns

### Rewrite Valve REST Support
**Location**: `java/org/apache/catalina/valves/rewrite/RewriteValve.java`  
**Pattern Examples**:
```
RewriteRule ^/api/v1/(.*)$ /rest/v1/$1 [L]
RewriteRule ^/users/([0-9]+)$ /user?id=$1 [L]
```

## Async Servlet Endpoints

### AsyncContext Support
**Interface**: `jakarta.servlet.AsyncContext`  
**Implementation**: `org.apache.catalina.core.AsyncContextImpl`  
**Pattern**:
```java
@WebServlet(asyncSupported = true)
AsyncContext async = request.startAsync();
```

## Filter Mappings

### Common Security Filters
- `CorsFilter` - CORS handling
- `CsrfPreventionFilter` - CSRF protection  
- `RemoteIpFilter` - IP forwarding
- `ExpiresFilter` - Cache control

### Compression Filters
- `CompressionFilter` - Response compression
- `GzipOutputFilter` - Gzip encoding

## Virtual Host Routing

### Host Resolution
**Class**: `org.apache.catalina.mapper.Mapper`  
**Resolution Order**:
1. Exact host match
2. Wildcard alias match
3. Default host

### Context Path Mapping
**Patterns**:
- `/` - Root context
- `/app` - Exact path
- `/app/*` - Path prefix
- `*.extension` - Extension mapping

## URL Pattern Matching

### Servlet Specification Patterns
1. **Exact**: `/exact/path`
2. **Path**: `/prefix/*`
3. **Extension**: `*.jsp`
4. **Default**: `/`

### Matching Priority
1. Exact match
2. Longest path prefix
3. Extension match
4. Default servlet

## Connector Endpoints

### HTTP Connector
**Default Port**: 8080  
**Protocol**: HTTP/1.1  
**Class**: `org.apache.coyote.http11.Http11NioProtocol`

### AJP Connector
**Default Port**: 8009  
**Protocol**: AJP/1.3  
**Class**: `org.apache.coyote.ajp.AjpNioProtocol`

### HTTPS Connector
**Default Port**: 8443  
**Protocol**: HTTP/1.1 with TLS  
**Configuration**: Requires keystore

## Request Routing Algorithm

```
1. Connector receives request
2. Mapper resolves:
   - Host (virtual host)
   - Context (webapp)
   - Wrapper (servlet)
3. Pipeline processes:
   - Engine Valve
   - Host Valve
   - Context Valve
   - Wrapper Valve
4. Servlet.service() invoked
```

## Programmatic Registration

### ServletContext API
```java
// In ServletContainerInitializer
ServletRegistration.Dynamic servlet = 
    ctx.addServlet("name", MyServlet.class);
servlet.addMapping("/path/*");
servlet.setAsyncSupported(true);
```

### Annotation-based
```java
@WebServlet(
    name = "MyServlet",
    urlPatterns = {"/api/*", "/v1/*"},
    asyncSupported = true
)
public class MyServlet extends HttpServlet {
    // Implementation
}
```