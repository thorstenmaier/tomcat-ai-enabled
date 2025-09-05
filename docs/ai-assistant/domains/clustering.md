# Clustering & High Availability Domain

## Purpose
Provides enterprise-grade clustering capabilities including session replication, distributed deployment, failure detection, and load balancing. Built on Apache Tribes group communication framework, this domain enables Tomcat to operate as a highly available, distributed application server with automatic failover and state synchronization.

## Architecture Overview
```
Cluster Architecture:
Channel (GroupChannel)
├── Interceptor Chain
│   ├── MessageDispatchInterceptor
│   ├── TcpFailureDetector
│   ├── EncryptInterceptor
│   ├── FragmentationInterceptor
│   └── OrderInterceptor
├── MembershipService (Multicast/Static/Cloud)
├── ChannelSender (ReplicationTransmitter)
└── ChannelReceiver (BioReceiver/NioReceiver)

Session Replication:
DeltaManager (All-to-All) ←→ BackupManager (Primary-Backup)
                ↓
        Delta Transmission
                ↓
        ReplicatedSession
```

## Key Components

### Apache Tribes Framework
- **`Channel`** (`o.a.catalina.tribes`) - Main communication interface
- **`GroupChannel`** (`o.a.catalina.tribes.group`) - Default channel implementation
- **`ChannelCoordinator`** (`o.a.catalina.tribes.group`) - Coordinates components
- **`Interceptor`** (`o.a.catalina.tribes`) - Message processing pipeline
- **`Member`** (`o.a.catalina.tribes`) - Cluster member representation
- **`MembershipService`** (`o.a.catalina.tribes.membership`) - Member discovery

### Session Replication
- **`DeltaManager`** (`o.a.catalina.ha.session`) - All-to-all session replication
- **`BackupManager`** (`o.a.catalina.ha.session`) - Primary-backup replication
- **`ReplicatedSession`** (`o.a.catalina.ha.session`) - Cluster-aware session
- **`DeltaRequest`** (`o.a.catalina.ha.session`) - Session change tracking
- **`SessionMessage`** (`o.a.catalina.ha.session`) - Session replication messages

### Communication Components
- **`ReplicationTransmitter`** (`o.a.catalina.tribes.transport`) - Message sending
- **`BioReceiver`** (`o.a.catalina.tribes.transport.bio`) - Blocking I/O receiver
- **`NioReceiver`** (`o.a.catalina.tribes.transport.nio`) - Non-blocking receiver
- **`McastService`** (`o.a.catalina.tribes.membership`) - Multicast membership
- **`KubernetesMembershipProvider`** (`o.a.catalina.tribes.membership`) - K8s discovery

### Distributed Services
- **`FarmWarDeployer`** (`o.a.catalina.ha.deploy`) - Cluster-wide deployment
- **`ReplicatedMap`** (`o.a.catalina.tribes.tipis`) - Distributed cache
- **`AbstractReplicatedMap`** (`o.a.catalina.tribes.tipis`) - Base replication logic
- **`ReplicatedContext`** (`o.a.catalina.ha.context`) - Distributed context attributes

## Entry Points

### Cluster Configuration
1. **`CatalinaCluster.start()`** - Initializes cluster components
2. **`SimpleTcpCluster.start()`** - Starts channel and managers
3. **`Channel.start()`** - Starts communication channel
4. **`Manager.start()`** - Initializes session managers

### Message Processing
- `Channel.send()` - Sends messages to cluster
- `ChannelListener.messageReceived()` - Handles incoming messages
- `Interceptor.sendMessage()` - Processes outbound messages
- `Interceptor.messageReceived()` - Processes inbound messages

## Communication Architecture

### Channel Operations
```java
// Channel interface provides core operations
public interface Channel {
    void send(Member[] destination, Serializable msg, int options);
    void addChannelListener(ChannelListener listener);
    void start(int svc) throws ChannelException;
    void stop(int svc) throws ChannelException;
    Member[] getMembers();
}
```

### Message Options
```java
// Channel send options (can be combined)
Channel.SEND_OPTIONS_DEFAULT         // Best effort
Channel.SEND_OPTIONS_USE_ACK        // Require acknowledgment
Channel.SEND_OPTIONS_SYNCHRONIZED   // Synchronous send
Channel.SEND_OPTIONS_ASYNCHRONOUS   // Async send (queue)
Channel.SEND_OPTIONS_SECURE         // Encrypt message
Channel.SEND_OPTIONS_UDP            // Use UDP transport
Channel.SEND_OPTIONS_MULTICAST      // Multicast to all
```

