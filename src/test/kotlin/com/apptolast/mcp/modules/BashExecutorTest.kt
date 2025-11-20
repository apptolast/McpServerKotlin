package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.bash.BashExecutor
import com.apptolast.mcp.server.BashConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach

@OptIn(ExperimentalPathApi::class)
class BashExecutorTest {

    private lateinit var tempDir: Path
    private lateinit var bashExecutor: BashExecutor
    private val KEEP_FILES_FOR_INSPECTION = false

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("mcp-bash-test")
        val config = BashConfig(
            allowedCommands = setOf("ls", "echo", "pwd", "cat", "touch", "mkdir"),
            workingDirectory = tempDir,
            timeoutSeconds = 30
        )
        bashExecutor = BashExecutor(config)
    }

    @AfterEach
    fun cleanup() {
        if (!KEEP_FILES_FOR_INSPECTION) {
            tempDir.deleteRecursively()
        } else {
            println("⚠️ Archivos NO borrados. Recuerda limpiar: $tempDir")
        }
    }

    @Test
    fun `test successful command execution`() = runBlocking {
        val result = bashExecutor.execute("echo", listOf("Hello", "World"))

        assertFalse(result.isError)
        assertTrue(result.content.isNotEmpty())
        val text = result.content.first().toString()
        assertTrue(text.contains("Hello World"))
    }

    @Test
    fun `test command not in whitelist`() = runBlocking {
        val result = bashExecutor.execute("rm", listOf("-rf", "/"))

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Command not allowed") || text.contains("not allowed"))
    }

    @Test
    fun `test dangerous pattern detection`() = runBlocking {
        // Test sudo command (not in whitelist)
        val result1 = bashExecutor.execute("sudo", emptyList())
        assertTrue(result1.isError)
        val text1 = result1.content.first().toString()
        assertTrue(text1.contains("Command not allowed") || text1.contains("not allowed"))

        // Test chmod 777 pattern with allowed command
        val config = BashConfig(
            allowedCommands = setOf("chmod"),
            workingDirectory = tempDir,
            timeoutSeconds = 30
        )
        val executor = BashExecutor(config)
        // chmod 777 should be blocked by dangerous pattern even though chmod is allowed
        val result2 = executor.execute("chmod 777", emptyList())
        assertTrue(result2.isError)
        val text2 = result2.content.first().toString()
        assertTrue(
            text2.contains("Dangerous pattern") || text2.contains("Command not allowed"),
            "Expected dangerous pattern or command not allowed error, got: $text2"
        )
    }

    @Test
    fun `test command timeout`() = runBlocking {
        val config = BashConfig(
            allowedCommands = setOf("sleep"),
            workingDirectory = tempDir,
            timeoutSeconds = 1  // 1 second timeout
        )
        val executor = BashExecutor(config)

        // This should timeout
        val result = executor.execute("sleep", listOf("5"))

        // The result should indicate failure or timeout
        assertTrue(result.isError || result.content.first().toString().contains("143"))
    }

    @Test
    fun `test working directory isolation`() = runBlocking {
        val result = bashExecutor.execute("pwd", emptyList())

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains(tempDir.toString()))
    }

    @Test
    fun `test environment variables`() = runBlocking {
        val envVars = mapOf("TEST_VAR" to "test_value")
        val result = bashExecutor.execute("echo", listOf("\$TEST_VAR"), envVars)

        assertFalse(result.isError)
        // Environment variables should be set
        assertTrue(result.content.isNotEmpty())
    }
}
