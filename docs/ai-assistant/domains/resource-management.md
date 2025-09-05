# Resource Management Domain

## Purpose
Manages JNDI resources, database connection pooling, resource factories, and dependency injection. This domain provides enterprise-grade resource management including DataSources, mail sessions, JMS connections, and arbitrary JavaBeans with lifecycle management, security controls, and transaction integration.

## Architecture Overview
```
Resource Management Stack:
Application Code
      ↓
JNDI Lookup (java:comp/env/*)
      ↓
NamingContext (Security Layer)
      ↓
Resource Factory (Creation)
      ↓
Resource Implementation (e.g., ConnectionPool)
      ↓
External Resource (Database, JMS, etc.)

Configuration Flow:
server.xml/context.xml → NamingResourcesImpl → ResourceFactory → Resource Instance
```

## Key Components

### JNDI Infrastructure
- **`NamingContext`** (`o.a.naming`) - Core JNDI context implementation
- **`ContextAccessController`** (`o.a.naming`) - Security and access control
- **`ContextBindings`** (`o.a.naming`) - Thread-local context management
- **`NamingContextListener`** (`o.a.catalina.core`) - Lifecycle integration
- **`NamingResourcesImpl`** (`o.a.catalina.deploy`) - Resource configuration

### Resource Factories
- **`FactoryBase`** (`o.a.naming.factory`) - Base factory implementation
- **`DataSourceFactory`** (`o.a.tomcat.jdbc.pool`) - Database connection pools
- **`BeanFactory`** (`o.a.naming.factory`) - Generic JavaBean factory
- **`ResourceLinkFactory`** (`o.a.naming.factory`) - Cross-context resource sharing
- **`MailSessionFactory`** (`o.a.naming.factory`) - Mail session factory

### Connection Pooling
- **`ConnectionPool`** (`o.a.tomcat.jdbc.pool`) - Core pool implementation
- **`PoolConfiguration`** (`o.a.tomcat.jdbc.pool`) - Pool configuration interface
- **`DataSource`** (`o.a.tomcat.jdbc.pool`) - JDBC DataSource wrapper
- **`JdbcInterceptor`** (`o.a.tomcat.jdbc.pool`) - Interceptor framework
- **`PooledConnection`** (`o.a.tomcat.jdbc.pool`) - Wrapped connection

### Resource References
- **`ResourceRef`** (`o.a.naming`) - Resources requiring authentication
- **`ResourceEnvRef`** (`o.a.naming`) - Environment references
- **`ResourceLinkRef`** (`o.a.naming`) - Cross-context resource links
- **`Injectable`** (`o.a.catalina.core`) - Dependency injection support

## Entry Points

### Configuration Entry Points
1. **`server.xml`** → `<GlobalNamingResources>` → Global resources
2. **`context.xml`** → `<Resource>` → Context-specific resources
3. **`web.xml`** → `<resource-ref>` → Application resource requirements
4. **`@Resource` annotation** → Annotation-driven injection

### Runtime Entry Points
- `InitialContext.lookup()` - JNDI resource lookup
- `@Resource` field injection - Container-managed injection
- `ResourceFactory.getObjectInstance()` - Factory-based creation

## JNDI Architecture

### Context Hierarchy
```
Standard JNDI Tree:
java:
├── comp/
│   ├── env/          (Application environment)
│   ├── UserTransaction (JTA transactions)
│   └── ORB           (CORBA ORB)
├── global/           (Global resources)
└── module/           (Module-scoped resources)
```

### Security Model
```java
// Token-based access control
public class ContextAccessController {
    private Hashtable<Object, Object> readOnlyTokens;
    private Hashtable<Object, Object> securityTokens;
    
    public boolean checkSecurityToken(String name, Object token);
    public void setSecurityToken(Object name, Object token);
}
```

### Context Binding
```java
// Thread-local context association
public class ContextBindings {
    private static Hashtable<Object, Context> objectBindings;
    private static Hashtable<Thread, Context> threadBindings;
    private static Hashtable<ClassLoader, Context> clBindings;
}
```

## Resource Configuration

### Global Resources
```xml
<!-- server.xml -->
<GlobalNamingResources>
    <Resource name="jdbc/SharedDB"
              auth="Container" 
              type="javax.sql.DataSource"
              factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              driverClassName="org.postgresql.Driver"
              url="jdbc:postgresql://localhost/mydb"
              username="user" 
              password="pass"
              maxTotal="20"
              maxIdle="10"
              minIdle="2"/>
              
    <Resource name="mail/Session"
              auth="Container"
              type="jakarta.mail.Session"
              factory="org.apache.naming.factory.MailSessionFactory"
              mail.smtp.host="smtp.example.com"
              mail.smtp.port="587"
              mail.smtp.auth="true"/>
</GlobalNamingResources>
```

### Context Resources
```xml
<!-- context.xml -->
<Context>
    <Resource name="jdbc/LocalDB"
              auth="Container"
              type="javax.sql.DataSource" 
              factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost/appdb"
              username="app"
              password="secret"
              testOnBorrow="true"
              validationQuery="SELECT 1"/>
              
    <ResourceLink name="jdbc/SharedDB"
                  global="jdbc/SharedDB"
                  type="javax.sql.DataSource"/>
</Context>
```

