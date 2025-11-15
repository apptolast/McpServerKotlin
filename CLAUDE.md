# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 🎯 PRINCIPIOS FUNDAMENTALES - LÉELOS SIEMPRE

### Reglas de Oro para Desarrollo

1. **NO ALUCINAR NI INVENTAR NADA** - Nunca inventes código, funcionalidades o información que no exista
2. **REVISAR BIEN EL AÑO ACTUAL (2025)** - Cuando busques documentación en internet, verifica que estés usando información actualizada a 2025
3. **BUSCAR SIEMPRE EN DOCUMENTACIÓN OFICIAL** - Consulta SIEMPRE la documentación oficial de todo el stack tecnológico (Kotlin, Ktor, MCP, PostgreSQL, MongoDB, etc.)
4. **ESCRIBIR CÓDIGO MANTENIBLE, ESCALABLE, LEGIBLE Y SOSTENIBLE** - Escribe código humano, limpio, fácil de leer. NO código máquina complicado que no funcione. Tiene que ser FUNCIONAL
5. **COMPRENDER EL PROPÓSITO DEL MCP SERVER** - Este proyecto permite a las IAs construir aplicaciones Full-Stack (backend + frontend) de forma autónoma
6. **UTILIZAR AGENTES PARA MÁXIMA EFICIENCIA** - Usa agentes cuando sea necesario para garantizar eficiencia
7. **AUTODISEÑAR MECANISMOS PARA FALLAR MENOS** - Implementa validaciones, checks y mecanismos que reduzcan errores
8. **DISCUTIR IDEAS, PREGUNTAR SIEMPRE, NO ASUMIR, CONSULTAR TODO** - Pregunta cuando no entiendas algo. Ayuda a pensar, organizar ideas y materializarlas. NO asumas nada

### Propósito del Proyecto

**MCP Server que permite construir Apps Full-Stack (backend + frontend) con IAs de forma autónoma**

- **Entorno Objetivo:** Pod `archlinux-desktop-0` en namespace `cyberlab`
- **Cluster:** `AppToLastServer`
- **Sistema Base:** Arch Linux
- **Puertos Expuestos:** 3000 (HTTP/MCP), 3001 (Metrics)

---

## Project Overview

This is a production-ready **MCP (Model Context Protocol) Full-Stack Server** implemented in Kotlin, designed to enable AI agents to autonomously build and manage applications. The server provides a JSON-RPC 2.0 API with 6 core modules for filesystem operations, bash execution, GitHub integration, knowledge graph memory, and database connectivity (PostgreSQL and MongoDB).

**Technology Stack:**
- Kotlin 2.2.0 with JVM target (JDK 21+)
- Ktor 3.0.1 (lightweight, coroutine-based HTTP server)
- Gradle 8.10+ with Kotlin DSL
- Kotlin Coroutines for all async operations
- **MCP SDK:** `io.modelcontextprotocol:kotlin-sdk:0.6.0`
- Main class: `com.apptolast.mcp.ApplicationKt`

### Target Infrastructure

- **Pod:** `archlinux-desktop-0`
- **Namespace:** `cyberlab`
- **Cluster:** `AppToLastServer`
- **Base OS:** Arch Linux
- **Ports:** 3000 (MCP/HTTP), 3001 (Prometheus Metrics)
- **Services:** Expuestos en puertos 3000 y 3001

---

## Essential Commands

### Build
```bash
# Clean build (creates fat JAR in build/libs/mcp-fullstack-server-1.0.0.jar)
./gradlew clean build

# Build without tests
./gradlew build -x test
```

### Run
```bash
# Run with Gradle
./gradlew run

# Run JAR directly
java -jar build/libs/mcp-fullstack-server-1.0.0.jar

# Run with custom configuration
export MCP_HOST=0.0.0.0
export MCP_PORT=3000
./gradlew run
```

### Test
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "FilesystemModuleTest"

# Run with coverage report
./gradlew test jacocoTestReport
```

**Testing Stack:** JUnit 5 (Jupiter), Kotest 5.9.1 for assertions, MockK 1.13.12 for mocking

### Docker
```bash
# Build image
docker build -t mcp-fullstack-server -f docker/Dockerfile .

# Run container with volume mount
docker run -p 3000:3000 \
  -e MCP_WORKING_DIR=/workspace \
  -v $(pwd)/workspace:/workspace \
  mcp-fullstack-server
```

### Kubernetes
```bash
# Automated deployment (builds, dockerizes, deploys)
./deploy.sh

# Deploy with custom version and namespace
VERSION=1.0.0 NAMESPACE=cyberlab DEPLOY_TO_K8S=true ./deploy.sh

# Manual deployment
kubectl apply -f k8s/

# Port forward for local access
kubectl port-forward svc/mcp-fullstack-server 3000:3000 -n cyberlab

