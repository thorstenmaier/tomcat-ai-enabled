# The AI-First Codebase: Apache Tomcat as Living Laboratory

## Vision: The Future of Software Development

Software development is evolving. Large codebases with millions of lines have traditionally been difficult to navigate, understand, and maintain. Architectural decisions often drift from implementation reality, documentation becomes outdated, and critical knowledge remains trapped in the minds of senior developers.

This repository explores a practical solution: integrating AI agents directly into the development workflow to bridge these gaps.

The approach demonstrated here shows how AI agents can work alongside developers to make large codebases more accessible and maintainable. Rather than replacing developers, these agents serve as intelligent assistants that help navigate complexity, maintain documentation, and ensure architectural consistency. The result is a codebase that remains comprehensible and maintainable even as it grows in size and complexity.

The key insight: **Context is everything, but context is limited.**

Modern AI agents can process and understand code with remarkable sophistication, but they face a fundamental constraint—their context window. Even the most advanced models can only hold about 1% of a large codebase like Apache Tomcat in their working memory. This repository demonstrates how to bridge that gap through intelligent documentation, dynamic context management, and collaborative agent workflows.

## The Central Paradigm: AI as Primary Documentation Audience

Traditional documentation targets humans and always disappoints someone. We've inverted this approach: **optimize documentation for AI comprehension first**. This paradigm shift unlocks two transformative capabilities:

### 1. Enhanced Development Through Deep Understanding

When AI agents truly understand the system through comprehensive documentation, they become powerful development partners:

- **Pattern-Aware Code Generation**: The AI writes code that follows existing patterns, uses established utilities, and maintains architectural consistency
- **Context-Aware Refactoring**: Changes consider ripple effects across components, maintaining system integrity
- **Intelligent Problem Solving**: The AI can reason about complex interactions, suggest optimal solutions, and prevent common pitfalls
- **Continuous Learning**: Every development session enriches the documentation, creating a virtuous cycle of improvement

### 2. Dynamic Documentation for Every Audience

With deep system understanding, the AI can instantly generate perfectly tailored documentation:

- **For Developers**: "The RequestFilterValve follows the interceptor pattern. Similar implementation in AuthenticatorValve.java:234"
- **For Architects**: "Request processing uses chain-of-responsibility with hierarchical container delegation"
- **For Auditors**: "Input validation occurs at transport (NioEndpoint), protocol (Http11Processor), and application layers"
- **For Executives**: "Tomcat processes web requests securely and efficiently, handling 10,000+ concurrent users"

This isn't translation—it's dynamic generation from deep understanding.

## What This Repository Represents

This is Apache Tomcat 12.0—but transformed. It's not just a web server; it's a **proof of concept for AI-First software development**. With ~726,745 lines of code across 2,732 Java files, Tomcat represents the complexity of real-world enterprise software. We've augmented it with AI-enabling infrastructure that demonstrates how brownfield projects can evolve into AI-collaborative environments.

### The Transformation Journey

Starting with a traditional codebase, we've added layers of AI-enabling infrastructure:
1. **Structured documentation** that AI agents can navigate intelligently
2. **Context management strategies** that work within token limits
3. **Collaborative workflows** between specialized sub-agents
4. **Self-improvement mechanisms** that capture and codify learning
5. **Environment-specific adaptations** for real-world development scenarios

## Core Concepts and Technologies

### 1. The Primary Interface: CLAUDE.md

**The Challenge**: AI agents entering a 726,745-line codebase are like tourists dropped in Tokyo without a map—overwhelmed and lost. Traditional READMEs are written for humans who can infer context, but AI agents need explicit navigation instructions and operational knowledge to be effective.

**The Solution**: We created CLAUDE.md as the single source of truth for AI agents—a carefully crafted entry point that provides just enough context to navigate the entire codebase without overwhelming the limited context window. This file acts as the AI's operational manual, teaching it not just what the code does, but how to work with it effectively.

`CLAUDE.md` serves as this primary interface, containing:

- **Essential commands** for building, testing, and running the application
- **Architecture overview** with component hierarchies and execution flows
- **Environment-specific configurations** (like macOS SDKMAN paths)
- **Dynamic loading instructions** for pulling in additional documentation as needed
- **Self-improvement protocols** for continuous documentation enhancement

This file is intentionally kept under 10% of the context window, leaving room for actual work while providing essential navigation.

