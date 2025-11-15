package com.apptolast.mcp.modules.filesystem

import com.apptolast.mcp.server.FilesystemConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

class FilesystemModule(
    private val config: FilesystemConfig
) {
    
    private fun validatePath(path: String): Result<Path> {
        val normalizedPath = Paths.get(path).normalize().toAbsolutePath()
        
        // Path traversal protection
        val isAllowed = config.allowedDirectories.any { allowedDir ->
            try {
                normalizedPath.startsWith(allowedDir)
            } catch (e: Exception) {
                false
            }
        }
        
        if (!isAllowed) {
            return Result.failure(
                SecurityException("Access denied: path outside allowed directories")
            )
        }
        
        return Result.success(normalizedPath)
    }
    
    private fun validateFileExtension(path: Path): Boolean {
        if (config.allowedExtensions.isEmpty()) return true
        if (config.allowedExtensions.contains("")) return true
        
        val extension = path.fileName.toString().substringAfterLast('.', "")
        return config.allowedExtensions.contains(extension) || extension.isEmpty()
    }
    
    suspend fun readFile(path: String, encoding: String = "UTF-8"): ToolResult {
        return validatePath(path).fold(
            onSuccess = { validPath ->
                withContext(Dispatchers.IO) {
                    try {
                        if (!Files.exists(validPath)) {
                            return@withContext ToolResult.error("File does not exist: $path")
                        }
                        
                        if (!validateFileExtension(validPath)) {
                            return@withContext ToolResult.error("File extension not allowed")
                        }
                        
                        val size = Files.size(validPath)
                        if (size > config.maxFileSize) {
                            return@withContext ToolResult.error(
                                "File too large: $size bytes exceeds ${config.maxFileSize} bytes"
                            )
                        }
                        
                        val content = validPath.readText(Charset.forName(encoding))
                        logger.info { "Read file: $path (${content.length} characters)" }
                        
                        ToolResult(
                            content = listOf(
                                TextContent(text = content)
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to read file: $path" }
                        ToolResult.error("Failed to read file: ${e.message}")
                    }
                }
            },
            onFailure = { e ->
                logger.warn { "Path validation failed: ${e.message}" }
                ToolResult.error(e.message ?: "Failed to validate path")
            }
        )
    }
    
    suspend fun writeFile(
        path: String,
        content: String,
        mode: WriteMode = WriteMode.CREATE
    ): ToolResult {
        return validatePath(path).fold(
            onSuccess = { validPath ->
                withContext(Dispatchers.IO) {
                    try {
                        if (!validateFileExtension(validPath)) {
                            return@withContext ToolResult.error("File extension not allowed")
                        }
                        
                        when (mode) {
                            WriteMode.CREATE -> {
                                if (Files.exists(validPath)) {
                                    return@withContext ToolResult.error("File already exists")
                                }
                                Files.createDirectories(validPath.parent)
                                validPath.writeText(content)
                            }
                            WriteMode.OVERWRITE -> {
                                Files.createDirectories(validPath.parent)
                                validPath.writeText(content)
                            }
                            WriteMode.APPEND -> {
                                Files.createDirectories(validPath.parent)
                                if (!Files.exists(validPath)) {
                                    validPath.writeText(content)
                                } else {
                                    validPath.appendText(content)
                                }
                            }
                        }
                        
                        logger.info { "Wrote file: $path (${content.length} characters, mode=$mode)" }
                        
                        ToolResult(
                            content = listOf(
                                TextContent(text = "File written successfully: ${validPath.fileName}")
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to write file: $path" }
                        ToolResult.error("Failed to write file: ${e.message}")
                    }
                }
            },
            onFailure = { e ->
                logger.warn { "Path validation failed: ${e.message}" }
                ToolResult.error(e.message ?: "Failed to validate path")
            }
        )
    }
    
    suspend fun listDirectory(
        path: String,
        recursive: Boolean = false,
        maxDepth: Int = 2
    ): ToolResult {
        return validatePath(path).fold(
            onSuccess = { validPath ->
                withContext(Dispatchers.IO) {
                    try {
                        if (!Files.exists(validPath)) {
                            return@withContext ToolResult.error("Directory does not exist: $path")
                        }
                        
                        if (!Files.isDirectory(validPath)) {
                            return@withContext ToolResult.error("Path is not a directory: $path")
                        }
                        
                        val entries = if (recursive) {
                            Files.walk(validPath, maxDepth)
                                .filter { it != validPath }
                                .map { entry ->
                                    val relativePath = validPath.relativize(entry)
                                    val type = if (Files.isDirectory(entry)) "dir" else "file"
                                    val size = if (Files.isRegularFile(entry)) Files.size(entry) else 0
                                    "$type: $relativePath (${size} bytes)"
                                }
                                .toList()
                        } else {
                            Files.list(validPath)
                                .map { entry ->
                                    val name = entry.fileName
                                    val type = if (Files.isDirectory(entry)) "dir" else "file"
                                    val size = if (Files.isRegularFile(entry)) Files.size(entry) else 0
                                    "$type: $name (${size} bytes)"
                                }
                                .toList()
                        }
                        
                        val result = entries.joinToString("\n")
                        logger.info { "Listed directory: $path (${entries.size} entries)" }
                        
                        ToolResult(
                            content = listOf(
                                TextContent(text = result)
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to list directory: $path" }
                        ToolResult.error("Failed to list directory: ${e.message}")
                    }
                }
            },
            onFailure = { e ->
                logger.warn { "Path validation failed: ${e.message}" }
                ToolResult.error(e.message ?: "Failed to validate path")
            }
        )
    }
    
    suspend fun createDirectory(path: String, recursive: Boolean = true): ToolResult {
        return validatePath(path).fold(
            onSuccess = { validPath ->
                withContext(Dispatchers.IO) {
                    try {
                        if (Files.exists(validPath)) {
                            return@withContext ToolResult.error("Directory already exists")
                        }
                        
                        if (recursive) {
                            Files.createDirectories(validPath)
                        } else {
                            Files.createDirectory(validPath)
                        }
                        
                        logger.info { "Created directory: $path" }
                        
                        ToolResult(
                            content = listOf(
                                TextContent(text = "Directory created successfully: ${validPath.fileName}")
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to create directory: $path" }
                        ToolResult.error("Failed to create directory: ${e.message}")
                    }
                }
            },
            onFailure = { e ->
                logger.warn { "Path validation failed: ${e.message}" }
                ToolResult.error(e.message ?: "Failed to validate path")
            }
        )
    }
    
    suspend fun deleteFile(path: String, recursive: Boolean = false): ToolResult {
        return validatePath(path).fold(
            onSuccess = { validPath ->
                withContext(Dispatchers.IO) {
                    try {
                        if (!Files.exists(validPath)) {
                            return@withContext ToolResult.error("File or directory does not exist")
                        }
                        
                        if (Files.isDirectory(validPath) && !recursive) {
                            val hasChildren = Files.list(validPath).findAny().isPresent
                            if (hasChildren) {
                                return@withContext ToolResult.error(
                                    "Directory is not empty. Use recursive=true to delete non-empty directories"
                                )
                            }
                        }
                        
                        if (recursive && Files.isDirectory(validPath)) {
                            Files.walk(validPath)
                                .sorted(Comparator.reverseOrder())
                                .forEach { Files.delete(it) }
                        } else {
                            Files.delete(validPath)
                        }
                        
                        logger.info { "Deleted: $path" }
                        
                        ToolResult(
                            content = listOf(
                                TextContent(text = "Deleted successfully: ${validPath.fileName}")
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to delete: $path" }
                        ToolResult.error("Failed to delete: ${e.message}")
                    }
                }
            },
            onFailure = { e ->
                logger.warn { "Path validation failed: ${e.message}" }
                ToolResult.error(e.message ?: "Failed to validate path")
            }
        )
    }
}

enum class WriteMode {
    CREATE,
    OVERWRITE,
    APPEND
}
