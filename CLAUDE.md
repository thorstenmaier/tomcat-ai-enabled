
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

#### Automated Self-Improvement Command
The `/improve-yourself` command implements systematic documentation updates:
- **Usage**: Run periodically or after significant development sessions
- **Purpose**: Capture new insights, patterns, and troubleshooting knowledge
- **Scope**: Updates all AI documentation files with session learnings
- **Frequency**: Recommended after major debugging, performance work, or when discovering new patterns

#### Manual Self-Improvement Checklist
Before completing any task, check for:
1. **Repeated issues** that could be documented (e.g., JAVA_HOME problems, port conflicts)
2. **Project-specific patterns** not in standard documentation (e.g., SDKMAN paths)
3. **Useful command combinations** discovered during work
4. **Environment quirks** that affect builds or runtime
5. **New debugging techniques** or performance optimizations
6. **Updated dependencies** or configuration changes

Update this file proactively when encountering new patterns or solutions.

## AI Navigation Structure

### Codebase Statistics
- **Total Lines**: ~726,745 (580,174 main + 146,571 tests)
- **Java Files**: 2,732
- **Architecture**: Modular monolith with component-based design

### Critical Execution Paths

#### Request Processing Flow
1. **Network Layer** → `NioEndpoint` receives connection
2. **Protocol Layer** → `Http11Processor` parses HTTP
3. **Adapter Layer** → `CoyoteAdapter` bridges to Catalina
4. **Container Pipeline** → Engine → Host → Context → Wrapper
5. **Servlet Execution** → Application code runs

#### Component Hierarchy
```
Server (org.apache.catalina.Server)
└── Service (org.apache.catalina.Service)  
    ├── Connector (org.apache.coyote)
    └── Engine (org.apache.catalina.Engine)
        └── Host (org.apache.catalina.Host)
            └── Context (org.apache.catalina.Context)
                └── Wrapper (org.apache.catalina.Wrapper)
```

### Performance Hotspots
- `NioEndpoint` - I/O operations (java/org/apache/tomcat/util/net/)
- `CoyoteAdapter.service()` - Request processing bridge  
- `StandardWrapperValve.invoke()` - Servlet invocation
- `Request.java` - Most frequently modified file (connector & coyote packages)
- Buffer management in `org/apache/tomcat/util/buf/`

### Where to Find What

| Need | Location |
|------|----------|
| Servlet Implementation | `java/org/apache/catalina/` |
| HTTP Protocol Handling | `java/org/apache/coyote/` |
| JSP Compilation | `java/org/apache/jasper/` |
| WebSocket Support | `java/org/apache/tomcat/websocket/` |
| Session Replication | `java/org/apache/catalina/tribes/` |
| Database Pooling | `java/org/apache/tomcat/jdbc/` |
| Authentication | `java/org/apache/catalina/authenticator/` |
| Security Realms | `java/org/apache/catalina/realm/` |
| Expression Language | `java/org/apache/el/` |
| JNDI Implementation | `java/org/apache/naming/` |

### Dynamic Context Loading
**IMPORTANT**: Load additional documentation files on demand to optimize context usage. Use the Read tool to load these files only when needed:

#### Context-Specific Loading Rules
- **Architecture questions**: Load `docs/ai-assistant/architecture.md`
- **Domain-specific work**: Load relevant file from `docs/ai-assistant/domains/` (connector, security, lifecycle, etc.)
- **Service interactions**: Load `docs/ai-assistant/services/` files
- **Code pattern questions**: Load `docs/ai-assistant/patterns.md`
- **Search/navigation help**: Load `docs/ai-assistant/search-hints.md`  
- **API/class lookup**: Load relevant files from `docs/ai-assistant/indexes/`
- **Performance/memory issues**: Load relevant files from `docs/ai-assistant/memory/`
- **Conceptual navigation**: Load files from `docs/ai-assistant/semantic/`
- **Testing issues**: Load `docs/ai-assistant/testing.md`
- **Development workflow**: Load `docs/ai-assistant/workflow.md`

#### Available Documentation Structure
```
docs/ai-assistant/
├── architecture.md           # System design & component relationships
├── domains/                  # Functional area deep dives (10 domains)
├── services/                 # Component catalog & interactions  
├── patterns.md              # Tomcat-specific coding patterns
├── search-hints.md          # Concept-to-file quick reference
├── indexes/                 # API endpoints, class hierarchies, dependencies
├── memory/                  # Hotspots, patterns, troubleshooting
├── semantic/                # Conceptual navigation & relationships
├── testing.md               # Comprehensive testing strategies
└── workflow.md              # Development best practices
```

### Common Issues & Quick Fixes
- **Port 8080 in use**: `lsof -i :8080` then `kill <PID>`
- **JAVA_HOME not found**: Set explicitly for SDKMAN installations
- **Memory leaks on reload**: Check ThreadLocal cleanup in WebappClassLoaderBase
- **Test failures**: Often timing-related, increase timeouts
- **Character encoding**: Set encoding before reading request parameters
- **Session replication**: Check for non-serializable attributes

### No-Go Areas (Do Not Modify)
- `java/org/apache/el/parser/*` - Generated by JavaCC, modify .jj files instead
- `java/org/apache/tomcat/util/json/*` - Generated parser code
- `java/org/apache/tomcat/dbcp/dbcp2/*` - Repackaged Apache Commons DBCP
- `java/org/apache/tomcat/dbcp/pool2/*` - Repackaged Apache Commons Pool

## External Documentation Resources

### Context7 MCP Server Integration
The Context7 MCP server provides extensive Apache Tomcat documentation access:

#### Connection Details
- **Library ID**: `/websites/tomcat_apache_tomcat-10_1-doc` (31,279 code snippets)
- **Tool**: `mcp__context7__get-library-docs`
- **Coverage**: API docs, configuration examples, build system, lifecycle management, clustering, connection pooling, WebSocket support, Windows service management

#### Usage Patterns
- **Architecture Questions**: Query component interactions, design patterns, lifecycle management
- **Configuration Issues**: Retrieve specific configuration examples and best practices
- **Performance Tuning**: Access performance optimization guides and tuning parameters
- **Security Configuration**: Get security-related configuration and implementation details
- **Custom Development**: Find examples for custom valve, realm, or manager implementations

#### Integration Workflow
1. **Research Phase**: Use Context7 for comprehensive background on Tomcat features
2. **Implementation Phase**: Query specific APIs and implementation patterns
3. **Configuration Phase**: Retrieve configuration examples and best practices
4. **Troubleshooting**: Access diagnostic guides and common issue resolutions

**Access Pattern**: `mcp__context7__get-library-docs` with library ID and specific topic query