### 2. Dynamic Context Loading Architecture

**The Challenge**: AI context windows are precious real estate—even Claude's 200K token window can only hold about 1% of Tomcat's codebase. Loading everything upfront wastes this limited resource on information that might never be needed, leaving no room for actual work.

**The Solution**: We architected a hierarchical documentation system that loads on demand. Think of it as lazy loading for AI knowledge—the agent starts with high-level navigation and progressively loads deeper documentation only when needed. This maximizes the working memory available for the actual task at hand.

The breakthrough insight: **Not all documentation needs to be loaded at once.**

```
docs/ai-assistant/
├── architecture.md           # Loaded for system design questions
├── domains/                  # 10 specialized domains, loaded on demand
│   ├── request-processing.md
│   ├── security.md
│   └── ...
├── services/                 # Component interactions
├── patterns.md              # Coding conventions
├── indexes/                 # API and class hierarchies
└── semantic/                # Conceptual navigation maps
```

Each file is designed to be:
- **Self-contained**: Can be understood without requiring other files
- **Cross-referenced**: Points to related documentation
- **Token-optimized**: Provides maximum value within size constraints

### 3. Sub-Agent Orchestration

**The Challenge**: Complex tasks like "analyze all security vulnerabilities" or "refactor the authentication system" require examining hundreds of files—far exceeding what a single AI context can handle. Traditional approaches either fail or produce shallow, incomplete results.

**The Solution**: We implemented a multi-agent architecture where specialized sub-agents handle focused research tasks, saving their findings to files that the main agent can read. It's like having a research team where each member investigates a specific aspect and reports back with synthesized findings. This allows us to tackle enterprise-scale problems that would otherwise be impossible.

Complex tasks exceed single context windows. Our solution: **Specialized sub-agents with focused responsibilities.**

#### Available Agents (`.claude/agents/`)
- **research-context-optimizer**: Performs deep research, saves findings to files
- **test-orchestrator**: Intelligently selects minimal test suites
- **dependency-analyzer**: Maps component relationships
- **code-validator**: Ensures code quality and security compliance

#### The Collaboration Pattern
```
1. Parent agent creates context: docs/claude/tasks/context-session.md
2. Sub-agent performs focused research
3. Results saved to: docs/claude/research/[type]-[timestamp].md
4. Parent reads report and implements with full context
5. Knowledge persists across sessions
```

This pattern prevents context overflow while maintaining continuity.

### 4. Persistent Knowledge Management

**The Challenge**: Every AI session starts from zero. The agent might solve the same problem differently each time, forget project-specific conventions, or lose valuable insights discovered in previous sessions. This wastes time and creates inconsistency.

**The Solution**: We built a persistent knowledge layer where every interaction contributes to a growing knowledge base. Research findings, discovered patterns, and project decisions are saved as markdown files that future sessions can reference. The AI literally learns from its past experiences, getting smarter with every interaction. This transforms one-off AI assistance into a continuously improving development partner.

#### Knowledge Layers
- **CLAUDE.md**: Core operational knowledge
- **Domain documentation**: Deep technical understanding
- **Research reports**: Specific investigation results
- **Session contexts**: Current project state

#### The Self-Improvement Protocol

The `/improve-yourself` command implements systematic learning:
```bash
# After a complex debugging session
/improve-yourself

# The agent will:
1. Analyze session activities
2. Extract new patterns and solutions
3. Update relevant documentation
4. Commit improvements to the repository
```

This ensures the AI assistant gets smarter with every interaction.

### 5. MCP Server Integration

**The Challenge**: Documentation, API references, and best practices are scattered across the internet. Loading all potentially useful information into the context window is impossible, yet the AI needs access to authoritative sources to make informed decisions.

**The Solution**: We integrated Model Context Protocol (MCP) servers that provide on-demand access to external knowledge sources. When the AI needs specific Tomcat documentation or API details, it queries the MCP server and receives just the relevant information. This extends the AI's knowledge reach without consuming precious context tokens—like having a library card instead of carrying all the books.

- **Context7 Server**: Access to 31,279 Tomcat documentation snippets
- **Library ID**: `/websites/tomcat_apache_tomcat-10_1-doc`
- **Usage**: Query-based retrieval of specific documentation

This provides authoritative information without storing it locally.

### 6. Custom Commands for Standardization

