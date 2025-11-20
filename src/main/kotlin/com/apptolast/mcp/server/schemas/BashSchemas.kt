package com.apptolast.mcp.server.schemas

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * JSON Schemas for Bash execution module
 *
 * Provides 1 tool:
 * - bashExecute: Execute bash commands with security validation
 */
object BashSchemas {

    /**
     * Schema for bashExecute tool
     * Executes a bash command with arguments and environment variables
     *
     * Security: Commands are validated against whitelist and dangerous patterns are blocked
     */
    val execute = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("command") {
                put("type", "string")
                put("description", "The bash command to execute (must be in allowed commands list)")
            }
            putJsonObject("args") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                }
                put("description", "Command arguments (optional)")
            }
            putJsonObject("env") {
                put("type", "object")
                putJsonObject("additionalProperties") {
                    put("type", "string")
                }
                put("description", "Environment variables for the command (optional)")
            }
        },
        required = listOf("command")
    )
}