# View logs
kubectl logs -f deployment/mcp-fullstack-server -n cyberlab
```

---

## Architecture

### Core Design Principles

1. **Security-First Design**
   - Path traversal protection via real path resolution
   - Command whitelisting (no blacklisting)
   - Dangerous pattern detection (fork bombs, rm -rf /, sudo, etc.)
   - File size limits (10MB default)
   - Read-only database queries by design
   - Timeout protection (5 minutes default for commands)

2. **Module Independence**
   - Each module is self-contained with its own configuration
   - Can be enabled/disabled independently
   - Clear separation of concerns
   - Easy to test in isolation

3. **Async-First with Coroutines**
   - All I/O operations use Kotlin coroutines
   - Non-blocking request handling
   - Efficient resource usage

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       MCP Client (Claude/AI)                     │
│                    [JSON-RPC 2.0 Protocol]                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP/SSE/Stdio
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                  MCP Kotlin Server (Ktor)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐    │
│  │   Routing    │  │ Auth & Auth  │  │   Tool Registry   │    │
│  │   Layer      │  │   Middleware │  │                   │    │
│  └──────────────┘  └──────────────┘  └───────────────────┘    │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼───────┐  ┌───────▼────────┐
│   Filesystem   │  │   GitHub     │  │    Memory      │
│   Operations   │  │   Integration│  │  Knowledge     │
│   Module       │  │   Module     │  │    Graph       │
└────────┬───────┘  └──────┬───────┘  └───────┬────────┘
         │                  │                  │
┌────────▼────────┐  ┌─────▼────────┐  ┌─────▼────────┐
│  Bash Executor  │  │ Git Commands │  │ JSONL Storage│
│  (Secure Shell) │  │   & API      │  │              │
└─────────────────┘  └──────────────┘  └──────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼───────┐  ┌───────▼────────┐
│   PostgreSQL   │  │   MongoDB    │  │   Resources    │
│   Connector    │  │   Connector  │  │   Management   │
└────────────────┘  └──────────────┘  └────────────────┘
```

### Module Breakdown

**1. Filesystem Module** (`src/main/kotlin/com/apptolast/mcp/modules/filesystem/`)
- **Operations:** readFile, writeFile, listDirectory, createDirectory, deleteFile
- **Modes:** CREATE, OVERWRITE, APPEND for writes; recursive for directory listing
- **Security:** Allowed directory validation, file extension whitelist, size limits
- **Implementation Example:**
```kotlin
private fun validatePath(path: String): Result<Path> {
    val normalizedPath = Paths.get(path).normalize().toAbsolutePath()

    // Path traversal protection
    val isAllowed = allowedDirectories.any { allowedDir ->
        normalizedPath.startsWith(allowedDir)
    }

    if (!isAllowed) {
        return Result.failure(
            SecurityException("Access denied: path outside allowed directories")
        )
    }

    return Result.success(normalizedPath)
}
```

**2. Bash Execution Module** (`src/main/kotlin/com/apptolast/mcp/modules/bash/`)
- **Safe command execution** with argument and environment variable support
- **Security:** Command whitelist, dangerous pattern detection, working directory isolation
- **Default allowed commands:** ls, cat, grep, find, git, npm, gradle, docker, kubectl, curl, etc.
- **Dangerous patterns blocked:** Fork bombs, `rm -rf /`, `sudo`, `dd`, `mkfs`, `chmod 777`

**3. GitHub Integration Module** (`src/main/kotlin/com/apptolast/mcp/modules/github/`)
- **Operations:** status, commit, push, clone, log, branch management
- **Uses:** Eclipse JGit 6.10.0 for Git operations
- **Authentication:** Token-based via GITHUB_TOKEN env var

**4. Knowledge Graph Memory Module** (`src/main/kotlin/com/apptolast/mcp/modules/memory/`)
- **Operations:** createEntities, createRelations, searchNodes, openNodes
- **Storage:** JSONL-based (knowledge_graph.jsonl) for simplicity and portability
- **No database dependency** - suitable for MVP/initial version
- **Structure:**
```kotlin
data class Entity(
    val name: String,
    val entityType: String,
    val observations: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Relation(
    val from: String,
    val to: String,
    val relationType: String,
    val createdAt: Instant
)
```

**5. PostgreSQL Module** (`src/main/kotlin/com/apptolast/mcp/modules/database/`)
- **Operations:** executeQuery (read-only), getSchema, testConnection
- **Security:** Blocks INSERT/UPDATE/DELETE/DROP/CREATE/ALTER/TRUNCATE
- **Row limit:** 1000 by default
- **Validation:**
```kotlin
private fun isReadOnlyQuery(sql: String): Boolean {
    val normalized = sql.trim().uppercase()
    val readOnlyPatterns = listOf("SELECT", "SHOW", "DESCRIBE", "EXPLAIN")
    val writePatterns = listOf("INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER")

    return readOnlyPatterns.any { normalized.startsWith(it) } &&
           writePatterns.none { normalized.contains(it) }
}
```

