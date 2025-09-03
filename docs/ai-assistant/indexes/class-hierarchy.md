# Apache Tomcat Class Hierarchy Index

## Core Container Hierarchy

### Container Base Classes
```
Container (interface)
└── ContainerBase (abstract)
    ├── StandardEngine
    ├── StandardHost  
    ├── StandardContext
    └── StandardWrapper
```

**Key Files**:
- `org.apache.catalina.Container` - Base container interface
- `org.apache.catalina.core.ContainerBase` - Abstract implementation
- `org.apache.catalina.core.StandardEngine` - Request routing engine
- `org.apache.catalina.core.StandardHost` - Virtual host container
- `org.apache.catalina.core.StandardContext` - Web application context
- `org.apache.catalina.core.StandardWrapper` - Servlet wrapper

### Lifecycle Hierarchy
```
Lifecycle (interface)
└── LifecycleBase (abstract)
    ├── ContainerBase
    ├── ConnectorBase
    ├── ProtocolHandler implementations
    ├── StandardServer
    ├── StandardService
    └── WebappClassLoaderBase
```

**Key Files**:
- `org.apache.catalina.Lifecycle` - Lifecycle interface
- `org.apache.catalina.util.LifecycleBase` - Abstract lifecycle implementation
- `org.apache.catalina.LifecycleEvent` - Lifecycle event object
- `org.apache.catalina.LifecycleListener` - Event listener interface

## Request Processing Hierarchy

### Valve Pipeline
```
Valve (interface)
└── ValveBase (abstract)
    ├── AccessLogValve
    ├── RemoteIpValve
    ├── ErrorReportValve
    ├── StandardEngineValve
    ├── StandardHostValve
    ├── StandardContextValve
    ├── StandardWrapperValve
    └── AuthenticatorBase (abstract)
        ├── BasicAuthenticator
        ├── DigestAuthenticator
        ├── FormAuthenticator
        ├── SSLAuthenticator
        └── SpnegoAuthenticator
```

**Key Files**:
- `org.apache.catalina.Valve` - Valve interface
- `org.apache.catalina.valves.ValveBase` - Abstract valve implementation
- `org.apache.catalina.authenticator.AuthenticatorBase` - Authentication valve base
- `org.apache.catalina.valves.*` - Built-in valve implementations

### Request/Response Objects
```
HttpServletRequest (interface)
└── Request (org.apache.catalina.connector)
    └── RequestFacade (facade pattern)

HttpServletResponse (interface)
└── Response (org.apache.catalina.connector)
    └── ResponseFacade (facade pattern)
```

**Protocol Level Objects**:
```
org.apache.coyote.Request
org.apache.coyote.Response
└── Used by protocol processors (Http11, Http2, AJP)
```

## Protocol Processing Hierarchy

### Protocol Handlers
```
ProtocolHandler (interface)
└── AbstractProtocol (abstract)
    ├── Http11NioProtocol
    ├── Http11Nio2Protocol  
    ├── Http11AprProtocol
    ├── Http2Protocol
    └── AjpNioProtocol
```

**Key Files**:
- `org.apache.coyote.ProtocolHandler` - Protocol handler interface
- `org.apache.coyote.AbstractProtocol` - Abstract protocol implementation
- `org.apache.coyote.http11.*` - HTTP/1.1 implementations
- `org.apache.coyote.http2.*` - HTTP/2 implementation
- `org.apache.coyote.ajp.*` - AJP implementations

### Processors
```
Processor (interface)
└── AbstractProcessor (abstract)
    ├── Http11Processor
    ├── AjpProcessor
    └── StreamProcessor (HTTP/2)
```

### Network I/O Endpoints
```
AbstractEndpoint (abstract)
├── NioEndpoint
├── Nio2Endpoint
└── AprEndpoint
```

**Key Files**:
- `org.apache.tomcat.util.net.AbstractEndpoint` - I/O endpoint base
- `org.apache.tomcat.util.net.NioEndpoint` - NIO implementation
- `org.apache.tomcat.util.net.Nio2Endpoint` - NIO.2 implementation

## Session Management Hierarchy