### Application Declaration
```xml
<!-- web.xml -->
<web-app>
    <resource-ref>
        <res-ref-name>jdbc/LocalDB</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
    </resource-ref>
</web-app>
```

## Connection Pooling

### Pool Configuration
```java
// Comprehensive pool settings
public interface PoolConfiguration {
    // Core settings
    String getDriverClassName();
    String getUrl();
    String getUsername();
    String getPassword();
    
    // Pool sizing
    int getInitialSize();        // Initial connections
    int getMaxTotal();           // Maximum connections  
    int getMaxIdle();           // Maximum idle
    int getMinIdle();           // Minimum idle
    
    // Validation
    boolean isTestOnBorrow();   // Test on checkout
    boolean isTestOnReturn();   // Test on return
    boolean isTestWhileIdle();  // Test idle connections
    String getValidationQuery(); // Validation SQL
    int getValidationInterval(); // Validation frequency
    
    // Timeouts
    int getMaxWait();           // Wait for connection
    int getRemoveAbandonedTimeout(); // Abandoned timeout
    boolean isRemoveAbandoned(); // Enable cleanup
    boolean isLogAbandoned();   // Log abandoned
    
    // Advanced
    String getJdbcInterceptors(); // Interceptor chain
    String getConnectionProperties(); // Driver properties
}
```

### Interceptor Framework
```java
// Connection enhancement pipeline
public abstract class JdbcInterceptor {
    public abstract void reset(ConnectionPool parent, PooledConnection con);
    public abstract Object invoke(Object proxy, Method method, Object[] args);
    
    // Built-in interceptors:
    // ConnectionState - Track connection state
    // StatementFinalizer - Auto-close statements
    // StatementCache - Cache prepared statements  
    // SlowQueryReport - Log slow queries
    // ResetAbandonedTimer - Reset abandon timer
}
```

### Pool Implementation
```java
// Core pool management
public class ConnectionPool {
    private final BlockingQueue<PooledConnection> idle; // Available connections
    private final Set<PooledConnection> busy;           // Active connections
    private final AtomicInteger size;                   // Current pool size
    
    public Connection getConnection() throws SQLException;
    public void returnConnection(PooledConnection con);
    public void close(boolean force);
}
```

## Factory Pattern

### Resource Factory Interface
```java
public interface ObjectFactory {
    Object getObjectInstance(Object obj, Name name, Context nameCtx, 
                           Hashtable<?,?> environment);
}
```

### DataSource Factory
```java
public class DataSourceFactory implements ObjectFactory {
    public Object getObjectInstance(Object obj, Name name, 
                                  Context nameCtx, Hashtable<?,?> environment) {
        if (obj instanceof ResourceRef) {
            ResourceRef ref = (ResourceRef) obj;
            // Create DataSource from reference
            return createDataSource(ref);
        }
        return null;
    }
}
```

### Generic Bean Factory
```java
// Creates any JavaBean-compliant resource
public class BeanFactory implements ObjectFactory {
    public Object getObjectInstance(Object obj, Name name,
                                  Context nameCtx, Hashtable<?,?> environment) {
        // Use introspection to create and configure bean
        Class<?> beanClass = loadClass(className);
        Object bean = beanClass.newInstance();
        // Set properties via reflection
        return bean;
    }
}
```

## Resource Lifecycle

### Context Startup
```java
public class NamingContextListener implements LifecycleListener {
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
            createNamingContext();  // Set up JNDI context
        } else if (Lifecycle.START_EVENT.equals(event.getType())) {
            bindGlobalResources();  // Bind global resources
            bindContextResources(); // Bind context resources
        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            cleanupResources();     // Close connections, etc.
        }
    }
}
```

### Resource Injection
```java
// Annotation-based injection
@Resource(name="jdbc/MyDB")
private DataSource dataSource;

// Lookup-based access
InitialContext ctx = new InitialContext();
DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
```

## Transaction Integration

### JTA Support
```java
// Transaction-aware DataSource
<Resource name="jdbc/JTADS"
          factory="org.apache.tomcat.dbcp.dbcp2.managed.BasicManagedDataSourceFactory"
          transactionManager="java:comp/TransactionManager"
          xaDataSource="com.mysql.cj.jdbc.MysqlXADataSource"
          .../>
```

### Managed Connections
```java
// XA-aware connection factory
public class XAConnectionFactory implements ConnectionFactory {
    public Connection createConnection() throws SQLException {
        XAConnection xaConn = xaDataSource.getXAConnection();
        // Enlist XAResource in current transaction
        return new ManagedConnection(xaConn);
    }
}
```

## Common Operations

### Lookup Resource
```java
// Standard JNDI lookup
Context initCtx = new InitialContext();
Context envCtx = (Context) initCtx.lookup("java:comp/env");
DataSource ds = (DataSource) envCtx.lookup("jdbc/MyDB");

// Direct lookup
DataSource ds = (DataSource) new InitialContext()
    .lookup("java:comp/env/jdbc/MyDB");
```

