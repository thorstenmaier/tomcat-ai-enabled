---
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(lsof:*), Bash(kill:*), Bash(export:*), Bash(./output/build/bin/catalina.sh:*)
description: Check git status, summarize changes, and start Tomcat server
---

First check for any uncommitted changes and summarize them, then start Apache Tomcat server.

Steps to perform:
1. Run `git status` to check for staged/unstaged changes
2. If changes exist, run `git diff` to understand what changed and provide a brief summary
3. Check if Tomcat is already running with `lsof -i :8080`
4. If port 8080 is in use, ask if I should kill the existing process
5. Export JAVA_HOME for SDKMAN: `export JAVA_HOME=/Users/$(whoami)/.sdkman/candidates/java/current`
6. Start Tomcat in background: `./output/build/bin/catalina.sh run`
7. Verify startup success and provide the access URL (http://localhost:8080)