# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-15

### Added

- Initial implementation of MCP Full-Stack Server
- Filesystem Operations Module with security validation
  - Read, write, create, delete files and directories
  - Path traversal protection
  - File size and extension limits
- Bash Execution Module with command whitelisting
  - Safe command execution
  - Dangerous pattern detection
  - Timeout protection
- Knowledge Graph Memory Module
  - Entity and relation management
  - JSONL-based persistent storage
  - Graph search capabilities
- GitHub Integration Module
  - Git operations (status, commit, push, clone, log, branch)
  - Authentication support via tokens
- Database Connector Modules
  - PostgreSQL module with read-only query support
  - MongoDB module with find, aggregate, and list operations
  - Connection testing and schema introspection
- Resource Management Module
  - Custom documentation management
  - Binary and text resource support
  - URI-based resource access
- Ktor-based HTTP server
  - Health and readiness endpoints
  - Configuration via environment variables
- Docker containerization
  - Multi-stage build for optimized images
  - Alpine-based runtime
- Kubernetes deployment manifests
  - Deployment, Service, ConfigMap, Secrets
  - RBAC configuration
  - PersistentVolumeClaims for data storage
- Comprehensive documentation
  - README with quick start guide
  - Technical specification document
  - Contributing guidelines
  - API documentation
- Test infrastructure
  - Unit tests for filesystem module
  - JUnit 5 and Kotest integration
- Deployment script for automated deployment

### Security

- Path validation and traversal protection
- Command whitelisting and validation
- Dangerous pattern detection in bash commands
- File size limits (10MB default)
- File extension filtering
- Execution timeouts
- Working directory isolation
- Read-only database queries for safety

## [Unreleased]

### Planned

- Complete MCP JSON-RPC protocol implementation
- Audit logging system
- Prometheus metrics integration
- Grafana dashboards
- Additional database connectors (MySQL)
- Enhanced error handling and validation
- Performance optimizations
- Additional integration tests
- CI/CD pipeline configuration
