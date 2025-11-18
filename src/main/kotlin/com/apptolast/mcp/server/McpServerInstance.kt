package com.apptolast.mcp.server

import com.apptolast.mcp.modules.bash.BashExecutor
import com.apptolast.mcp.modules.database.MongoDBModule
import com.apptolast.mcp.modules.database.PostgreSQLModule
import com.apptolast.mcp.modules.filesystem.FilesystemModule
import com.apptolast.mcp.modules.github.GitHubModule
import com.apptolast.mcp.modules.memory.MemoryModule
import com.apptolast.mcp.modules.resources.ResourceModule
import com.apptolast.mcp.server.registration.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.Role
import kotlinx.coroutines.flow.MutableStateFlow

private val logger = KotlinLogging.logger {}

/**
 * MCP Server instance using the official MCP Kotlin SDK
 *
 * This server exposes 28+ tools across 7 modules:
 * - Filesystem (5 tools): readFile, writeFile, listDirectory, createDirectory, deleteFile
 * - Bash (1 tool): bashExecute
 * - GitHub (6 tools): gitStatus, gitCommit, gitPush, gitClone, gitLog, gitBranch
 * - Memory (4 tools): createEntities, createRelations, searchNodes, openNodes
 * - PostgreSQL (3 tools): postgresQuery, postgresGetSchema, postgresTestConnection
 * - MongoDB (5 tools): mongoFind, mongoListCollections, mongoCount, mongoAggregate, mongoTestConnection
 * - Resources (4 tools): resourcesList, resourcesRead, resourcesCreate, resourcesDelete
 */
class McpServerInstance(private val config: ServerConfig) {

    // Initialize all modules
    val filesystemModule = FilesystemModule(config.filesystem)
    val bashExecutor = BashExecutor(config.bash)
    val githubModule = GitHubModule(config.github)
    val memoryModule = MemoryModule(config.memory)
    val postgresModule = PostgreSQLModule(config.database.postgresql)
    val mongoModule = MongoDBModule(config.database.mongodb)
    val resourceModule = ResourceModule(config.resources)

    // MCP Server capabilities
    private val capabilities = ServerCapabilities(
        tools = ServerCapabilities.Tools(
            listChanged = true  // Server can notify when tools change
        ),
        resources = ServerCapabilities.Resources(
            subscribe = true,  // Clients can subscribe to resource changes
            listChanged = true  // Server can notify when resources change
        ),
        prompts = ServerCapabilities.Prompts(
            listChanged = false  // Prompts are static for now
        )
        // Note: Logging capability removed - not available in SDK 0.7.7
    )

    // Server information
    private val serverInfo = Implementation(
        name = "mcp-fullstack-server",
        version = "1.0.0"
    )

    // Create the MCP server using the official SDK
    val server = Server(
        serverInfo = serverInfo,
        options = ServerOptions(capabilities = capabilities)
    )

    /**
     * Register all 28+ tools with the MCP server
     *
     * This calls extension functions defined in the registration/ package
     * Each module has its own registration file that implements the actual tool registration
     */
    suspend fun registerAllTools() {
        logger.info { "Registering tools with MCP server..." }

        // Import the extension functions from registration package
        // These are defined in:
        // - com.apptolast.mcp.server.registration.FilesystemToolsRegistration
        // - com.apptolast.mcp.server.registration.BashToolsRegistration
        // - com.apptolast.mcp.server.registration.GitHubToolsRegistration
        // - com.apptolast.mcp.server.registration.MemoryToolsRegistration
        // - com.apptolast.mcp.server.registration.DatabaseToolsRegistration
        // - com.apptolast.mcp.server.registration.ResourceToolsRegistration

        // Register tools from each module (these are extension functions)
        this.registerFilesystemTools()      // 5 tools
        this.registerBashTools()            // 1 tool
        this.registerGitHubTools()          // 6 tools
        this.registerMemoryTools()          // 4 tools
        this.registerDatabaseTools()        // 8 tools (3 PostgreSQL + 5 MongoDB)
        this.registerResourceTools()        // 4 tools

        logger.info { "Successfully registered all 28 tools" }
    }

    /**
     * Register all resources available in the resource module
     */
    suspend fun registerAllResources() {
        logger.info { "Registering resources with MCP server..." }

        val resources = resourceModule.listResources()

        // TODO: Register each resource with server.addResource()
        // This requires accessing the resource content from the ToolResult

        logger.info { "Successfully registered ${resources.content.size} resources" }
    }

    /**
     * Register prompt templates (optional enhancement)
     *
     * NOTE: Temporarily disabled due to Role enum uncertainty
     */
    suspend fun registerPrompts() {
        logger.info { "Prompts registration skipped (Role enum values unknown)" }
        return

        // DISABLED CODE BELOW - Role enum values need verification
        /*
        logger.info { "Registering prompts with MCP server..." }

        // Example prompt for code review
        server.addPrompt(
            name = "code-review",
            description = "Review code for quality, security, and best practices",
            arguments = listOf(
                PromptArgument(
                    name = "language",
                    description = "Programming language of the code",
                    required = true
                ),
                PromptArgument(
                    name = "code",
                    description = "The code to review",
                    required = true
                )
            )
        ) { request: GetPromptRequest ->
            val language = request.arguments?.get("language") ?: "unknown"
            val code = request.arguments?.get("code") ?: ""

            GetPromptResult(
                description = "Code review for $language",
                messages = listOf(
                    PromptMessage(
                        role = Role.USER,
                        content = TextContent(
                            text = """
                            Please review this $language code for:
                            1. Security vulnerabilities (SQL injection, XSS, path traversal, etc.)
                            2. Code quality and maintainability
                            3. Best practices and design patterns
                            4. Performance considerations
                            5. Error handling

                            Code:
                            ```$language
                            $code
                            ```

                            Provide specific, actionable feedback.
                            """.trimIndent()
                        )
                    )
                )
            )
        }

        // Example prompt for test generation
        server.addPrompt(
            name = "generate-tests",
            description = "Generate unit tests for code",
            arguments = listOf(
                PromptArgument(
                    name = "language",
                    description = "Programming language",
                    required = true
                ),
                PromptArgument(
                    name = "code",
                    description = "Code to generate tests for",
                    required = true
                ),
                PromptArgument(
                    name = "framework",
                    description = "Testing framework to use",
                    required = false
                )
            )
        ) { request: GetPromptRequest ->
            val language = request.arguments?.get("language") ?: "unknown"
            val code = request.arguments?.get("code") ?: ""
            val framework = request.arguments?.get("framework") ?: "standard testing framework"

            GetPromptResult(
                description = "Generate tests for $language code",
                messages = listOf(
                    PromptMessage(
                        role = Role.USER,
                        content = TextContent(
                            text = """
                            Generate comprehensive unit tests for this $language code using $framework.

                            Code:
                            ```$language
                            $code
                            ```

                            Include:
                            - Happy path tests
                            - Edge case tests
                            - Error handling tests
                            - Mock setup if needed
                            """.trimIndent()
                        )
                    )
                )
            )
        }

        logger.info { "Successfully registered prompts" }
        */
    }

    /**
     * Note: The actual tool registration implementations are in separate files:
     * - FilesystemToolsRegistration.kt
     * - BashToolsRegistration.kt
     * - GitHubToolsRegistration.kt
     * - MemoryToolsRegistration.kt
     * - DatabaseToolsRegistration.kt (PostgreSQL + MongoDB)
     * - ResourceToolsRegistration.kt
     *
     * Each file contains extension functions for McpServerInstance that register the tools
     * by calling server.addTool() with the appropriate schemas and handlers.
     */
}
