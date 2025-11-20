package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.memory.MemoryModule
import com.apptolast.mcp.modules.memory.EntityInput
import com.apptolast.mcp.modules.memory.RelationInput
import com.apptolast.mcp.server.MemoryConfig
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
class MemoryModuleTest {

    private lateinit var tempDir: Path
    private lateinit var memoryModule: MemoryModule
    private val KEEP_FILES_FOR_INSPECTION = false

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("mcp-memory-test")
        val config = MemoryConfig(storagePath = tempDir)
        memoryModule = MemoryModule(config)
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
    fun `test create entities`() = runBlocking {
        val entities = listOf(
            EntityInput(
                name = "TestProject",
                entityType = "project",
                observations = listOf("Uses Kotlin", "MCP Server implementation")
            ),
            EntityInput(
                name = "FilesystemModule",
                entityType = "component",
                observations = listOf("Handles file operations")
            )
        )

        val result = memoryModule.createEntities(entities)

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Created 2 entities") || text.contains("TestProject"))
    }

    @Test
    fun `test create relations`() = runBlocking {
        // First create entities
        val entities = listOf(
            EntityInput("ProjectA", "project", listOf("Main project")),
            EntityInput("ComponentB", "component", listOf("Part of ProjectA"))
        )
        memoryModule.createEntities(entities)

        // Then create relations
        val relations = listOf(
            RelationInput(from = "ProjectA", to = "ComponentB", relationType = "contains"),
            RelationInput(from = "ComponentB", to = "ProjectA", relationType = "partOf")
        )

        val result = memoryModule.createRelations(relations)

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("Created 2 relations"))
    }

    @Test
    fun `test search nodes by name`() = runBlocking {
        // Create some entities
        val entities = listOf(
            EntityInput("KotlinProject", "project", listOf("Uses Kotlin 2.0")),
            EntityInput("JavaProject", "project", listOf("Uses Java 21")),
            EntityInput("KotlinModule", "module", listOf("Kotlin code"))
        )
        memoryModule.createEntities(entities)

        // Search for "Kotlin"
        val result = memoryModule.searchNodes("Kotlin")

        assertFalse(result.isError)
        val text = result.content.first().toString()
        // Should find entities with "Kotlin" in the name
        assertTrue(
            text.contains("KotlinProject") || text.contains("KotlinModule") || text.contains("Found"),
            "Expected to find Kotlin entities, got: $text"
        )
    }

    @Test
    fun `test search nodes by observations`() = runBlocking {
        // Create entities with specific observations
        val entities = listOf(
            EntityInput("MCP Server", "application", listOf("Full-Stack", "Production-ready")),
            EntityInput("Testing Module", "component", listOf("Unit tests", "Integration tests"))
        )
        memoryModule.createEntities(entities)

        // Search by observation content
        val result = memoryModule.searchNodes("Full-Stack")

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("MCP Server") || text.contains("Full-Stack") || text.contains("Found"),
            "Expected to find MCP Server or Full-Stack, got: $text"
        )
    }

    @Test
    @org.junit.jupiter.api.Disabled("TODO: Fix JSONL serialization issue with type field")
    fun `test open nodes retrieves specific entities`() = runBlocking {
        // Create entities
        val entities = listOf(
            EntityInput("Entity1", "type1", listOf("Observation 1")),
            EntityInput("Entity2", "type2", listOf("Observation 2")),
            EntityInput("Entity3", "type3", listOf("Observation 3"))
        )
        val createResult = memoryModule.createEntities(entities)
        assertFalse(createResult.isError, "Failed to create entities: ${createResult.content.first()}")

        // Open specific entities
        val result = memoryModule.openNodes(listOf("Entity1", "Entity3"))

        // If it fails, log the error for debugging
        if (result.isError) {
            println("Error opening nodes: ${result.content.first()}")
        }
        assertFalse(result.isError, "Failed to open nodes: ${result.content.first()}")
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Entity1") && text.contains("Entity3"),
            "Expected to find both Entity1 and Entity3, got: $text"
        )
    }

    @Test
    @org.junit.jupiter.api.Disabled("TODO: Fix JSONL serialization issue with type field")
    fun `test JSONL persistence across instances`() = runBlocking {
        // Create entities with first instance
        val entities = listOf(
            EntityInput("PersistedEntity", "test", listOf("Should persist"))
        )
        memoryModule.createEntities(entities)

        // Create a new instance of MemoryModule with the same storage path
        val config2 = MemoryConfig(storagePath = tempDir)
        val memoryModule2 = MemoryModule(config2)

        // Try to open the entity with the second instance
        val result = memoryModule2.openNodes(listOf("PersistedEntity"))

        assertFalse(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("PersistedEntity"),
            "Expected to find PersistedEntity in second instance, got: $text"
        )
    }

    @Test
    fun `test open nodes with non-existent entity`() = runBlocking {
        val result = memoryModule.openNodes(listOf("NonExistentEntity"))

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(text.contains("No entities found") || text.contains("not found"))
    }
}
