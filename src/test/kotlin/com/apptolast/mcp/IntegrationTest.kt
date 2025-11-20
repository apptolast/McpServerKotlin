package com.apptolast.mcp

import com.apptolast.mcp.modules.bash.BashExecutor
import com.apptolast.mcp.modules.filesystem.FilesystemModule
import com.apptolast.mcp.modules.filesystem.WriteMode
import com.apptolast.mcp.modules.github.GitHubModule
import com.apptolast.mcp.modules.memory.EntityInput
import com.apptolast.mcp.modules.memory.MemoryModule
import com.apptolast.mcp.modules.memory.RelationInput
import com.apptolast.mcp.modules.resources.ResourceModule
import com.apptolast.mcp.server.*
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

/**
 * End-to-End Integration Test
 *
 * Tests the complete workflow of the MCP server by simulating a real-world scenario:
 * 1. Create a project structure with filesystem
 * 2. Execute bash commands to verify structure
 * 3. Store project metadata in knowledge graph
 * 4. Create project documentation as resources
 * 5. Initialize git repository and commit
 * 6. Query knowledge graph to retrieve project info
 */
@OptIn(ExperimentalPathApi::class)
class IntegrationTest {

    private lateinit var tempDir: Path
    private lateinit var filesystemModule: FilesystemModule
    private lateinit var bashExecutor: BashExecutor
    private lateinit var gitHubModule: GitHubModule
    private lateinit var memoryModule: MemoryModule
    private lateinit var resourceModule: ResourceModule
    private val KEEP_FILES_FOR_INSPECTION = false

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("mcp-integration-test")

        // Setup all modules
        filesystemModule = FilesystemModule(
            FilesystemConfig(
                allowedDirectories = listOf(tempDir),
                maxFileSize = 1024 * 1024,
                allowedExtensions = setOf("txt", "md", "json", "kt", "")
            )
        )

        bashExecutor = BashExecutor(
            BashConfig(
                allowedCommands = setOf("ls", "cat", "pwd", "find", "echo"),
                workingDirectory = tempDir,
                timeoutSeconds = 30
            )
        )

        gitHubModule = GitHubModule(
            GitHubConfig(
                repoPath = tempDir.resolve("project"),
                token = null
            )
        )

        val memoryDir = tempDir.resolve("memory")
        Files.createDirectories(memoryDir)
        memoryModule = MemoryModule(
            MemoryConfig(storagePath = memoryDir)
        )

