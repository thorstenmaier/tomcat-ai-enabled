# AI-Enabled Apache Tomcat Repository

## Purpose

This repository is **not** the standard Apache Tomcat codebase. Instead, it's an experimental environment designed to explore and demonstrate how AI agents can effectively navigate and work with large, complex codebases. Using Apache Tomcat 12.0 as the foundation (with its ~726,745 lines of code), this repository has been enhanced with AI-specific documentation and configurations to enable intelligent code navigation and understanding.

## What Has Been AI-Enabled?

This repository contains the complete Apache Tomcat 12.0 source code, augmented with:

### 1. AI Assistant Documentation (`CLAUDE.md`)
A comprehensive guide specifically designed for Claude Code (claude.ai/code) that includes:
- **Build system commands** and environment-specific configurations
- **Architecture overview** with component hierarchy and request processing flows
- **Dynamic context loading** rules to optimize token usage
- **Self-improvement protocol** for capturing and documenting new patterns
- **Performance hotspots** and common issues with quick fixes
- **Knowledge base** for future sessions including macOS/SDKMAN specifics

### 2. Structured AI Navigation System (`docs/ai-assistant/`)
A comprehensive documentation structure helping AI agents understand the codebase:

#### Domain Documentation (10 core domains)
- Request Processing, Security, Session Management
- Web Application Deployment, Network I/O, JSP/Expression Language
- Clustering, Resource Management, Configuration, Monitoring

#### Technical Indexes
- **API Endpoints** - Servlet mappings and REST endpoints
- **Class Hierarchies** - Component inheritance structures
- **Service Registry** - Component interactions and dependencies
- **Semantic Code Map** - Conceptual navigation paths

#### Development Support
- **Patterns** - Tomcat-specific coding patterns and conventions
- **Testing** - Comprehensive testing strategies and frameworks
- **Workflow** - Development best practices and processes
- **Search Hints** - Concept-to-file quick reference
- **Memory Cache** - Performance hotspots and troubleshooting

### 3. Custom AI Agents (`.claude/agents/`)
Specialized sub-agents for complex tasks:
- **test-orchestrator** - Intelligently selects minimal test suites based on code changes
- **dependency-analyzer** - Analyzes component dependencies and impact of changes
- **code-validator** - Performs comprehensive code validation including style and security
- **research-context-optimizer** - Focused research without flooding the context window

### 4. Custom Commands (`.claude/commands/`)
AI-specific commands to streamline common workflows:
- **/improve-yourself** - Systematic self-improvement protocol that analyzes session activities and updates AI documentation with new insights
- **/start-tomcat** - Automated Tomcat startup with git status check, port conflict resolution, and SDKMAN Java configuration

### 5. MCP Server Integration
- **Context7 Integration** - Access to 31,279 Tomcat documentation snippets
- **Library ID**: `/websites/tomcat_apache_tomcat-10_1-doc`
- Provides API docs, configuration examples, and best practices

### 6. Agent Communication Patterns
Established workflows for effective agent collaboration:
- **File-based context sharing** via `docs/claude/` directory
- **Research reports** saved as persistent markdown files
- **Task context** preserved across agent sessions
- **Clear handoff protocols** between parent and sub-agents

## How This Helps AI Agents

1. **Context Optimization**: Dynamic loading of relevant documentation reduces token usage
2. **Rapid Navigation**: Semantic maps and search hints enable quick file location
3. **Pattern Recognition**: Documented Tomcat-specific patterns prevent common mistakes
4. **Persistent Learning**: Self-improvement protocol captures new insights
5. **Specialized Workflows**: Sub-agents handle complex analysis without context overflow
6. **Environment Awareness**: Specific configurations for macOS, SDKMAN, and common issues

## Original Apache Tomcat Information

Apache Tomcat® is an open source implementation of the Jakarta Servlet, Jakarta Pages, Jakarta Expression Language and Jakarta WebSocket technologies, developed under the [Apache License version 2](https://www.apache.org/licenses/).

For standard Tomcat documentation, visit:
- [Tomcat 11](https://tomcat.apache.org/tomcat-11.0-doc/)
- [Tomcat 10](https://tomcat.apache.org/tomcat-10.1-doc/)
- [Tomcat 9](https://tomcat.apache.org/tomcat-9.0-doc/)

Apache Tomcat, Tomcat, Apache, the Apache feather, and the Apache Tomcat project logo are trademarks of the Apache Software Foundation.

## Quick Start for AI Agents

### Building and Running
```bash
# Build Tomcat
ant deploy

# For macOS with SDKMAN
export JAVA_HOME=~/.sdkman/candidates/java/current
./output/build/bin/catalina.sh run

# Check for port conflicts
lsof -i :8080 -i :8005 | grep LISTEN
```

### Key Files for AI Navigation
- **CLAUDE.md** - Primary AI assistant guide
- **docs/ai-assistant/** - Structured navigation documentation
- **.claude/agents/** - Custom agent configurations
- **docs/claude/** - Shared context and research reports

### Using the AI Enhancements

1. **Start with CLAUDE.md** - Contains essential commands and project overview
2. **Load domain docs on demand** - Use Read tool for specific domains when needed
3. **Leverage sub-agents** - Use Task tool for complex research or analysis
4. **Maintain context** - Save research to `docs/claude/` for persistence
5. **Use MCP for docs** - Query Context7 for official Tomcat documentation

## Repository Statistics

- **Total Lines**: ~726,745 (580,174 main + 146,571 tests)
- **Java Files**: 2,732
- **AI Documentation**: 22 specialized markdown files
- **Custom Agents**: 4 specialized sub-agents
- **MCP Documentation**: 31,279 code snippets available

## Learning and Contribution

This repository serves as a living example of how to make large codebases AI-friendly. Key innovations include:

- **Dynamic context management** to work within token limits
- **Semantic navigation** for conceptual code exploration
- **Agent collaboration patterns** for complex tasks
- **Self-improvement protocols** for continuous learning
- **Environment-specific adaptations** for real-world development

## License and Attribution

- Apache Tomcat source code: [Apache License version 2](https://www.apache.org/licenses/)
- AI enhancements and documentation: Experimental additions for AI navigation research
- Original Tomcat project: [Apache Software Foundation](https://tomcat.apache.org/)

## Further Information

- [Original CONTRIBUTING guide](CONTRIBUTING.md)
- [Original RUNNING instructions](RUNNING.txt)
- [Apache Tomcat official site](https://tomcat.apache.org/)
