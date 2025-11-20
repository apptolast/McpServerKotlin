package com.apptolast.mcp.server.schemas

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * JSON Schemas for GitHub/Git integration module
 *
 * Provides 6 tools:
 * - gitStatus: Get repository status
 * - gitCommit: Create a commit
 * - gitPush: Push changes to remote
 * - gitClone: Clone a repository
 * - gitLog: View commit history
 * - gitBranch: List or checkout branches
 */
object GitHubSchemas {

    /**
     * Schema for gitStatus tool
     * Shows the working tree status
     */
    val status = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("repoPath") {
                put("type", "string")
                put("description", "Path to the git repository (optional, uses configured default)")
            }
        },
        required = listOf()  // No required parameters
    )

    /**
     * Schema for gitCommit tool
     * Records changes to the repository
     */
    val commit = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("message") {
                put("type", "string")
                put("description", "Commit message")
            }
            putJsonObject("author") {
                put("type", "string")
                put("description", "Author name (optional, defaults to 'MCP Server')")
                put("default", "MCP Server")
            }
            putJsonObject("email") {
                put("type", "string")
                put("description", "Author email (optional, defaults to 'mcp@apptolast.com')")
                put("default", "mcp@apptolast.com")
            }
            putJsonObject("addAll") {
                put("type", "boolean")
                put("description", "Stage all changes before committing")
                put("default", false)
            }
        },
        required = listOf("message")
    )

    /**
     * Schema for gitPush tool
     * Push local commits to remote repository
     */
    val push = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("remote") {
                put("type", "string")
                put("description", "Remote name")
                put("default", "origin")
            }
            putJsonObject("branch") {
                put("type", "string")
                put("description", "Branch name (optional, uses current branch)")
            }
            putJsonObject("force") {
                put("type", "boolean")
                put("description", "Force push (use with caution!)")
                put("default", false)
            }
        },
        required = listOf()  // All parameters are optional
    )

    /**
     * Schema for gitClone tool
     * Clone a repository into a new directory
     */
    val clone = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("url") {
                put("type", "string")
                put("description", "Repository URL to clone")
            }
            putJsonObject("targetPath") {
                put("type", "string")
                put("description", "Target directory path (optional)")
            }
        },
        required = listOf("url")
    )

    /**
     * Schema for gitLog tool
     * Show commit logs
     */
    val log = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("maxCount") {
                put("type", "integer")
                put("description", "Maximum number of commits to show")
                put("default", 10)
                put("minimum", 1)
                put("maximum", 100)
            }
        },
        required = listOf()  // All parameters are optional
    )

    /**
     * Schema for gitBranch tool
     * List or checkout branches
     */
    val branch = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "Branch name (null to list all branches)")
            }
            putJsonObject("checkout") {
                put("type", "boolean")
                put("description", "Checkout the branch if true")
                put("default", false)
            }
        },
        required = listOf()  // All parameters are optional
    )
}