### Interceptor Chain
```java
// Message processing pipeline
MessageDispatchInterceptor  → Async message queuing
TcpFailureDetector         → Network failure detection
EncryptInterceptor         → Message encryption
FragmentationInterceptor   → Large message handling
OrderInterceptor          → Message ordering
GzipInterceptor          → Message compression
```

## Session Replication Strategies

### DeltaManager (All-to-All)
```java
// Replicates to all cluster members
<Manager className="org.apache.catalina.ha.session.DeltaManager"
         expireSessionsOnShutdown="false"
         notifyListenersOnReplication="true"
         notifySessionListenersOnReplication="false"
         notifyContainerListenersOnReplication="true"/>
```

**Characteristics:**
- Every node has complete session state
- High availability (multiple backups)
- Network overhead increases with cluster size
- Optimal for small clusters (2-4 nodes)

### BackupManager (Primary-Backup)
```java
// Primary-backup replication pattern
<Manager className="org.apache.catalina.ha.session.BackupManager"
         mapSendOptions="6" 
         rpcTimeout="15000"
         terminateOnStartFailure="false"/>
```

**Characteristics:**
- One primary, one backup per session
- Scales better with cluster size
- Lower network overhead
- Requires load balancer session affinity

### Delta Transmission
```java
// Only transmits changed attributes
class DeltaRequest implements Externalizable {
    public static final int TYPE_ATTRIBUTE = 0;
    public static final int TYPE_PRINCIPAL = 1;
    public static final int TYPE_LISTENER = 2;
    public static final int TYPE_MAXINACTIVE = 3;
    // Tracks specific changes, not entire session
}
```

## Membership Discovery

### Multicast-Based Discovery
```xml
<Membership className="org.apache.catalina.tribes.membership.McastService"
            address="228.0.0.4"
            port="45564"
            frequency="500"
            dropTime="3000"/>
```

### Static Membership
```xml
<Membership className="org.apache.catalina.tribes.membership.StaticMembershipService">
    <LocalMember className="org.apache.catalina.tribes.membership.StaticMember"
                port="4001" 
                securePort="-1" 
                host="127.0.0.1"
                domain="staging-cluster" 
                uniqueId="{0,1,2,3,4,5,6,7,8,9}"/>
</Membership>
```

### Cloud-Native Discovery
```xml
<Membership className="org.apache.catalina.tribes.membership.cloud.CloudMembershipService">
    <MembershipProvider 
        className="org.apache.catalina.tribes.membership.cloud.KubernetesMembershipProvider"
        namespace="default"
        serviceName="tomcat-cluster"/>
</Membership>
```

## Failure Detection & Recovery

### TCP Failure Detection
```java
// Network-level connectivity testing
TcpFailureDetector detector = new TcpFailureDetector();
detector.setConnectTimeout(1000);    // Connection timeout
detector.setPerformSendTest(true);   // Send test messages
detector.setPerformReadTest(true);   // Verify read capability
detector.setReadTestTimeout(5000);   // Read timeout
```

### Heartbeat Monitoring
```java
// TcpPingInterceptor configuration
<Interceptor className="org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor"
             interval="1000"
             useThread="true"/>
```

### Suspect Management
```java
// Member suspicion and recovery
class MemberImpl implements Member {
    private long suspectTime;  // When member became suspect
    private boolean suspect;   // Current suspect status
    // Configurable timeouts for failure detection
}
```

## Distributed Cache (ReplicatedMap)

### Basic Usage
```java
// Create replicated map
Channel channel = new GroupChannel();
ReplicatedMap<String, String> map = new ReplicatedMap<>(
    this, channel, 5000); // 5 second timeout

// Operations replicate to all nodes
map.put("key1", "value1");     // Replicates to cluster
String value = map.get("key1"); // Local read
```

### Advanced Features
```java
// Entry-level replication control
public interface ReplicatedMapEntry {
    boolean isDiffable();       // Support delta transmission
    byte[] getDiff();          // Get change delta
    void applyDiff(byte[] diff, int offset, int length);
    void resetDiff();          // Reset change tracking
}
```

## Distributed Deployment

### FarmWarDeployer
```xml
<Deployer className="org.apache.catalina.ha.deploy.FarmWarDeployer"
          tempDir="/tmp/war-temp/"
          deployDir="/tmp/war-deploy/"
          watchDir="/tmp/war-listen/"
          watchEnabled="true"/>
```

