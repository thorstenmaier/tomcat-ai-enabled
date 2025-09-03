# Apache Tomcat Service Registry

## Architecture Overview
**Type**: Modular Monolith with Component-based Architecture  
**Last Updated**: 2025-01-03

## Core Components

### 🔷 Catalina - Servlet Container Core
**Location**: `java/org/apache/catalina/`

**Responsibilities**:
- Request lifecycle management
- Container hierarchy (Server→Service→Engine→Host→Context→Wrapper)
- Session management
- Security and authentication
- Pipeline and valve processing

**Key Classes**:
- `StandardServer` - Top-level server instance
- `StandardService` - Groups connectors with container
- `StandardEngine` - Request routing to virtual hosts
- `StandardHost` - Virtual host implementation
- `StandardContext` - Web application context
- `StandardWrapper` - Individual servlet wrapper

**Dependencies**: Coyote, Tomcat-util  
**Communication**: 
- ← Receives HTTP requests via Coyote connectors
- → Invokes servlets and JSP compilation via Jasper

---

### 🔷 Coyote - Protocol Handler Framework
**Location**: `java/org/apache/coyote/`

**Responsibilities**:
- HTTP/1.1, HTTP/2, AJP protocol handling
- Connection management
- Request/response abstraction
- Async I/O operations

**Connectors**:
- **NIO** (`NioEndpoint`) - java.nio based, default connector
- **NIO2** (`Nio2Endpoint`) - java.nio2 based
- **APR** (`AprEndpoint`) - Apache Portable Runtime based

**Key Classes**:
- `Http11Processor` - HTTP/1.1 protocol processing
- `Http2Protocol` - HTTP/2 support
- `AjpProcessor` - AJP protocol for reverse proxy
- `CoyoteAdapter` - Bridge between Coyote and Catalina

**Dependencies**: Tomcat-util, Juli  
**Communication**:
- ← TCP/IP socket connections
- → Catalina container via Adapter pattern

---

### 🔷 Jasper - JSP Engine
**Location**: `java/org/apache/jasper/`

**Responsibilities**:
- JSP to servlet compilation
- Runtime JSP handling
- Tag library support
- EL expression evaluation integration

**Key Classes**:
- `JspServlet` - Main JSP request handler
- `Compiler` - JSP to Java compilation
- `JspCompilationContext` - Compilation context
- `JDTCompiler` - Eclipse JDT-based Java compilation

**Dependencies**: Catalina, EL, Tomcat-util  
**Communication**:
- ← JSP requests from Catalina
- → Compiled servlet invocations

---

### 🔷 Expression Language (EL)
**Location**: `java/org/apache/el/`

**Responsibilities**:
- EL expression parsing
- Expression evaluation
- Function mapping
- Variable resolution

**Key Classes**:
- `ExpressionFactoryImpl` - EL factory implementation
- `ELParser` - Expression parser (JavaCC generated)
- `ELContext` - Evaluation context
- `ELResolver` - Variable/property resolver chain

**Dependencies**: Tomcat-util  
**Communication**:
- ← EL expressions from JSP/JSF
- → Bean property access, method invocations

---

### 🔷 WebSocket
**Location**: `java/org/apache/tomcat/websocket/`

**Responsibilities**:
- WebSocket protocol handling
- Frame processing
- Session management
- Client/server endpoints

**Key Classes**:
- `WsServerContainer` - Server-side WebSocket container
- `WsSession` - WebSocket session implementation
- `WsFrameBase` - Frame processing base
- `WsHttpUpgradeHandler` - HTTP to WebSocket upgrade

**Dependencies**: Catalina, Coyote, Tomcat-util  
**Communication**:
- ← WebSocket upgrade requests
- ↔ Bidirectional WebSocket frames

---

### 🔷 Tribes - Group Communication Framework
**Location**: `java/org/apache/catalina/tribes/`

**Responsibilities**:
- Cluster membership management
- Group messaging
- Session replication
- Fault tolerance

**Key Classes**:
- `GroupChannel` - Main channel implementation
- `McastService` - Multicast membership
- `ChannelSender` - Message sending
- `ChannelReceiver` - Message receiving

**Dependencies**: Catalina, Tomcat-util  
**Communication**:
- ↔ Multicast/unicast cluster messages
- → Session state replication

---

### 🔷 Database Connection Pooling
**Location**: `java/org/apache/tomcat/jdbc/` & `java/org/apache/tomcat/dbcp/`

**Two Implementations**:
1. **Tomcat JDBC Pool** - High-performance, Tomcat-specific
2. **Commons DBCP** - Repackaged Apache Commons DBCP

**Key Classes**:
- `ConnectionPool` - Tomcat JDBC pool core
- `BasicDataSource` - Commons DBCP datasource
- `PooledConnection` - Pooled connection wrapper

**Dependencies**: Naming, Tomcat-util  
**Communication**:
- ← DataSource requests from applications
- → JDBC database connections

---

## Event System

### Lifecycle Events
```
BEFORE_INIT → AFTER_INIT
BEFORE_START → AFTER_START  
BEFORE_STOP → AFTER_STOP
BEFORE_DESTROY → AFTER_DESTROY
```

### Container Events
- `ADD_CHILD` / `REMOVE_CHILD` - Container hierarchy changes
- `ADD_VALVE` / `REMOVE_VALVE` - Pipeline modifications
- `CONTAINER_BACKGROUND_PROCESS` - Periodic tasks

### Session Events
- `SESSION_CREATED` / `SESSION_DESTROYED`
- `SESSION_ATTRIBUTE_SET` / `SESSION_ATTRIBUTE_REMOVED`

---

## Critical Execution Paths

### Request Processing Pipeline
```
1. Coyote ProtocolHandler (accepts connection)
   ↓
2. Coyote Processor (parses HTTP)
   ↓
3. CoyoteAdapter (protocol → container bridge)
   ↓
4. Catalina Pipeline (valve chain processing)
   ↓
5. StandardWrapper (servlet location)
   ↓
6. Servlet/JSP execution
```

### Startup Sequence
```
Bootstrap.main()
  → Catalina.load()
    → Server.init()
      → Service.init()
        → Connector.init()
        → Engine.init()
          → Host.init()
            → Context.init()
              → Wrapper.init()
```

### Shutdown Sequence
```
Server.stop()
  → Service.stop()
    → Connector.stop()
    → Container.stop()
      → (reverse of startup)
  → Server.destroy()
```

---

## Deployment Configurations

### Standalone Deployment
- **Entry Point**: `org.apache.catalina.startup.Bootstrap`
- **Configuration**: `conf/server.xml`
- **Usage**: Traditional server deployment

### Embedded Deployment
- **Entry Point**: `org.apache.catalina.startup.Tomcat`
- **Configuration**: Programmatic API
- **Usage**: Spring Boot, microservices

### Clustered Deployment
- **Components**: Tribes + ClusterValve
- **Configuration**: `<Cluster>` element in server.xml
- **Usage**: High availability, session replication