### Configure Connection Pool
```java
// Programmatic configuration
PoolProperties p = new PoolProperties();
p.setUrl("jdbc:mysql://localhost:3306/mysql");
p.setDriverClassName("com.mysql.cj.jdbc.Driver");
p.setUsername("root");
p.setPassword("password");
p.setMaxTotal(30);
p.setInitialSize(10);
p.setTestOnBorrow(true);
p.setValidationQuery("SELECT 1");

DataSource datasource = new DataSource();
datasource.setPoolProperties(p);
```

### Custom Resource Factory
```java
public class MyResourceFactory implements ObjectFactory {
    public Object getObjectInstance(Object obj, Name name,
                                  Context nameCtx, Hashtable<?,?> env) {
        if (obj instanceof ResourceRef) {
            ResourceRef ref = (ResourceRef) obj;
            String type = ref.getClassName();
            
            if ("com.example.MyResource".equals(type)) {
                return createMyResource(ref);
            }
        }
        return null;
    }
}
```

## Performance Optimization

### Connection Pool Tuning
```xml
<!-- Optimal production settings -->
<Resource name="jdbc/OptimizedDB"
          type="javax.sql.DataSource"
          factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
          
          <!-- Pool sizing -->
          maxTotal="50"
          maxIdle="25"  
          minIdle="10"
          initialSize="10"
          
          <!-- Validation -->
          testOnBorrow="false"
          testOnReturn="false" 
          testWhileIdle="true"
          validationInterval="30000"
          validationQuery="SELECT 1"
          
          <!-- Timeouts -->
          maxWait="10000"
          removeAbandoned="true"
          removeAbandonedTimeout="60"
          logAbandoned="true"
          
          <!-- Advanced -->
          jdbcInterceptors="org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;
                           org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;
                           org.apache.tomcat.jdbc.pool.interceptor.StatementCache(max=200)"
          jmxEnabled="true"
          fairQueue="true"/>
```

### Monitoring Integration
```java
// JMX monitoring
ObjectName poolMBean = new ObjectName(
    "tomcat.jdbc:type=ConnectionPool,name=MyPool");
    
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
int active = (Integer) server.getAttribute(poolMBean, "Active");
int idle = (Integer) server.getAttribute(poolMBean, "Idle");
long createdCount = (Long) server.getAttribute(poolMBean, "CreatedCount");
```

## Testing Strategies

### Unit Tests
```java
// Test resource creation
@Test
public void testResourceFactory() throws Exception {
    ResourceRef ref = new ResourceRef("javax.sql.DataSource", 
        "Container", null, null);
    ref.add(new StringRefAddr("driverClassName", "org.h2.Driver"));
    ref.add(new StringRefAddr("url", "jdbc:h2:mem:test"));
    
    DataSourceFactory factory = new DataSourceFactory();
    DataSource ds = (DataSource) factory.getObjectInstance(
        ref, null, null, null);
    
    assertNotNull(ds);
    try (Connection conn = ds.getConnection()) {
        assertTrue(conn.isValid(1000));
    }
}
```

### Integration Tests
```java
// Test JNDI integration
@Test
public void testJNDILookup() throws Exception {
    System.setProperty("java.naming.factory.initial", 
        "org.apache.naming.java.javaURLContextFactory");
    System.setProperty("java.naming.factory.url.pkgs", 
        "org.apache.naming");
        
    InitialContext ctx = new InitialContext();
    DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/TestDB");
    assertNotNull(ds);
}
```

## Common Issues & Solutions

### Issue: Resource Not Found
- **Solution**: Check resource declaration in web.xml
- Verify context.xml resource configuration
- Confirm JNDI name matches lookup string

### Issue: Connection Pool Exhaustion
- **Solution**: Increase maxTotal pool size
- Enable abandoned connection detection
- Implement proper connection cleanup

### Issue: Memory Leaks
- **Solution**: Configure removeAbandoned settings
- Monitor for unclosed connections
- Use connection interceptors for debugging

### Issue: Slow Database Access
- **Solution**: Enable connection validation
- Configure appropriate pool sizes
- Use connection caching and statement caching

## Security Considerations

### Resource Access Control
```java
// Token-based security
ContextAccessController.setSecurityToken("myResource", securityToken);
ContextAccessController.checkSecurityToken("myResource", providedToken);
```

### Connection Security
- Use encrypted database connections (SSL)
- Store credentials in external configuration
- Implement connection authentication
- Monitor resource access patterns

### JNDI Security
- Restrict JNDI context modifications
- Use read-only contexts when appropriate
- Validate resource factory implementations
- Audit resource configuration changes

## Related Documentation
- [Web Application Deployment](webapp-deployment.md) - Resource binding during deployment
- [Configuration Domain](configuration.md) - server.xml and context.xml parsing
- [Security Domain](security.md) - Resource authentication integration
- [Monitoring Domain](monitoring.md) - Resource monitoring and JMX