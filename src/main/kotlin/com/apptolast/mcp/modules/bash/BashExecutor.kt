package com.apptolast.mcp.modules.bash

import com.apptolast.mcp.server.BashConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.exec.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class BashExecutor(
    private val config: BashConfig
) {
    
    companion object {
        // Dangerous command patterns compiled once for efficiency
        private val DANGEROUS_PATTERNS = listOf(
            Regex("rm\\s+-rf\\s+/"),
            Regex("dd\\s+if="),
            Regex(":\\(\\)\\{.*:\\|:.*&.*\\};?.*:"),  // Fork bomb pattern
            Regex("mkfs\\."),
            Regex("sudo"),
            Regex("su\\s+"),
            Regex("chmod\\s+777")
        )
    }
    
    private fun validateCommand(command: String, args: List<String> = emptyList()): Result<String> {
        val baseCommand = command.trim().split(Regex("\\s+")).firstOrNull() ?: ""
        
        if (!config.allowedCommands.contains(baseCommand)) {
            return Result.failure(
                SecurityException("Command not allowed: $baseCommand")
            )
        }
        
        // Check for dangerous patterns in command and each argument separately
        // This prevents bypasses via argument splitting or special characters
        DANGEROUS_PATTERNS.forEach { pattern ->
            // Check base command
            if (pattern.containsMatchIn(command)) {
                return Result.failure(
                    SecurityException("Dangerous pattern detected in command")
                )
            }
            
            // Check each argument individually
            args.forEach { arg ->
                if (pattern.containsMatchIn(arg)) {
                    return Result.failure(
                        SecurityException("Dangerous pattern detected in argument")
                    )
                }
            }
        }
        
        // Also check full command line as a final safety net
        // This catches patterns that span multiple arguments
        val fullCommandLine = if (args.isEmpty()) {
            command
        } else {
            "$command ${args.joinToString(" ")}"
        }
        
        DANGEROUS_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(fullCommandLine)) {
                return Result.failure(
                    SecurityException("Dangerous pattern detected in full command line")
                )
            }
        }
        
        return Result.success(command)
    }
    
    suspend fun execute(
        command: String,
        args: List<String> = emptyList(),
        env: Map<String, String> = emptyMap()
    ): ToolResult {
        return validateCommand(command, args).fold(
            onSuccess = { validCommand ->
                withContext(Dispatchers.IO) {
                    try {
                        val commandLine = CommandLine.parse(validCommand)
                        args.forEach { commandLine.addArgument(it, false) }
                        
                        val executor = DefaultExecutor()
                        executor.workingDirectory = config.workingDirectory.toFile()
                        
                        val watchdog = ExecuteWatchdog(
                            TimeUnit.SECONDS.toMillis(config.timeoutSeconds)
                        )
                        executor.watchdog = watchdog
                        
                        val outputStream = ByteArrayOutputStream()
                        val errorStream = ByteArrayOutputStream()
                        
                        val pumpStreamHandler = PumpStreamHandler(outputStream, errorStream)
                        executor.streamHandler = pumpStreamHandler
                        
                        // Set up environment
                        val envVars = System.getenv().toMutableMap()
                        envVars.putAll(env)
                        
                        val exitValue = try {
                            executor.execute(commandLine, envVars)
                        } catch (e: ExecuteException) {
                            e.exitValue
                        }
                        
                        val stdout = outputStream.toString("UTF-8")
                        val stderr = errorStream.toString("UTF-8")
                        
                        val result = buildString {
                            appendLine("Command: $validCommand ${args.joinToString(" ")}")
                            appendLine("Exit Code: $exitValue")
                            if (stdout.isNotEmpty()) {
                                appendLine("\nOutput:")
                                appendLine(stdout)
                            }
                            if (stderr.isNotEmpty()) {
                                appendLine("\nError Output:")
                                appendLine(stderr)
                            }
                        }
                        
                        logger.info { "Executed command: $validCommand (exit=$exitValue)" }
                        
                        if (exitValue == 0) {
                            ToolResult(
                                content = listOf(TextContent(text = result))
                            )
                        } else {
                            ToolResult(
                                content = listOf(TextContent(text = result)),
                                isError = true
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Command execution failed: $validCommand" }
                        ToolResult.error("Command execution failed: ${e.message}")
                    }
                }
            },
            onFailure = { e ->
                logger.warn { "Command validation failed: ${e.message}" }
                ToolResult.error(e.message ?: "Command validation failed")
            }
        )
    }
}
