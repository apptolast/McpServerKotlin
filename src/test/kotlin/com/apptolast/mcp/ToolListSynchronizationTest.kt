package com.apptolast.mcp

import com.apptolast.mcp.server.ToolRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test to ensure tool lists remain synchronized across the codebase.
 *
 * Now that we have a centralized ToolRegistry.TOOLS_BY_MODULE as the single source of truth,
 * this test verifies that the registry is consistent and follows naming conventions.
 *
 * This test prevents:
 * - Inconsistent tool counts
 * - Missing modules
 * - Incorrect tool naming conventions
 */
class ToolListSynchronizationTest {

    /**
     * Expected tool counts per module (documented baseline)
     */
    private val expectedToolCounts = mapOf(
        "filesystem" to 5,  // readFile, writeFile, listDirectory, createDirectory, deleteFile
        "bash" to 1,        // execute
        "github" to 6,      // status, commit, push, clone, log, branch
        "memory" to 4,      // createEntities, createRelations, searchNodes, openNodes
        "postgresql" to 3,  // executeQuery, getSchema, testConnection
        "mongodb" to 5,     // find, listCollections, countDocuments, aggregate, testConnection
        "resources" to 4    // listResources, readResource, createResource, deleteResource
    )

    @Test
    fun `verify ToolRegistry has correct tool counts per module`() {
        // Verify each module has the expected number of tools
        expectedToolCounts.forEach { (module, expectedCount) ->
            val actualCount = ToolRegistry.getModuleToolCount(module)
            assertEquals(
                expectedCount,
                actualCount,
                "Module '$module' has $actualCount tools in ToolRegistry but should have $expectedCount tools"
            )
        }
    }

    @Test
    fun `verify ToolRegistry total tool count matches expected`() {
        val expectedTotal = expectedToolCounts.values.sum()
        
        assertEquals(
            expectedTotal,
            ToolRegistry.TOTAL_TOOLS,
            "Total tool count in ToolRegistry (${ToolRegistry.TOTAL_TOOLS}) doesn't match expected ($expectedTotal)"
        )
    }

    @Test
    fun `verify all expected modules are present in ToolRegistry`() {
        val expectedModules = expectedToolCounts.keys
        val actualModules = ToolRegistry.getAllModules()

        assertEquals(
            expectedModules.sorted(),
            actualModules.sorted(),
            "Module list in ToolRegistry doesn't match expected modules"
        )
    }

    @Test
    fun `verify tool names follow consistent naming conventions`() {
        // Bash module: should use 'execute' not 'bashExecute'
        assertTrue(
            ToolRegistry.hasToolInModule("bash", "execute"),
            "Bash module should expose 'execute' tool"
        )

        // PostgreSQL tools should use camelCase starting with lowercase
        ToolRegistry.TOOLS_BY_MODULE["postgresql"]?.forEach { toolName ->
            assertTrue(
                toolName.first().isLowerCase(),
                "PostgreSQL tool '$toolName' should start with lowercase letter"
            )
        }

        // MongoDB tools should use camelCase starting with lowercase
        ToolRegistry.TOOLS_BY_MODULE["mongodb"]?.forEach { toolName ->
            assertTrue(
                toolName.first().isLowerCase(),
                "MongoDB tool '$toolName' should start with lowercase letter"
            )
        }
    }

    @Test
    fun `verify ToolRegistry TOOLS_BY_MODULE contains all expected tools`() {
        // Filesystem
        assertEquals(
            listOf("readFile", "writeFile", "listDirectory", "createDirectory", "deleteFile"),
            ToolRegistry.TOOLS_BY_MODULE["filesystem"],
            "Filesystem tools don't match expected"
        )

        // Bash
        assertEquals(
            listOf("execute"),
            ToolRegistry.TOOLS_BY_MODULE["bash"],
            "Bash tools don't match expected"
        )

        // GitHub
        assertEquals(
            listOf("status", "commit", "push", "clone", "log", "branch"),
            ToolRegistry.TOOLS_BY_MODULE["github"],
            "GitHub tools don't match expected"
        )

        // Memory
        assertEquals(
            listOf("createEntities", "createRelations", "searchNodes", "openNodes"),
            ToolRegistry.TOOLS_BY_MODULE["memory"],
            "Memory tools don't match expected"
        )

        // PostgreSQL
        assertEquals(
            listOf("executeQuery", "getSchema", "testConnection"),
            ToolRegistry.TOOLS_BY_MODULE["postgresql"],
            "PostgreSQL tools don't match expected"
        )

        // MongoDB
        assertEquals(
            listOf("find", "listCollections", "countDocuments", "aggregate", "testConnection"),
            ToolRegistry.TOOLS_BY_MODULE["mongodb"],
            "MongoDB tools don't match expected"
        )

        // Resources
        assertEquals(
            listOf("listResources", "readResource", "createResource", "deleteResource"),
            ToolRegistry.TOOLS_BY_MODULE["resources"],
            "Resources tools don't match expected"
        )
    }
}
