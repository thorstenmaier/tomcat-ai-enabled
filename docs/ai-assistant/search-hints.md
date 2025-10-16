# Smart Search Hints for Apache Tomcat

## Quick Concept-to-File Mappings

### Authentication & Security
- **"authentication"** → `java/org/apache/catalina/authenticator/`, `java/org/apache/catalina/realm/`
- **"login"** → `FormAuthenticator.java`, `BasicAuthenticator.java`, `DigestAuthenticator.java`
- **"security constraints"** → `SecurityConstraint.java`, `SecurityCollection.java`
- **"SSL/TLS"** → `java/org/apache/tomcat/util/net/SSLUtil.java`, connector configurations
- **"CSRF protection"** → `CsrfPreventionFilter.java`
- **"realm"** → `java/org/apache/catalina/realm/`

### Session Management
- **"session"** → `java/org/apache/catalina/session/`, `StandardManager.java`, `PersistentManager.java`
- **"session interfaces"** → `Session.java`, `Manager.java`, `Store.java`, `SessionIdGenerator.java`
- **"session implementations"** → `StandardSession.java`, `StandardSessionFacade.java`, `StandardSessionAccessor.java`
- **"session managers"** → `ManagerBase.java`, `StandardManager.java`, `PersistentManagerBase.java`, `PersistentManager.java`
- **"session replication"** → `java/org/apache/catalina/ha/session/`, `DeltaManager.java`, `DeltaSession.java`
- **"session clustering"** → `ClusterManager.java`, `ClusterSession.java`, `BackupManager.java`, `DistributedManager.java`
- **"session persistence"** → `StoreBase.java`, `FileStore.java`, `DataSourceStore.java`
- **"session timeout"** → `StandardSession.java`, `SessionConfig.java`, `ManagerBase.backgroundProcess()`
- **"session validation"** → `SessionListener.java`, `SessionEvent.java`, expiration in managers
- **"session ID generation"** → `SessionIdGeneratorBase.java`, `StandardSessionIdGenerator.java`
- **"session listeners"** → `ClusterSessionListener.java`, `ReplicatedSessionListener.java`, `SingleSignOnListener.java`
- **"session messages"** → `SessionMessage.java`, `SessionMessageImpl.java` (clustering)
- **"session utilities"** → `SessionUtils.java`, `CrawlerSessionManagerValve.java`, `SessionInitializerFilter.java`

### Request Processing
- **"request processing"** → `java/org/apache/catalina/connector/`, `CoyoteAdapter.java`
- **"HTTP parsing"** → `java/org/apache/coyote/http11/`, `Http11Processor.java`
- **"request pipeline"** → `java/org/apache/catalina/valves/`, `Pipeline.java`
- **"filters"** → `ApplicationFilterChain.java`, `ApplicationFilterFactory.java`
- **"async requests"** → `AsyncContextImpl.java`, `AsyncStateMachine.java`

### WebSocket
- **"websocket"** → `java/org/apache/tomcat/websocket/`
- **"websocket server"** → `WsServerContainer.java`, `WsHttpUpgradeHandler.java`
- **"websocket frames"** → `WsFrameBase.java`, `WsFrameClient.java`, `WsFrameServer.java`
- **"websocket session"** → `WsSession.java`

### JSP & EL
- **"JSP compilation"** → `java/org/apache/jasper/compiler/`, `Compiler.java`
- **"JSP runtime"** → `java/org/apache/jasper/runtime/`, `PageContextImpl.java`
- **"expression language"** → `java/org/apache/el/`, `ExpressionFactoryImpl.java`
- **"EL parser"** → `java/org/apache/el/parser/`, `ELParser.java` (generated)
- **"tag libraries"** → `java/org/apache/jasper/compiler/`, `TagLibraryInfoImpl.java`

### Database & JNDI
- **"connection pooling"** → `java/org/apache/tomcat/jdbc/pool/`, `java/org/apache/tomcat/dbcp/`
- **"datasource"** → `DataSource.java`, `BasicDataSource.java`, `DataSourceFactory.java`
- **"JNDI"** → `java/org/apache/naming/`, `NamingContext.java`
- **"resource binding"** → `ResourceFactory.java`, `factory/` package

### Configuration
- **"server.xml parsing"** → `java/org/apache/catalina/startup/`, `Catalina.java`
- **"web.xml parsing"** → `ContextConfig.java`, `WebRuleSet.java`
- **"context configuration"** → `StandardContext.java`, `ContextConfig.java`
- **"connector configuration"** → `Connector.java`, protocol handlers in `coyote/`

### Deployment
- **"webapp deployment"** → `java/org/apache/catalina/startup/`, `HostConfig.java`
- **"war deployment"** → `ExpandWar.java`, `DeploymentManager.java`
- **"classloader"** → `java/org/apache/catalina/loader/`, `WebappClassLoaderBase.java`
- **"annotation scanning"** → `ContextConfig.java`, `WebAnnotationScanner.java`

### Network I/O
- **"NIO"** → `java/org/apache/tomcat/util/net/NioEndpoint.java`
- **"HTTP/2"** → `java/org/apache/coyote/http2/`, `Http2Protocol.java`
- **"AJP"** → `java/org/apache/coyote/ajp/`, `AjpProcessor.java`
- **"connectors"** → `java/org/apache/catalina/connector/Connector.java`

