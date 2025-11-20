# Usage Guide - MCP Full-Stack Server Kotlin

## Quick Start

### 1. Build and Run

```bash
# Build the project
./gradlew clean build

# Run locally
./gradlew run

# Run with custom configuration
export MCP_HOST=0.0.0.0
export MCP_PORT=3000
./gradlew run

# Run JAR directly
java -jar build/libs/mcp-fullstack-server-1.0.0.jar
```

### 2. Server Endpoints

- **Health Check**: `GET http://localhost:3000/health`
- **Readiness Check**: `GET http://localhost:3000/ready`
- **Server Info**: `GET http://localhost:3000/info`
- **Tools List**: `GET http://localhost:3000/tools`

### 3. Connecting Claude Desktop/Code

Add to your Claude Desktop configuration (`~/.config/claude-code/config.json`):

```json
{
  "mcpServers": {
    "mcp-fullstack-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-fullstack-server-1.0.0.jar",
        "--stdio"
      ],
      "env": {
        "MCP_WORKING_DIR": "/workspace",
        "MCP_ALLOWED_DIRS": "/workspace,/home/user/projects",
        "GITHUB_TOKEN": "your-github-token"
      }
    }
  }
}
```

## Available Tools

### Filesystem Tools (5)

#### readFile
Read file contents with encoding support.

```json
{
  "name": "readFile",
  "arguments": {
    "path": "/workspace/myfile.txt",
    "encoding": "UTF-8"
  }
}
```

#### writeFile
Write content to a file with different modes.

```json
{
  "name": "writeFile",
  "arguments": {
    "path": "/workspace/output.txt",
    "content": "Hello, World!",
    "mode": "CREATE"
  }
}
```

Modes: `CREATE`, `OVERWRITE`, `APPEND`

#### listDirectory
List directory contents with recursive option.

```json
{
  "name": "listDirectory",
  "arguments": {
    "path": "/workspace",
    "recursive": true,
    "maxDepth": 3
  }
}
```

#### createDirectory
Create directories with recursive option.

```json
{
  "name": "createDirectory",
  "arguments": {
    "path": "/workspace/new-project/src",
    "recursive": true
  }
}
```

#### deleteFile
Delete files or directories.

```json
{
  "name": "deleteFile",
  "arguments": {
    "path": "/workspace/temp-file.txt",
    "recursive": false
  }
}
```

### Bash Tools (1)

#### execute
Execute shell commands safely.

```json
{
  "name": "execute",
  "arguments": {
    "command": "ls",
    "args": ["-la", "/workspace"],
    "env": {"CUSTOM_VAR": "value"}
  }
}
```

**Allowed Commands** (default):
`ls`, `cat`, `grep`, `find`, `echo`, `pwd`, `mkdir`, `touch`, `mv`, `cp`, `rm`, `git`, `npm`, `yarn`, `gradle`, `docker`, `kubectl`, `curl`, `wget`, `jq`

### GitHub Tools (6)

#### status
Get Git repository status.

```json
{
  "name": "status",
  "arguments": {}
}
```

#### commit
Commit changes with message.

```json
{
  "name": "commit",
  "arguments": {
    "message": "feat: add new feature",
    "files": ["src/main.kt", "README.md"],
    "author": "Developer Name",
    "email": "developer@example.com"
  }
}
```

#### push
Push commits to remote.

```json
{
  "name": "push",
  "arguments": {
    "remote": "origin",
    "branch": "main",
    "force": false
  }
}
```

#### clone
Clone a repository.

```json
{
  "name": "clone",
  "arguments": {
    "url": "https://github.com/user/repo.git",
    "targetPath": "my-project"
  }
}
```

#### log
Get commit history.

```json
{
  "name": "log",
  "arguments": {
    "maxCount": 10
  }
}
```

#### branch
Manage Git branches.

```json
{
  "name": "branch",
  "arguments": {
    "name": "feature/new-feature",
    "checkout": true
  }
}
```

### Memory Tools (4)

#### createEntities
Create entities in knowledge graph.

