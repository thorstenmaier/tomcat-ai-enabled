# Command: /tdd-workflow

## Purpose
Implements Test-Driven Development workflow as recommended in Claude Code best practices.

## TDD Cycle
1. **RED**: Write failing test
2. **GREEN**: Write minimal code to pass
3. **REFACTOR**: Improve code while keeping tests green

## Usage
```
/tdd-workflow "Add request rate limiting to prevent DOS attacks"
```

## Execution Steps

### 1. Understand Requirements
- Analyze the feature request
- Identify acceptance criteria
- Determine test scenarios

### 2. Write Failing Tests
```java
// Example structure
@Test
public void testRateLimitingPreventsExcessiveRequests() {
    // Given: Rate limit of 100 requests per minute
    // When: 101 requests are made
    // Then: 101st request should be rejected
}
```

### 3. Run Tests (Verify Failure)
```bash
ant test -Dtest.name="**/TestRateLimiting.java"
```

### 4. Implement Minimal Solution
- Write just enough code to pass tests
- Don't add unnecessary features
- Focus on making tests green

### 5. Verify Tests Pass
```bash
ant test -Dtest.name="**/TestRateLimiting.java"
```

### 6. Refactor
- Improve code structure
- Extract methods/classes
- Apply design patterns
- Ensure tests still pass

### 7. Add Edge Cases
- Write tests for boundary conditions
- Handle error scenarios
- Test performance implications

### 8. Integration Tests
- Test with full Tomcat container
- Verify interaction with other components
- Check configuration handling

## Arguments
- `$ARGUMENTS`: Feature description to implement using TDD

## Best Practices
- Write test first, code second
- One test at a time
- Keep tests simple and focused
- Test behavior, not implementation
- Maintain fast test execution
- Use descriptive test names

## Benefits
- Immediate feedback on code correctness
- Built-in regression test suite
- Better design through test-first thinking
- Documentation through test cases
- Confidence in refactoring