---
allowed-tools: Read, Grep, Glob, Edit, MultiEdit, Write, Bash(ant:*), Task
description: Synchronize inline comments with implementation before generating meta documentation
---

Ensure all inline comments within a given Java class are accurate and up-to-date before generating or updating higher-level meta documentation (class summaries, architectural overviews, or module-level docs).

This command operates at the class level, analyzing all methods, properties, and implementation details to verify that inline documentation reflects the current code reality.

## Core Philosophy

**Code is truth. Comments are interpretation.**

Inline comments must accurately reflect what the code actually does, not what it was intended to do. This command enforces that principle by:
1. Analyzing actual implementation
2. Comparing with existing comments
3. Updating outdated or missing documentation
4. Only then enabling meta documentation generation

## Command Arguments

**Required**: File path to a Java class
```bash
/sync-comments java/org/apache/catalina/core/StandardContext.java
```

**Optional**: Generate meta documentation after sync
```bash
/sync-comments java/org/apache/catalina/core/StandardContext.java --generate-meta
```

## Execution Workflow

### Phase 1: Class Analysis
1. **Read Target Class**: Load the specified Java class file
2. **Parse Structure**: Identify all methods, fields, and nested classes
3. **Extract Existing Comments**: Catalog all current inline documentation
   - Javadoc comments
   - Inline method comments
   - Field documentation
   - Implementation notes

### Phase 2: Implementation Verification
4. **Method Analysis**: For each method in the class:
   - Read actual implementation logic
   - Identify key operations and side effects
   - Check for exception handling patterns
   - Note synchronization/concurrency patterns
   - Identify dependencies on other components

5. **Field Analysis**: For each field:
   - Determine actual usage patterns
   - Identify initialization and lifecycle
   - Check thread-safety implications
   - Note access patterns (private/protected/public)

6. **Cross-Reference Check**:
   - Verify method comments match implementation
   - Ensure field documentation reflects actual usage
   - Check that @param, @return, @throws tags are accurate
   - Validate code examples in comments still work

### Phase 3: Comment Synchronization
7. **Identify Discrepancies**:
   - Missing comments on public/protected methods
   - Outdated comments not reflecting current logic
   - Incorrect parameter or return type descriptions
   - Stale references to removed code or patterns
   - Missing security or performance considerations

8. **Generate Updated Comments**:
   - **For methods**: What it does, why it exists, important considerations
   - **For fields**: Purpose, lifecycle, thread-safety notes
   - **For classes**: Role in architecture, key responsibilities
   - **For exceptions**: When thrown, recovery strategies
   - **Follow Tomcat documentation patterns** from existing well-documented classes

9. **Apply Updates**: Use Edit or MultiEdit to update comments
   - Maintain existing Javadoc format
   - Preserve custom annotations and tags
   - Keep concise and accurate
   - Follow project style guide

### Phase 4: Validation
10. **Consistency Check**:
    - Verify updated comments match implementation
    - Check all public APIs are documented
    - Ensure cross-references are valid
    - Validate code compiles after changes

11. **Pattern Compliance**:
    - Match Tomcat documentation standards
    - Follow Java documentation conventions
    - Maintain consistency with related classes

### Phase 5: Meta Documentation (Optional)
12. **Generate Class Summary** (if --generate-meta flag):
    - Create or update class-level documentation
    - Generate architectural context
    - Document relationships to other components
    - Update relevant domain documentation in `docs/ai-assistant/domains/`

13. **Update Cross-References**:
    - Update `docs/ai-assistant/services/` if component interactions changed
    - Add to `docs/ai-assistant/patterns.md` if new patterns emerged
    - Update `docs/ai-assistant/search-hints.md` for discoverability

## Comment Quality Standards

### Good Inline Comments
```java
/**
 * Processes incoming HTTP requests through the container pipeline.
 *
 * This method serves as the entry point for request handling, delegating
 * to the appropriate valve chain. Thread-safety is ensured through the
 * request-specific processing pipeline.
 *
 * @param request the HTTP request to process (must not be null)
 * @param response the HTTP response to populate (must not be null)
 * @throws IOException if I/O error occurs during processing
 * @throws ServletException if servlet-specific error occurs
 */
public void invoke(Request request, Response response)
    throws IOException, ServletException {
    // Implementation
}
```

### Poor Inline Comments (to fix)
```java
/**
 * Invoke method
 * @param request request
 * @param response response
 */
public void invoke(Request request, Response response) {
    // TODO: fix this
}
```

## Analysis Techniques

### Implementation Pattern Detection
- **Locks/Synchronization**: Document thread-safety approach
- **Resource Management**: Note lifecycle and cleanup
- **Error Handling**: Explain exception strategies
- **Performance Patterns**: Document optimization techniques
- **Security Checks**: Highlight validation and sanitization

### Context-Aware Documentation
- **Servlet Container Context**: Reference container lifecycle
- **Protocol Handling**: Note HTTP/AJP specifics
- **Pipeline Architecture**: Explain valve chain position
- **Configuration**: Document settable properties
- **Backward Compatibility**: Note API evolution

## Integration with Existing Workflows

### Pre-Commit Hook Integration
```bash
# Before committing changes to a class
/sync-comments java/org/apache/catalina/core/ModifiedClass.java

# Then proceed with commit
git add java/org/apache/catalina/core/ModifiedClass.java
git commit -m "Update: Enhanced feature X"
```

### Documentation Generation Workflow
```bash
# Step 1: Sync inline comments
/sync-comments java/org/apache/catalina/connector/Connector.java --generate-meta

# Step 2: Review changes
git diff java/org/apache/catalina/connector/Connector.java

# Step 3: Update broader documentation if needed
/improve-yourself
```

