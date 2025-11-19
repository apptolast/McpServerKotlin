# MCP Full-Stack Server

An MCP (Model Context Protocol) Full-Stack Server implemented in Kotlin that enables AI agents to autonomously build and manage applications.

## Overview

This server implements the MCP protocol to provide AI agents with powerful capabilities:

- **Filesystem Operations**: Secure file reading, writing, and directory management
- **Bash Execution**: Safe command execution with whitelisting and validation
- **GitHub Integration**: Version control operations (git commands)
- **Knowledge Graph Memory**: Persistent memory system for AI context
- **Database Connectors**: PostgreSQL, MongoDB, and MySQL integration
- **Resource Management**: Custom documentation and resource handling

## Features

✅ **Security First**
- Path traversal protection
- Command whitelisting
- File size and extension validation
- Dangerous pattern detection

✅ **Kubernetes Ready**
- Docker support
- Kubernetes manifests included
- Health checks and readiness probes
- Resource limits and requests

✅ **Observable**
- Structured logging
- Health and readiness endpoints
- Metrics support (planned)

✅ **CI/CD Ready**
- Automated Docker builds with GitHub Actions
- Docker Hub integration
- Multi-branch deployment support (main/develop)
- Kubernetes deployment automation

## Quick Start

### Prerequisites

- JDK 21+
- Gradle 8.10+
- Kotlin 2.2.0+

### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew run
```

Or with custom configuration:

```bash
export MCP_HOST=0.0.0.0
export MCP_PORT=3000
export MCP_WORKING_DIR=/workspace
./gradlew run
```

### Running with Docker

```bash
docker build -t mcp-fullstack-server -f docker/Dockerfile .
docker run -p 3000:3000 mcp-fullstack-server
```

## Configuration

The server is configured via `src/main/resources/application.conf`. You can override settings using environment variables:

### Server Configuration
- `MCP_HOST`: Server host (default: 0.0.0.0)
- `MCP_PORT`: Server port (default: 3000)

### Filesystem Configuration
- `MCP_ALLOWED_DIRS`: Comma-separated list of allowed directories
- `MCP_MAX_FILE_SIZE`: Maximum file size in bytes (default: 10MB)

### Bash Configuration
- `MCP_ALLOWED_COMMANDS`: Comma-separated list of allowed commands
- `MCP_WORKING_DIR`: Working directory for bash commands
- `MCP_COMMAND_TIMEOUT`: Command timeout in seconds (default: 300)

### GitHub Configuration
- `MCP_REPO_PATH`: Path to git repository
- `GITHUB_TOKEN`: GitHub personal access token

### Database Configuration
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `MONGODB_CONNECTION_STRING`, `MONGODB_DB`

### Memory Configuration
- `MCP_MEMORY_PATH`: Path for knowledge graph storage

## Project Structure

```
mcp-fullstack-server/
├── src/main/kotlin/com/apptolast/mcp/
│   ├── Application.kt              # Main application entry point
│   ├── server/
│   │   └── ServerConfig.kt         # Configuration management
│   ├── modules/
│   │   ├── filesystem/             # Filesystem operations
│   │   ├── bash/                   # Bash command execution
│   │   ├── github/                 # Git/GitHub integration
│   │   ├── memory/                 # Knowledge graph memory
│   │   ├── database/               # Database connectors
│   │   └── resources/              # Resource management
│   ├── transport/                  # MCP transport layers
│   ├── security/                   # Security validation
│   └── util/                       # Utilities and protocol definitions
├── src/main/resources/
│   ├── application.conf            # Application configuration
│   └── logback.xml                 # Logging configuration
├── docker/
│   └── Dockerfile                  # Docker image definition
└── k8s/                           # Kubernetes manifests
```

## API Endpoints

### Health and Status
- `GET /`: Server information
- `GET /health`: Health check
- `GET /ready`: Readiness check
- `GET /info`: Server capabilities and version

## Modules

### Filesystem Module
Provides secure filesystem operations with:
- Path validation and traversal protection
- File size limits
- Extension whitelisting
- Read, write, list, create, and delete operations

### Bash Executor Module
Safe command execution with:
- Command whitelisting
- Dangerous pattern detection
- Timeout protection
- Working directory isolation

### Memory Module
Knowledge graph for persistent AI memory:
- Entity creation and storage
- Relationship management
- Graph search and traversal
- JSONL-based storage

## Security

The server implements multiple security layers:

1. **Path Validation**: All filesystem operations validate paths against allowed directories
2. **Command Whitelisting**: Only explicitly allowed commands can be executed
3. **Dangerous Pattern Detection**: Blocks known dangerous command patterns
4. **File Size Limits**: Prevents large file operations
5. **Extension Filtering**: Only allowed file extensions can be accessed
6. **Timeout Protection**: Commands have execution time limits

## Development

### Running Tests

```bash
./gradlew test
```

### Code Style

This project follows Kotlin coding conventions. Format code with:

```bash
./gradlew ktlintFormat
```

## Deployment

### Docker

The project includes automated CI/CD with GitHub Actions:

```bash
# Automated deployment (triggered on push to main/develop)
git push origin main
```

See `.github/QUICKSTART.md` for complete CI/CD documentation.

### Manual Docker Build

```bash
docker build -t mcp-fullstack-server -f docker/Dockerfile .
docker run -p 3000:3000 mcp-fullstack-server
```

### Kubernetes

Apply the Kubernetes manifests:

```bash
kubectl apply -f k8s/
```

This will create:
- Deployment with the MCP server
- Service for internal/external access
- ConfigMap for configuration
- Secrets for sensitive data
- PersistentVolumeClaims for data storage

For automated Kubernetes updates after CI/CD builds, see `.github/workflows/README.md`.

## License

See LICENSE file for details.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues and questions:
- Create an issue on GitHub
- Check the technical specification document

## Roadmap

See the technical specification document for the complete implementation roadmap covering:
- Phase 1: Foundations (Filesystem, Bash)
- Phase 2: Git and Memory Integration
- Phase 3: Database Connectors
- Phase 4: Containerization and K8s
- Phase 5: Observability and Testing
- Phase 6: Hardening and Production

## Technical Specification

For detailed technical information, architecture, and implementation details, see `McpServerTecnicalDocument.txt`.
