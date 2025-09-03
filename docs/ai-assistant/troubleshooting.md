# Apache Tomcat Troubleshooting Guide

## Quick Diagnosis Checklist

### Server Won't Start
1. ✅ Check JAVA_HOME is set correctly
2. ✅ Verify ports 8080/8005 are available
3. ✅ Review catalina.out for errors
4. ✅ Check file permissions on Tomcat directories
5. ✅ Validate server.xml syntax

### Application Won't Deploy
1. ✅ Check webapp directory structure
2. ✅ Validate web.xml syntax
3. ✅ Review context.xml configuration
4. ✅ Check class loading issues
5. ✅ Examine deployment logs

### Performance Issues
1. ✅ Monitor thread pool utilization
2. ✅ Check memory usage patterns
3. ✅ Review garbage collection logs  
4. ✅ Analyze connection pool metrics
5. ✅ Examine slow query patterns

## Startup Issues

### Issue: Port Already in Use
```
SEVERE: Failed to initialize connector [Connector[HTTP/1.1-8080]]
org.apache.catalina.LifecycleException: Protocol handler initialization failed
Caused by: java.net.BindException: Address already in use
```

**Diagnosis**:
```bash
# Find process using port 8080
lsof -i :8080
netstat -tlnp | grep 8080  # Linux
netstat -an | grep 8080    # macOS

# Find and kill process
kill -9 <PID>
```

**Solutions**:
1. Change port in server.xml: `<Connector port="8081" .../>`
2. Kill conflicting process
3. Use different IP binding: `address="127.0.0.1"`

### Issue: JAVA_HOME Not Set
```
Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
At least one of these environment variable is needed to run this program
```

**Diagnosis**:
```bash
echo $JAVA_HOME
which java
/usr/libexec/java_home     # macOS
update-alternatives --list java  # Linux
```

**Solutions**:
```bash
# Set JAVA_HOME permanently
export JAVA_HOME=/path/to/java
echo 'export JAVA_HOME=/path/to/java' >> ~/.bashrc

# macOS with SDKMAN
export JAVA_HOME=~/.sdkman/candidates/java/current

# macOS system Java
export JAVA_HOME=$(/usr/libexec/java_home)
```

### Issue: Permission Denied
```
java.io.FileNotFoundException: /opt/tomcat/logs/catalina.out (Permission denied)
```

**Diagnosis**:
```bash
ls -la $CATALINA_HOME
ps aux | grep tomcat
whoami
```

**Solutions**:
```bash
# Fix ownership
sudo chown -R tomcat:tomcat /opt/tomcat/

# Fix permissions
chmod 755 /opt/tomcat/bin/*.sh
chmod 644 /opt/tomcat/conf/*
chmod 755 /opt/tomcat/logs/

# Run as correct user
sudo -u tomcat $CATALINA_HOME/bin/startup.sh
```

### Issue: OutOfMemoryError on Startup
```
java.lang.OutOfMemoryError: Java heap space
```

**Diagnosis**:
```bash
# Check current heap size
java -XX:+PrintFlagsFinal -version | grep HeapSize

# Monitor memory during startup
jstat -gc <pid> 1s
```

**Solutions**:
```bash
# Increase heap size in setenv.sh
export CATALINA_OPTS="-Xms512m -Xmx2048m"

# Enable heap dump on OOM
export CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/"

# Use G1GC for large heaps
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
```

## Deployment Issues

### Issue: ClassNotFoundException
```
java.lang.ClassNotFoundException: com.example.MyServlet
```

**Diagnosis**:
```bash
# Check webapp structure
find webapps/myapp -name "*.class" | head -10
jar -tf webapps/myapp/WEB-INF/lib/myapp.jar | grep MyServlet

# Check classloader hierarchy
# Add to servlet:
ClassLoader cl = Thread.currentThread().getContextClassLoader();
while (cl != null) {
    System.out.println("ClassLoader: " + cl.getClass().getName());
    cl = cl.getParent();
}
```

**Solutions**:
1. Verify class is in correct location: `WEB-INF/classes/` or `WEB-INF/lib/`
2. Check package structure matches directory structure
3. Ensure JAR files are not corrupted
4. Review parent-first vs child-first class loading