```json
{
  "name": "createEntities",
  "arguments": {
    "entities": [
      {
        "name": "MyProject",
        "entityType": "project",
        "observations": ["Uses Kotlin", "MCP Server implementation"]
      }
    ]
  }
}
```

#### createRelations
Create relations between entities.

```json
{
  "name": "createRelations",
  "arguments": {
    "relations": [
      {
        "from": "MyProject",
        "to": "FilesystemModule",
        "relationType": "contains"
      }
    ]
  }
}
```

#### searchNodes
Search entities by name or observations.

```json
{
  "name": "searchNodes",
  "arguments": {
    "query": "Kotlin"
  }
}
```

#### openNodes
Retrieve specific entities.

```json
{
  "name": "openNodes",
  "arguments": {
    "names": ["MyProject", "FilesystemModule"]
  }
}
```

### PostgreSQL Tools (3)

#### executeQuery
Execute read-only SQL queries.

```json
{
  "name": "executeQuery",
  "arguments": {
    "sql": "SELECT * FROM users WHERE age > ?",
    "params": [18],
    "maxRows": 100
  }
}
```

**Security**: Only `SELECT`, `SHOW`, `DESCRIBE`, `EXPLAIN` queries allowed.

#### getSchema
Get database schema information.

```json
{
  "name": "getSchema",
  "arguments": {}
}
```

#### testConnection
Test PostgreSQL connection.

```json
{
  "name": "testConnection",
  "arguments": {}
}
```

### MongoDB Tools (5)

#### find
Find documents in collection.

```json
{
  "name": "find",
  "arguments": {
    "collection": "users",
    "filter": "{\"age\": {\"$gt\": 18}}",
    "limit": 50,
    "sort": "{\"name\": 1}"
  }
}
```

#### listCollections
List all collections.

```json
{
  "name": "listCollections",
  "arguments": {}
}
```

#### countDocuments
Count documents matching filter.

```json
{
  "name": "countDocuments",
  "arguments": {
    "collection": "users",
    "filter": "{\"active\": true}"
  }
}
```

#### aggregate
Run aggregation pipeline.

```json
{
  "name": "aggregate",
  "arguments": {
    "collection": "orders",
    "pipeline": "{\"pipeline\": [{\"$group\": {\"_id\": \"$status\", \"count\": {\"$sum\": 1}}}]}"
  }
}
```

#### testConnection
Test MongoDB connection.

```json
{
  "name": "testConnection",
  "arguments": {}
}
```

### Resource Tools (4)

#### listResources
List available resources.

```json
{
  "name": "listResources",
  "arguments": {}
}
```

#### readResource
Read resource content.

```json
{
  "name": "readResource",
  "arguments": {
    "uri": "resource://docs/api-guide.md"
  }
}
```

#### createResource
Create a new resource.

```json
{
  "name": "createResource",
  "arguments": {
    "name": "project-notes.md",
    "content": "# Project Notes\\n\\n...",
    "mimeType": "text/markdown"
  }
}
```

#### deleteResource
Delete a resource.

```json
{
  "name": "deleteResource",
  "arguments": {
    "uri": "resource://temp/old-notes.txt"
  }
}
```

## Configuration

### Environment Variables

```bash
# Server
export MCP_HOST=0.0.0.0
export MCP_PORT=3000

# Filesystem
export MCP_ALLOWED_DIRS="/workspace,/home/user/projects,/tmp/mcp-builds"
export MCP_MAX_FILE_SIZE=10485760  # 10MB

# Bash
export MCP_ALLOWED_COMMANDS="ls,cat,grep,find,git,npm,gradle"
export MCP_WORKING_DIR="/workspace"
export MCP_COMMAND_TIMEOUT=300  # seconds

# GitHub
export MCP_REPO_PATH="/workspace/repo"
export GITHUB_TOKEN="your-github-token"

# Memory
export MCP_MEMORY_PATH="/workspace/.mcp-memory"

# PostgreSQL
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=mcp_db
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=secret

# MongoDB
export MONGODB_CONNECTION_STRING=mongodb://localhost:27017
export MONGODB_DB=mcp_db

# Resources
export MCP_RESOURCES_PATH="/workspace/resources"
```

### Configuration File

