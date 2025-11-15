package com.apptolast.mcp.modules.resources

import com.apptolast.mcp.server.ResourcesConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.readText
import kotlin.streams.toList

private val logger = KotlinLogging.logger {}

@Serializable
data class ResourceMetadata(
    val uri: String,
    val name: String,
    val mimeType: String,
    val description: String?,
    val size: Long,
    val lastModified: String
)

sealed class ResourceContent {
    abstract val uri: String
    abstract val mimeType: String
}

data class TextResourceContent(
    override val uri: String,
    override val mimeType: String,
    val text: String
) : ResourceContent()

data class BlobResourceContent(
    override val uri: String,
    override val mimeType: String,
    val blob: String // Base64 encoded
) : ResourceContent()

class ResourceModule(
    private val config: ResourcesConfig
) {
    
    init {
        Files.createDirectories(config.resourcesPath)
    }
    
    suspend fun listResources(): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val resources = Files.walk(config.resourcesPath)
                    .filter { Files.isRegularFile(it) }
                    .map { file ->
                        ResourceMetadata(
                            uri = "resource://${config.resourcesPath.relativize(file)}",
                            name = file.fileName.toString(),
                            mimeType = Files.probeContentType(file) ?: "application/octet-stream",
                            description = extractDescription(file),
                            size = Files.size(file),
                            lastModified = Files.getLastModifiedTime(file).toInstant().toString()
                        )
                    }
                    .toList()
                
                val result = buildString {
                    appendLine("Resources (${resources.size}):")
                    appendLine()
                    
                    resources.forEach { res ->
                        appendLine("URI: ${res.uri}")
                        appendLine("  Name: ${res.name}")
                        appendLine("  Type: ${res.mimeType}")
                        appendLine("  Size: ${res.size} bytes")
                        res.description?.let { appendLine("  Description: $it") }
                        appendLine()
                    }
                }
                
                logger.info { "Listed ${resources.size} resources" }
                
                ToolResult(
                    content = listOf(TextContent(text = result))
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to list resources" }
                ToolResult.error("Failed to list resources: ${e.message}")
            }
        }
    }
    
    suspend fun readResource(uri: String): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val path = resolveUri(uri)
                    ?: return@withContext ToolResult.error("Invalid resource URI: $uri")
                
                if (!Files.exists(path)) {
                    return@withContext ToolResult.error("Resource not found: $uri")
                }
                
                val mimeType = Files.probeContentType(path) ?: "text/plain"
                
                val content = when {
                    mimeType.startsWith("text/") || 
                    mimeType == "application/json" ||
                    mimeType == "application/xml" -> {
                        path.readText()
                    }
                    else -> {
                        val bytes = Files.readAllBytes(path)
                        Base64.getEncoder().encodeToString(bytes)
                    }
                }
                
                logger.info { "Read resource: $uri" }
                
                ToolResult(
                    content = listOf(
                        TextContent(text = "Resource: $uri\nType: $mimeType\n\n$content")
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to read resource" }
                ToolResult.error("Failed to read resource: ${e.message}")
            }
        }
    }
    
    suspend fun createResource(
        name: String,
        content: String,
        mimeType: String = "text/plain"
    ): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val path = config.resourcesPath.resolve(name)
                
                if (Files.exists(path)) {
                    return@withContext ToolResult.error("Resource already exists: $name")
                }
                
                // Create parent directories if needed
                Files.createDirectories(path.parent)
                
                // Write content
                if (mimeType.startsWith("text/") || mimeType == "application/json") {
                    Files.writeString(path, content)
                } else {
                    // Assume content is base64 encoded for binary files
                    val bytes = Base64.getDecoder().decode(content)
                    Files.write(path, bytes)
                }
                
                val uri = "resource://${config.resourcesPath.relativize(path)}"
                
                logger.info { "Created resource: $uri" }
                
                ToolResult(
                    content = listOf(
                        TextContent(text = "Resource created: $uri\nType: $mimeType\nSize: ${Files.size(path)} bytes")
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to create resource" }
                ToolResult.error("Failed to create resource: ${e.message}")
            }
        }
    }
    
    suspend fun deleteResource(uri: String): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val path = resolveUri(uri)
                    ?: return@withContext ToolResult.error("Invalid resource URI: $uri")
                
                if (!Files.exists(path)) {
                    return@withContext ToolResult.error("Resource not found: $uri")
                }
                
                Files.delete(path)
                
                logger.info { "Deleted resource: $uri" }
                
                ToolResult(
                    content = listOf(TextContent(text = "Resource deleted: $uri"))
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to delete resource" }
                ToolResult.error("Failed to delete resource: ${e.message}")
            }
        }
    }
    
    private fun resolveUri(uri: String): Path? {
        if (!uri.startsWith("resource://")) {
            return null
        }
        
        val relativePath = uri.removePrefix("resource://")
        val resolved = config.resourcesPath.resolve(relativePath).normalize()
        
        // Validate path is within resources directory using real paths to prevent symlink/case issues
        return try {
            val resourcesRealPath = config.resourcesPath.toRealPath()
            val resolvedRealPath = resolved.toRealPath()
            if (!resolvedRealPath.startsWith(resourcesRealPath)) {
                null
            } else {
                resolvedRealPath
            }
        } catch (e: Exception) {
            // If path doesn't exist yet or can't be resolved, return null
            null
        }
    }
    
    private fun extractDescription(file: Path): String? {
        // Try to extract description from file metadata or first lines
        if (file.toString().endsWith(".md")) {
            try {
                val firstLines = Files.lines(file).limit(5).toList()
                return firstLines.find { it.startsWith("#") }?.removePrefix("#")?.trim()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return null
    }
}