**Features:**
- Watches for new WAR files
- Replicates to all cluster members
- Synchronized deployment across nodes
- Handles deployment conflicts

## Performance Considerations

### Network Optimization
```xml
<!-- Optimize for network performance -->
<Sender className="org.apache.catalina.tribes.transport.ReplicationTransmitter">
    <Transport className="org.apache.catalina.tribes.transport.nio.PooledParallelSender"
               maxRetryAttempts="2"
               poolSize="2"
               timeout="3000"/>
</Sender>
```

### Memory Management
```java
// Configure session storage options
<Manager className="org.apache.catalina.ha.session.DeltaManager"
         maxInactiveInterval="1800"     // 30 minutes
         sessionAttributeNameFilter=".*" // Replicate all attributes
         sessionAttributeValueClassNameFilter=".*"/>
```

### Compression & Encryption
```xml
<!-- Message compression -->
<Interceptor className="org.apache.catalina.tribes.group.interceptors.GzipInterceptor"/>

<!-- Message encryption -->
<Interceptor className="org.apache.catalina.tribes.group.interceptors.EncryptInterceptor"
             encryptionKey="cafebabedeadbeefcafebabe"/>
```

## Common Operations

### Start Cluster
```java
CatalinaCluster cluster = new SimpleTcpCluster();
cluster.setContainer(host);
cluster.start();
// Automatically starts channel, managers, deployers
```

### Send Custom Message
```java
public class CustomMessage implements Serializable {
    private String data;
    // Implement message payload
}

Channel channel = cluster.getChannel();
Member[] members = channel.getMembers();
channel.send(members, new CustomMessage("data"), 
    Channel.SEND_OPTIONS_USE_ACK);
```

### Monitor Cluster Health
```java
// JMX monitoring
ObjectName clusterMBean = new ObjectName(
    "Catalina:type=Cluster,host=localhost");
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
String[] memberNames = (String[]) server.getAttribute(
    clusterMBean, "memberNames");
```

## Testing Strategies

### Unit Tests
```java
// Test channel communication
@Test
public void testChannelCommunication() throws Exception {
    GroupChannel channel1 = new GroupChannel();
    GroupChannel channel2 = new GroupChannel();
    
    channel1.start(Channel.DEFAULT);
    channel2.start(Channel.DEFAULT);
    
    channel1.send(channel2.getMembers(), 
        new TestMessage("test"), Channel.SEND_OPTIONS_DEFAULT);
}
```

### Integration Tests
```java
// Test session replication
@Test 
public void testSessionReplication() throws Exception {
    // Start two Tomcat instances with clustering
    // Create session on instance 1
    // Verify session exists on instance 2
}
```

### Load Testing
- Simulate high session creation/modification rates
- Test failover scenarios under load
- Measure replication latency and throughput
- Monitor memory usage during replication

## Common Issues & Solutions

### Issue: Split-Brain Scenarios
- **Solution**: Configure proper membership timeouts
- Use static membership for predictable environments
- Implement quorum-based decision making

### Issue: High Network Traffic
- **Solution**: Use BackupManager for large clusters
- Enable message compression
- Filter session attributes to replicate

### Issue: Session Failover Delays
- **Solution**: Tune failure detection timeouts
- Optimize network connectivity
- Use sticky session load balancing

### Issue: Memory Leaks in Clustering
- **Solution**: Monitor ReplicatedMap usage
- Configure proper session timeouts
- Use session attribute filters

## Security Considerations

### Message Encryption
```xml
<Interceptor className="org.apache.catalina.tribes.group.interceptors.EncryptInterceptor"
             encryptionKey="16ByteKey123456"
             encryptionAlgorithm="AES/CBC/PKCS5Padding"/>
```

### Network Security
- Use private VLANs for cluster communication
- Configure firewall rules for cluster ports
- Enable SSL/TLS for administrative interfaces
- Validate cluster member certificates

### Access Control
- Restrict cluster membership discovery
- Authenticate deployment operations
- Audit cluster configuration changes
- Monitor suspicious replication patterns

## Related Documentation
- [Session Management Domain](session-management.md) - Session lifecycle integration
- [Network I/O Domain](network-io.md) - Low-level communication
- [Web Application Deployment](webapp-deployment.md) - Distributed deployment
- [Monitoring Domain](monitoring.md) - Cluster health monitoring