**6. MongoDB Module** (`src/main/kotlin/com/apptolast/mcp/modules/database/`)
- **Operations:** find, listCollections, countDocuments, aggregate, testConnection
- **Uses:** MongoDB Kotlin Driver 5.2.0 with coroutine support

**7. Resource Management Module** (`src/main/kotlin/com/apptolast/mcp/modules/resources/`)
- **URI-based resource access:** resource://path/to/file
- **Operations:** listResources, readResource, createResource, deleteResource
- **MIME type detection** and markdown description support

### Key Files

- **Entry Point:** `src/main/kotlin/com/apptolast/mcp/Application.kt`
- **Tool Registry:** `src/main/kotlin/com/apptolast/mcp/server/ToolRegistry.kt` - Registra todos los tools disponibles
- **Configuration:** `src/main/kotlin/com/apptolast/mcp/server/ServerConfig.kt` (type-safe config classes)
- **Protocol Definitions:** `src/main/kotlin/com/apptolast/mcp/util/McpProtocol.kt` (JSON-RPC 2.0 models)
- **Config File:** `src/main/resources/application.conf` (HOCON format with environment overrides)
- **Logging Config:** `src/main/resources/logback.xml` (logs to console and `logs/mcp-server.log`)

### Server Endpoints

- `GET /` - Server information
- `GET /health` - Health check (returns "OK")
- `GET /ready` - Readiness check (returns "READY")
- `GET /info` - Server capabilities and version
- `GET /metrics` (port 3001) - Prometheus metrics

---

## Security Implementation

### Defense in Depth

El sistema implementa múltiples capas de seguridad:

#### 1. Path Validation
```kotlin
object PathValidator {
    fun validate(path: String, allowedRoots: List<Path>): Result<Path> {
        val normalized = Paths.get(path).normalize().toAbsolutePath()

        // Check for path traversal
        if (path.contains("..") || path.contains("./")) {
            return Result.failure(
                SecurityException("Path traversal detected")
            )
        }

        // Check if within allowed roots
        val isAllowed = allowedRoots.any { root ->
            try {
                normalized.startsWith(root.toRealPath())
            } catch (e: Exception) {
                false
            }
        }

        if (!isAllowed) {
            return Result.failure(
                SecurityException("Access denied: path outside allowed directories")
            )
        }

        return Result.success(normalized)
    }
}
```

#### 2. Command Validation
```kotlin
object CommandValidator {
    private val DANGEROUS_PATTERNS = listOf(
        Regex("rm\\s+-rf\\s+/"),
        Regex("dd\\s+if="),
        Regex(":(){ :|:& };:"),  // Fork bomb
        Regex("mkfs\\."),
        Regex("sudo"),
        Regex("su\\s+"),
        Regex("chmod\\s+777")
    )

    fun validate(command: String, allowedCommands: Set<String>): Result<String> {
        // Check if command is in allowed list
        val baseCommand = command.trim().split(" ").first()
        if (!allowedCommands.contains(baseCommand)) {
            return Result.failure(
                SecurityException("Command not allowed: $baseCommand")
            )
        }

        // Check for dangerous patterns
        DANGEROUS_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(command)) {
                return Result.failure(
                    SecurityException("Dangerous pattern detected in command")
                )
            }
        }

        return Result.success(command)
    }
}
```

#### 3. Input Sanitization
```kotlin
object InputSanitizer {
    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(255)
    }

    fun sanitizeSql(sql: String): Result<String> {
        val dangerous = listOf(
            "DROP", "DELETE", "TRUNCATE", "ALTER",
            "CREATE", "INSERT", "UPDATE", "GRANT", "REVOKE"
        )

        val upperSql = sql.uppercase()
        dangerous.forEach { keyword ->
            if (upperSql.contains(keyword)) {
                return Result.failure(
                    SecurityException("Dangerous SQL keyword detected: $keyword")
                )
            }
        }

        return Result.success(sql)
    }
}
```

#### 4. Audit Logging
```kotlin
@Serializable
data class AuditEvent(
    val timestamp: Instant,
    val eventType: AuditEventType,
    val clientId: String?,
    val tool: String,
    val action: String,
    val success: Boolean,
    val details: Map<String, String> = emptyMap(),
    val error: String? = null
)

enum class AuditEventType {
    FILESYSTEM_OPERATION,
    BASH_EXECUTION,
    DATABASE_QUERY,
    GITHUB_OPERATION,
    MEMORY_ACCESS,
    RESOURCE_ACCESS
}
```

#### 5. Rate Limiting
```kotlin
class RateLimiter(
    private val maxRequests: Int,
    private val windowSeconds: Long
) {
    private val requests = ConcurrentHashMap<String, MutableList<Instant>>()

    fun checkLimit(clientId: String): Boolean {
        val now = Clock.System.now()
        val windowStart = now.minus(windowSeconds.seconds)

        val clientRequests = requests.getOrPut(clientId) { mutableListOf() }

        // Remove old requests
        clientRequests.removeIf { it < windowStart }

        if (clientRequests.size >= maxRequests) {
            return false
        }

        clientRequests.add(now)
        return true
    }
}
```

