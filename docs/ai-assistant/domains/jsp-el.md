# JSP & Expression Language Domain

## Purpose
Handles JavaServer Pages compilation, execution, and Expression Language evaluation. This domain transforms JSP files into servlets, manages their lifecycle, processes tag libraries, and evaluates EL expressions at runtime to enable dynamic web content generation.

## Architecture Overview
```
JSP Compilation Pipeline:
JSP File → Parser → Validator → Generator → Compiler → Servlet Class
                ↓
          Node Tree (AST)
                ↓
          Java Source
                ↓
          Bytecode

Expression Language:
EL String → ELParser → AST Nodes → Evaluator → Result
                ↓
          Cached Expression
                ↓
          ELResolver Chain
```

## Key Components

### JSP Compiler (Jasper)
- **`JspServlet`** (`o.a.jasper.servlet`) - Main servlet handling JSP requests
- **`Compiler`** (`o.a.jasper.compiler`) - Orchestrates compilation pipeline
- **`Parser`** (`o.a.jasper.compiler`) - Parses JSP syntax to Node tree
- **`ParserController`** (`o.a.jasper.compiler`) - Two-pass parsing coordinator
- **`Generator`** (`o.a.jasper.compiler`) - Generates Java source from Nodes
- **`Validator`** (`o.a.jasper.compiler`) - Validates JSP correctness
- **`JDTCompiler`** (`o.a.jasper.compiler`) - Eclipse JDT-based Java compiler
- **`JspRuntimeContext`** (`o.a.jasper.compiler`) - Manages JSP lifecycle

### Expression Language
- **`ExpressionFactoryImpl`** (`o.a.el`) - Factory for creating EL expressions
- **`ExpressionBuilder`** (`o.a.el.lang`) - Builds and caches expression trees
- **`ELParser`** (`o.a.el.parser`) - JavaCC-generated EL parser
- **`EvaluationContext`** (`o.a.el.lang`) - Runtime evaluation context
- **`ELResolver`** (`jakarta.el`) - Chain of responsibility for variable resolution
- **`ELSupport`** (`o.a.el.lang`) - Type coercion utilities

### Tag Libraries
- **`TldScanner`** (`o.a.jasper.servlet`) - Discovers and parses TLD files
- **`TagFileProcessor`** (`o.a.jasper.compiler`) - Compiles tag files
- **`TagHandlerPool`** (`o.a.jasper.runtime`) - Pools tag handler instances
- **`TagLibraryInfo`** (`o.a.jasper.compiler`) - TLD metadata representation

### Runtime Support
- **`PageContextImpl`** (`o.a.jasper.runtime`) - JSP page context implementation
- **`JspFactoryImpl`** (`o.a.jasper.runtime`) - Creates PageContext instances
- **`HttpJspBase`** (`o.a.jasper.runtime`) - Base class for compiled JSPs
- **`JspServletWrapper`** (`o.a.jasper.servlet`) - Individual JSP lifecycle manager

## Entry Points

### Request Processing
1. **`JspServlet.service()`** - Receives JSP requests
2. **`JspServletWrapper.service()`** - Manages compilation/execution
3. **`HttpJspBase._jspService()`** - Generated service method

### Compilation Triggers
- First request to JSP (development mode)
- JSP file modification detected
- Dependent file changes (includes, tag files)
- Manual precompilation request

## Compilation Pipeline

### Phase 1: Parsing
```java
// Two-pass parsing for correct directive processing
ParserController controller = new ParserController();
// Pass 1: Parse directives only
controller.parse(jspFile);
// Pass 2: Full parsing with correct settings
Node.Nodes pageNodes = controller.parse(jspFile);
```

### Phase 2: Validation
```java
Validator validator = new Validator(compiler);
validator.validate();
// Checks:
// - Directive conflicts
// - Tag attribute requirements
// - Scripting variable declarations
```

### Phase 3: Code Generation
```java
Generator generator = new Generator(ctxt, pageNodes);
generator.generate();
// Produces:
// - Java source code
// - SMAP debugging info
// - Tag handler pool initialization
```

### Phase 4: Java Compilation
```java
JDTCompiler compiler = new JDTCompiler();
compiler.compile();
// Options:
// - In-memory compilation (default)
// - Ant compiler alternative
// - Custom classpath handling
```

## Expression Language Processing

### Expression Parsing
```java
// EL expressions are parsed and cached
ExpressionBuilder builder = new ExpressionBuilder(expression);
Node node = builder.build(); // Creates AST
// Cache stores up to 5000 expressions by default
```

### Evaluation Chain
```java
// ELResolver chain for variable resolution:
ImplicitObjectELResolver
  → MapELResolver
  → ResourceBundleELResolver  
  → ListELResolver
  → ArrayELResolver
  → BeanELResolver
  → ScopedAttributeELResolver
```

### Type Coercion
```java
// Automatic type conversion
ELSupport.coerceToType(value, targetType);
// Handles:
// - Primitives ↔ Wrappers
// - String → Number/Boolean
// - Enum conversions
```

## Tag Library Processing

### TLD Discovery
```java
// Startup scanning locations:
1. Platform tags (built-in)
2. web.xml <jsp-config> entries
3. /WEB-INF/*.tld
4. /WEB-INF/lib/*.jar!/META-INF/*.tld
5. Tag files in /WEB-INF/tags/
```

