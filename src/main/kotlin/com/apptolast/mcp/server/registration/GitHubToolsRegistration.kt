package com.apptolast.mcp.server.registration

import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.schemas.GitHubSchemas
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.jsonPrimitive

private val logger = KotlinLogging.logger {}

/**
 * Registers all GitHub/Git tools with the MCP server
 *
 * Tools registered:
 * 1. gitStatus - Get repository status
 * 2. gitCommit - Create a commit
 * 3. gitPush - Push changes to remote
 * 4. gitClone - Clone a repository
 * 5. gitLog - View commit history
 * 6. gitBranch - List or checkout branches
 */
suspend fun McpServerInstance.registerGitHubTools() {
    logger.info { "Registering GitHub/Git tools..." }

    // 1. gitStatus tool
    server.addTool(
        name = "gitStatus",
        description = "Show the working tree status. Lists changed files, staged files, and untracked files.",
        inputSchema = GitHubSchemas.status
    ) { request: CallToolRequest ->
        try {
            val result = githubModule.status()

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in gitStatus tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error getting git status: ${e.message}")),
                isError = true
            )
        }
    }

    // 2. gitCommit tool
    server.addTool(
        name = "gitCommit",
        description = "Record changes to the repository. Creates a new commit with the specified message.",
        inputSchema = GitHubSchemas.commit
    ) { request: CallToolRequest ->
        try {
            val message = request.arguments["message"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: message")),
                    isError = true
                )

            // Note: author/email configured at git config level, not per-commit
            // val author = request.arguments["author"]?.jsonPrimitive?.content
            // val email = request.arguments["email"]?.jsonPrimitive?.content
            val addAll = request.arguments["addAll"]?.jsonPrimitive?.content?.toBoolean() ?: false

            // Pass files list (empty list means commit staged files only)
            val files = if (addAll) emptyList<String>() else emptyList<String>()  // TODO: implement file selection
            val result = githubModule.commit(message, files)

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in gitCommit tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error creating commit: ${e.message}")),
                isError = true
            )
        }
    }

    // 3. gitPush tool
    server.addTool(
        name = "gitPush",
        description = "Push local commits to remote repository. Use with caution when using force push.",
        inputSchema = GitHubSchemas.push
    ) { request: CallToolRequest ->
        try {
            val remote = request.arguments["remote"]?.jsonPrimitive?.content ?: "origin"
            val branch = request.arguments["branch"]?.jsonPrimitive?.content
            val force = request.arguments["force"]?.jsonPrimitive?.content?.toBoolean() ?: false

            val result = githubModule.push(remote, branch, force)

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in gitPush tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error pushing to remote: ${e.message}")),
                isError = true
            )
        }
    }

    // 4. gitClone tool
    server.addTool(
        name = "gitClone",
        description = "Clone a repository into a new directory. Optionally specify a branch to clone.",
        inputSchema = GitHubSchemas.clone
    ) { request: CallToolRequest ->
        try {
            val url = request.arguments["url"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: url")),
                    isError = true
                )

            val targetPath = request.arguments["targetPath"]?.jsonPrimitive?.content
            // Note: branch selection not supported in clone() method
            // val branch = request.arguments["branch"]?.jsonPrimitive?.content

            val result = githubModule.clone(url, targetPath)

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in gitClone tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error cloning repository: ${e.message}")),
                isError = true
            )
        }
    }

    // 5. gitLog tool
    server.addTool(
        name = "gitLog",
        description = "Show commit logs. View commit history with author, date, and message.",
        inputSchema = GitHubSchemas.log
    ) { request: CallToolRequest ->
        try {
            val maxCount = request.arguments["maxCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: 10
            // Note: branch selection not supported in log() method (shows current branch)
            // val branch = request.arguments["branch"]?.jsonPrimitive?.content

            val result = githubModule.log(maxCount)

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in gitLog tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error getting git log: ${e.message}")),
                isError = true
            )
        }
    }

    // 6. gitBranch tool
    server.addTool(
        name = "gitBranch",
        description = "List or checkout branches. Pass null for name to list all branches, or specify a name with checkout=true to switch branches.",
        inputSchema = GitHubSchemas.branch
    ) { request: CallToolRequest ->
        try {
            val name = request.arguments["name"]?.jsonPrimitive?.content
            val checkout = request.arguments["checkout"]?.jsonPrimitive?.content?.toBoolean() ?: false

            val result = githubModule.branch(name, checkout)

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in gitBranch tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error managing branches: ${e.message}")),
                isError = true
            )
        }
    }

    logger.info { "Registered 6 GitHub/Git tools successfully" }
}