### Issue: Web.xml Parsing Error
```
SEVERE: Parse Fatal Error at line 10 column 15: The content of element type "web-app" must match...
```

**Diagnosis**:
```bash
# Validate XML syntax
xmllint --noout webapps/myapp/WEB-INF/web.xml

# Check against schema
xmllint --schema web-app_5_0.xsd webapps/myapp/WEB-INF/web.xml
```

**Common Fixes**:
```xml
<!-- Correct DOCTYPE -->
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee 
         https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
         version="5.0">
</web-app>

<!-- Close all tags properly -->
<servlet-mapping>
    <servlet-name>MyServlet</servlet-name>
    <url-pattern>/my/*</url-pattern>
</servlet-mapping>
```

### Issue: Context Fails to Start
```
SEVERE: A child container failed during start
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: 
Failed to start component [StandardEngine[Catalina].StandardHost[localhost].StandardContext[/myapp]]
```

**Diagnosis Steps**:
```bash
# Check context.xml
cat conf/context.xml
cat webapps/myapp/META-INF/context.xml

# Review application logs
tail -f logs/localhost.*.log
tail -f logs/catalina.out

# Check resource dependencies
grep -r "jdbc" webapps/myapp/WEB-INF/
grep -r "jndi" webapps/myapp/WEB-INF/
```

**Common Solutions**:
1. Fix resource definitions in context.xml
2. Ensure database drivers are in lib/
3. Check annotation scanning configuration
4. Verify servlet-api version compatibility

## Performance Issues

### Issue: High CPU Usage
```
top - load average: 5.2, 4.8, 3.1
PID    USER  %CPU %MEM  COMMAND
12345  tomcat 300   45   java
```

**Diagnosis**:
```bash
# Thread dump analysis
kill -3 <tomcat_pid>  # Generates thread dump in catalina.out

# CPU profiling with built-in tools
jstack <pid> > threaddump.txt
jstat -gc <pid> 1s 10  # GC statistics

# Find CPU-intensive threads
top -H -p <pid>
printf "%x\n" <thread_id>  # Convert to hex for thread dump
```

**Solutions**:
```bash
# Tune thread pool
<Connector port="8080" 
           maxThreads="200"
           minSpareThreads="10"
           maxSpareThreads="50"/>

# Enable parallel GC
export CATALINA_OPTS="-XX:+UseParallelGC -XX:ParallelGCThreads=4"

# Reduce logging verbosity
# In logging.properties
.level = WARNING
```

### Issue: Memory Leaks
```
SEVERE: The web application [myapp] appears to have started a thread but has failed to stop it.
SEVERE: The web application [myapp] created a ThreadLocal with key of type [java.lang.ThreadLocal]
```

**Diagnosis**:
```bash
# Heap dump analysis
jcmd <pid> GC.run_finalization
jcmd <pid> GC.run
jmap -dump:live,format=b,file=heap.hprof <pid>

# Monitor heap usage
jstat -gccapacity <pid>
jstat -gcutil <pid> 1s
```

**Prevention Patterns**:
```java
// Proper ThreadLocal cleanup
public class MyServlet extends HttpServlet {
    private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();
    
    @Override
    public void destroy() {
        userContext.remove(); // Always clean up
    }
}

// Proper resource cleanup
@Override
public void contextDestroyed(ServletContextEvent sce) {
    // Stop background threads
    executorService.shutdown();
    
    // Clear ThreadLocals
    ThreadLocalCleaner.cleanUp();
    
    // Close database connections
    try {
        dataSource.close();
    } catch (Exception e) {
        log.warn("Error closing datasource", e);
    }
}
```

### Issue: Slow Database Queries
```
# Long-running queries in application logs
2024-01-15 10:30:15 WARN [http-nio-8080-exec-42] Query took 5234ms: SELECT * FROM users...
```