Edit `src/main/resources/application.conf`:

```hocon
server {
  host = "0.0.0.0"
  port = 3000
}

filesystem {
  allowedDirectories = ["/workspace", "/home/claude/projects"]
  maxFileSize = 10485760
}

bash {
  allowedCommands = ["ls", "cat", "git", "npm"]
  workingDirectory = "/workspace"
  timeoutSeconds = 300
}
```

## Example Workflows

### 1. Create a New Project

```kotlin
// 1. Create project directory
writeFile("/workspace/my-app/README.md", "# My App")

// 2. Initialize Git
execute("git", ["init"], {})
commit("Initial commit", ["."])

// 3. Store metadata in knowledge graph
createEntities([{
  name: "MyApp",
  entityType: "project",
  observations: ["Created today", "Kotlin project"]
}])

// 4. Create documentation resource
createResource("my-app-docs.md", "# Documentation\\n...", "text/markdown")
```

### 2. Analyze Existing Code

```kotlin
// 1. List files
listDirectory("/workspace/existing-project", true)

// 2. Read source files
readFile("/workspace/existing-project/src/Main.kt")

// 3. Run analysis commands
execute("grep", ["-r", "TODO", "/workspace/existing-project/src"])

// 4. Store findings in knowledge graph
createEntities([{
  name: "CodeAnalysis",
  entityType: "analysis",
  observations: ["Found 5 TODOs", "Needs refactoring in Module X"]
}])
```

### 3. Database Operations

```kotlin
// 1. Test connection
testConnection()

// 2. Get schema
getSchema()

// 3. Query data
executeQuery("SELECT * FROM users WHERE created_at > ?", ["2025-01-01"], 100)

// 4. Store results in files
writeFile("/workspace/query-results.json", results)
```

## Security Best Practices

1. **Always use allowed directories** - Don't bypass path validation
2. **Never commit secrets** - Use environment variables
3. **Use read-only queries** - Write operations are blocked
4. **Validate command inputs** - Only whitelisted commands allowed
5. **Set reasonable limits** - File size, query results, timeouts
6. **Use GITHUB_TOKEN** - For private repository access
7. **Monitor logs** - Check `logs/mcp-server.log` for security events

## Troubleshooting

### Server Won't Start
```bash
# Check if port is in use
lsof -i :3000

# Check logs
tail -f logs/mcp-server.log

# Verify configuration
cat src/main/resources/application.conf
```

### Command Execution Fails
```bash
# Verify command is allowed
grep "allowedCommands" src/main/resources/application.conf

# Check working directory exists
ls -la $MCP_WORKING_DIR
```

### Database Connection Fails
```bash
# Test PostgreSQL
psql -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER -d $POSTGRES_DB

# Test MongoDB
mongosh $MONGODB_CONNECTION_STRING
```

### Path Access Denied
```bash
# Verify path is in allowed directories
echo $MCP_ALLOWED_DIRS

# Check permissions
ls -la /workspace
```

## Advanced Usage

### Custom Tool Integration

See `CLAUDE.md` for instructions on adding custom tools to the server.

### Docker Deployment

```bash
# Build image
docker build -t mcp-server -f docker/Dockerfile .

# Run with volume mounts
docker run -p 3000:3000 \
  -e MCP_WORKING_DIR=/workspace \
  -v $(pwd)/workspace:/workspace \
  mcp-server
```

### Kubernetes Deployment

```bash
# Deploy to cluster
VERSION=1.0.0 NAMESPACE=cyberlab DEPLOY_TO_K8S=true ./deploy.sh

# Check status
kubectl get pods -n cyberlab -l app=mcp-server

# View logs
kubectl logs -f deployment/mcp-fullstack-server -n cyberlab
```

## Resources

- **GitHub Repository**: [MCP Server Kotlin](https://github.com/yourusername/mcp-fullstack-server)
- **MCP Protocol**: https://modelcontextprotocol.io
- **CLAUDE.MD**: Project-specific instructions
- **TESTING_GUIDE.MD**: Testing instructions
- **README.MD**: Quick reference

---

**Last Updated**: 2025-11-20
**Version**: 1.0.0
