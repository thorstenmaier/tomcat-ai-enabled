# Network I/O Domain

## Purpose
Handles all network I/O operations, connection management, and protocol endpoints for HTTP, HTTPS, AJP, and WebSocket protocols.

## Architecture Overview
```
Socket Accept → Connection Pool → Protocol Processing → Application
      ↓               ↓                    ↓              ↓
   Acceptor      SocketProcessor      Http11Processor   Servlet
```

## Key Components

### Endpoint Implementations
- **`NioEndpoint`** - Non-blocking I/O using java.nio (default)
- **`Nio2Endpoint`** - Asynchronous I/O using java.nio2
- **`AprEndpoint`** - Apache Portable Runtime (native performance)

### Connection Management
- **`Acceptor`** - Accepts incoming socket connections
- **`Poller`** - Monitors socket events (NIO only)
- **`SocketProcessor`** - Processes individual connections
- **`SocketWrapper`** - Abstracts socket operations

### Threading Model
- **`Executor`** - Thread pool for request processing
- **`TaskQueue`** - Custom queue for better thread management
- **`TaskThread`** - Enhanced thread with better monitoring

## Network Flow Detailed

### 1. Connection Acceptance
```java
public class Acceptor implements Runnable {
    @Override
    public void run() {
        while (endpoint.isRunning()) {
            try {
                // Accept new connection
                SocketChannel socket = serverSocket.accept();
                socket.configureBlocking(false);
                
                // Configure socket options
                setSocketOptions(socket);
                
                // Register with poller or process directly
                if (endpoint.isRunning()) {
                    endpoint.setSocketOptions(socket);
                }
            } catch (IOException e) {
                // Handle accept errors
            }
        }
    }
}
```

### 2. Socket Configuration
```java
protected boolean setSocketOptions(SocketChannel socket) {
    try {
        // Configure socket options
        socket.socket().setReceiveBufferSize(socketProperties.getRxBufSize());
        socket.socket().setSendBufferSize(socketProperties.getTxBufSize());
        socket.socket().setTcpNoDelay(socketProperties.getTcpNoDelay());
        socket.socket().setKeepAlive(socketProperties.getSoKeepAlive());
        socket.socket().setSoLinger(socketProperties.getSoLingerOn(), 
                                   socketProperties.getSoLingerTime());
        socket.socket().setSoTimeout(socketProperties.getSoTimeout());
        
        // Create socket wrapper
        NioSocketWrapper socketWrapper = new NioSocketWrapper(socket, this);
        
        // Register with poller
        getPoller().register(socketWrapper);
        return true;
    } catch (IOException e) {
        return false;
    }
}
```

### 3. Event Polling (NIO)
```java
public class Poller implements Runnable {
    private Selector selector;
    
    @Override
    public void run() {
        while (true) {
            try {
                // Wait for socket events
                int keyCount = selector.select(selectorTimeout);
                if (keyCount == 0) {
                    continue;
                }
                
                // Process ready sockets
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey sk = iterator.next();
                    NioSocketWrapper attachment = (NioSocketWrapper) sk.attachment();
                    iterator.remove();
                    
                    if (sk.isReadable() || sk.isWritable()) {
                        processSocket(attachment, SocketEvent.OPEN_READ, true);
                    }
                }
            } catch (IOException e) {
                // Handle polling errors
            }
        }
    }
}
```

### 4. Socket Processing
```java
public class SocketProcessor implements Runnable {
    private SocketWrapperBase<?> socketWrapper;
    private SocketEvent event;
    
    @Override
    public void run() {
        synchronized (socketWrapper) {
            if (socketWrapper.isClosed()) {
                return;
            }
            
            // Get protocol handler
            ConnectionHandler<S> handler = getHandler();
            SocketState state = SocketState.OPEN;
            
            do {
                // Process socket
                state = handler.process(socketWrapper, event);
                
                // Handle state transitions
                if (state == SocketState.CLOSED) {
                    close(socketWrapper, event);
                } else if (state == SocketState.UPGRADING) {
                    // Protocol upgrade (WebSocket, HTTP/2)
                    handler = getUpgradeHandler(socketWrapper);
                }
            } while (state == SocketState.ASYNC_END);
        }
    }
}
```

## Connector Configuration

### HTTP Connector
```xml
<Connector port="8080" 
           protocol="HTTP/1.1"
           connectionTimeout="20000"
           maxThreads="200"
           minSpareThreads="10"
           maxSpareThreads="75"
           acceptCount="100"
           enableLookups="false"
           compression="on"
           compressionMinSize="2048"
           noCompressionUserAgents="gozilla, traviata"
           compressableMimeType="text/html,text/xml,text/javascript,text/css,text/plain,application/javascript,application/json"/>
```

### HTTPS Connector
```xml
<Connector port="8443"
           protocol="HTTP/1.1"
           SSLEnabled="true"
           scheme="https"
           secure="true"
           maxThreads="150"
           clientAuth="false"
           sslProtocol="TLS"
           sslImplementationName="org.apache.tomcat.util.net.openssl.OpenSSLImplementation">
    <SSLHostConfig>
        <Certificate certificateKeystoreFile="conf/keystore.jks"
                     certificateKeystorePassword="password"
                     type="RSA"/>
    </SSLHostConfig>
</Connector>
```