---

## Configuration

### Critical Environment Variables

**Server:**
- `MCP_HOST` - Server host (default: 0.0.0.0)
- `MCP_PORT` - Server port (default: 3000)

**Filesystem:**
- `MCP_ALLOWED_DIRS` - Comma-separated allowed directories (default: /workspace, /home/claude/projects, /tmp/mcp-builds)
- `MCP_MAX_FILE_SIZE` - Max file size in bytes (default: 10485760 = 10MB)

**Bash:**
- `MCP_ALLOWED_COMMANDS` - Comma-separated allowed commands
- `MCP_WORKING_DIR` - Working directory (default: /workspace)
- `MCP_COMMAND_TIMEOUT` - Command timeout in seconds (default: 300)

**GitHub:**
- `MCP_REPO_PATH` - Path to git repository
- `GITHUB_TOKEN` - GitHub personal access token

**PostgreSQL:**
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

**MongoDB:**
- `MONGODB_CONNECTION_STRING`, `MONGODB_DB`

**Memory:**
- `MCP_MEMORY_PATH` - Path for knowledge graph storage (default: /workspace/.mcp-memory)

### Complete application.conf Structure

```hocon
server {
  host = "0.0.0.0"
  host = ${?MCP_HOST}

  port = 3000
  port = ${?MCP_PORT}
}

filesystem {
  allowedDirectories = [
    "/workspace",
    "/home/claude/projects",
    "/tmp/mcp-builds"
  ]
  allowedDirectories = ${?MCP_ALLOWED_DIRS}

  maxFileSize = 10485760  # 10MB
  maxFileSize = ${?MCP_MAX_FILE_SIZE}

  allowedExtensions = [
    "kt", "java", "js", "ts", "py", "go", "rs",
    "json", "yaml", "yml", "toml", "xml",
    "md", "txt", "html", "css", "scss",
    "sh", "bash", "zsh",
    "sql", "graphql",
    "Dockerfile", "Makefile"
  ]
}

bash {
  allowedCommands = [
    "ls", "cat", "grep", "find", "echo", "pwd",
    "mkdir", "touch", "mv", "cp", "rm",
    "git", "npm", "yarn", "pnpm",
    "gradle", "gradlew", "kotlinc", "javac",
    "docker", "docker-compose",
    "kubectl", "helm",
    "curl", "wget", "jq"
  ]
  allowedCommands = ${?MCP_ALLOWED_COMMANDS}

  workingDirectory = "/workspace"
  workingDirectory = ${?MCP_WORKING_DIR}

  timeoutSeconds = 300  # 5 minutes
  timeoutSeconds = ${?MCP_COMMAND_TIMEOUT}
}

github {
  repoPath = "/workspace/repo"
  repoPath = ${?MCP_REPO_PATH}

  token = ""
  token = ${?GITHUB_TOKEN}
}

memory {
  storagePath = "/workspace/.mcp-memory"
  storagePath = ${?MCP_MEMORY_PATH}
}

database {
  postgresql {
    host = "localhost"
    host = ${?POSTGRES_HOST}

    port = 5432
    port = ${?POSTGRES_PORT}

    database = "mcp_db"
    database = ${?POSTGRES_DB}

    username = "postgres"
    username = ${?POSTGRES_USER}

    password = ""
    password = ${?POSTGRES_PASSWORD}
  }

  mongodb {
    connectionString = "mongodb://localhost:27017"
    connectionString = ${?MONGODB_CONNECTION_STRING}

    database = "mcp_db"
    database = ${?MONGODB_DB}
  }
}

resources {
  path = "/workspace/resources"
  path = ${?MCP_RESOURCES_PATH}
}
```

### Default Security Settings

**Allowed File Extensions:**
kt, java, js, ts, py, go, rs, json, yaml, yml, toml, xml, md, txt, html, css, scss, sh, bash, zsh, sql, graphql, Dockerfile, Makefile, and files without extension

**Allowed Directories:**
- /workspace
- /home/claude/projects
- /tmp/mcp-builds

---

## Monitoring and Observability

### Prometheus Metrics

El servidor expone métricas en el puerto 3001:

