package com.apptolast.mcp.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * MCP Protocol models based on JSON-RPC 2.0
 */

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement?,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement?,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class ToolResult(
    val content: List<ContentItem> = emptyList(),
    val isError: Boolean = false
) {
    companion object {
        fun success(text: String): ToolResult {
            return ToolResult(
                content = listOf(TextContent(text = text))
            )
        }
        
        fun error(message: String): ToolResult {
            return ToolResult(
                content = listOf(TextContent(text = message)),
                isError = true
            )
        }
    }
}

@Serializable
sealed class ContentItem {
    abstract val type: String
}

@Serializable
data class TextContent(
    override val type: String = "text",
    val text: String
) : ContentItem()

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class ServerCapabilities(
    val tools: ToolsCapability? = null,
    val resources: ResourcesCapability? = null
)

@Serializable
data class ToolsCapability(
    val listChanged: Boolean = false
)

@Serializable
data class ResourcesCapability(
    val subscribe: Boolean = false,
    val listChanged: Boolean = false
)
