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
    "sql": "SELECT * FROM users WHERE age > 18",
    "maxRows": 100
  }
}
```

**Note**: Parameter placeholders (`?`) are not supported by this tool. Use direct value interpolation in the SQL string.

**⚠️ Security Warning**: Direct value interpolation can lead to SQL injection vulnerabilities. **Never interpolate untrusted user input directly into SQL strings.**

**How to Safely Handle User Input:**

- **Validate input:** If you must interpolate values, use strict validation and whitelisting. Only allow expected values (e.g., numbers, specific column names, etc.), and reject or sanitize anything else.

  > **Note:** Do not rely on generic "escape" functions for SQL input. The commonly referenced `StringEscapeUtils.escapeSql` method from Apache Commons Lang has been removed in modern versions because it does **not** provide adequate protection against SQL injection.

  > **If you cannot use parameterized queries, avoid interpolating untrusted input.** Some database drivers may provide their own escaping/sanitization methods (e.g., PostgreSQL's `PGConnection.escapeString()`), but these are not a substitute for proper validation and whitelisting.

- **Best Practice:** The industry standard is to use parameterized queries (prepared statements), which are not currently supported by this tool. **If possible, avoid using this tool for queries involving untrusted input.**

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
    "content": "# Project Notes\n\n...",
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

```json
[
  {
    "name": "writeFile",
    "arguments": {
      "path": "/workspace/my-app/README.md",
      "content": "# My App",
      "mode": "CREATE"
    }
  },
  {
    "name": "execute",
    "arguments": {
      "command": "git",
      "args": ["init"],
      "workingDir": "/workspace/my-app"
    }
  },
  {
    "name": "commit",
    "arguments": {
      "message": "Initial commit",
      "files": ["."]
    }
  },
  {
    "name": "createEntities",
    "arguments": {
      "entities": [
        {
          "name": "MyApp",
          "entityType": "project",
          "observations": ["Created today", "Kotlin project"]
        }
      ]
    }
  },
  {
    "name": "createResource",
    "arguments": {
      "name": "my-app-docs.md",
      "content": "# Documentation\n...",
      "mimeType": "text/markdown"
    }
  }
]
```

### 2. Analyze Existing Code

```json
[
  {
    "name": "listDirectory",
    "arguments": {
      "path": "/workspace/existing-project",
      "recursive": true
    }
  },
  {
    "name": "readFile",
    "arguments": {
      "path": "/workspace/existing-project/src/Main.kt"
    }
  },
  {
    "name": "execute",
    "arguments": {
      "command": "grep",
      "args": ["-r", "TODO", "/workspace/existing-project/src"]
    }
  },
  {
    "name": "createEntities",
    "arguments": {
      "entities": [
        {
          "name": "CodeAnalysis",
          "entityType": "analysis",
          "observations": ["Found 5 TODOs", "Needs refactoring in Module X"]
        }
      ]
    }
  }
]
```

### 3. Database Operations

**⚠️ Security Note**: When constructing SQL queries with interpolated values, always validate and sanitize inputs to prevent SQL injection.

```json
[
  {
    "name": "testConnection",
    "arguments": {}
  },
  {
    "name": "getSchema",
    "arguments": {}
  },
  {
    "name": "executeQuery",
    "arguments": {
      "sql": "SELECT * FROM users WHERE created_at > '2025-01-01'",
      "maxRows": 100
    }
  },
  {
    "name": "writeFile",
    "arguments": {
      "path": "/workspace/query-results.json",
      "content": "[results]",
      "mode": "CREATE"
    }
  }
]
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

- **GitHub Repository**: [MCP Server Kotlin](https://github.com/apptolast/McpServerKotlin)
- **MCP Protocol**: https://modelcontextprotocol.io
- **CLAUDE.MD**: Project-specific instructions
- **TESTING_GUIDE.MD**: Testing instructions
- **README.MD**: Quick reference

---

**Last Updated**: 2025-11-20
**Version**: 1.0.0