**The Challenge**: Every developer (and AI agent) might approach common tasks differently—starting Tomcat, running tests, or updating documentation. This creates inconsistency, repeated mistakes, and wasted time figuring out project-specific workflows.

**The Solution**: We created custom commands that encapsulate complex, project-specific workflows into simple, memorable commands. These aren't just aliases—they include error handling, environment detection, and best practices baked in. When an AI agent runs `/start-tomcat`, it automatically handles JAVA_HOME detection, port conflicts, and platform differences. This ensures every team member—human or AI—follows the same proven workflows.

#### Available Commands (`.claude/commands/`)
- **/improve-yourself**: Captures session learning into documentation
- **/start-tomcat**: Handles environment detection, port conflicts, Java configuration
- **/sync-comments**: Synchronizes inline comments with implementation before generating meta documentation

These commands encapsulate complex workflows that would otherwise require manual documentation lookup.

### 7. Foundation-First Documentation Integrity

**The Challenge**: Documentation hierarchies are often built top-down—starting with high-level architecture diagrams and working down to implementation details. This creates a critical problem: meta documentation (architectural overviews, system diagrams, API summaries) becomes disconnected from the actual code it describes. When inline comments are outdated or missing, any meta documentation built on top of them inherits and amplifies these inaccuracies. The result is a documentation pyramid built on sand.

**The Solution**: We enforce a **bottom-up documentation discipline** where truth flows from implementation to abstraction, never the reverse. The `/sync-comments` command embodies this principle by ensuring inline documentation accurately reflects actual code behavior before allowing any meta documentation generation.

#### The Documentation Pyramid

```
┌─────────────────────────────────────────┐
│   Architecture Documentation           │  ← Generated from verified facts
│   (System diagrams, component maps)    │
├─────────────────────────────────────────┤
│   Domain Documentation                  │  ← Synthesized from class summaries
│   (Functional area overviews)          │
├─────────────────────────────────────────┤
│   Class/API Documentation               │  ← Derived from inline comments
│   (Public interfaces, usage guides)    │
├─────────────────────────────────────────┤
│   Inline Comments & Javadoc            │  ← Must match implementation
│   (Method docs, field descriptions)    │
└─────────────────────────────────────────┘
         ↑
    CODE (Source of Truth)
```

**The Principle**: Code is reality. Comments interpret reality. Meta documentation synthesizes interpretations. If the interpretation is wrong, everything above it is wrong.

#### How Foundation-First Works in Practice

**Traditional (Broken) Workflow:**
```
1. Developer changes implementation
2. Forgets to update inline comments
3. Architect updates high-level docs based on outdated comments
4. Documentation pyramid collapses into fiction
```

**Foundation-First (Correct) Workflow:**
```
1. Developer changes implementation
2. /sync-comments verifies inline comments match code
3. Inline comments updated to reflect actual behavior
4. Meta documentation generated from verified inline comments
5. Documentation pyramid stands on solid foundation
```

#### The Truth Propagation Chain

Every layer of documentation must derive its truth from the layer below:

1. **Implementation Layer** (Ground Truth)
   - What the code actually does
   - Verifiable through execution and testing
   - The only source of absolute truth

2. **Inline Documentation Layer** (Direct Interpretation)
   - `/sync-comments` ensures comments match implementation
   - Describes behavior, parameters, exceptions, side effects
   - Must be validated before use in meta documentation

3. **Class/API Layer** (Synthesis)
   - Summarizes inline documentation across methods
   - Explains public contracts and usage patterns
   - Generated only after inline docs are verified

4. **Domain/Architecture Layer** (Abstraction)
   - Synthesizes patterns across multiple classes
   - Documents component interactions and design decisions
   - Built from verified class-level documentation

#### Why This Matters

**Scenario: Outdated Inline Comments Lead to Architectural Misconceptions**

```java
// Inline comment says:
/**
 * Processes requests synchronously in the calling thread.
 * Thread-safe due to synchronized access.
 */
public void processRequest(Request req) {
    // But actual implementation:
    executorService.submit(() -> asyncProcessor.handle(req));
}
```

If meta documentation is generated from this outdated comment:
- Architecture docs claim synchronous processing
- Performance analysis assumes blocking behavior
- Thread pool sizing recommendations are wrong
- Security model assumes synchronous call chains