```kotlin
object Metrics {
    // Tool invocation metrics
    val toolInvocations = Counter.build()
        .name("mcp_tool_invocations_total")
        .help("Total number of tool invocations")
        .labelNames("tool", "status")
        .register()

    val toolDuration = Histogram.build()
        .name("mcp_tool_duration_seconds")
        .help("Tool execution duration in seconds")
        .labelNames("tool")
        .buckets(0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)
        .register()

    // Filesystem metrics
    val filesystemOperations = Counter.build()
        .name("mcp_filesystem_operations_total")
        .help("Total filesystem operations")
        .labelNames("operation", "status")
        .register()

    // Bash execution metrics
    val bashExecutions = Counter.build()
        .name("mcp_bash_executions_total")
        .help("Total bash command executions")
        .labelNames("command", "status")
        .register()

    // Database metrics
    val databaseQueries = Counter.build()
        .name("mcp_database_queries_total")
        .help("Total database queries")
        .labelNames("database_type", "status")
        .register()

    // Knowledge Graph metrics
    val knowledgeGraphSize = Gauge.build()
        .name("mcp_knowledge_graph_entities")
        .help("Number of entities in knowledge graph")
        .register()

    // System metrics
    val activeConnections = Gauge.build()
        .name("mcp_active_connections")
        .help("Number of active MCP connections")
        .register()

    val errors = Counter.build()
        .name("mcp_errors_total")
        .help("Total number of errors")
        .labelNames("module", "error_type")
        .register()
}
```

### Health Checks

```kotlin
@Serializable
data class HealthStatus(
    val status: Status,
    val timestamp: Instant,
    val components: Map<String, ComponentHealth>
) {
    enum class Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY
    }
}

class HealthCheckService(
    private val components: List<HealthCheckComponent>
) {
    suspend fun check(): HealthStatus {
        val componentHealths = components.associate { component ->
            component.name to component.check()
        }

        val overallStatus = when {
            componentHealths.all { it.value.status == HealthStatus.Status.HEALTHY } ->
                HealthStatus.Status.HEALTHY
            componentHealths.any { it.value.status == HealthStatus.Status.UNHEALTHY } ->
                HealthStatus.Status.UNHEALTHY
            else ->
                HealthStatus.Status.DEGRADED
        }

        return HealthStatus(
            status = overallStatus,
            timestamp = Clock.System.now(),
            components = componentHealths
        )
    }
}
```

### Grafana Dashboard

Dashboards preconfigurados para:
- Tool Invocations Rate (por tool)
- Tool Duration p95 (percentile 95)
- Error Rate (por módulo y tipo)
- Knowledge Graph Size (gauge)
- Active Connections (gauge)

---

## Usage Examples

### Crear Aplicación Spring Boot Desde Cero

**Prompt a la IA:**
```
Crea una aplicación Spring Boot REST API para gestionar tareas (TODO app)
con las siguientes características:
- CRUD completo de tareas
- PostgreSQL como base de datos
- Docker Compose para desarrollo local
- Tests unitarios e integración
- Dockerfile para producción
- README con instrucciones
```

**Secuencia de Tools invocados:**
1. `create_directory` - Crear estructura del proyecto
2. `write_file` - Generar `build.gradle.kts`
3. `write_file` - Crear `Application.kt`
4. `write_file` - Crear `TaskController.kt`
5. `write_file` - Crear `TaskService.kt`
6. `write_file` - Crear `TaskRepository.kt`
7. `write_file` - Crear `Task.kt` (Entity)
8. `write_file` - Crear `application.yml`
9. `write_file` - Crear `Dockerfile`
10. `write_file` - Crear `docker-compose.yml`
11. `write_file` - Crear tests
12. `bash_execute` - `gradle build`
13. `bash_execute` - `docker-compose up -d`
14. `postgres_query` - Verificar conexión a BD
15. `git_commit` - Commit inicial
16. `write_file` - Generar `README.md`
17. `create_entities` - Guardar metadata en Knowledge Graph

### Depurar y Mejorar Aplicación Existente

**Prompt a la IA:**
```
Analiza el proyecto en /workspace/my-app y:
1. Identifica problemas de performance
2. Sugiere mejoras de arquitectura
3. Actualiza dependencias obsoletas
4. Añade tests faltantes
5. Documenta cambios en el Knowledge Graph
```

**Secuencia de Tools:**
1. `list_directory` - Explorar estructura
2. `read_file` - Leer `build.gradle.kts`
3. `read_file` - Leer código fuente
4. `postgres_query` - Analizar queries lentas
5. `search_nodes` - Buscar contexto previo en memoria
6. `bash_execute` - `gradle dependencies`
7. `write_file` - Actualizar dependencias
8. `write_file` - Refactorizar código
9. `write_file` - Añadir tests
10. `bash_execute` - `gradle test`
11. `git_commit` - Commit de mejoras
12. `create_relations` - Relacionar cambios en Knowledge Graph

---

## Important Architectural Decisions

### 1. Ktor vs Spring Boot
**Decision:** Use Ktor for HTTP server
**Rationale:** Lightweight, Kotlin-first design, native coroutine support, no unnecessary overhead

### 2. JSONL for Knowledge Graph Storage
**Decision:** Use JSONL instead of a graph database
**Rationale:** Simple, portable, line-by-line parsing, easy backup/restore, no database dependency - suitable for MVP

### 3. Read-Only Database Queries
**Decision:** PostgreSQL module enforces read-only queries
**Rationale:** Safety-first approach - prevents accidental data modification or deletion through the MCP interface

