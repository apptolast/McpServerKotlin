package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.resources.ResourceModule
import com.apptolast.mcp.server.ResourcesConfig
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
class ResourcesModuleTest {

    private lateinit var tempDir: Path
    private lateinit var resourceModule: ResourceModule
    private val KEEP_FILES_FOR_INSPECTION = false

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("mcp-resources-test")
        val config = ResourcesConfig(resourcesPath = tempDir)
        resourceModule = ResourceModule(config)
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
    fun `test list resources on empty directory`() = runBlocking {
        val result = resourceModule.listResources()

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Resources (0)"))
    }

    @Test
    fun `test list resources with files`() = runBlocking {
        // Create some test resources
        val file1 = tempDir.resolve("test1.txt")
        val file2 = tempDir.resolve("test2.json")
        file1.writeText("Test content 1")
        file2.writeText("""{"test": "data"}""")

        val result = resourceModule.listResources()

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Resources (2)"), "Expected 2 resources in listing")
        assertTrue(text.contains("test1.txt") && text.contains("test2.json"), "Expected both files in listing")
    }

    @Test
    fun `test create and read text resource`() = runBlocking {
        val resourceName = "new-resource.txt"
        val content = "This is a test resource"

        // Create resource
        val createResult = resourceModule.createResource(resourceName, content, "text/plain")
        assertFalse(createResult.isError)
        val createText = createResult.content.first().toString()
        assertTrue(createText.contains("Resource created"))

        // Read the created resource
        val uri = "resource://$resourceName"
        val readResult = resourceModule.readResource(uri)
        assertFalse(readResult.isError)
        val readText = readResult.content.first().toString()
        assertTrue(readText.contains(content))
    }

    @Test
    fun `test create resource that already exists`() = runBlocking {
        val resourceName = "duplicate.txt"

        // Create first time
        val firstResult = resourceModule.createResource(resourceName, "First content")
        assertFalse(firstResult.isError)

        // Try to create again - should fail
        val secondResult = resourceModule.createResource(resourceName, "Second content")
        assertTrue(secondResult.isError)
        val text = secondResult.content.first().toString()
        assertTrue(text.contains("already exists"))
    }

    @Test
    fun `test read non-existent resource`() = runBlocking {
        val uri = "resource://non-existent-file.txt"

        val result = resourceModule.readResource(uri)

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("not found") || text.contains("Resource not found"))
    }

    @Test
    fun `test read resource with invalid URI`() = runBlocking {
        // URI without resource:// prefix
        val result = resourceModule.readResource("invalid-uri")

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Invalid") || text.contains("invalid"))
    }

    @Test
    fun `test delete resource`() = runBlocking {
        val resourceName = "to-delete.txt"

        // Create resource first
        resourceModule.createResource(resourceName, "Content to delete")

        // Delete it
        val uri = "resource://$resourceName"
        val deleteResult = resourceModule.deleteResource(uri)

        assertFalse(deleteResult.isError)
        val text = deleteResult.content.first().toString()
        assertTrue(text.contains("deleted"))

        // Verify it's deleted by trying to read it
        val readResult = resourceModule.readResource(uri)
        assertTrue(readResult.isError)
    }

    @Test
    fun `test delete non-existent resource`() = runBlocking {
        val uri = "resource://does-not-exist.txt"

        val result = resourceModule.deleteResource(uri)

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("not found"))
    }

    @Test
    fun `test create JSON resource`() = runBlocking {
        val resourceName = "data.json"
        val jsonContent = """{"name": "test", "value": 123}"""

        val result = resourceModule.createResource(resourceName, jsonContent, "application/json")

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Resource created"))
        assertTrue(text.contains("application/json"))
    }

    @Test
    fun `test list resources shows metadata`() = runBlocking {
        // Create a test file
        val file = tempDir.resolve("metadata-test.txt")
        file.writeText("Test content for metadata")

        val result = resourceModule.listResources()

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Name:"), "Expected 'Name:' in metadata")
        assertTrue(text.contains("metadata-test.txt"), "Expected filename in metadata")
        assertTrue(text.contains("Type:"), "Expected 'Type:' in metadata")
        assertTrue(text.contains("Size:"), "Expected 'Size:' in metadata")
    }
}