**After /sync-comments:**
```java
/**
 * Processes requests asynchronously using the shared executor service.
 *
 * Submissions are non-blocking; actual processing occurs in worker threads
 * from the configured thread pool. Caller receives immediate return.
 * Thread-safety is ensured through the executor's internal synchronization.
 *
 * @param req the request to process asynchronously
 * @see #executorService configured via setExecutorService()
 */
public void processRequest(Request req) {
    executorService.submit(() -> asyncProcessor.handle(req));
}
```

Now meta documentation correctly describes:
- Asynchronous architecture
- Non-blocking request handling
- Thread pool resource management
- Accurate performance characteristics

#### Integration with Development Workflow

The `/sync-comments` command enforces foundation-first discipline at critical points:

**Before Pull Requests:**
```bash
# Ensure all changed classes have accurate inline documentation
/sync-comments java/org/apache/catalina/core/ChangedClass.java
# Only then generate PR description from verified documentation
```

**Before Meta Documentation Updates:**
```bash
# Step 1: Verify foundation
/sync-comments java/org/apache/catalina/connector/Connector.java --generate-meta
# Step 2: Meta docs are now generated from truth
# Step 3: Update architectural overviews if needed
```

**During Refactoring:**
```bash
# After implementation changes
/sync-comments path/to/refactored/Class.java
# Ensures documentation reflects new reality
# Prevents documentation debt accumulation
```

#### The Compound Effect

When every layer of documentation is built on verified truth from the layer below:

- **Accuracy Compounds**: Each layer inherits correctness from its foundation
- **Trust Increases**: Developers and AI agents rely on documentation with confidence
- **Maintenance Simplifies**: Fixing documentation means fixing the lowest incorrect layer
- **AI Effectiveness Multiplies**: AI agents working from accurate documentation make better decisions

#### The Anti-Pattern: Top-Down Documentation Decay

Without foundation-first discipline:
```
Time 0:  All documentation accurate ✓
Month 1: Inline comments drift (10% outdated)
Month 3: API docs inherit inaccuracies (30% wrong)
Month 6: Architecture docs describe fiction (60% misleading)
Year 1:  Documentation is actively harmful ✗
```

With foundation-first discipline:
```
Every Change: /sync-comments enforces accuracy
Every Layer:   Built on verified foundation
Every Session: AI agents trust documentation
Every Year:    Documentation remains useful
```

#### Foundation-First as Cultural Practice

This isn't just a technical practice—it's a cultural commitment to **truth over convenience**:

- **Code is truth**: When documentation conflicts with implementation, code wins
- **Verify before synthesize**: Never build abstractions on unverified facts
- **Bottom-up validation**: Always check foundations before constructing higher layers
- **Continuous reconciliation**: Documentation drift is technical debt that compounds

The `/sync-comments` command automates this cultural practice, making foundation-first documentation the path of least resistance rather than a heroic discipline.

## The Enterprise Architecture Revolution

### Solving the Architecture-Implementation Gap

For decades, Enterprise Architects have faced a fundamental problem: **Architecture documentation and implementation inevitably drift apart.** PowerPoint slides age, Confluence pages become outdated, and architectural decisions made in ivory towers fail to reflect the reality of the codebase. Meanwhile, developers make pragmatic decisions that never bubble up to architectural documentation.

This repository demonstrates a radical solution: **Architecture as Code, Living in Code.**

#### Traditional Problems (Before)
- **Documentation Drift**: Architecture documents live separately from code, becoming stale within months
- **Ivory Tower Syndrome**: Architects work disconnected from implementation realities
- **Lost Rationale**: Architectural decisions lack context, their reasoning forgotten
- **Compliance Theater**: Teams scramble before audits to update documentation
- **Knowledge Silos**: Architectural knowledge trapped in specific individuals
- **Pattern Violations**: No automated enforcement of architectural patterns
- **Impact Blindness**: Changes happen without understanding architectural implications

#### The AI-First Solution (Now)
- **Single Source of Truth**: Architecture documentation lives in the repository, versioned with code
- **Continuous Validation**: Every commit validates against architectural constraints
- **Living Documentation**: AI agents update architecture docs with every structural change
- **Rationale Preservation**: Architectural Decision Records (ADRs) linked to actual implementations
- **Automated Compliance**: Governance rules encoded and continuously checked
- **Democratic Knowledge**: Every developer has access to complete architectural context
- **Guided Evolution**: AI ensures changes align with architectural vision

### Architecture Governance Through AI

The AI agent becomes the guardian of architectural integrity:

