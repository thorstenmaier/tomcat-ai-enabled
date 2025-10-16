# Apache Tomcat Development Workflow

## Development Environment Setup

### Prerequisites
```bash
# Required tools
- Java 17+ JDK
- Apache Ant 1.10.15+
- Git
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

# Optional but recommended
- Maven (for dependency management understanding)
- Docker (for testing in containers)
- JProfiler/YourKit (for performance analysis)
```

### First-Time Setup (Complete Walkthrough)

This section walks you through setting up Tomcat from scratch, including installing all prerequisites.

#### Step 1: Install Java Development Kit

**Check existing Java installation:**
```bash
java -version
```

**macOS - Find Java installations:**
```bash
# List all installed JDKs
/usr/libexec/java_home -V

# Get path to default JDK
/usr/libexec/java_home
```

**Install Java if needed:**
- **macOS**: Download from [Microsoft OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download) or use Homebrew: `brew install openjdk@21`
- **Linux**: `sudo apt install openjdk-21-jdk` (Ubuntu/Debian) or `sudo yum install java-21-openjdk-devel` (RHEL/CentOS)
- **Windows**: Download from [Microsoft OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download) or [Oracle](https://www.oracle.com/java/technologies/downloads/)

#### Step 2: Install Apache Ant

**Check if Ant is installed:**
```bash
ant -version
```

**Install Ant:**
- **macOS**: `brew install ant`
- **Linux**: `sudo apt install ant` (Ubuntu/Debian) or `sudo yum install ant` (RHEL/CentOS)
- **Windows**: Download from [Apache Ant website](https://ant.apache.org/bindownload.cgi) and add to PATH

**Verify installation:**
```bash
ant -version
# Should show: Apache Ant(TM) version 1.10.15 or later
```

#### Step 3: Clone and Configure

```bash
# Clone repository (if not already cloned)
git clone https://github.com/apache/tomcat.git
cd tomcat

# Create build.properties from template
cp build.properties.default build.properties

# Edit build.properties to set base.path for dependencies
# The base.path is where Ant will download and cache build dependencies
echo "base.path=${HOME}/tomcat-build-libs" >> build.properties
```

**What is base.path?**
The `base.path` directory is where Ant downloads compile dependencies like servlet APIs, JUnit libraries, and other required JARs. This allows dependency caching across builds.

#### Step 4: Build Tomcat

```bash
# Full build (creates working Tomcat instance in output/build/)
ant deploy
```

**What happens during `ant deploy`:**
1. Downloads all compile dependencies to `base.path` directory
2. Compiles ~1,750 Java source files
3. Packages JARs and creates directory structure
4. Copies configuration files to `output/build/`
5. Creates a complete, runnable Tomcat instance

**Build output location:** `output/build/` contains your built Tomcat server

#### Step 5: Set JAVA_HOME and Start Tomcat

**Find correct JAVA_HOME:**
```bash
# macOS - Using java_home utility (recommended)
export JAVA_HOME=$(/usr/libexec/java_home)
echo $JAVA_HOME

# macOS - If using SDKMAN
export JAVA_HOME=$HOME/.sdkman/candidates/java/current

# Linux - Find Java installation
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))

# Windows (PowerShell)
# Usually: C:\Program Files\Java\jdk-21
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

**Check for port conflicts:**
```bash
# Check if port 8080 is already in use
lsof -i :8080

# If occupied, kill the process
kill <PID>

# Or change the port in conf/server.xml before starting
```

**Start Tomcat:**
```bash
# Unix/Linux/macOS
./output/build/bin/catalina.sh run

# Windows
output\build\bin\catalina.bat run
```

**Verify startup:**
- Watch console output for "Server startup in [X] milliseconds"
- Open browser to: http://localhost:8080
- You should see the Tomcat welcome page

**Available URLs after startup:**
- Main page: http://localhost:8080
- Examples: http://localhost:8080/examples
- Documentation: http://localhost:8080/docs
- Manager: http://localhost:8080/manager (requires user configuration)

**Logs location:**
- Main log: `output/build/logs/catalina.out`
- Other logs: `output/build/logs/`

### Quick Setup (If Dependencies Already Installed)

If you already have Java and Ant installed:

```bash
# Clone and setup
git clone https://github.com/apache/tomcat.git
cd tomcat
cp build.properties.default build.properties
echo "base.path=${HOME}/tomcat-build-libs" >> build.properties

# Build and run (one-liner)
ant deploy && export JAVA_HOME=$(/usr/libexec/java_home) && ./output/build/bin/catalina.sh run
```

### Troubleshooting First-Time Setup

**Problem: "ant: command not found"**
- Solution: Install Apache Ant (see Step 2 above)
- On macOS with Homebrew: `brew install ant`

**Problem: "JAVA_HOME is not defined correctly"**
```bash
# Verify Java installation
java -version

# Find correct JAVA_HOME
/usr/libexec/java_home -V  # macOS
which java                  # Linux/macOS

# Set JAVA_HOME correctly (macOS)
export JAVA_HOME=$(/usr/libexec/java_home)

# Verify it points to JDK, not JRE
ls $JAVA_HOME/bin/javac  # Should exist for JDK
```

**Problem: "Port 8080 already in use"**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill <PID>

# Or check for both Tomcat ports
lsof -i :8080 -i :8005 | grep LISTEN
```

**Problem: "Build fails with download errors"**
- Check internet connection
- Verify proxy settings in build.properties if behind corporate proxy:
```properties
proxy.host=proxy.example.com
proxy.port=8080
proxy.user=username
proxy.password=password
```

**Problem: "OutOfMemoryError during build"**
```bash
# Increase Ant memory
export ANT_OPTS="-Xmx1024m"
ant deploy
```

### IDE Configuration

#### IntelliJ IDEA
```bash
# Generate IntelliJ project files
ant ide-intellij

# Import project
# File → Open → select tomcat directory
# Use existing sources, don't import from external model

# Configure JDK
# File → Project Structure → Project → Project SDK: Java 17+

# Set up run configuration
# Run → Edit Configurations → + → Application
# Main class: org.apache.catalina.startup.Bootstrap
# VM options: -Dcatalina.home=./output/build -Dcatalina.base=./output/build
# Working directory: ./output/build
```

#### Eclipse
```bash
# Generate Eclipse project files
ant ide-eclipse

# Import in Eclipse
# File → Import → General → Existing Projects into Workspace
# Select tomcat directory
```

#### VS Code
```json
// .vscode/launch.json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Tomcat",
            "request": "launch",
            "mainClass": "org.apache.catalina.startup.Bootstrap",
            "args": ["run"],
            "vmArgs": "-Dcatalina.home=./output/build -Dcatalina.base=./output/build",
            "cwd": "./output/build"
        }
    ]
}
```

## Development Workflow

### Branch Management Strategy
```bash
# Main development branch
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/TOMCAT-12345-new-feature

# Naming conventions:
# feature/TOMCAT-XXXXX-description
# bugfix/TOMCAT-XXXXX-description
# improvement/TOMCAT-XXXXX-description
```

### Code Development Cycle

#### 1. Planning Phase
```bash
# Research existing implementation
find java/ -name "*.java" -exec grep -l "similar-functionality" {} \;

# Check related Bugzilla issues
# https://bz.apache.org/bugzilla/

# Review existing tests
find test/ -name "*Test*.java" -exec grep -l "related-feature" {} \;
```

#### 2. Implementation Phase
```bash
# Make changes following existing patterns
# Follow code style guidelines in CONTRIBUTING.md

# Build frequently
ant compile

# Run related tests
ant test -Dtest.name="**/TestRelatedFeature*.java"

# Full build and test
ant clean compile test
```

#### 3. Testing Phase
```bash
# Unit tests (fast feedback)
ant test -Dtest.name="**/TestMyNewFeature.java"

# Integration tests
ant test -Dtest.name="**/TestMyNewFeatureIntegration.java"

# Regression tests (specific area)
ant test -Dtest.name="**/Test*Session*.java"

# Full test suite (before commit)
ant test
```

#### 4. Quality Assurance
```bash
# Code style validation
ant validate

# Static analysis (if available)
ant findbugs

# Performance regression check
ant test -Dtest.name="**/TestPerformance*.java"

# Manual testing
ant deploy
./output/build/bin/catalina.sh run
# Test functionality manually
```

### Code Style Guidelines

#### Java Code Style
```java
// Class structure
public class StandardExample extends BaseClass implements Interface {
    
    // Static fields first
    private static final String CONSTANT = "value";
    private static final Log log = LogFactory.getLog(StandardExample.class);
    
    // Instance fields
    private String instanceField;
    private boolean flagField = false;
    
    // Constructor
    public StandardExample(String parameter) {
        this.instanceField = parameter;
    }
    
    // Public methods
    public void publicMethod() {
        if (condition) {
            // 4-space indentation
            doSomething();
        }
    }
    
    // Private methods
    private void doSomething() {
        // Implementation
    }
}
```

#### Naming Conventions
```java
// Classes: PascalCase
public class MyNewFeature

// Methods and variables: camelCase  
public void processRequest()
private String requestId

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRY_COUNT = 3;

// Packages: lowercase with dots
org.apache.catalina.core
```

#### Documentation Standards
```java
/**
 * Brief description of the class purpose.
 * 
 * <p>Detailed description if needed. Explain the main responsibility
 * and how it fits into the overall architecture.</p>
 * 
 * @since Tomcat 12.0
 */
public class MyNewFeature {
    
    /**
     * Process the incoming request.
     * 
     * @param request the HTTP request to process
     * @param response the HTTP response to populate
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void processRequest(HttpServletRequest request, 
                              HttpServletResponse response)
            throws IOException, ServletException {
        // Implementation
    }
}
```

### Testing Best Practices

#### Test Structure
```java
public class TestMyNewFeature extends TomcatBaseTest {
    
    @Test
    public void testBasicFunctionality() throws Exception {
        // Given - setup test data
        MyNewFeature feature = new MyNewFeature();
        String testInput = "test-data";
        
        // When - execute the code under test
        String result = feature.process(testInput);
        
        // Then - verify the results
        assertEquals("expected-result", result);
    }
    
    @Test
    public void testErrorCondition() throws Exception {
        // Test error handling
        MyNewFeature feature = new MyNewFeature();
        
        try {
            feature.process(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Input cannot be null", e.getMessage());
        }
    }
    
    @Test
    public void testIntegrationWithTomcat() throws Exception {
        // Integration test with embedded Tomcat
        Tomcat.addServlet(context, "test", new TestServlet());
        context.addServletMappingDecoded("/test", "test");
        
        ByteChunk bc = getUrl(getUrl("/test"));
        assertEquals("Expected response", bc.toString());
    }
}
```

#### Test Categories
```java
// Unit tests - fast, isolated
@Category(UnitTest.class)
public class TestMyFeatureUnit {
    // Tests with mocks, no external dependencies
}

// Integration tests - medium speed
@Category(IntegrationTest.class)
public class TestMyFeatureIntegration {
    // Tests with real Tomcat instances
}

// System tests - slow, end-to-end
@Category(SystemTest.class)
public class TestMyFeatureSystem {
    // Full system tests with external dependencies
}
```

### Commit Guidelines

#### Commit Message Format
```
Component: Brief description (50 chars max)

Optional detailed explanation of the changes. Wrap at 72 characters.
Explain what changed and why, not how.

- Use bullet points for multiple changes
- Reference Bugzilla issues: BZ 12345
- Include performance impact if relevant

Reviewed-by: Reviewer Name <email@example.com>
```

#### Examples
```bash
# Good commit messages
"Fix BZ 12345 - NPE in StandardContext.startInternal()"

"Improve NIO connector performance by optimizing buffer reuse

- Reuse ByteBuffers in SocketWrapper to reduce allocation
- Add buffer pool monitoring via JMX
- Performance improvement: 15% fewer allocations under load

BZ 12346"

"Add support for SameSite cookie attribute

Implements Jakarta Servlet specification requirement for SameSite
cookie attribute handling in response cookies.

BZ 12347"
```

### Code Review Process

#### Pre-Review Checklist
- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] Performance impact assessed
- [ ] Backward compatibility maintained

#### Review Criteria
1. **Correctness**: Does the code do what it's supposed to?
2. **Performance**: Any negative performance impact?
3. **Security**: Introduces security vulnerabilities?
4. **Maintainability**: Easy to understand and modify?
5. **Testing**: Adequate test coverage?
6. **Documentation**: Sufficient comments and javadoc?

#### Common Review Comments
```java
// Performance concern
// Consider using StringBuilder instead of string concatenation
String result = "";
for (String item : items) {
    result += item; // Inefficient
}

// Better
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item);
}
String result = sb.toString();

// Security concern
// Validate input to prevent injection
public void processInput(String userInput) {
    // Bad: direct use without validation
    executeQuery("SELECT * FROM users WHERE name = '" + userInput + "'");
    
    // Good: parameterized query
    executeQuery("SELECT * FROM users WHERE name = ?", userInput);
}

// Thread safety concern
// This field should be volatile or use proper synchronization
private boolean stopRequested; // Not thread-safe

// Better
private volatile boolean stopRequested;
```

### Build and Release Process

#### Local Build Verification
```bash
# Clean build
ant clean compile

# Run tests
ant test

# Build distribution
ant release

# Verify distribution
cd output/release/v*/
tar -tzf apache-tomcat-*.tar.gz | head -20

# Test installed version
cd apache-tomcat-*/
export JAVA_HOME=/path/to/jdk
./bin/catalina.sh run
```

#### Pre-Commit Hooks
```bash
#!/bin/bash
# .git/hooks/pre-commit

# Check for common issues
echo "Running pre-commit checks..."

# Verify code compiles
ant compile
if [ $? -ne 0 ]; then
    echo "Build failed, commit aborted"
    exit 1
fi

# Run fast tests
ant test -Dtest.name="**/*Test*.java" -Dtest.timeout=300
if [ $? -ne 0 ]; then
    echo "Tests failed, commit aborted"
    exit 1
fi

# Check for debugging code
if git diff --cached --name-only | xargs grep -l "System.out.println\|printStackTrace" 2>/dev/null; then
    echo "Found debugging statements, please remove before committing"
    exit 1
fi

echo "Pre-commit checks passed"
```

### Performance Development Guidelines

#### Profiling Setup
```bash
# Add profiling JVM options
export CATALINA_OPTS="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder"

# Enable JFR recording
export CATALINA_OPTS="$CATALINA_OPTS -XX:+StartFlightRecording=duration=60s,filename=tomcat-profile.jfr"

# Memory profiling
export CATALINA_OPTS="$CATALINA_OPTS -XX:+PrintGC -XX:+PrintGCDetails"
```

#### Performance Testing
```java
@Test
public void testPerformance() throws Exception {
    int iterations = 10000;
    long startTime = System.nanoTime();
    
    for (int i = 0; i < iterations; i++) {
        // Code under test
        myFeature.process(testData);
    }
    
    long endTime = System.nanoTime();
    long avgTimeNanos = (endTime - startTime) / iterations;
    double avgTimeMs = avgTimeNanos / 1_000_000.0;
    
    // Verify performance regression threshold
    assertTrue("Average processing time too slow: " + avgTimeMs + "ms", 
               avgTimeMs < 1.0); // 1ms threshold
    
    System.out.println("Average processing time: " + avgTimeMs + "ms");
}
```

### Debugging Workflow

#### Debug Configuration
```bash
# Remote debugging
export CATALINA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

# Connect IDE debugger to localhost:8000

# Heap dump analysis
jmap -dump:live,format=b,file=heap-$(date +%Y%m%d-%H%M%S).hprof <pid>

# Thread dump
jstack <pid> > threads-$(date +%Y%m%d-%H%M%S).txt
```

#### Common Debug Points
```java
// Container lifecycle debugging
@Override
protected void startInternal() throws LifecycleException {
    if (log.isDebugEnabled()) {
        log.debug("Starting " + this.getClass().getSimpleName());
    }
    
    setState(LifecycleState.STARTING);
    
    // Your code here
    
    setState(LifecycleState.STARTED);
}

// Request processing debugging
@Override
public void invoke(Request request, Response response) 
        throws IOException, ServletException {
    
    if (log.isDebugEnabled()) {
        log.debug("Processing request: " + request.getRequestURI());
    }
    
    long start = System.currentTimeMillis();
    try {
        getNext().invoke(request, response);
    } finally {
        if (log.isDebugEnabled()) {
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Request processing took: " + elapsed + "ms");
        }
    }
}
```

### Documentation Updates

#### AI Documentation Self-Improvement
The `/improve-yourself` command provides systematic documentation updates:

**Usage Scenarios**:
- After completing significant debugging sessions
- Following performance optimization work  
- When discovering new patterns or architectural insights
- After resolving environment-specific issues
- Periodically to capture accumulated session knowledge

**Process**:
1. **Session Analysis**: Reviews git history, changes, and conversation context
2. **Gap Identification**: Compares session learnings with existing documentation
3. **Systematic Updates**: Updates CLAUDE.md, memory cache, troubleshooting guides
4. **Cross-Reference Validation**: Ensures consistency across all documentation files

#### Manual Documentation Updates
When to Update Documentation:
1. **API Changes**: New methods, changed signatures
2. **Configuration Changes**: New attributes, changed defaults
3. **Behavior Changes**: Different functionality
4. **Performance Improvements**: Significant optimizations
5. **Bug Fixes**: If behavior changes
6. **Build Process Changes**: New commands, dependency updates
7. **Environment-Specific Solutions**: Platform or tooling quirks

#### Documentation Locations
```bash
# Javadoc - in source files
java/org/apache/catalina/**/*.java

# Configuration reference
webapps/docs/config/*.xml

# How-to guides
webapps/docs/*.xml

# Architecture documentation
docs/ai-assistant/architecture.md
docs/ai-assistant/domains/*.md
```

### Continuous Integration

#### Local CI Simulation
```bash
# Simulate CI build
ant clean download-compile compile test

# Test on different JVMs
export JAVA_HOME=/path/to/jdk17
ant clean test

export JAVA_HOME=/path/to/jdk21
ant clean test

# Cross-platform testing (if available)
# Test on Linux, Windows, macOS
```

### Best Practices Summary

#### Code Quality
1. **Follow existing patterns** in the codebase
2. **Write comprehensive tests** for new functionality
3. **Consider performance impact** of changes
4. **Maintain backward compatibility** unless explicitly breaking
5. **Document public APIs** thoroughly

#### Development Process
1. **Small, focused commits** with clear messages
2. **Regular testing** throughout development
3. **Code review** before merging
4. **Performance testing** for critical paths
5. **Documentation updates** for user-facing changes

#### Documentation Creation Process
1. **Use TodoWrite for complex documentation tasks** - Track progress across multiple files
2. **Leverage research-context-optimizer agent** for comprehensive domain analysis
3. **Follow consistent structure** - Purpose, Architecture, Components, Entry Points, etc.
4. **Research first, implement second** - Use agents for research, parent for implementation
5. **Cross-reference related domains** - Ensure domains link to each other appropriately
6. **Commit documentation in logical chunks** - Group related documentation updates

#### Collaboration
1. **Communicate early** about significant changes
2. **Ask questions** when unsure about design decisions
3. **Share knowledge** through code comments and documentation
4. **Review others' code** constructively
5. **Keep up with** project discussions and decisions

This workflow ensures high-quality contributions to the Apache Tomcat project while maintaining the codebase's stability and performance characteristics.