**Diagnosis**:
```bash
# Enable slow query logging
# In context.xml
<Resource name="jdbc/MyDS" 
          logSlowQueries="true"
          slowQueryThreshold="1000"
          logWriter="java.lang.System.out"/>

# Connection pool monitoring
# JMX metrics: Tomcat:type=DataSource,class=javax.sql.DataSource,name="jdbc/MyDS"
```

**Solutions**:
```xml
<!-- Connection pool tuning -->
<Resource name="jdbc/MyDS"
          initialSize="5"
          maxTotal="20"
          maxIdle="10"
          minIdle="5"
          maxWaitMillis="10000"
          testOnBorrow="true"
          testWhileIdle="true"
          validationQuery="SELECT 1"/>
```

## Network and Connectivity Issues

### Issue: Connection Timeouts
```
java.net.SocketTimeoutException: Read timed out
```

**Diagnosis**:
```bash
# Check network connectivity
telnet hostname 8080
nc -zv hostname 8080

# Monitor connection statistics
netstat -an | grep :8080 | wc -l
ss -tuln | grep :8080
```

**Solutions**:
```xml
<!-- Connector timeout tuning -->
<Connector port="8080"
           connectionTimeout="60000"
           keepAliveTimeout="15000"
           maxKeepAliveRequests="100"
           socketBuffer="65536"/>
```

### Issue: SSL/TLS Problems
```
javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
```

**Diagnosis**:
```bash
# Test SSL connection
openssl s_client -connect hostname:8443 -servername hostname

# Check certificate validity
keytool -list -keystore keystore.jks
openssl x509 -in cert.pem -text -noout

# Verify cipher suites
nmap --script ssl-enum-ciphers -p 8443 hostname
```

**Solutions**:
```xml
<!-- SSL configuration -->
<SSLHostConfig protocols="TLSv1.2,TLSv1.3"
               ciphers="ECDHE-RSA-AES256-GCM-SHA384,ECDHE-RSA-AES128-GCM-SHA256"
               honorCipherOrder="true">
    <Certificate certificateKeystoreFile="keystore.jks"
                 certificateKeystorePassword="password"
                 certificateKeyAlias="tomcat"/>
</SSLHostConfig>
```

## Session Issues

### Issue: Sessions Not Persisting
```
# User sessions lost after server restart
```

**Diagnosis**:
```bash
# Check session manager configuration
grep -A 10 -B 5 "Manager" conf/context.xml

# Verify session files
ls -la work/Catalina/localhost/ROOT/SESSIONS.ser

# Monitor session metrics via JMX
jconsole # Connect to Tomcat process
```

**Solutions**:
```xml
<!-- Enable session persistence -->
<Manager className="org.apache.catalina.session.PersistentManager"
         saveOnRestart="true"
         maxIdleBackup="60">
    <Store className="org.apache.catalina.session.FileStore"
           directory="sessions"/>
</Manager>
```

### Issue: Session Clustering Problems
```
SEVERE: Unable to serialize delta request for sessionId [ABC123]
java.io.NotSerializableException: com.example.NonSerializableObject
```

**Diagnosis**:
```bash
# Check cluster configuration
grep -A 20 "Cluster" conf/server.xml

# Monitor cluster membership
tail -f logs/catalina.out | grep -i cluster

# Test multicast connectivity
tcpdump -i eth0 host 228.0.0.4 and port 45564
```

**Solutions**:
```java
// Ensure session attributes are serializable
public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Mark non-serializable fields as transient
    private transient DataSource dataSource;
    
    // Implement custom serialization if needed
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // Custom serialization logic
    }
}
```

## Application-Specific Issues

### Issue: JSP Compilation Errors
```
org.apache.jasper.JasperException: Unable to compile class for JSP
```

**Diagnosis**:
```bash
# Check JSP compilation directory
ls -la work/Catalina/localhost/myapp/org/apache/jsp/

# Review generated Java source
cat work/Catalina/localhost/myapp/org/apache/jsp/index_jsp.java

# Check JSP compiler configuration
grep -r "jsp" conf/web.xml
```

**Solutions**:
1. Fix JSP syntax errors
2. Ensure proper JSP directive declarations
3. Check Java version compatibility
4. Verify tag library declarations