### AJP Connector
```xml
<Connector protocol="AJP/1.3"
           address="::1"
           port="8009"
           redirectPort="8443"
           maxThreads="200"
           connectionTimeout="600000"
           packetSize="65536"/>
```

## Performance Optimizations

### Buffer Management
```java
public class SocketBufferHandler {
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    
    public ByteBuffer expand(ByteBuffer buffer, int newSize) {
        if (buffer.capacity() >= newSize) {
            return buffer;
        }
        
        // Allocate larger buffer
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(newSize);
        
        // Copy existing data
        buffer.flip();
        newBuffer.put(buffer);
        
        return newBuffer;
    }
    
    public void unReadReadBuffer(ByteBuffer returnedInput) {
        if (returnedInput != null) {
            readBuffer.position(readBuffer.position() - returnedInput.remaining());
        }
    }
}
```

### Connection Pooling
```java
// Keep-alive connection management
public class ConnectionHandler {
    private final ConcurrentMap<SocketWrapper, Long> keepAliveConnections = 
        new ConcurrentHashMap<>();
    
    public SocketState process(SocketWrapperBase<?> wrapper, SocketEvent event) {
        // Process request
        SocketState state = processor.process(wrapper, event);
        
        if (state == SocketState.OPEN) {
            // Keep connection alive
            keepAliveConnections.put(wrapper, System.currentTimeMillis());
            registerForEvent(wrapper, SocketEvent.OPEN_READ, true);
        }
        
        return state;
    }
    
    // Periodic cleanup of idle connections
    public void expireConnections() {
        long now = System.currentTimeMillis();
        
        for (Map.Entry<SocketWrapper, Long> entry : keepAliveConnections.entrySet()) {
            if (now - entry.getValue() > keepAliveTimeout) {
                entry.getKey().close();
                keepAliveConnections.remove(entry.getKey());
            }
        }
    }
}
```

### Thread Pool Optimization
```java
public class TaskQueue extends LinkedBlockingQueue<Runnable> {
    private ThreadPoolExecutor parent = null;
    
    @Override
    public boolean offer(Runnable o) {
        // If we have idle threads, use them
        if (parent.getPoolSize() < parent.getMaximumPoolSize()) {
            return false; // Force thread creation
        }
        
        // Otherwise queue the task
        return super.offer(o);
    }
    
    public boolean force(Runnable o) {
        // Force task into queue
        return super.offer(o);
    }
}
```

## SSL/TLS Implementation

### SSL Engine Configuration
```java
public class SSLUtil {
    
    public SSLEngine createSSLEngine(String host, int port) {
        SSLEngine engine = sslContext.createSSLEngine(host, port);
        
        // Configure protocols
        String[] enabledProtocols = getEnabledProtocols();
        engine.setEnabledProtocols(enabledProtocols);
        
        // Configure cipher suites
        String[] enabledCiphers = getEnabledCipherSuites();
        engine.setEnabledCipherSuites(enabledCiphers);
        
        // Configure client authentication
        if (clientAuth.equals("required")) {
            engine.setNeedClientAuth(true);
        } else if (clientAuth.equals("optional")) {
            engine.setWantClientAuth(true);
        }
        
        return engine;
    }
}
```

### SSL Handshake
```java
public int handshake(boolean read, boolean write) throws IOException {
    SSLEngineResult.HandshakeStatus handshakeStatus = 
        sslEngine.getHandshakeStatus();
    
    while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
           handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
        
        switch (handshakeStatus) {
            case NEED_UNWRAP:
                if (netInBuffer.hasRemaining()) {
                    SSLEngineResult result = sslEngine.unwrap(netInBuffer, appInBuffer);
                    handshakeStatus = result.getHandshakeStatus();
                } else {
                    // Need more network data
                    return 0;
                }
                break;
                
            case NEED_WRAP:
                SSLEngineResult result = sslEngine.wrap(EMPTY_BUF, netOutBuffer);
                handshakeStatus = result.getHandshakeStatus();
                flush();
                break;
                
            case NEED_TASK:
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null) {
                    task.run();
                }
                handshakeStatus = sslEngine.getHandshakeStatus();
                break;
        }
    }
    
    return 1; // Handshake complete
}
```

## Monitoring & Diagnostics

### Connection Statistics
```java
public class EndpointStatistics {
    private final AtomicLong connectionsAccepted = new AtomicLong(0);
    private final AtomicLong connectionsCurrent = new AtomicLong(0);
    private final AtomicLong connectionsMax = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    
    public void connectionOpened() {
        connectionsAccepted.incrementAndGet();
        long current = connectionsCurrent.incrementAndGet();
        
        // Update max if needed
        long max = connectionsMax.get();
        while (current > max && !connectionsMax.compareAndSet(max, current)) {
            max = connectionsMax.get();
        }
    }
    
    public void connectionClosed() {
        connectionsCurrent.decrementAndGet();
    }
}
```

