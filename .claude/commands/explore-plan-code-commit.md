# Command: /explore-plan-code-commit

## Purpose
Implements the recommended workflow from Anthropic's Claude Code best practices for structured development tasks.

## Workflow Phases

### 1. EXPLORE Phase
- Research existing codebase patterns
- Understand dependencies and interactions
- Identify similar implementations
- Document findings in research reports

### 2. PLAN Phase
- Create detailed implementation plan
- Break down into subtasks using TodoWrite
- Identify potential risks and edge cases
- Define success criteria

### 3. CODE Phase
- Implement following discovered patterns
- Write tests alongside implementation
- Validate against existing code style
- Ensure architectural consistency

### 4. COMMIT Phase
- Review all changes
- Run validation (tests, linting)
- Update documentation
- Create descriptive commit message

## Usage
```
/explore-plan-code-commit "Implement new caching mechanism for session management"
```

## Execution Steps
1. **Explore**: Search for existing caching patterns
   - `grep -r "cache" --include="*.java"`
   - Load relevant domain documentation
   - Create research report

2. **Plan**: Design implementation approach
   - Create TodoWrite list with specific tasks
   - Document architectural decisions
   - Identify test scenarios

3. **Code**: Implement the solution
   - Follow discovered patterns
   - Write unit tests
   - Add integration tests
   - Update relevant documentation

4. **Commit**: Finalize changes
   - Run `ant test`
   - Run `ant validate`
   - Execute `/improve-yourself`
   - Create commit with comprehensive message

## Arguments
- `$ARGUMENTS`: The feature or task description to implement

## Best Practices
- Always complete EXPLORE before PLAN
- Never skip directly to CODE
- Document learnings for future sessions
- Ensure tests pass before COMMIT
- Update documentation as part of implementation