```markdown
Developer: "I need to add a new caching layer"
AI Agent: *Loads architecture.md*
         *Checks existing caching patterns*
         *Validates against architectural principles*
         "Based on our architecture, caching should be implemented using
          the existing CacheManager pattern in resource-management domain.
          This maintains our single-responsibility principle and prevents
          cache proliferation. Here's how to integrate it..."
```

This isn't just documentation—it's **active architectural governance**.

### Documentation as Living Architecture

The AI-First documentation approach fundamentally changes how architecture is communicated:

**Traditional Architecture Documentation:**
- Static diagrams that become obsolete
- Text descriptions that nobody reads
- Decisions without implementation context
- Generic examples that don't match reality

**AI-Generated Architecture Views:**
- Real-time generation from actual code structure
- Tailored to stakeholder's concern and expertise level
- Decisions linked to implementing code
- Examples extracted from actual implementations

```markdown
# For a CTO evaluating architectural fitness
AI: "Current architecture health metrics:
    - Coupling: Average 3.2 components per dependency (healthy)
    - Cohesion: 87% of changes touch single module (excellent)
    - Tech debt: 14% of code marked for refactoring (manageable)
    - Pattern compliance: 94% follows documented patterns (strong)

    Risk areas requiring attention:
    - Session management approaching scalability limits at 10K concurrent
    - Legacy AJP connector has 47 known CVEs, migration recommended
    - WebSocket implementation lacks proper backpressure handling"

# For a Developer implementing a new feature
AI: "Based on existing patterns in the codebase:
    - Similar feature implemented in: RequestFilterValve.java:234
    - Follow the Valve pattern for request interception
    - Register in context.xml or programmatically via Pipeline
    - Test pattern: TestRequestFilterValve.java shows approach
    - Performance consideration: Valve adds ~0.1ms per request"
```

### The Architect's New Toolkit

#### 1. Architectural Decision Records (ADRs) with Code Links
```markdown
# ADR-001: Asynchronous Processing Architecture
Decision: Use thread pools for async operations
Rationale: Predictable resource usage, easier monitoring
Implementation: java/org/apache/catalina/core/AsyncContextImpl.java:45-89
Validation: ant test -Dtest.name="**/TestAsync*.java"
```

#### 2. Architecture Fitness Functions
Built into the AI agent's validation:
- Component coupling metrics
- Dependency direction validation
- Layer violation detection
- Pattern compliance checking
- Performance regression alerts

#### 3. Evolutionary Architecture Support
The AI agent enables:
- **Incremental Migration**: Guided refactoring maintaining working system
- **Parallel Architectures**: Supporting old and new patterns during transition
- **Automated Impact Analysis**: Understanding ripple effects before changes
- **Risk Assessment**: Identifying architectural debt and vulnerabilities

### Real-World Architecture Scenarios

#### Scenario 1: Microservices Extraction
```bash
Developer: "We need to extract the authentication module as a microservice"

AI Agent:
1. Analyzes all dependencies on authentication components
2. Creates detailed extraction plan preserving interfaces
3. Identifies required configuration changes
4. Generates migration scripts maintaining backward compatibility
5. Updates architecture documentation with new boundaries
6. Creates new ADR documenting the decision and approach
```

#### Scenario 2: Security Architecture Enforcement
```bash
Developer: "Adding new endpoint for user data access"

AI Agent:
1. Checks security architecture policies
2. Ensures authentication/authorization patterns
3. Validates input sanitization requirements
4. Confirms audit logging implementation
5. Updates security domain documentation
6. Alerts if GDPR/compliance implications
```

#### Scenario 3: Performance Architecture Optimization
```bash
Developer: "Users report slow response times"

AI Agent:
1. Loads performance architecture documentation
2. Identifies architectural hotspots from patterns
3. Suggests architecturally-aligned optimizations
4. Validates changes don't violate design principles
5. Documents performance improvements in architecture
```

### The Enterprise Architecture Lifecycle

With AI-First development, the architecture lifecycle transforms:

#### Planning Phase
- AI agents analyze existing architecture before changes
- Architectural impacts assessed automatically
- Trade-offs documented and linked to decisions

#### Implementation Phase
- Every line of code validated against architectural constraints
- Patterns enforced automatically
- Deviations require explicit architectural decision records

#### Operation Phase
- Architecture documentation always reflects production reality
- Performance metrics validate architectural assumptions
- Incidents linked back to architectural decisions