### 4. Multi-stage Docker Build
**Decision:** Separate build and runtime stages
**Rationale:** Smaller final image (Alpine-based JRE), faster deployments, better security (build tools not in production)

### 5. Configuration via HOCON + Environment Variables
**Decision:** Typesafe Config (HOCON) with environment variable overrides
**Rationale:** Type-safe configuration, clear defaults in code, easy Docker/K8s integration, follows 12-factor app principles

---

## Development Setup

### Prerequisites
- JDK 21 or higher
- Gradle 8.10+ (wrapper included)
- Docker (optional, for containerization)
- Kubernetes cluster (optional, for deployment)

### Local Development
1. Clone repository
2. Build: `./gradlew build`
3. Run tests: `./gradlew test`
4. Start server: `./gradlew run`
5. Access at http://localhost:3000

### IDE Configuration
- **Main Class:** `com.apptolast.mcp.ApplicationKt`
- **Working Directory:** Project root
- **VM Options:** `-Xmx2048m` (as per gradle.properties)

### Best Practices for AI Development

#### Tool Registry Pattern
All AI-accessible tools are registered in `ToolRegistry.kt`:
- FilesystemTools: readFile, writeFile, listDirectory, etc.
- BashTools: executeCommand
- GitHubTools: status, commit, push, clone
- MemoryTools: createEntities, searchNodes
- DatabaseTools: executeQuery, getSchema
- ResourceTools: listResources, readResource

#### Security-First Development
Every tool call MUST:
1. Validate inputs (paths, commands, queries)
2. Check permissions (allowlists, whitelists)
3. Log to audit trail
4. Handle errors gracefully
5. Return structured results

#### Knowledge Graph Usage
Store project context in the knowledge graph:
- **Entities:** Projects, Files, Dependencies, Technologies
- **Relations:** uses, dependsOn, implements, extends
- **Observations:** Decisions made, patterns used, issues resolved

---

## Deployment

### Docker Deployment
The multi-stage Dockerfile (`docker/Dockerfile`) creates a minimal Alpine-based image:
- Build stage uses gradle:8.10-jdk21
- Runtime stage uses eclipse-temurin:21-jre-alpine
- Runs as non-root user (mcp, uid 1000)
- Includes tools: bash, git, curl, jq, postgresql-client, mongodb-tools

### Kubernetes Deployment
Manifests in `k8s/`:
- **deployment.yaml** - Deployment + PersistentVolumeClaim (10Gi)
- **service.yaml** - ClusterIP and LoadBalancer services
- **configmap.yaml** - Configuration data
- **secrets.yaml** - Secret data (template)
- **rbac.yaml** - ServiceAccount and permissions
- **hpa.yaml** - HorizontalPodAutoscaler (1-5 replicas)

**Resource Limits:**
- Requests: 500m CPU, 512Mi memory
- Limits: 2000m CPU, 2Gi memory

**Health Probes:** Configured for `/health` (liveness) and `/ready` (readiness)

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mcp-server-hpa
  namespace: cyberlab
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mcp-fullstack-server
  minReplicas: 1
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Automated Deployment
The `deploy.sh` script automates:
1. Building the application
2. Creating Docker image
3. Optional registry push
4. Optional K8s deployment

Usage: `VERSION=1.0.0 NAMESPACE=cyberlab DEPLOY_TO_K8S=true ./deploy.sh`

---

## Backup and Disaster Recovery

### Backup Script

```bash
#!/bin/bash
# backup-mcp-data.sh

NAMESPACE="cyberlab"
BACKUP_DIR="/backup/mcp/$(date +%Y%m%d_%H%M%S)"

mkdir -p ${BACKUP_DIR}

# Backup Knowledge Graph
kubectl exec -n ${NAMESPACE} deployment/mcp-fullstack-server -- \
  tar czf - /workspace/.mcp-memory | \
  cat > ${BACKUP_DIR}/knowledge-graph.tar.gz

# Backup workspace
kubectl exec -n ${NAMESPACE} deployment/mcp-fullstack-server -- \
  tar czf - /workspace/projects | \
  cat > ${BACKUP_DIR}/workspace.tar.gz

# Backup PostgreSQL
kubectl exec -n ${NAMESPACE} deployment/postgres -- \
  pg_dump -U mcp_user mcp_applications | \
  gzip > ${BACKUP_DIR}/postgres.sql.gz

# Backup MongoDB
kubectl exec -n ${NAMESPACE} deployment/mongodb -- \
  mongodump --archive | \
  gzip > ${BACKUP_DIR}/mongodb.archive.gz

echo "✅ Backup completed: ${BACKUP_DIR}"
```

### Recovery Procedures

