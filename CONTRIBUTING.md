# Contributing to MCP Full-Stack Server

Thank you for your interest in contributing to the MCP Full-Stack Server project!

## Development Setup

### Prerequisites

- JDK 21 or higher
- Gradle 8.10 or higher
- Docker (for containerization)
- Kubernetes cluster (optional, for deployment testing)

### Getting Started

1. Clone the repository:
```bash
git clone https://github.com/apptolast/McpServerKotlin.git
cd McpServerKotlin
```

2. Build the project:
```bash
./gradlew build
```

3. Run tests:
```bash
./gradlew test
```

4. Run the server locally:
```bash
./gradlew run
```

## Project Structure

- `src/main/kotlin/com/apptolast/mcp/` - Main application code
  - `Application.kt` - Main entry point
  - `server/` - Server configuration
  - `modules/` - Core modules (filesystem, bash, github, memory, database, resources)
  - `util/` - Utilities and protocol definitions
- `src/main/resources/` - Configuration files
- `src/test/` - Test code
- `docker/` - Docker configuration
- `k8s/` - Kubernetes manifests

## How to Contribute

### Reporting Bugs

- Use the GitHub issue tracker
- Include steps to reproduce
- Include expected vs actual behavior
- Include relevant logs and stack traces

### Suggesting Features

- Check if the feature is already in the roadmap
- Create a detailed proposal
- Explain the use case and benefits

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass (`./gradlew test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Use coroutines for async operations

### Testing

- Write tests for all new functionality
- Ensure test coverage for edge cases
- Use descriptive test names
- Mock external dependencies when appropriate

## Module Development

### Adding a New Module

1. Create a new package under `modules/`
2. Implement the module class with appropriate security checks
3. Add configuration to `ServerConfig.kt`
4. Add configuration properties to `application.conf`
5. Write comprehensive tests
6. Update documentation

### Security Guidelines

All modules must implement:

- Input validation
- Path/command validation
- Size/timeout limits
- Error handling
- Audit logging

### Example Module Structure

```kotlin
class MyModule(
    private val config: MyConfig
) {
    suspend fun myOperation(params: ...): ToolResult {
        return try {
            // Validate inputs
            validateInput(params)
            
            // Perform operation
            val result = performOperation(params)
            
            // Return success
            ToolResult.success(result)
        } catch (e: Exception) {
            logger.error(e) { "Operation failed" }
            ToolResult.error("Operation failed: ${e.message}")
        }
    }
}
```

## Release Process

1. Update version in `build.gradle.kts`
2. Update CHANGELOG.md
3. Create a new tag
4. Build and test
5. Create GitHub release
6. Build and push Docker image
7. Deploy to staging
8. Deploy to production

## Questions?

- Check the README.md
- Review the technical specification document
- Open a discussion on GitHub
- Contact the maintainers

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
