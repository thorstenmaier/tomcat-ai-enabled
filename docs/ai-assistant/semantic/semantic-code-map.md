# Semantic Code Map - Apache Tomcat

**Project**: Apache Tomcat 12.0  
**Last Updated**: 2025-01-03  
**Purpose**: Semantic navigation through conceptual relationships

## Concept Clusters

### 🔄 Request Handling (Complexity: ⭐⭐⭐⭐)
**Description**: HTTP request processing pipeline  
**Core Concepts**: protocol_parsing, request_mapping, filter_chain, servlet_invocation

**Entry Points**:
- `org.apache.coyote.http11.Http11Processor`
- `org.apache.catalina.connector.CoyoteAdapter`  
- `org.apache.catalina.core.StandardWrapperValve`

**High Relevance Files**:
- `java/org/apache/catalina/connector/Request.java`
- `java/org/apache/catalina/connector/Response.java`
- `java/org/apache/coyote/Request.java`
- `java/org/apache/coyote/Response.java`

**Medium Relevance Files**:
- `java/org/apache/catalina/core/ApplicationFilterChain.java`
- `java/org/apache/catalina/valves/*Valve.java`

**Linked Concepts**: [Security](#-security), [Session Management](#-session-management), [Async Processing](#-async-processing)

---

### 🔐 Security (Complexity: ⭐⭐⭐⭐)
**Description**: Authentication, authorization, and security constraints  
**Core Concepts**: authentication, authorization, ssl_tls, session_security, csrf_protection

**Entry Points**:
- `org.apache.catalina.authenticator.*Authenticator`
- `org.apache.catalina.realm.*Realm`
- `org.apache.catalina.filters.CsrfPreventionFilter`

**High Relevance Files**:
- `java/org/apache/catalina/authenticator/AuthenticatorBase.java`
- `java/org/apache/catalina/realm/RealmBase.java`
- `java/org/apache/catalina/realm/JNDIRealm.java`
- `java/org/apache/catalina/realm/DataSourceRealm.java`

**Medium Relevance Files**:
- `java/org/apache/catalina/security/SecurityUtil.java`
- `java/org/apache/catalina/filters/RemoteIpFilter.java`

**Linked Concepts**: [Session Management](#-session-management), [Request Handling](#-request-handling), [Configuration](#-configuration)

---

### 👤 Session Management (Complexity: ⭐⭐⭐)
**Description**: HTTP session lifecycle and replication  
**Core Concepts**: session_creation, session_persistence, session_replication, session_expiration

**Entry Points**:
- `org.apache.catalina.session.StandardManager`
- `org.apache.catalina.session.PersistentManager`
- `org.apache.catalina.ha.session.DeltaManager`

**High Relevance Files**:
- `java/org/apache/catalina/session/StandardSession.java`
- `java/org/apache/catalina/session/StandardManager.java`
- `java/org/apache/catalina/ha/session/DeltaSession.java`

**Medium Relevance Files**:
- `java/org/apache/catalina/session/FileStore.java`
- `java/org/apache/catalina/session/JDBCStore.java`

**Linked Concepts**: [Clustering](#-clustering), [Security](#-security), [Memory Management](#performance)

---

### 🚀 Web Application Deployment (Complexity: ⭐⭐⭐⭐)
**Description**: Web application deployment and lifecycle  
**Core Concepts**: war_deployment, context_configuration, class_loading, resource_loading

**Entry Points**:
- `org.apache.catalina.startup.HostConfig`
- `org.apache.catalina.startup.ContextConfig`
- `org.apache.catalina.core.StandardContext`

**High Relevance Files**:
- `java/org/apache/catalina/startup/HostConfig.java`
- `java/org/apache/catalina/startup/ContextConfig.java`
- `java/org/apache/catalina/loader/WebappClassLoaderBase.java`

**Medium Relevance Files**:
- `java/org/apache/catalina/startup/WebAnnotationScanner.java`
- `java/org/apache/catalina/deploy/WebXml.java`

**Linked Concepts**: [Configuration](#-configuration), [Class Loading](#class-loading), [JNDI](#-jndi)

---

### ⚡ Async Processing (Complexity: ⭐⭐⭐⭐⭐)
**Description**: Asynchronous servlet and WebSocket handling  
**Core Concepts**: async_servlets, websockets, non_blocking_io, event_driven

**Entry Points**:
- `org.apache.catalina.core.AsyncContextImpl`
- `org.apache.tomcat.websocket.server.WsServerContainer`
- `org.apache.coyote.AsyncStateMachine`

**High Relevance Files**:
- `java/org/apache/catalina/core/AsyncContextImpl.java`
- `java/org/apache/coyote/AsyncStateMachine.java`
- `java/org/apache/tomcat/websocket/server/WsHttpUpgradeHandler.java`

**Medium Relevance Files**:
- `java/org/apache/catalina/core/AsyncListenerWrapper.java`
- `java/org/apache/tomcat/websocket/WsSession.java`

**Linked Concepts**: [Request Handling](#-request-handling), [Connection Pooling](#-connection-pooling), [Performance](#-performance)

---

### 🌐 Clustering (Complexity: ⭐⭐⭐⭐⭐)
**Description**: High availability and session replication  
**Core Concepts**: membership, replication, failover, load_balancing

**Entry Points**:
- `org.apache.catalina.ha.ClusterManager`
- `org.apache.catalina.tribes.Channel`
- `org.apache.catalina.ha.tcp.SimpleTcpCluster`

**High Relevance Files**:
- `java/org/apache/catalina/ha/tcp/SimpleTcpCluster.java`
- `java/org/apache/catalina/tribes/group/GroupChannel.java`
- `java/org/apache/catalina/ha/session/DeltaManager.java`

**Medium Relevance Files**:
- `java/org/apache/catalina/tribes/membership/McastService.java`
- `java/org/apache/catalina/ha/ClusterValve.java`

**Linked Concepts**: [Session Management](#-session-management), [Network I/O](#-network-io), [Configuration](#-configuration)

---

### 📄 JSP Processing (Complexity: ⭐⭐⭐⭐)
**Description**: JSP compilation and execution  
**Core Concepts**: jsp_compilation, tag_libraries, el_evaluation, jsp_runtime

**Entry Points**:
- `org.apache.jasper.servlet.JspServlet`
- `org.apache.jasper.compiler.Compiler`
- `org.apache.jasper.runtime.JspRuntimeContext`

**High Relevance Files**:
- `java/org/apache/jasper/compiler/JspUtil.java`
- `java/org/apache/jasper/compiler/Generator.java`
- `java/org/apache/jasper/runtime/PageContextImpl.java`

**Medium Relevance Files**:
- `java/org/apache/jasper/compiler/TagLibraryInfoImpl.java`
- `java/org/apache/el/ExpressionFactoryImpl.java`

**Linked Concepts**: [EL Processing](#el_processing), [Compilation](#compilation), [Web App Deployment](#-web-application-deployment)

---

### 💾 Connection Pooling (Complexity: ⭐⭐⭐)
**Description**: Database and network connection management  
**Core Concepts**: pool_management, connection_validation, resource_cleanup, jdbc

**Entry Points**:
- `org.apache.tomcat.jdbc.pool.DataSource`
- `org.apache.tomcat.dbcp.dbcp2.BasicDataSource`
- `org.apache.tomcat.util.net.AbstractEndpoint`

**High Relevance Files**:
- `java/org/apache/tomcat/jdbc/pool/ConnectionPool.java`
- `java/org/apache/tomcat/dbcp/dbcp2/PoolingDataSource.java`
- `java/org/apache/tomcat/util/net/SocketProcessorBase.java`

**Medium Relevance Files**:
- `java/org/apache/tomcat/jdbc/pool/interceptor/ConnectionState.java`
- `java/org/apache/tomcat/util/net/Acceptor.java`

**Linked Concepts**: [JNDI](#-jndi), [Resource Management](#resource_management), [Performance](#-performance)

---

### 🔍 JNDI (Complexity: ⭐⭐)
**Description**: Java Naming and Directory Interface implementation  
**Core Concepts**: naming_context, resource_binding, environment_entries, ejb_references

**Entry Points**:
- `org.apache.naming.NamingContext`
- `org.apache.naming.factory.ResourceFactory`
- `org.apache.catalina.core.NamingContextListener`

**High Relevance Files**:
- `java/org/apache/naming/NamingContext.java`
- `java/org/apache/naming/factory/DataSourceLinkFactory.java`
- `java/org/apache/catalina/core/NamingContextListener.java`

**Medium Relevance Files**:
- `java/org/apache/naming/SelectorContext.java`
- `java/org/apache/naming/factory/BeanFactory.java`

**Linked Concepts**: [Connection Pooling](#-connection-pooling), [Web App Deployment](#-web-application-deployment), [Configuration](#-configuration)

---

### ⚡ Performance (Complexity: ⭐⭐⭐⭐⭐)
**Description**: Performance optimization and monitoring  
**Core Concepts**: nio_optimization, buffer_management, thread_pooling, jmx_monitoring

**Entry Points**:
- `org.apache.tomcat.util.net.NioEndpoint`
- `org.apache.tomcat.util.buf.ByteChunk`
- `org.apache.catalina.mbeans.MBeanFactory`

**High Relevance Files**:
- `java/org/apache/tomcat/util/net/NioEndpoint.java`
- `java/org/apache/tomcat/util/net/SocketBufferHandler.java`
- `java/org/apache/tomcat/util/threads/ThreadPoolExecutor.java`

**Medium Relevance Files**:
- `java/org/apache/tomcat/util/buf/StringCache.java`
- `java/org/apache/catalina/core/StandardThreadExecutor.java`

**Linked Concepts**: [Connection Pooling](#-connection-pooling), [Async Processing](#-async-processing), [Monitoring](#monitoring)

## Semantic Relationships

### Inheritance Hierarchies

#### Container Hierarchy
```
Container (interface)
└── ContainerBase (abstract)
    ├── StandardEngine
    ├── StandardHost  
    ├── StandardContext
    └── StandardWrapper
```

#### Valve Hierarchy  
```
Valve (interface)
└── ValveBase (abstract)
    ├── AccessLogValve
    ├── RemoteIpValve
    ├── ErrorReportValve
    └── AuthenticatorBase (abstract)
        ├── BasicAuthenticator
        ├── DigestAuthenticator
        └── FormAuthenticator
```

#### Realm Hierarchy
```
Realm (interface)
└── RealmBase (abstract)
    ├── JNDIRealm (LDAP integration)
    ├── DataSourceRealm (Database)
    ├── MemoryRealm (XML file)
    ├── JAASRealm (JAAS integration)
    └── CombinedRealm (Multiple realms)
```

### Component Dependencies

#### Core Dependencies
- **Catalina** depends on → Coyote, Juli, Naming
- **Jasper** depends on → Catalina, EL  
- **WebSocket** depends on → Catalina, Coyote
- **Tribes** depends on → Catalina

#### Protocol Dependencies
- **HTTP/1.1** → Http11Processor → CoyoteAdapter → Catalina
- **HTTP/2** → Http2UpgradeHandler → StreamProcessor → CoyoteAdapter
- **WebSocket** → WsHttpUpgradeHandler → WsFrameServer → Application

### Cross-Cutting Concerns

#### Logging (Juli)
**Affects**: All components  
**Implementation**: `org.apache.juli.*`  
**Configuration**: `conf/logging.properties`  
**Pattern**: One logger per class, hierarchical configuration

#### Lifecycle Management  
**Affects**: All containers, connectors, services  
**Interface**: `org.apache.catalina.Lifecycle`  
**Events**: `init → start → stop → destroy`  
**Pattern**: Template method in `LifecycleBase`

#### JMX Management
**Affects**: Major components  
**Implementation**: `org.apache.catalina.mbeans.*`  
**Registry**: `org.apache.tomcat.util.modeler.Registry`  
**Pattern**: MBean per manageable component

## Navigation Hints

### By Use Case

#### Adding New Protocol
1. **Start**: `org.apache.coyote.ProtocolHandler`
2. **Implement**: Processor interface, UpgradeProtocol
3. **Register**: In Connector configuration  
4. **Example**: Look at `Http2Protocol.java`

#### Custom Authentication
1. **Start**: `org.apache.catalina.authenticator.AuthenticatorBase`
2. **Implement**: `authenticate()` method
3. **Configure**: In `context.xml` or `web.xml`
4. **Example**: `BasicAuthenticator.java`

#### Session Storage Backend
1. **Start**: `org.apache.catalina.session.StoreBase`
2. **Implement**: `save()`, `load()` methods  
3. **Configure**: Manager configuration in context
4. **Example**: `JDBCStore.java`

#### Custom Request Processing
1. **Start**: `org.apache.catalina.valves.ValveBase`
2. **Implement**: `invoke()` method
3. **Deploy**: In `server.xml` or `context.xml`
4. **Example**: `AccessLogValve.java`

### By Technology

#### Servlet 3.0+ Async
**Key Classes**:
- `AsyncContext` - Interface
- `AsyncStateMachine` - State management
- `AsyncContextImpl` - Implementation

#### HTTP/2 Support
**Key Classes**:
- `Http2Protocol` - Protocol handler
- `Http2UpgradeHandler` - Protocol upgrade
- `Stream` - HTTP/2 stream

#### WebSocket Implementation
**Key Classes**:
- `WsServerContainer` - Server container
- `WsSession` - WebSocket session
- `WsFrameBase` - Frame processing

#### JSP & Expression Language
**Key Classes**:
- `ExpressionFactory` - EL factory
- `ELContext` - Evaluation context  
- `ELResolver` - Resolution chain

## Conceptual Navigation Map

### Request Flow Concepts
```
Network → Protocol → Adapter → Pipeline → Servlet
   ↓         ↓         ↓         ↓         ↓
Socket    Parsing   Bridge   Valves   Execution
```

### Security Flow Concepts
```
Request → Authentication → Authorization → Resource Access
   ↓           ↓              ↓              ↓
Identify   Validate      Check Roles   Grant/Deny
```

### Session Lifecycle Concepts
```
Create → Use → Replicate → Expire → Cleanup
   ↓      ↓        ↓         ↓        ↓
  ID   Attributes  Cluster   Timeout  Memory
```

### Deployment Concepts
```
WAR → Unpack → Scan → Configure → Initialize → Start
 ↓       ↓      ↓        ↓          ↓         ↓
File   Extract  Metadata  Context   Resources  Ready
```

## Usage Patterns

### Finding Related Code
1. **By Concept**: Use semantic clusters above
2. **By Inheritance**: Follow class hierarchies
3. **By Dependency**: Trace component dependencies
4. **By Use Case**: Follow navigation hints

### Understanding Complexity
- ⭐ Simple (utilities, helpers)
- ⭐⭐ Moderate (configuration, basic components)  
- ⭐⭐⭐ Complex (session management, JSP)
- ⭐⭐⭐⭐ Very Complex (request processing, security)
- ⭐⭐⭐⭐⭐ Extremely Complex (clustering, async I/O)

### Performance Impact Assessment
**Critical Path**: Request handling, I/O processing  
**High Impact**: Session management, security checks  
**Medium Impact**: JSP compilation, configuration parsing  
**Low Impact**: Logging, monitoring, utilities