1. **Knowledge Graph Recovery:**
   ```bash
   kubectl cp ${BACKUP_DIR}/knowledge-graph.tar.gz \
     cyberlab/mcp-fullstack-server:/tmp/
   kubectl exec -n cyberlab deployment/mcp-fullstack-server -- \
     tar xzf /tmp/knowledge-graph.tar.gz -C /workspace/.mcp-memory
   ```

2. **Database Recovery:**
   ```bash
   gunzip < ${BACKUP_DIR}/postgres.sql.gz | \
     kubectl exec -i -n cyberlab deployment/postgres -- \
     psql -U mcp_user mcp_applications
   ```

---

## CI/CD Pipeline

### GitHub Actions Workflow

Archivo `.github/workflows/ci-cd.yml`:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

    - name: Run tests
      run: ./gradlew test

    - name: Run integration tests
      run: ./gradlew integrationTest

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Build with Gradle
      run: ./gradlew build -x test

    - name: Build Docker image
      run: |
        docker build -t apptolast.com/mcp-fullstack-server:${{ github.sha }} .
        docker tag apptolast.com/mcp-fullstack-server:${{ github.sha }} \
                   apptolast.com/mcp-fullstack-server:latest

    - name: Push to registry
      run: |
        echo ${{ secrets.REGISTRY_PASSWORD }} | docker login apptolast.com -u ${{ secrets.REGISTRY_USER }} --password-stdin
        docker push apptolast.com/mcp-fullstack-server:${{ github.sha }}
        docker push apptolast.com/mcp-fullstack-server:latest

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
    - uses: actions/checkout@v3

    - name: Set up kubectl
      uses: azure/setup-kubectl@v3

    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/mcp-fullstack-server \
          mcp-server=apptolast.com/mcp-fullstack-server:${{ github.sha }} \
          -n cyberlab

        kubectl rollout status deployment/mcp-fullstack-server -n cyberlab