### Session Managers
```
Manager (interface)
└── ManagerBase (abstract)
    ├── StandardManager
    ├── PersistentManager
    └── ClusterManagerBase (abstract)
        ├── DeltaManager
        └── BackupManager
```

### Session Objects
```
HttpSession (interface)
└── Session (org.apache.catalina)
    ├── StandardSession
    ├── PersistentSession
    └── DeltaSession (clustering)
```

### Session Stores
```
Store (interface)
└── StoreBase (abstract)
    ├── FileStore
    └── JDBCStore
```

**Key Files**:
- `org.apache.catalina.Manager` - Session manager interface
- `org.apache.catalina.session.*` - Session management implementations
- `org.apache.catalina.ha.session.*` - Clustering session support

## Security Hierarchy

### Authentication Realms
```
Realm (interface)
└── RealmBase (abstract)
    ├── MemoryRealm
    ├── JNDIRealm
    ├── DataSourceRealm
    ├── JAASRealm
    ├── UserDatabaseRealm
    ├── LockOutRealm
    └── CombinedRealm
```

### Security Constraints
```
SecurityConstraint
SecurityCollection
LoginConfig
```

**Key Files**:
- `org.apache.catalina.Realm` - Realm interface
- `org.apache.catalina.realm.*` - Realm implementations
- `org.apache.catalina.deploy.SecurityConstraint` - Security constraint definition

## JSP & Expression Language Hierarchy

### JSP Compilation
```
Compiler (abstract)
├── JDTCompiler (Eclipse JDT)
└── JavaCompiler (traditional javac)
```

### JSP Runtime
```
HttpJspBase (abstract)
└── Generated JSP servlets extend this

PageContext (abstract)
└── PageContextImpl
```

### Expression Language
```
ELContext (abstract)
└── StandardELContext

ELResolver (abstract)
├── ArrayELResolver
├── BeanELResolver
├── CompositeELResolver
├── ListELResolver
├── MapELResolver
├── ResourceBundleELResolver
├── StaticFieldELResolver
└── OptionalELResolver
```

**Key Files**:
- `org.apache.jasper.compiler.*` - JSP compilation
- `org.apache.jasper.runtime.*` - JSP runtime support
- `org.apache.el.*` - Expression language implementation

## WebSocket Hierarchy

### Server Components
```
ServerContainer (interface)
└── WsServerContainer

ServerEndpoint (annotation)
└── Annotated endpoint classes

Endpoint (abstract)
└── Programmatic endpoint classes
```

### Session Management
```
Session (interface)
└── WsSession
```

**Key Files**:
- `org.apache.tomcat.websocket.server.*` - Server-side WebSocket
- `org.apache.tomcat.websocket.*` - WebSocket core implementation

## Clustering Hierarchy

### Cluster Communication
```
Channel (interface)
└── GroupChannel

ChannelListener (interface)
└── Various clustering components

Member (interface)
└── MemberImpl
```

### Cluster Managers
```
ClusterManager (interface)
└── ClusterManagerBase (abstract)
    ├── DeltaManager
    └── BackupManager
```

**Key Files**:
- `org.apache.catalina.tribes.*` - Group communication framework
- `org.apache.catalina.ha.*` - High availability components

## Utility Hierarchies

### Class Loaders
```
ClassLoader
└── URLClassLoader
    └── WebappClassLoaderBase (abstract)
        ├── WebappClassLoader
        └── ParallelWebappClassLoader
```

### JNDI Implementation
```
Context (interface)
└── NamingContext

ObjectFactory (interface)
└── Various factory implementations
    ├── ResourceFactory
    ├── DataSourceFactory
    └── BeanFactory
```

### Buffer Management
```
ByteChunk
CharChunk
MessageBytes
```

**Key Files**:
- `org.apache.catalina.loader.*` - Class loading
- `org.apache.naming.*` - JNDI implementation
- `org.apache.tomcat.util.buf.*` - Buffer utilities

## Exception Hierarchies

### Protocol Exceptions
```
Exception
├── IOException
│   └── CloseNowException
├── RuntimeException
│   └── ProtocolException
└── ServletException
    └── Various servlet exceptions
```