#### Evolution Phase
- Technical debt tracked at architectural level
- Refactoring guided by architectural principles
- Modernization paths evaluated against current state

### Metrics for Architectural Health

The AI agent continuously monitors:

1. **Architecture-Code Alignment Score**: How well code matches documented architecture
2. **Pattern Consistency Index**: Percentage of code following established patterns
3. **Documentation Freshness**: Time since last architecture update vs code changes
4. **Dependency Cleanliness**: Violations of architectural layer boundaries
5. **Technical Debt Trajectory**: Architectural debt accumulation/reduction rate
6. **Compliance Coverage**: Percentage of governance rules automatically validated

### Enterprise Integration Patterns

The AI-First approach integrates with enterprise processes:

#### Architecture Review Boards (ARB)
- AI pre-validates submissions against architectural standards
- Generates impact analysis for review
- Documents decisions directly in code repository
- Tracks implementation of ARB decisions

#### Compliance and Audit
- Continuous compliance checking vs point-in-time audits
- Audit trails generated from git history
- Architecture documentation always audit-ready
- Automated evidence collection for compliance

#### Technology Radar Integration
- AI agent aware of approved/deprecated technologies
- Suggests migrations when using deprecated patterns
- Prevents introduction of unapproved technologies
- Documents exceptions with proper justification

## Context Management: The Critical Challenge

### The 1% Problem

With only ~1% of the codebase fitting in context, we must be strategic:

1. **Hierarchical Documentation**: Start high-level, drill down as needed
2. **Lazy Loading**: Only load what's immediately relevant
3. **Context Recycling**: Use sub-agents for research, keep results in files
4. **Semantic Navigation**: Use concept maps rather than file listings
5. **Pattern Recognition**: Document patterns once, reference everywhere

### Context Management Strategies

#### For Large Tasks
```
Main Agent (Context: Navigation + Current Task)
    ├── Research Agent (Context: Deep file exploration)
    │   └── Saves to: docs/claude/research/
    ├── Test Agent (Context: Test framework + specific tests)
    │   └── Returns: Minimal test suite
    └── Validation Agent (Context: Style guides + security rules)
        └── Returns: Compliance report
```

#### For Ongoing Work
- **Session files**: Maintain state in `docs/claude/tasks/`
- **Incremental loading**: Add context as needed, remove when done
- **Summary documents**: Condense findings into actionable insights

## Development Workflow: Human-AI Collaboration

### The New Development Cycle

1. **Task Definition** → Human describes the goal
2. **Research Phase** → AI agent explores codebase, potentially using sub-agents
3. **Planning Phase** → AI presents approach, human reviews
4. **Implementation** → AI writes code following discovered patterns
5. **Validation** → Automated testing and code quality checks
6. **Documentation Update** → AI updates relevant documentation
7. **Review** → Human reviews both code and documentation changes
8. **Learning Capture** → Self-improvement protocol preserves insights

### Quality Assurance in AI-Assisted Development

**Every change requires:**
- Code review by human developers
- Documentation review for accuracy
- Automated test execution
- Pattern compliance validation
- Architecture alignment check

The AI agent ensures these checks through:
```bash
# Built into CLAUDE.md
ant test                    # Run tests
ant validate               # Check code style
/improve-yourself          # Update documentation
```

### Team Collaboration Patterns

**For Simple Tasks:**
- Direct interaction with AI agent
- Immediate implementation
- Documentation auto-updates

**For Complex Features:**
- Initial research with sub-agents
- Research reports in `docs/claude/research/`
- Collaborative planning session
- Incremental implementation with reviews

**For Architecture Changes:**
- Load `docs/ai-assistant/architecture.md`
- Propose changes with impact analysis
- Update architecture documentation
- Implement with continuous validation

## Technical Implementation Details

### Repository Structure
```
tomcat/
├── CLAUDE.md                    # Primary AI interface
├── .claude/
│   ├── agents/                  # Sub-agent configurations
│   └── commands/                # Custom command definitions
├── docs/
│   ├── ai-assistant/           # Structured AI documentation
│   │   ├── architecture.md    # System design
│   │   ├── domains/           # 10 functional domains
│   │   ├── services/          # Component catalog
│   │   ├── patterns.md        # Coding patterns
│   │   └── semantic/          # Navigation maps
│   └── claude/                # Dynamic working directory
│       ├── tasks/             # Current session context
│       └── research/          # Research reports
├── java/                      # Tomcat source code
├── test/                      # Test suites
└── webapps/                   # Sample applications
```