```

---

## Implementation Roadmap

### Phase 1: Foundations (Weeks 1-2)
**Objectives:**
- ✅ Set up Kotlin project with Gradle
- ✅ Implement MCP Server core
- ✅ Develop Filesystem module
- ✅ Develop Bash Execution module
- ✅ Implement basic security system

**Deliverables:** Compilable Kotlin project, Functional MCP Server with Stdio transport, 2 operational modules, Basic unit tests

### Phase 2: Git and Memory Integration (Weeks 3-4)
**Objectives:**
- ✅ Implement GitHub Integration module
- ✅ Develop Knowledge Graph Memory
- ✅ Add Resources module
- ✅ Implement audit logging

**Deliverables:** Fully functional Git module, Persistent memory system, Custom documentation system, Audit logs

### Phase 3: Databases (Weeks 5-6)
**Objectives:**
- ✅ Implement PostgreSQL connector
- ✅ Implement MongoDB connector
- ✅ Implement MySQL connector (optional)
- ✅ Add query validation and sanitization

**Deliverables:** 3 functional DB connectors, SQL validation system, Integration tests with real DBs

### Phase 4: Containerization and K8s (Week 7)
**Objectives:**
- ✅ Create optimized Dockerfile
- ✅ Configure Kubernetes deployment
- ✅ Implement health checks
- ✅ Configure persistent volumes

**Deliverables:** Functional Docker image, Complete K8s manifests, Deployment on archlinux-desktop-0 pod

### Phase 5: Observability and Testing (Week 8)
**Objectives:**
- ✅ Integrate Prometheus metrics
- ✅ Configure Grafana dashboards
- ✅ Implement end-to-end tests
- ✅ Performance optimization

**Deliverables:** Complete metrics system, Monitoring dashboards, E2E test suite, Complete technical documentation

### Phase 6: Hardening and Production (Weeks 9-10)
**Objectives:**
- ✅ Complete security audit
- ✅ Performance tuning
- ✅ Disaster recovery plan
- ✅ Operations documentation

**Deliverables:** Production-ready system, Operational runbooks, Backup/restore plan, Troubleshooting manual

---

## Troubleshooting

### Common Issues

**JDK Version Mismatch**
- Ensure JDK 21+ is installed: `java -version`
- Gradle wrapper uses configured JDK

**Port Already in Use**
- Change port: `export MCP_PORT=3001`
- Check running processes: `lsof -i :3000`

**Database Connection Failures**
- Verify connection strings and credentials
- Use `testConnection()` operations to debug
- Check PostgreSQL/MongoDB are running and accessible
- Verify network connectivity in Kubernetes: `kubectl get svc -n cyberlab`

**File Permission Errors**
- Ensure MCP_ALLOWED_DIRS includes the target directory
- Check file/directory permissions
- Verify Docker volume mounts are correct
- Check pod user permissions: Pod runs as user `mcp` (uid 1000)

**Command Execution Blocked**
- Check if command is in MCP_ALLOWED_COMMANDS
- Review dangerous pattern detection in BashExecutor.kt:2007-2037
- Ensure working directory is valid

**Path Traversal Errors**
- All paths must be within allowed directories
- Path normalization is enforced: `Paths.get(path).normalize().toAbsolutePath()`
- Cannot use `..` or `./` in paths

**Security Validation Failures**
- SQL queries must start with SELECT/SHOW/DESCRIBE/EXPLAIN
- No write operations (INSERT/UPDATE/DELETE) allowed in PostgreSQL module
- File size must be under 10MB by default
- Only whitelisted file extensions allowed

**Knowledge Graph Issues**
- JSONL file location: `/workspace/.mcp-memory/knowledge_graph.jsonl`
- Each line must be valid JSON with `type` field (`entity` or `relation`)
- File must be readable/writable by pod user

**Kubernetes Deployment Issues**
- Check pod status: `kubectl get pods -n cyberlab -l app=mcp-server`
- View pod logs: `kubectl logs -f deployment/mcp-fullstack-server -n cyberlab`
- Check events: `kubectl get events -n cyberlab --sort-by='.lastTimestamp'`
- Verify PVCs are bound: `kubectl get pvc -n cyberlab`

### Logs
- Console output during development
- File logs: `logs/mcp-server.log` (30-day rolling policy)
- Kubernetes: `kubectl logs -f deployment/mcp-fullstack-server -n cyberlab`
- Audit logs: Check AuditLogger output in application logs

---

## Known Limitations / Future Work

Based on IMPLEMENTATION.md and the technical specification, the following are planned but not yet fully implemented:

1. **Full MCP JSON-RPC Protocol Handler** - Complete request/response routing
2. **Comprehensive Audit Logging** - Enhanced security event tracking and persistence
3. **Advanced Metrics & Monitoring** - Full Prometheus integration with custom metrics
4. **Rate Limiting Middleware** - Request throttling per client
5. **Authentication Middleware** - User authentication and authorization
6. **Multi-tenancy Support** - Isolated environments per tenant
7. **MySQL Connector** - Third database connector (driver already in dependencies)
8. **Complete CI/CD Pipeline** - Full GitHub Actions workflows
9. **Tool Marketplace** - Extensible tool system for custom tools
10. **Advanced ML Features** - Knowledge Graph enhancements with machine learning

---

## Appendices

### A. Glossary of Terms

- **MCP** - Model Context Protocol: Protocol for AI-tool communication
- **Tool** - Function that AI can invoke to perform operations
- **Resource** - Content that AI can read (documentation, files)
- **Prompt** - Interaction template for AI
- **Knowledge Graph** - Graph of entities and relations for persistent memory
- **Transport** - Communication mechanism (Stdio, HTTP, SSE)
- **JSONL** - JSON Lines: One JSON object per line format
- **Tool Registry** - Central registration point for all available tools
- **Audit Event** - Logged security event with timestamp and details

### B. FAQ

**Q: Can the AI execute dangerous commands?**
A: No. All commands go through strict validation with a whitelist. Dangerous patterns like fork bombs, `rm -rf /`, `sudo`, etc. are blocked.

**Q: How does the system scale?**
A: Using Kubernetes HPA based on CPU/memory metrics. Can scale from 1 to 5 replicas automatically.

**Q: Does data persist between restarts?**
A: Yes, using PersistentVolumeClaims in Kubernetes. Knowledge Graph, workspace, and databases all persist.

**Q: What happens if the server fails?**
A: Kubernetes automatically restarts the pod. Data persists in PVCs. Health probes detect failures quickly.

**Q: Can I add custom tools?**
A: Yes, implement the tool interface and register it in ToolRegistry.kt. Follow the security patterns.

**Q: How do I connect Claude Desktop?**
A: Use kubectl port-forward or configure the MCP server endpoint in Claude Desktop settings.

### C. Security Checklist

Before deployment, verify:
- [ ] All file paths validated against allowed directories
- [ ] All bash commands validated against whitelist
- [ ] All SQL queries validated for read-only operations
- [ ] Dangerous patterns checked in all user inputs
- [ ] Audit logging enabled for all operations
- [ ] Rate limiting configured per client
- [ ] Secrets stored in Kubernetes Secrets, never in code
- [ ] Docker image runs as non-root user
- [ ] RBAC configured with minimal permissions
- [ ] Health checks configured
- [ ] Backup procedures tested

### D. Additional Resources

- **MCP Documentation:** https://modelcontextprotocol.io
- **Kotlin SDK:** https://github.com/modelcontextprotocol/kotlin-sdk
- **Servers Repository:** https://github.com/modelcontextprotocol/servers
- **Community Discussions:** https://github.com/orgs/modelcontextprotocol/discussions
- **Kotlin Documentation:** https://kotlinlang.org/docs/
- **Ktor Documentation:** https://ktor.io/docs/
- **Kubernetes Documentation:** https://kubernetes.io/docs/

---

**Documento actualizado:** 15 de Noviembre de 2025
**Versión:** 1.0
**Proyecto:** MCP Full-Stack Server en Kotlin para construcción autónoma de aplicaciones con IAs
