package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.github.GitHubModule
import com.apptolast.mcp.server.GitHubConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class GitHubModuleTest {

    private lateinit var tempDir: Path
    private lateinit var githubModule: GitHubModule
    private val KEEP_FILES_FOR_INSPECTION = false

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("mcp-git-test")
        val config = GitHubConfig(
            repoPath = tempDir,
            token = null  // No token needed for local tests
        )
        githubModule = GitHubModule(config)
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
    fun `test git status on new repo`() = runBlocking {
        val result = githubModule.status()

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Branch:") || text.contains("clean"))
    }

    @Test
    fun `test git commit with file`() = runBlocking {
        // Create a test file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Test content")

        // Commit the file
        val result = githubModule.commit(
            message = "Test commit",
            files = listOf("test.txt"),
            author = "Test Author",
            email = "test@test.com"
        )

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Committed:") || text.contains("Test commit"))
    }

    @Test
    fun `test git status shows modified files`() = runBlocking {
        // Create and commit a file
        val testFile = tempDir.resolve("file1.txt")
        testFile.writeText("Initial content")
        githubModule.commit(
            message = "Initial commit",
            files = listOf("file1.txt")
        )

        // Modify the file
        testFile.writeText("Modified content")

        // Check status
        val result = githubModule.status()
        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Modified") || text.contains("file1.txt"))
    }

    @Test
    fun `test git log shows commits`() = runBlocking {
        // Create and commit a file
        val testFile = tempDir.resolve("log-test.txt")
        testFile.writeText("Content for log test")
        githubModule.commit(
            message = "First commit for log test",
            files = listOf("log-test.txt")
        )

        // Get log
        val result = githubModule.log(maxCount = 5)

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("commit") || text.contains("Commit history"))
        assertTrue(text.contains("First commit for log test"))
    }

    @Test
    fun `test git branch list`() = runBlocking {
        // List branches (should show at least the current branch)
        val result = githubModule.branch(name = null, checkout = false)

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Branches:") || text.contains("main") || text.contains("master"))
    }

    @Test
    fun `test git branch create`() = runBlocking {
        // Create an initial commit first (needed to create branches)
        val testFile = tempDir.resolve("init.txt")
        testFile.writeText("Initial content")
        githubModule.commit(message = "Initial commit", files = listOf("."))

        // Create a new branch
        val result = githubModule.branch(name = "feature/test-branch", checkout = false)

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Created branch") || text.contains("feature/test-branch"))
    }

    @Test
    fun `test git branch checkout`() = runBlocking {
        // Create an initial commit first (needed to checkout branches)
        val testFile = tempDir.resolve("init.txt")
        testFile.writeText("Initial content")
        githubModule.commit(message = "Initial commit", files = listOf("."))

        // Create and checkout a new branch
        val result = githubModule.branch(name = "feature/checkout-test", checkout = true)

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Switched") || text.contains("checkout-test"))
    }
}