### Key Statistics
- **Codebase Size**: ~726,745 lines (580,174 main + 146,571 tests)
- **Java Files**: 2,732
- **AI Documentation**: 22+ specialized markdown files
- **Context Window Usage**: ~10% for base documentation
- **Available Context**: ~90% for actual work
- **Documentation Coverage**: 10 core domains fully documented

### Integration Points

1. **IDE Integration**: AI agents work alongside traditional IDEs
2. **Version Control**: All AI documentation is versioned with code
3. **CI/CD Pipeline**: AI validations integrated into build process
4. **Code Review**: AI-generated changes follow standard review process
5. **Documentation System**: AI updates linked to code changes

## Proven Patterns and Best Practices

### Pattern 1: Research-First Development
```
1. Research existing patterns using grep/glob
2. Document findings in research report
3. Implement following discovered patterns
4. Validate against existing tests
```

### Pattern 2: Context-Preserving Refactoring
```
1. Create session context file
2. Analyze dependencies with sub-agent
3. Implement changes incrementally
4. Update context file after each step
```

### Pattern 3: Test-Driven AI Development
```
1. AI writes tests based on requirements
2. AI implements code to pass tests
3. AI validates against broader test suite
4. AI documents new patterns discovered
```

### Pattern 4: Documentation-Code Synchronization
```
1. Every code change triggers documentation review
2. Architecture changes update architecture.md
3. New patterns added to patterns.md
4. Search hints updated for new components
```

## Getting Started: For Developers

### Prerequisites
- Java Development Kit (JDK) 17 or later
- Apache Ant 1.10.15 or later
- AI Agent access (Claude Code recommended)

### Quick Start
```bash
# 1. Build the project
ant deploy

# 2. For macOS with SDKMAN
export JAVA_HOME=~/.sdkman/candidates/java/current

# 3. Start Tomcat
./output/build/bin/catalina.sh run

# 4. Access at http://localhost:8080
```

### For AI Agents
1. **Read CLAUDE.md** for project overview
2. **Check git status** to understand current state
3. **Load relevant domain docs** as needed
4. **Use sub-agents** for complex research
5. **Run /improve-yourself** after significant work

### For Human Developers
1. **Describe tasks clearly** to the AI agent
2. **Review AI-generated research** before implementation
3. **Validate AI code changes** against requirements
4. **Ensure documentation updates** are accurate
5. **Contribute patterns** back to the documentation

## The Evolution Path: From Traditional to AI-First

### Stage 1: Documentation Enhancement (Current)
- Comprehensive documentation for AI navigation
- Basic sub-agent workflows
- Manual self-improvement triggers

### Stage 2: Automated Learning (Next)
- Automatic pattern detection
- Continuous documentation updates
- Proactive suggestion system

### Stage 3: Full AI Integration (Future)
- AI-driven architecture evolution
- Automated refactoring campaigns
- Self-optimizing codebase structure

## Claude Code Best Practices Implementation