### Issue: Filter/Servlet Ordering Problems
```
# Filters not executing in expected order
```

**Solutions**:
```xml
<!-- Use filter-mapping order for execution sequence -->
<filter-mapping>
    <filter-name>FirstFilter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
</filter-mapping>

<filter-mapping>
    <filter-name>SecondFilter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
</filter-mapping>

<!-- Or use @WebFilter with explicit order -->
@WebFilter(filterName = "MyFilter", urlPatterns = "/*", 
           initParams = @WebInitParam(name = "order", value = "1"))
```

## Monitoring and Debugging Tools

### Built-in Diagnostic Tools
```bash
# JVM diagnostic commands
jcmd <pid> help                    # List available commands
jcmd <pid> VM.flags               # JVM flags
jcmd <pid> VM.system_properties   # System properties
jcmd <pid> Thread.print           # Thread dump
jcmd <pid> GC.class_histogram     # Class histogram

# Memory analysis
jmap -histo <pid>                 # Object histogram
jmap -dump:live,format=b,file=heap.hprof <pid>  # Heap dump

# Performance monitoring
jstat -gc <pid> 1s                # GC statistics
jstat -gccapacity <pid>           # GC capacity info
```

### Log Analysis
```bash
# Common log patterns
grep -E "(SEVERE|ERROR)" logs/catalina.out
grep -i "exception" logs/catalina.out | head -20
grep -i "timeout" logs/catalina.out

# Performance analysis
awk '/Request/ {print $1, $2, $NF}' logs/localhost_access_log.*.txt | sort -k3 -nr | head -20

# Memory usage tracking
grep -E "OutOfMemory|GC" logs/catalina.out
```

### JMX Monitoring
```bash
# Connect to JMX (add to CATALINA_OPTS)
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false

# Key MBeans to monitor:
# - Catalina:type=ThreadPool,name="http-nio-8080" (connector threads)
# - Catalina:type=Manager,host=localhost,context=/myapp (sessions)
# - java.lang:type=Memory (heap usage)
# - java.lang:type=GarbageCollector,name=* (GC stats)
```

## Emergency Recovery Procedures

### Server Hanging/Unresponsive
```bash
# 1. Generate thread dump
kill -3 <tomcat_pid>
jstack <tomcat_pid> > threaddump_$(date +%Y%m%d_%H%M%S).txt

# 2. Check for deadlocks
grep -A 10 -B 10 "deadlock" threaddump_*.txt

# 3. Force kill if necessary (last resort)
kill -9 <tomcat_pid>

# 4. Check for core dump
ls -la /tmp/java_pid*.hprof
ls -la /tmp/hs_err_pid*.log
```

### Disk Space Issues
```bash
# Check disk space
df -h

# Clean up logs
find logs/ -name "*.log" -mtime +7 -delete
find logs/ -name "*.txt" -mtime +7 -delete

# Clean up temp files
rm -rf work/Catalina/*/*
rm -rf temp/*

# Rotate logs
logrotate /etc/logrotate.d/tomcat
```

### Configuration Recovery
```bash
# Backup current config
cp -r conf/ conf.backup.$(date +%Y%m%d)

# Restore from backup
cp -r conf.backup.20240101/* conf/

# Validate configuration
$CATALINA_HOME/bin/catalina.sh configtest
```

## Prevention Best Practices

### Monitoring Setup
1. **Health Checks**: Implement `/health` endpoint
2. **Metrics Collection**: Use JMX or APM tools
3. **Log Aggregation**: Centralized logging solution
4. **Alerting**: CPU, memory, disk, and error rate thresholds

### Configuration Management
1. **Version Control**: Keep configs in Git
2. **Environment-specific**: Use property substitution
3. **Validation**: Automated config testing
4. **Documentation**: Keep runbooks updated

### Capacity Planning
1. **Load Testing**: Regular performance testing
2. **Resource Monitoring**: Track trends over time
3. **Scaling Triggers**: Defined thresholds
4. **Disaster Recovery**: Tested backup/restore procedures

This troubleshooting guide covers the most common issues encountered in Tomcat environments and provides systematic approaches to diagnosis and resolution.