        val resourcesDir = tempDir.resolve("resources")
        Files.createDirectories(resourcesDir)
        resourceModule = ResourceModule(
            ResourcesConfig(resourcesPath = resourcesDir)
        )
    }

    @AfterEach
    fun cleanup() {
        if (!KEEP_FILES_FOR_INSPECTION) {
            tempDir.deleteRecursively()
        } else {
            println("⚠️ Files NOT deleted. Remember to clean: $tempDir")
        }
    }

    @Test
    @org.junit.jupiter.api.Disabled("TODO: Fix MemoryModule JSONL serialization issue")
    fun `test complete E2E workflow`() = runBlocking {
        // ==================== PHASE 1: CREATE PROJECT STRUCTURE ====================
        println("=== PHASE 1: Creating project structure ===")

        // Create project directory
        val projectDir = tempDir.resolve("project").toString()
        val createDirResult = filesystemModule.createDirectory(projectDir)
        assertFalse(createDirResult.isError, "Failed to create project directory")

        // Create README file
        val readmePath = "$projectDir/README.md"
        val readmeContent = """
            # Test Project

            This is a test project created during integration testing.

            ## Features
            - Filesystem operations
            - Git integration
            - Knowledge graph memory
            - Resource management
        """.trimIndent()

        val writeResult = filesystemModule.writeFile(readmePath, readmeContent, WriteMode.CREATE)
        assertFalse(writeResult.isError, "Failed to create README")

        // Create source file
        val srcPath = "$projectDir/Main.kt"
        val srcContent = """
            fun main() {
                println("Hello, MCP Server!")
            }
        """.trimIndent()

        filesystemModule.writeFile(srcPath, srcContent, WriteMode.CREATE)

        // ==================== PHASE 2: VERIFY WITH BASH ====================
        println("=== PHASE 2: Verifying structure with bash ===")

        val lsResult = bashExecutor.execute("ls", emptyList())
        assertFalse(lsResult.isError, "Failed to list directory")
        assertTrue(lsResult.content.first().toString().contains("project"))

        // ==================== PHASE 3: STORE IN KNOWLEDGE GRAPH ====================
        println("=== PHASE 3: Storing project metadata in knowledge graph ===")

        // Create entities
        val entities = listOf(
            EntityInput(
                name = "TestProject",
                entityType = "project",
                observations = listOf(
                    "Created during integration test",
                    "Uses Kotlin",
                    "Includes README and source code"
                )
            ),
            EntityInput(
                name = "README.md",
                entityType = "file",
                observations = listOf("Main documentation file")
            ),
            EntityInput(
                name = "Main.kt",
                entityType = "file",
                observations = listOf("Kotlin source file", "Entry point")
            )
        )

        val createEntitiesResult = memoryModule.createEntities(entities)
        assertFalse(createEntitiesResult.isError, "Failed to create entities")

        // Create relations
        val relations = listOf(
            RelationInput(from = "TestProject", to = "README.md", relationType = "contains"),
            RelationInput(from = "TestProject", to = "Main.kt", relationType = "contains")
        )

        val createRelationsResult = memoryModule.createRelations(relations)
        assertFalse(createRelationsResult.isError, "Failed to create relations")

        // ==================== PHASE 4: CREATE PROJECT DOCUMENTATION ====================
        println("=== PHASE 4: Creating project documentation as resources ===")

        val docContent = """
            # Project Documentation

            This project demonstrates the MCP Server capabilities.

            ## Components
            - Filesystem: File operations
            - Bash: Command execution
            - Git: Version control
            - Memory: Knowledge graph storage
            - Resources: Documentation management
        """.trimIndent()

        val createResourceResult = resourceModule.createResource(
            name = "project-docs.md",
            content = docContent,
            mimeType = "text/markdown"
        )
        assertFalse(createResourceResult.isError, "Failed to create resource")

        // ==================== PHASE 5: GIT INITIALIZATION ====================
        println("=== PHASE 5: Initializing git repository ===")

        val gitStatusResult = gitHubModule.status()
        assertFalse(gitStatusResult.isError, "Failed to get git status")

        val gitCommitResult = gitHubModule.commit(
            message = "Initial commit - Integration test",
            files = listOf("."),
            author = "Integration Test",
            email = "test@mcp-server.com"
        )
        assertFalse(gitCommitResult.isError, "Failed to commit")

        val gitLogResult = gitHubModule.log(maxCount = 1)
        assertFalse(gitLogResult.isError, "Failed to get git log")
        assertTrue(gitLogResult.content.first().toString().contains("Initial commit"))

        // ==================== PHASE 6: QUERY KNOWLEDGE GRAPH ====================
        println("=== PHASE 6: Querying knowledge graph ===")

        val searchResult = memoryModule.searchNodes("TestProject")
        assertFalse(searchResult.isError, "Failed to search knowledge graph")
        val searchText = searchResult.content.first().toString()
        assertTrue(searchText.contains("TestProject"), "Expected TestProject in search results")
        assertTrue(searchText.contains("README.md") || searchText.contains("Main.kt"), "Expected related entities in search results")

        val openNodesResult = memoryModule.openNodes(listOf("TestProject"))
        assertFalse(openNodesResult.isError, "Failed to open nodes")
        assertTrue(openNodesResult.content.first().toString().contains("Created during integration test"))

        // ==================== PHASE 7: VERIFY FILE READING ====================
        println("=== PHASE 7: Verifying file reading ===")

        val readResult = filesystemModule.readFile(readmePath)
        assertFalse(readResult.isError, "Failed to read README")
        assertTrue(readResult.content.first().toString().contains("Test Project"))

        // ==================== PHASE 8: VERIFY RESOURCES ====================
        println("=== PHASE 8: Verifying resources ===")

        val listResourcesResult = resourceModule.listResources()
        assertFalse(listResourcesResult.isError, "Failed to list resources")
        assertTrue(listResourcesResult.content.first().toString().contains("project-docs.md"))

        val readResourceResult = resourceModule.readResource("resource://project-docs.md")
        assertFalse(readResourceResult.isError, "Failed to read resource")
        assertTrue(readResourceResult.content.first().toString().contains("Project Documentation"))

        // ==================== SUCCESS ====================
        println("=== ✅ INTEGRATION TEST COMPLETED SUCCESSFULLY ===")
    }

    @Test
    @org.junit.jupiter.api.Disabled("TODO: Fix MemoryModule JSONL serialization issue")
    fun `test cross-module data consistency`() = runBlocking {
        // This test verifies that data created by one module can be accessed by another

        // Create a file
        val testFile = tempDir.resolve("test.txt").toString()
        filesystemModule.writeFile(testFile, "Test content", WriteMode.CREATE)

        // Store reference in knowledge graph
        memoryModule.createEntities(
            listOf(EntityInput("test.txt", "file", listOf("Created by filesystem")))
        )

        // Verify with bash
        val lsResult = bashExecutor.execute("ls", emptyList())
        assertTrue(lsResult.content.first().toString().contains("test.txt"))

        // Query knowledge graph
        val searchResult = memoryModule.searchNodes("test.txt")
        assertTrue(searchResult.content.first().toString().contains("test.txt"))

        // Read file back
        val readResult = filesystemModule.readFile(testFile)
        assertTrue(readResult.content.first().toString().contains("Test content"))
    }

    @Test
    fun `test error handling across modules`() = runBlocking {
        // Test that errors are properly propagated and handled

        // Try to read non-existent file
        val readResult = filesystemModule.readFile(tempDir.resolve("nonexistent.txt").toString())
        assertTrue(readResult.isError)

        // Try to execute disallowed command
        val execResult = bashExecutor.execute("rm", listOf("-rf", "/"))
        assertTrue(execResult.isError)

        // Try to open non-existent entity
        val openResult = memoryModule.openNodes(listOf("NonExistentEntity"))
        assertTrue(openResult.isError)

        // All errors should be handled gracefully without exceptions
    }
}
