# Tomcat Development Agents

This directory contains specialized Claude Code agents designed to optimize Apache Tomcat development workflows while minimizing context usage.

## Available Agents

### 🧪 test-orchestrator
**Purpose**: Intelligent test selection based on code changes  
**Usage**: Minimize test execution time while ensuring complete coverage  
**Saves**: Reduces test time from full suite (~45min) to targeted tests (~3-10min)

### ✅ code-validator  
**Purpose**: Comprehensive code quality validation  
**Usage**: Style, pattern, performance, and security analysis  
**Saves**: Automated validation instead of manual code review cycles

### 🔗 dependency-analyzer
**Purpose**: Component dependency impact analysis  
**Usage**: Understand change effects across Tomcat architecture  
**Saves**: Prevents unexpected breaking changes and guides testing scope

## Usage Examples

### Quick Test Selection
```bash
# After modifying HTTP processor classes
claude --agent test-orchestrator "Modified Http11Processor.java and related classes"
```

### Code Quality Check  
```bash
# Before committing changes
claude --agent code-validator "Review changes in java/org/apache/catalina/core/"
```

### Impact Analysis
```bash  
# Before major refactoring
claude --agent dependency-analyzer "Planning to modify ContainerBase lifecycle methods"
```

## Design Philosophy

### Context Optimization
- **Atomic Operations**: Each agent performs discrete tasks with clear inputs/outputs
- **No Context Sharing**: Agents operate independently without shared state
- **Complete Results**: Agents return comprehensive, actionable reports
- **Minimal Loading**: Only necessary code/docs loaded per analysis

### Prevention of Context Breaks
- **Single Responsibility**: Each agent focused on specific domain expertise
- **Stateless Design**: No ongoing communication between main thread and agents
- **Self-contained Analysis**: All required knowledge embedded in agent prompts
- **Deterministic Outputs**: Consistent report formats for easy integration

## Integration with Tomcat Workflow

These agents integrate seamlessly with existing Tomcat development practices:

1. **Pre-commit Hooks**: Run code-validator before git commits
2. **CI/CD Pipeline**: Use test-orchestrator for optimized test selection  
3. **Code Review**: Use dependency-analyzer to understand change scope
4. **Refactoring**: Combine all agents for comprehensive impact assessment

## Agent Architecture Benefits

### Traditional Approach
- Load entire codebase into context
- Manual analysis and decision making
- High token usage (50-100k+ tokens)
- Context window limitations

### Agent-based Approach  
- Specialized knowledge per domain
- Automated analysis with expert insights
- Low token usage per task (5-15k tokens)
- Scalable to complex codebases

## Future Agent Candidates

Based on usage patterns, consider adding:
- `performance-profiler`: Bottleneck identification
- `security-auditor`: Vulnerability scanning
- `documentation-sync`: Auto-update docs from code changes
- `configuration-validator`: Tomcat config optimization

## Contributing New Agents

When creating new agents:

1. **Single Purpose**: Focus on one specific development task
2. **Clear Interface**: Define exact input/output formats
3. **Domain Expertise**: Embed comprehensive knowledge in system prompt
4. **Action Oriented**: Provide specific, implementable recommendations
5. **Context Conscious**: Minimize external file loading requirements

## Troubleshooting

### Agent Not Found
Ensure agent files are in `.claude/agents/` directory with `.md` extension

### Unexpected Results  
Verify input format matches agent's expected interface documented in each agent file

### Performance Issues
Check that agents aren't loading unnecessary context - review tool permissions in YAML frontmatter

---

*These agents represent a context-efficient approach to managing complex development workflows in large codebases like Apache Tomcat.*