### Tag Handler Lifecycle
```java
// With pooling enabled:
Tag tag = pool.get(handlerClass);
try {
    tag.setPageContext(pageContext);
    tag.setParent(parent);
    // Set attributes...
    tag.doStartTag();
    tag.doEndTag();
} finally {
    pool.reuse(tag);
}
```

## Runtime Execution Model

### Generated Servlet Structure
```java
public class jsp_name extends HttpJspBase {
    public void _jspInit() { /* Initialization */ }
    public void _jspDestroy() { /* Cleanup */ }
    public void _jspService(HttpServletRequest request,
                           HttpServletResponse response) {
        PageContext pageContext = _jspxFactory.getPageContext(...);
        try {
            // Generated JSP content
        } finally {
            _jspxFactory.releasePageContext(pageContext);
        }
    }
}
```

### Dependency Tracking
```java
// JspRuntimeContext tracks dependencies
class JspServletWrapper {
    List<String> dependents; // Included files
    long lastModified;       // For change detection
    // Background thread checks every 4 seconds
}
```

## Performance Optimizations

### Compilation Caching
```java
// Avoid recompilation
if (wrapper.getLastModified() < jspFile.lastModified()) {
    wrapper.compile(); // Recompile needed
}
```

### Expression Caching
```java
// ExpressionBuilder maintains cache
private static final ConcurrentCache<String, Node> cache =
    new ConcurrentCache<>(5000); // Default size
```

### Tag Handler Pooling
```java
// Configure in web.xml
<init-param>
    <param-name>enablePooling</param-name>
    <param-value>true</param-value>
</init-param>
```

### Precompilation
```java
// Command-line precompilation
JspC jspc = new JspC();
jspc.setWebXmlFragment("/WEB-INF/generated_web.xml");
jspc.setCompile(true);
jspc.execute();
```

## Common Operations

### Configure JSP Servlet
```xml
<servlet>
    <servlet-name>jsp</servlet-name>
    <servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>
    <init-param>
        <param-name>development</param-name>
        <param-value>false</param-value>
    </init-param>
    <init-param>
        <param-name>checkInterval</param-name>
        <param-value>60</param-value>
    </init-param>
</servlet>
```

### Custom EL Functions
```xml
<!-- In TLD file -->
<function>
    <name>formatDate</name>
    <function-class>com.example.Functions</function-class>
    <function-signature>
        String format(java.util.Date, String)
    </function-signature>
</function>
```

### Custom Tag Development
```java
public class CustomTag extends SimpleTagSupport {
    public void doTag() throws JspException, IOException {
        JspWriter out = getJspContext().getOut();
        out.println("Custom output");
    }
}
```

## Dependencies

### Direct Dependencies
- **Servlet API** → Request/response handling
- **JavaCC** → EL parser generation
- **Eclipse JDT** → Java compilation
- **SMAP** → Source debugging support

### Integration Points
- **Web Application Deployment** → JSP discovery
- **Request Processing** → Servlet invocation
- **Security** → JSP access control
- **Session Management** → Session scope in EL

## Testing Strategies

### Unit Tests
```java
// Test EL evaluation
ExpressionFactory factory = new ExpressionFactoryImpl();
ValueExpression ve = factory.createValueExpression(
    context, "${1 + 2}", Integer.class);
assertEquals(3, ve.getValue(context));
```

### Integration Tests
```java
// Test JSP compilation
Tomcat tomcat = getTomcatInstance();
Context ctx = tomcat.addContext("", docBase);
Tomcat.addServlet(ctx, "jsp", new JspServlet());
ctx.addServletMappingDecoded("*.jsp", "jsp");
// Request JSP and verify output
```

### Performance Tests
- Measure JSP compilation time
- Benchmark EL expression evaluation
- Test tag handler pooling effectiveness
- Profile memory usage during compilation

## Common Issues & Solutions

### Issue: Slow First Request
- **Solution**: Enable JSP precompilation
- Use `jsp_precompile` parameter
- Deploy precompiled JSPs

### Issue: Memory Leaks with Tag Pools
- **Solution**: Configure appropriate pool size
- Monitor pool usage via JMX
- Consider disabling pooling for rarely-used tags

### Issue: EL Not Evaluating
- **Solution**: Check `isELIgnored` setting
- Verify web.xml version (2.4+ enables EL by default)
- Check for escaped expressions (`\${...}`)

### Issue: JSP Compilation Errors
- **Solution**: Check Java compiler settings
- Verify classpath includes required libraries
- Enable development mode for detailed errors

## Domain Expert Knowledge

### Design Patterns
- **Visitor Pattern** - Node tree traversal in Generator
- **Factory Pattern** - ExpressionFactory, JspFactory
- **Object Pool** - Tag handler and PageContext pooling
- **Template Method** - HttpJspBase service method

### Best Practices
1. Disable development mode in production
2. Use JSTL instead of scriptlets
3. Precompile JSPs for production deployment
4. Configure appropriate checkInterval
5. Use tag files for reusable components

### Performance Tuning
```properties
# Optimal production settings
development=false
checkInterval=0
enablePooling=true
genStringAsCharArray=true
trimSpaces=true
suppressSmap=true
```

### Security Considerations
- Escape user input in EL expressions
- Disable scriptlets when possible
- Validate TLD files from untrusted sources
- Use CSP headers to prevent XSS
- Configure secure EL evaluation

## Related Documentation
- [Web Application Deployment](webapp-deployment.md) - JSP discovery and loading
- [Request Processing](request-processing.md) - JSP request routing
- [Security Domain](security.md) - JSP access control
- [Configuration Domain](configuration.md) - JSP servlet configuration