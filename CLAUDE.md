# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Apache Tomcat 12.0 - Open source implementation of Jakarta Servlet 6.2, Jakarta Pages 4.1, Jakarta Expression Language 6.1, Jakarta WebSocket 2.3, and Jakarta Authentication 3.2 specifications.

## Build System

Apache Ant-based build system. Requires:
- Java Development Kit (JDK) 17 or later
- Apache Ant 1.10.15 or later

## Essential Commands

### Build Commands
```bash
# Full build (creates working Tomcat instance)
ant deploy

# Clean build directories
ant clean

# Build and run all tests
ant test

# Run tests without recompiling
ant test-only

# Run a specific test class
ant test -Dtest.name="**/TestClassName.java"

# Create release distribution
ant release

# Build documentation
ant build-docs

# Generate Javadoc
ant javadoc
```

### Development Commands
```bash
# Validate source code style
ant validate

# Download compile dependencies
ant download-compile

# Compile source code only
ant compile

# IDE setup
ant ide-eclipse     # Eclipse
ant ide-intellij    # IntelliJ IDEA
ant ide-netbeans    # NetBeans
```

### Running Tomcat

**Important**: On macOS with SDKMAN, JAVA_HOME must be set explicitly:

```bash
# After building with 'ant deploy'

# For macOS with SDKMAN-managed Java:
export JAVA_HOME=/Users/$(whoami)/.sdkman/candidates/java/current
./output/build/bin/catalina.sh run

# For systems with standard Java installation:
./output/build/bin/catalina.sh run     # Unix/Linux/Mac
output\build\bin\catalina.bat run      # Windows

# Or set CATALINA_HOME and use scripts
export CATALINA_HOME=./output/build
$CATALINA_HOME/bin/startup.sh
```

**Troubleshooting**:
- If port 8080 is in use: `lsof -i :8080 | grep LISTEN` then `kill <PID>`
- If JAVA_HOME not found: Check with `which java` and set JAVA_HOME to parent of bin directory
- Default URL after startup: http://localhost:8080

## Architecture

### Source Structure
- `java/jakarta/` - Jakarta EE API implementations (Servlet, EL, WebSocket, etc.)
- `java/org/apache/catalina/` - Core Catalina servlet container
- `java/org/apache/coyote/` - Coyote connector framework (HTTP, AJP protocols)
- `java/org/apache/jasper/` - Jasper JSP engine
- `java/org/apache/tomcat/` - Utility classes and shared components
- `java/org/apache/el/` - Expression Language implementation

### Key Components

**Catalina (Servlet Container)**
- Server → Service → Engine → Host → Context → Wrapper
- Manages lifecycle, request processing pipeline, session management

**Coyote (Connector Framework)**
- Protocol handlers for HTTP/1.1, HTTP/2, AJP
- NIO, NIO2, and APR connector implementations
- Request/response processing abstraction

**Jasper (JSP Engine)**
- JSP compilation to servlets
- Runtime JSP handling
- Tag library support

**Pipeline Architecture**
- Valve-based request processing pipeline
- Each container has a pipeline with valves
- StandardEngineValve → StandardHostValve → StandardContextValve → StandardWrapperValve

### Testing

Tests located in `test/` directory, organized by package structure.
- Unit tests: Direct component testing
- Integration tests: Full container testing
- Performance tests: Benchmarking components
- WebSocket tests: Async communication testing

Test execution uses JUnit with custom Tomcat test infrastructure for container lifecycle management.

## Configuration

Build configuration via `build.properties` (create from `build.properties.default`):
```properties
# Required: Set download location for dependencies
base.path=/path/to/download/directory

# Optional: Proxy settings
proxy.host=proxy.example.com
proxy.port=8080
```

Runtime configuration:
- `conf/server.xml` - Server configuration
- `conf/web.xml` - Default servlet definitions
- `conf/catalina.properties` - Catalina system properties
- `conf/context.xml` - Default context configuration

## Knowledge Base for Future Sessions

### Environment-Specific Notes
- **macOS with SDKMAN**: Java installations are typically at `~/.sdkman/candidates/java/current`, not detected by `/usr/libexec/java_home`
- **Port conflicts**: Port 8080 and 8005 are commonly used; always check with `lsof` before starting
- **Build output**: The built Tomcat instance is in `output/build/`, not in the root directory

### Common Tasks and Solutions
1. **Quick rebuild and run**:
   ```bash
   ant clean deploy && export JAVA_HOME=~/.sdkman/candidates/java/current && ./output/build/bin/catalina.sh run
   ```

2. **Finding and killing Tomcat processes**:
   ```bash
   lsof -i :8080 -i :8005 | grep LISTEN
   ps aux | grep catalina
   ```

3. **Checking logs**:
   - Build logs: `output/build/logs/`
   - Test results: `output/build/logs/`
   - Catalina logs: `output/build/logs/catalina.out`

### Performance and Development Tips
- Use `ant test-only` to skip compilation when re-running tests
- The build system caches downloaded dependencies in `~/tomcat-build-libs/`
- For faster builds, avoid `ant clean` unless necessary
- Background Tomcat processes can be managed with process IDs from `lsof` output

### Self-Improvement Protocol
Before completing any task, check for:
1. **Repeated issues** that could be documented (e.g., JAVA_HOME problems, port conflicts)
2. **Project-specific patterns** not in standard documentation (e.g., SDKMAN paths)
3. **Useful command combinations** discovered during work
4. **Environment quirks** that affect builds or runtime

Update this file proactively when encountering new patterns or solutions.