### Clustering
- **"cluster"** → `java/org/apache/catalina/ha/`, `SimpleTcpCluster.java`
- **"tribes"** → `java/org/apache/catalina/tribes/`, `GroupChannel.java`
- **"membership"** → `McastService.java`, `StaticMember.java`
- **"replication"** → `ReplicationValve.java`, `DeltaRequest.java`

### Monitoring & Management
- **"JMX"** → `java/org/apache/catalina/mbeans/`, `MBeanFactory.java`
- **"logging"** → `java/org/apache/juli/`, `DirectJDKLog.java`
- **"valves"** → `java/org/apache/catalina/valves/`, `AccessLogValve.java`
- **"metrics"** → MBean classes, `Registry.java`

### Testing
- **"test utilities"** → `test/org/apache/catalina/startup/TomcatBaseTest.java`
- **"websocket tests"** → `test/org/apache/tomcat/websocket/`
- **"servlet tests"** → `test/org/apache/catalina/servlets/`
- **"protocol tests"** → `test/org/apache/coyote/`

## File Name Patterns

### Core Components
- `Standard*` → Main implementations (StandardServer, StandardContext, etc.)
- `*Base` → Abstract base classes (ContainerBase, AuthenticatorBase, etc.)
- `*Impl` → Interface implementations (AsyncContextImpl, ExpressionFactoryImpl)
- `*Factory` → Factory patterns (ApplicationFilterFactory, etc.)

### Protocol Handling
- `*Processor` → Protocol processors (Http11Processor, AjpProcessor)
- `*Handler` → Event handlers (WsHttpUpgradeHandler, etc.)
- `*Endpoint` → Network endpoints (NioEndpoint, Nio2Endpoint)

### Configuration
- `*Config` → Configuration classes (ContextConfig, HostConfig)
- `*Digester` → XML parsing (server.xml, web.xml)
- `*RuleSet` → Digester rules (NamingRuleSet, WebRuleSet)

### Utilities
- `*Util` → Utility classes (SSLUtil, CookieUtil, RequestUtil)
- `*Utils` → Static utility methods (CoyoteWriter, etc.)

## Package Patterns

### by Functionality
- `/connector/` → Request/Response handling
- `/authenticator/` → Authentication mechanisms  
- `/session/` → Session management
- `/startup/` → Initialization and configuration
- `/valves/` → Request interceptors
- `/realm/` → User/role repositories
- `/loader/` → Classloading
- `/ha/` → High availability
- `/mbeans/` → JMX management

### by Protocol
- `/http11/` → HTTP/1.1 implementation
- `/http2/` → HTTP/2 implementation  
- `/ajp/` → AJP implementation
- `/websocket/` → WebSocket implementation

### by Concern
- `/util/` → Utilities and helpers
- `/security/` → Security utilities
- `/deploy/` → Deployment descriptors
- `/filters/` → Standard filters

## Common Search Scenarios

### "How to add a new connector?"
**Start with**: `Connector.java`, protocol handlers in `java/org/apache/coyote/`  
**Example**: Look at `Http11NioProtocol.java` implementation

### "How to create custom authentication?"
**Start with**: `AuthenticatorBase.java`  
**Examples**: `BasicAuthenticator.java`, `FormAuthenticator.java`

### "How to implement session clustering?"
**Start with**: `ClusterManager.java`, `DeltaManager.java`
**Related**: `SimpleTcpCluster.java`, `DeltaSession.java`, `SessionMessage.java`, tribes package

### "How to add custom valve?"
**Start with**: `ValveBase.java`  
**Examples**: `AccessLogValve.java`, `RemoteIpValve.java`

### "How to handle async requests?"
**Start with**: `AsyncContextImpl.java`, `AsyncStateMachine.java`  
**Related**: servlet 3.0+ async support

### "How to customize JSP compilation?"
**Start with**: `JspServlet.java`, `Compiler.java`  
**Related**: `JspCompilationContext.java`, `JDTCompiler.java`

## Performance Investigation Paths

### "Slow request processing"
1. `CoyoteAdapter.service()` - Entry point
2. Pipeline valves - Check valve chain
3. `StandardWrapperValve.invoke()` - Servlet invocation
4. Filter chain - Check filter processing

### "Memory leaks"
1. `WebappClassLoaderBase` - Classloader leaks
2. `StandardManager.backgroundProcess()` - Session cleanup
3. ThreadLocal usage - Check cleanup patterns
4. Connection pools - Resource management

### "High CPU usage"
1. `NioEndpoint$Poller` - I/O processing
2. Background processors - Periodic tasks
3. JMX monitoring - MBean registration
4. Logging - Check log levels

## Quick Command Reference

### Find by concept
```bash
# Authentication code
find java/ -name "*uth*" -o -name "*Realm*"

# Session management
find java/ -name "*ession*" -o -name "*anager*" -o -name "*Store*"

# WebSocket
find java/ -path "*/websocket/*" -name "*.java"

# HTTP/2
find java/ -path "*/http2/*" -name "*.java"
```

### Grep by functionality
```bash
# Find servlet mappings
rg "@WebServlet|addServlet" java/

# Find lifecycle methods  
rg "initInternal|startInternal|stopInternal" java/

# Find authentication points
rg "authenticate|login|principal" java/

# Find async handling
rg "AsyncContext|startAsync" java/

# Find session classes
rg "class.*Session|interface.*Session" java/

# Find session management patterns
rg "Manager.*Session|Session.*Manager" java/
```