### Container Exceptions
```
Exception
└── LifecycleException
```

### JSP Exceptions
```
Exception
├── JspException
│   ├── SkipPageException
│   └── JspTagException
└── JspTagException
```

## Interface Implementations Map

### Core Interfaces
| Interface | Primary Implementation | Location |
|-----------|------------------------|----------|
| `Container` | `ContainerBase` | `o.a.catalina.core` |
| `Valve` | `ValveBase` | `o.a.catalina.valves` |
| `Manager` | `StandardManager` | `o.a.catalina.session` |
| `Realm` | `RealmBase` | `o.a.catalina.realm` |
| `Loader` | `WebappLoader` | `o.a.catalina.loader` |

### Protocol Interfaces  
| Interface | Primary Implementation | Location |
|-----------|------------------------|----------|
| `ProtocolHandler` | `Http11NioProtocol` | `o.a.coyote.http11` |
| `Processor` | `Http11Processor` | `o.a.coyote.http11` |
| `Endpoint` | `NioEndpoint` | `o.a.tomcat.util.net` |

### Servlet API Interfaces
| Interface | Tomcat Implementation | Location |
|-----------|----------------------|----------|
| `HttpServletRequest` | `Request` | `o.a.catalina.connector` |
| `HttpServletResponse` | `Response` | `o.a.catalina.connector` |
| `ServletContext` | `ApplicationContext` | `o.a.catalina.core` |
| `HttpSession` | `StandardSession` | `o.a.catalina.session` |

## Abstract Base Classes

### Key Abstract Classes
- **`ContainerBase`** - Base for all containers, implements lifecycle
- **`ValveBase`** - Base for valve implementations  
- **`AuthenticatorBase`** - Base for authentication mechanisms
- **`RealmBase`** - Base for user/role repositories
- **`ManagerBase`** - Base for session managers
- **`StoreBase`** - Base for session persistence
- **`LifecycleBase`** - Base for lifecycle management
- **`AbstractProtocol`** - Base for protocol handlers
- **`AbstractProcessor`** - Base for request processors
- **`AbstractEndpoint`** - Base for I/O endpoints

## Factory Pattern Implementations

### Object Factories
- **`DigesterFactory`** - Creates XML digesters
- **`WebXmlParser`** - Parses web.xml files
- **`ContextConfig`** - Creates and configures contexts
- **`HostConfig`** - Creates and configures hosts

### Connection Factories
- **`DataSourceFactory`** - Creates DataSource objects
- **`ResourceFactory`** - Creates generic resources
- **`BeanFactory`** - Creates JavaBean objects

## Component Lookup Guide

### By Function
| Function | Start Here | Base Class |
|----------|------------|------------|
| Request Processing | `CoyoteAdapter` | `ValveBase` |
| Authentication | `AuthenticatorBase` | `AuthenticatorBase` |
| Session Management | `StandardManager` | `ManagerBase` |
| Class Loading | `WebappClassLoader` | `WebappClassLoaderBase` |
| Protocol Handling | `Http11Processor` | `AbstractProcessor` |
| Network I/O | `NioEndpoint` | `AbstractEndpoint` |
| JSP Processing | `JspServlet` | `HttpServlet` |
| Clustering | `DeltaManager` | `ClusterManagerBase` |

### By Package
| Package | Primary Purpose | Key Classes |
|---------|----------------|-------------|
| `o.a.catalina.core` | Core container | `Standard*` classes |
| `o.a.catalina.connector` | Request/Response | `Request`, `Response` |
| `o.a.catalina.valves` | Request processing | `*Valve` classes |
| `o.a.catalina.session` | Session management | `*Manager`, `*Session` |
| `o.a.catalina.realm` | Authentication | `*Realm` classes |
| `o.a.coyote.http11` | HTTP/1.1 protocol | `Http11*` classes |
| `o.a.jasper.compiler` | JSP compilation | `*Compiler` classes |
| `o.a.tomcat.util.net` | Network I/O | `*Endpoint` classes |

This hierarchy index provides a comprehensive map of Tomcat's class relationships, making it easier to understand the codebase structure and locate relevant implementations.