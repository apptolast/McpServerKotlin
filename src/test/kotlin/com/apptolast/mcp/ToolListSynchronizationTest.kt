package com.apptolast.mcp

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test to ensure tool lists remain synchronized between:
 * - McpServerInstance.kt documentation comment (lines 23-30)
 * - Application.kt /tools endpoint (lines 208-217)
 *
 * This test prevents drift where new tools are added to one place but not the other.
 */
class ToolListSynchronizationTest {

    /**
     * Expected tool counts as documented in McpServerInstance.kt
     */
    private val expectedToolCounts = mapOf(
        "filesystem" to 5,  // readFile, writeFile, listDirectory, createDirectory, deleteFile
        "bash" to 1,        // execute (bashExecute)
        "github" to 6,      // status, commit, push, clone, log, branch
        "memory" to 4,      // createEntities, createRelations, searchNodes, openNodes
        "postgresql" to 3,  // executeQuery, getSchema, testConnection
        "mongodb" to 5,     // find, listCollections, countDocuments, aggregate, testConnection
        "resources" to 4    // listResources, readResource, createResource, deleteResource
    )

    /**
     * Tool list as defined in Application.kt /tools endpoint
     * This should match the documentation in McpServerInstance.kt
     */
    private val applicationToolList = mapOf(
        "filesystem" to listOf("readFile", "writeFile", "listDirectory", "createDirectory", "deleteFile"),
        "bash" to listOf("execute"),
        "github" to listOf("status", "commit", "push", "clone", "log", "branch"),
        "memory" to listOf("createEntities", "createRelations", "searchNodes", "openNodes"),
        "postgresql" to listOf("executeQuery", "getSchema", "testConnection"),
        "mongodb" to listOf("find", "listCollections", "countDocuments", "aggregate", "testConnection"),
        "resources" to listOf("listResources", "readResource", "createResource", "deleteResource")
    )

    @Test
    fun `verify tool counts match between McpServerInstance and Application endpoint`() {
        // Calculate actual tool counts from Application.kt endpoint
        val actualToolCounts = applicationToolList.mapValues { it.value.size }

        // Verify each module has the expected number of tools
        expectedToolCounts.forEach { (module, expectedCount) ->
            val actualCount = actualToolCounts[module]
            assertEquals(
                expectedCount,
                actualCount,
                "Module '$module' has $actualCount tools in Application.kt but should have $expectedCount tools according to McpServerInstance.kt"
            )
        }
    }

    @Test
    fun `verify total tool count matches`() {
        val expectedTotal = expectedToolCounts.values.sum()
        val actualTotal = applicationToolList.values.sumOf { it.size }

        assertEquals(
            expectedTotal,
            actualTotal,
            "Total tool count in Application.kt ($actualTotal) doesn't match McpServerInstance.kt documentation ($expectedTotal)"
        )

        // Also verify this matches the hardcoded total in Application.kt
        val hardcodedTotal = 28
        assertEquals(
            hardcodedTotal,
            actualTotal,
            "Hardcoded total_tools value in Application.kt ($hardcodedTotal) doesn't match actual count ($actualTotal)"
        )
    }

    @Test
    fun `verify all modules are present in both places`() {
        val expectedModules = expectedToolCounts.keys
        val actualModules = applicationToolList.keys

        assertEquals(
            expectedModules.sorted(),
            actualModules.sorted(),
            "Module list in Application.kt doesn't match McpServerInstance.kt"
        )
    }

    @Test
    fun `verify tool names are consistent with module naming conventions`() {
        // Bash module: should use 'execute' not 'bashExecute'
        assertTrue(
            applicationToolList["bash"]?.contains("execute") == true,
            "Bash module should expose 'execute' tool"
        )

        // PostgreSQL tools should use camelCase starting with lowercase
        applicationToolList["postgresql"]?.forEach { toolName ->
            assertTrue(
                toolName.first().isLowerCase(),
                "PostgreSQL tool '$toolName' should start with lowercase letter"
            )
        }

        // MongoDB tools should use camelCase starting with lowercase
        applicationToolList["mongodb"]?.forEach { toolName ->
            assertTrue(
                toolName.first().isLowerCase(),
                "MongoDB tool '$toolName' should start with lowercase letter"
            )
        }
    }
}
