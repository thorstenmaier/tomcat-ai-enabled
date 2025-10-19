---
allowed-tools: Bash(git log:*), Bash(git diff:*), Bash(git status:*), Read, Glob, Grep, Edit, MultiEdit, Write, Task
description: Analyze current session activities and update AI documentation structure with new insights
---

Analyze the current development session and systematically update the Apache Tomcat AI documentation structure with new insights, patterns, and processes discovered during this session.

This command implements a comprehensive self-improvement protocol to ensure the AI documentation stays current and valuable for future sessions.

## Analysis Phases

### Phase 1: Session Activity Analysis
1. **Git History Review**: Examine recent commits and changes made during this session
   - Run `git log --oneline -10` to see recent commits
   - Run `git status` to check for uncommitted changes
   - Run `git diff` to understand current modifications

2. **Documentation Structure Review**: Scan all AI-assistant documentation files
   - Read CLAUDE.md for current project guidance
   - Review all files in `docs/ai-assistant/` directory structure
   - Identify documentation categories and current content

3. **Conversation Context Analysis**: Analyze the current session's conversation
   - Identify new processes, commands, or workflows discovered
   - Note any environment-specific solutions or workarounds
   - Capture new patterns or architectural insights
   - Record performance optimizations or debugging techniques

### Phase 2: Gap Analysis and Update Planning
4. **Content Gap Identification**: Compare session learnings with existing documentation
   - Identify missing information in domain maps
   - Note new troubleshooting patterns for ai-memory-cache.md
   - Find workflow improvements for workflow.md
   - Discover new architectural patterns for architecture.md

5. **Update Prioritization**: Determine which documentation needs updates
   - Critical: Safety-critical patterns, security insights
   - High: Performance improvements, new processes
   - Medium: Enhanced troubleshooting, updated examples
   - Low: Minor clarifications, formatting improvements

### Phase 3: Documentation Updates
6. **Systematic Updates**: Update documentation files with new insights
   - **CLAUDE.md**: Update project overview, commands, troubleshooting
   - **ai-memory-cache.md**: Add new hotspots, issues, debugging hints
   - **workflow.md**: Update development processes and best practices
   - **architecture.md**: Add architectural insights and component relationships
   - **Domain files**: Update domain-specific knowledge and patterns
   - **patterns.md**: Add new coding patterns discovered

7. **Cross-Reference Updates**: Ensure consistency across all documentation
   - Update file references and cross-links
   - Synchronize command examples and procedures
   - Verify all paths and technical details are current

### Phase 4: Quality Assurance and Validation
8. **Documentation Validation**: Verify all updates are accurate and useful
   - Check that all new commands and processes are correctly documented
   - Ensure examples are accurate and complete
   - Validate that file paths and technical references are correct
   - Avoid Pure Duplication: Ensure no duplicate documentation without added value
     - Each file should have a distinct purpose and perspective
     - Cross-references are preferred over copying content
     - If similar information appears in multiple files, ensure each adds unique value
     - Use links to refer to canonical sources rather than duplicating content

9. **Self-Improvement Protocol Execution**: Document the improvement process itself
   - Record what types of insights were most valuable to capture
   - Note which documentation sections see the most updates
   - Track patterns in session-to-session learning

## Update Categories

### Process Updates
- New build procedures or commands
- Updated runtime configurations
- Enhanced debugging techniques
- Improved testing strategies

### Environmental Insights
- Platform-specific solutions (macOS, Linux, Windows)
- SDKMAN or other tool-specific configurations
- Port management and process handling
- Development environment optimizations

### Architectural Discoveries
- Component interaction patterns
- Performance characteristics
- Security considerations
- Integration patterns

### Troubleshooting Knowledge
- Common error patterns and solutions
- Debugging procedures
- Performance bottlenecks
- Configuration issues

### Development Workflow Improvements
- Code review patterns
- Testing strategies
- Commit message improvements
- Collaboration techniques

## Output Format

### Progress Reporting
- Provide clear status updates for each analysis phase
- Report specific files being updated and why
- Summarize key insights being captured

### Update Summary
- List all documentation files modified
- Highlight the most significant improvements
- Note any new patterns or processes documented

### Future Session Value
- Explain how these updates will benefit future AI sessions
- Identify areas that still need improvement
- Suggest follow-up documentation tasks

This command ensures that knowledge gained during development sessions is systematically captured and made available for future work, creating a continuously improving development experience.