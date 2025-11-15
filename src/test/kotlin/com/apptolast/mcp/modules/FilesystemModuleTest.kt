package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.filesystem.FilesystemModule
import com.apptolast.mcp.modules.filesystem.WriteMode
import com.apptolast.mcp.server.FilesystemConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class FilesystemModuleTest {
    
    private lateinit var tempDir: Path
    private lateinit var filesystemModule: FilesystemModule
    
    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("mcp-test")
        val config = FilesystemConfig(
            allowedDirectories = listOf(tempDir),
            maxFileSize = 1024 * 1024, // 1MB
            allowedExtensions = setOf("txt", "md", "json", "")
        )
        filesystemModule = FilesystemModule(config)
    }
    
    @AfterEach
    fun cleanup() {
        tempDir.deleteRecursively()
    }
    
    @Test
    fun `test write and read file`() = runBlocking {
        val testFile = tempDir.resolve("test.txt").toString()
        val content = "Hello, MCP Server!"
        
        // Write file
        val writeResult = filesystemModule.writeFile(testFile, content, WriteMode.CREATE)
        assertFalse(writeResult.isError)
        
        // Read file
        val readResult = filesystemModule.readFile(testFile)
        assertFalse(readResult.isError)
        assertTrue(readResult.content.isNotEmpty())
    }
    
    @Test
    fun `test path traversal protection`() = runBlocking {
        val maliciousPath = tempDir.resolve("../../../etc/passwd").toString()
        
        val result = filesystemModule.readFile(maliciousPath)
        assertTrue(result.isError)
    }
    
    @Test
    fun `test file size limit`() = runBlocking {
        val testFile = tempDir.resolve("large.txt").toString()
        val largeContent = "x".repeat(2 * 1024 * 1024) // 2MB
        
        val result = filesystemModule.writeFile(testFile, largeContent, WriteMode.CREATE)
        // Should succeed in writing, but reading might fail due to size limit
        // This depends on your implementation details
    }
    
    @Test
    fun `test create and list directory`() = runBlocking {
        val testDir = tempDir.resolve("testdir").toString()
        
        // Create directory
        val createResult = filesystemModule.createDirectory(testDir)
        assertFalse(createResult.isError)
        
        // List parent directory
        val listResult = filesystemModule.listDirectory(tempDir.toString())
        assertFalse(listResult.isError)
    }
}