### Bulk Synchronization
For synchronizing multiple related classes:
```bash
# Use with explore-plan-code-commit workflow
/explore-plan-code-commit "Sync comments for session management classes"
# Agent will:
# 1. EXPLORE: Find all session-related classes
# 2. PLAN: Prioritize classes by importance
# 3. CODE: Run /sync-comments on each class
# 4. COMMIT: Batch commit documentation updates
```

## Output Format

### Progress Reporting
- **Phase indicators**: Show current analysis phase
- **Class statistics**: Methods analyzed, fields documented, comments updated
- **Discrepancy summary**: What was outdated or missing
- **Update summary**: What was changed and why

### Completion Report
```
✅ Synchronized comments for: StandardContext.java

📊 Statistics:
  - Methods analyzed: 47
  - Fields analyzed: 23
  - Comments updated: 12
  - Missing documentation added: 5
  - Outdated references fixed: 3

📝 Key Updates:
  - Updated lifecycle documentation to reflect async context handling
  - Added thread-safety notes for context reload operations
  - Documented new security validation in addParameter()
  - Fixed stale references to removed JMX integration

🔗 Meta Documentation:
  - Updated: docs/ai-assistant/domains/webapp-deployment.md
  - Updated: docs/ai-assistant/services/context-management.md

✨ All inline comments now accurately reflect implementation.
```

## Use Cases

### 1. Pre-Release Documentation Audit
Ensure all public APIs are properly documented before release:
```bash
/sync-comments java/jakarta/servlet/http/HttpServletRequest.java --generate-meta
```

### 2. Refactoring Documentation Sync
After significant refactoring, update all affected documentation:
```bash
/sync-comments java/org/apache/catalina/core/ApplicationContext.java
```

### 3. Onboarding Preparation
Improve documentation for complex classes before new team members join:
```bash
/sync-comments java/org/apache/coyote/http11/Http11Processor.java --generate-meta
```

### 4. Architecture Documentation
Synchronize documentation when architectural patterns change:
```bash
/sync-comments java/org/apache/catalina/valves/ValveBase.java --generate-meta
```

### 5. Bug Fix Documentation
After fixing a bug, ensure comments explain the corrected behavior:
```bash
/sync-comments java/org/apache/catalina/session/StandardSession.java
```

## Quality Assurance

### Automated Checks
- Verify all public methods have documentation
- Check @param tags match actual parameters
- Validate @return descriptions match return types
- Ensure @throws documents all declared exceptions
- Confirm code examples in comments are valid

### Manual Review Points
- Accuracy of technical descriptions
- Clarity for developers unfamiliar with code
- Completeness of security/performance notes
- Consistency with related class documentation
- Appropriateness of detail level

## Best Practices

### When to Run
✅ **Good Times**:
- After completing a feature implementation
- Before creating a pull request
- When refactoring existing code
- After fixing bugs with behavioral changes
- Before major releases

❌ **Avoid Running**:
- During active development (wait for stability)
- On generated code (parser files, etc.)
- On third-party repackaged code

### Command Combinations
- **/sync-comments** → **/improve-yourself**: Complete documentation update
- **/tdd-workflow** → **/sync-comments**: Document after TDD implementation
- **/explore-plan-code-commit** → **/sync-comments**: Include in structured workflow

### Integration Points
- **Code Review**: Verify comment accuracy during review
- **CI/CD**: Run as documentation validation step
- **Architecture Review**: Ensure documented design matches implementation

## Technical Implementation Notes

### Tool Usage
- **Read**: Load Java class and related files
- **Grep**: Find similar documentation patterns
- **Glob**: Discover related classes for consistency
- **Edit/MultiEdit**: Update comments efficiently
- **Task**: Use sub-agents for complex analysis
- **Bash(ant)**: Validate compilation after changes

### Sub-Agent Delegation
For large or complex classes (>1000 lines):
1. Use `research-context-optimizer` agent to analyze implementation
2. Agent saves findings to `docs/claude/research/comment-analysis-[timestamp].md`
3. Main agent reads report and generates updated comments
4. Prevents context overflow while maintaining accuracy

### Performance Considerations
- Process one class at a time for accuracy
- Use MultiEdit for batch comment updates
- Cache analysis results for related methods
- Limit meta documentation generation to essential updates

## Troubleshooting

### Issue: Comments seem generic
**Solution**: Increase context by loading related domain documentation first

### Issue: Updates break Javadoc generation
**Solution**: Validate with `ant javadoc` after changes, fix formatting

### Issue: Comments contradict architecture docs
**Solution**: Flag discrepancy, update both inline and architectural docs

### Issue: Performance concerns in comments
**Solution**: Cross-reference with `docs/ai-assistant/memory/` for known patterns

## Future Enhancements

Potential extensions to this command:
- **Batch mode**: Process multiple classes in a package
- **Diff mode**: Only update comments that changed since last commit
- **Template mode**: Apply consistent comment structure across similar classes
- **Metrics mode**: Generate documentation coverage reports
- **Integration mode**: Sync with external documentation systems

## Success Criteria

A successful sync-comments execution achieves:
1. ✅ All public APIs have accurate, complete documentation
2. ✅ Inline comments reflect actual implementation behavior
3. ✅ No misleading or outdated documentation remains
4. ✅ Code compiles and passes existing tests
5. ✅ Documentation follows project standards
6. ✅ Meta documentation (if requested) is updated and accurate

This command ensures that inline documentation serves as a reliable foundation for higher-level architectural and API documentation, maintaining the critical code-documentation synchronization that makes AI-assisted development effective.