### Performance Monitoring
```java
public class SocketProcessorMonitor extends SocketProcessor {
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);
    private static final AtomicLong requestCount = new AtomicLong(0);
    
    @Override
    public void run() {
        long start = System.nanoTime();
        
        try {
            super.run();
        } finally {
            long elapsed = System.nanoTime() - start;
            totalProcessingTime.addAndGet(elapsed);
            requestCount.incrementAndGet();
            
            // Log slow requests
            long elapsedMs = elapsed / 1_000_000;
            if (elapsedMs > SLOW_REQUEST_THRESHOLD) {
                log.warn("Slow request processing: {}ms", elapsedMs);
            }
        }
    }
    
    public static double getAverageProcessingTime() {
        long count = requestCount.get();
        return count > 0 ? totalProcessingTime.get() / (double) count / 1_000_000 : 0;
    }
}
```

## Testing Network Components

### Unit Tests
```java
@Test
public void testSocketAccept() throws Exception {
    NioEndpoint endpoint = new NioEndpoint();
    endpoint.setPort(0); // Random port
    endpoint.init();
    endpoint.start();
    
    int port = endpoint.getLocalPort();
    
    // Connect to endpoint
    try (SocketChannel client = SocketChannel.open()) {
        client.connect(new InetSocketAddress("localhost", port));
        assertTrue(client.isConnected());
        
        // Verify connection accepted
        Thread.sleep(100);
        assertTrue(endpoint.getCurrentThreadsBusy() > 0);
    }
    
    endpoint.stop();
    endpoint.destroy();
}
```

### Load Testing
```java
@Test
public void testConcurrentConnections() throws Exception {
    int maxConnections = 100;
    CountDownLatch latch = new CountDownLatch(maxConnections);
    AtomicInteger successCount = new AtomicInteger(0);
    
    for (int i = 0; i < maxConnections; i++) {
        executor.execute(() -> {
            try (Socket socket = new Socket("localhost", getPort())) {
                socket.getOutputStream().write("GET / HTTP/1.1\r\n\r\n".getBytes());
                socket.getOutputStream().flush();
                
                // Read response
                byte[] buffer = new byte[1024];
                socket.getInputStream().read(buffer);
                successCount.incrementAndGet();
            } catch (IOException e) {
                // Connection failed
            } finally {
                latch.countDown();
            }
        });
    }
    
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    assertEquals(maxConnections, successCount.get());
}
```

## Common Issues & Solutions

### Issue: Too Many Open Files
**Cause**: File descriptor limit reached  
**Solution**: 
```bash
# Increase OS limits
ulimit -n 65536

# Configure connector appropriately
<Connector maxConnections="10000" acceptCount="100"/>
```

### Issue: Connection Timeouts
**Cause**: Slow clients or network issues  
**Solution**:
```xml
<Connector connectionTimeout="60000"
           keepAliveTimeout="15000"
           maxKeepAliveRequests="100"/>
```

### Issue: SSL Performance Issues
**Cause**: Inefficient SSL configuration  
**Solution**:
```xml
<!-- Use hardware acceleration -->
<Connector sslImplementationName="org.apache.tomcat.util.net.openssl.OpenSSLImplementation">
    <SSLHostConfig protocols="TLSv1.2,TLSv1.3"
                   ciphers="ECDHE-RSA-AES256-GCM-SHA384,ECDHE-RSA-AES128-GCM-SHA256"/>
</Connector>
```

### Issue: Memory Leaks from Direct Buffers
**Cause**: Direct ByteBuffers not released  
**Solution**:
```java
public void releaseBuffers() {
    if (readBuffer != null && readBuffer.isDirect()) {
        DirectByteBufferUtils.cleanDirectBuffer(readBuffer);
    }
    if (writeBuffer != null && writeBuffer.isDirect()) {
        DirectByteBufferUtils.cleanDirectBuffer(writeBuffer);
    }
}
```

## Performance Tuning Guidelines

### OS-Level Tuning
```bash
# Increase connection limits
echo 'net.core.somaxconn=65536' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_max_syn_backlog=65536' >> /etc/sysctl.conf

# TCP tuning
echo 'net.ipv4.tcp_keepalive_time=600' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_keepalive_intvl=60' >> /etc/sysctl.conf
```

### JVM Tuning
```bash
# Direct memory for NIO buffers
-XX:MaxDirectMemorySize=1G

# GC tuning for low latency
-XX:+UseG1GC -XX:MaxGCPauseMillis=50

# Native memory tracking
-XX:NativeMemoryTracking=summary
```

## Dependencies
- **Depends on**: JVM NIO implementation, SSL libraries, OS networking stack
- **Used by**: All protocol handlers (HTTP, AJP, WebSocket), Coyote adapter

## Related Domains
- **[Request Processing](request-processing.md)** - Protocol processing integration  
- **[Clustering](clustering.md)** - Network communication between nodes
- **[Monitoring](monitoring.md)** - Network performance metrics