This project follows [Anthropic's Claude Code best practices](https://www.anthropic.com/engineering/claude-code-best-practices) with the following implementations:

### ✅ Implemented Best Practices

1. **CLAUDE.md Documentation**
   - Comprehensive project overview and commands
   - Build instructions and troubleshooting
   - Architecture documentation
   - Self-improvement protocols

2. **Custom Commands** (`.claude/commands/`)
   - `/improve-yourself` - Captures session learnings
   - `/start-tomcat` - Environment-aware startup
   - `/explore-plan-code-commit` - Structured development workflow
   - `/tdd-workflow` - Test-driven development cycle
   - `/sync-comments` - Synchronizes inline comments with implementation

3. **Multi-Agent Architecture**
   - Research context optimizer for deep analysis
   - Test orchestrator for intelligent test selection
   - Dependency analyzer for impact assessment
   - Code validator for quality assurance

4. **MCP Server Integration**
   - `.mcp.json` configuration for Context7 documentation access
   - 31,279+ Tomcat documentation snippets available
   - Query-based retrieval without context pollution

5. **Context Management Strategies**
   - Dynamic documentation loading
   - Hierarchical documentation structure
   - Session persistence in `docs/claude/`
   - Research reports for context recycling

### 🚀 Advanced Workflows

#### Explore-Plan-Code-Commit Workflow
```bash
/explore-plan-code-commit "Implement new feature"
# 1. EXPLORE: Research existing patterns
# 2. PLAN: Design implementation approach  
# 3. CODE: Implement following patterns
# 4. COMMIT: Validate and document changes
```

#### Test-Driven Development
```bash
/tdd-workflow "Add new functionality"
# RED → GREEN → REFACTOR cycle
```

### 💡 Context Optimization Tips

1. **Use `/clear` regularly** to reset context window
2. **Multiple CLAUDE.md files** can be placed in subdirectories for specialized contexts
3. **Git worktrees** for parallel task management:
   ```bash
   git worktree add ../tomcat-feature-x feature-branch
   ```
4. **Thinking modes** for complex problems:
   - Normal: Standard processing
   - "think": Deeper analysis
   - "think hard": Complex problem solving
   - "ultrathink": Maximum reasoning depth

### 🔄 Context Reset Strategies

When context becomes cluttered:
1. Save current findings to `docs/claude/research/`
2. Use `/clear` to reset
3. Reload only essential documentation
4. Reference saved research reports

### 📊 Visual Iteration
For UI/UX work, use screenshots:
- Provide visual references for desired outcomes
- Iterate on implementations with visual feedback
- Document visual requirements in issues

## Contributing to AI-First Development

This repository is a living experiment. Contributions welcome in:

1. **Documentation Patterns**: New ways to structure AI-readable docs
2. **Agent Workflows**: Improved sub-agent collaboration patterns
3. **Context Optimization**: Better token usage strategies
4. **Tool Integration**: MCP servers, IDE plugins, CI/CD hooks
5. **Learning Mechanisms**: Enhanced self-improvement protocols

## Metrics and Success Indicators

### Quantitative Metrics
- **Context Efficiency**: <10% base documentation overhead
- **Navigation Speed**: Find any component in <3 searches
- **Pattern Coverage**: 95% of code follows documented patterns
- **Test Coverage**: Maintained or improved with AI changes

### Qualitative Indicators
- Developers spend more time on creative problem-solving
- Onboarding time reduced from weeks to days
- Architecture and implementation stay synchronized
- Tribal knowledge becomes explicit documentation

## Conclusion: A Practical Approach to AI-Assisted Development

This repository demonstrates how AI agents can be integrated into existing software development workflows to address longstanding challenges. By working within the constraints of limited context windows, we've developed practical patterns for making large codebases more manageable and maintainable.

The techniques presented here—dynamic context loading, sub-agent orchestration, persistent knowledge management, and self-improvement protocols—offer a reproducible approach for enhancing any large codebase with AI assistance. These patterns help maintain the critical connection between architecture and implementation while making complex systems more accessible to all team members.

Most importantly, this project embodies a fundamental paradigm shift in documentation: **AI agents as the primary audience**. When we optimize documentation for AI comprehension rather than human consumption, we unlock the ability to dynamically generate perfectly tailored documentation for any audience—from curious children to enterprise architects, from security auditors to performance engineers. This isn't just about better documentation; it's about making complex systems truly accessible to everyone who needs to understand them, in exactly the way they need to understand them.

The future of software development isn't about choosing between human developers and AI agents—it's about creating systems where both can thrive together, each leveraging their unique strengths. This repository shows one practical path toward that future.

This is not a complete solution but rather a working example of how traditional software development can evolve to incorporate AI assistance effectively.

## License and Attribution

- **Apache Tomcat Source**: [Apache License version 2](https://www.apache.org/licenses/)
- **AI-Enablement Layer**: Experimental additions for AI-First development research
- **Original Project**: [Apache Software Foundation](https://tomcat.apache.org/)

Apache Tomcat, Tomcat, Apache, the Apache feather, and the Apache Tomcat project logo are trademarks of the Apache Software Foundation.

## Further Resources

- **Original Tomcat Documentation**: [tomcat.apache.org](https://tomcat.apache.org/)
- **CONTRIBUTING Guide**: [CONTRIBUTING.md](CONTRIBUTING.md)
- **Running Instructions**: [RUNNING.txt](RUNNING.txt)
- **AI Assistant Guide**: [CLAUDE.md](CLAUDE.md)
- **Architecture Documentation**: [docs/ai-assistant/architecture.md](docs/ai-assistant/architecture.md)
