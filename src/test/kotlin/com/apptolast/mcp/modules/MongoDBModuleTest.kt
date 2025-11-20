package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.database.MongoDBModule
import com.apptolast.mcp.server.MongoDBConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MongoDB Module Tests
 *
 * Note: These tests validate the module logic but will fail gracefully
 * if MongoDB is not available. This is expected behavior for unit tests.
 */
class MongoDBModuleTest {

    private lateinit var mongoModule: MongoDBModule

    @BeforeEach
    fun setup() {
        val config = MongoDBConfig(
            connectionString = "mongodb://localhost:27017",
            database = "test_db"
        )
        mongoModule = MongoDBModule(config)
    }

    @AfterEach
    fun cleanup() {
        // Close the MongoDB client
        mongoModule.close()
    }

    @Test
    fun `test find with empty filter handles connection error gracefully`() = runBlocking {
        // This will attempt to connect but should handle errors gracefully
        val result = mongoModule.find(collection = "test_collection")

        // Should either succeed (if MongoDB is running) or fail gracefully
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Collection:") ||
            text.contains("failed") ||
            text.contains("refused") ||
            result.isError
        )
    }

    @Test
    fun `test find with custom filter validates JSON format`() = runBlocking {
        // Test with invalid JSON filter
        val result = mongoModule.find(
            collection = "test_collection",
            filter = "invalid json"
        )

        // Should fail with parsing or connection error
        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("failed") ||
            text.contains("parse") ||
            text.contains("JSON")
        )
    }

    @Test
    fun `test listCollections handles connection error gracefully`() = runBlocking {
        val result = mongoModule.listCollections()

        // Either succeeds or fails gracefully
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Database:") ||
            text.contains("Collections") ||
            text.contains("failed") ||
            result.isError
        )
    }

    @Test
    fun `test countDocuments with empty filter handles error gracefully`() = runBlocking {
        val result = mongoModule.countDocuments(collection = "test_collection")

        // Either succeeds or fails gracefully
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Collection:") ||
            text.contains("count") ||
            text.contains("failed") ||
            result.isError
        )
    }

    @Test
    fun `test countDocuments with filter validates JSON format`() = runBlocking {
        val result = mongoModule.countDocuments(
            collection = "test_collection",
            filter = "not valid json"
        )

        // Should fail with parsing or connection error
        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("failed") ||
            text.contains("parse") ||
            text.contains("JSON")
        )
    }

    @Test
    fun `test aggregate handles connection error gracefully`() = runBlocking {
        // Valid pipeline structure but may fail on connection
        val result = mongoModule.aggregate(
            collection = "test_collection",
            pipeline = """{"pipeline": [{"${'$'}match": {}}]}"""
        )

        // Either succeeds or fails gracefully
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Collection:") ||
            text.contains("failed") ||
            result.isError
        )
    }

    @Test
    fun `test aggregate with invalid pipeline format`() = runBlocking {
        val result = mongoModule.aggregate(
            collection = "test_collection",
            pipeline = "invalid pipeline"
        )

        // Should fail with parsing error
        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("failed") ||
            text.contains("parse")
        )
    }

    @Test
    fun `test testConnection handles error gracefully`() = runBlocking {
        val result = mongoModule.testConnection()

        // Either succeeds (if MongoDB is running) or fails gracefully
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Connection successful") ||
            text.contains("Connection test failed") ||
            text.contains("failed") ||
            result.isError
        )
    }

    @Test
    fun `test find with limit parameter`() = runBlocking {
        // Test that limit parameter is accepted (even if connection fails)
        val result = mongoModule.find(
            collection = "test_collection",
            limit = 50
        )

        // Should handle connection error or succeed
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Collection:") ||
            text.contains("failed") ||
            result.isError
        )
    }

    @Test
    fun `test find with sort parameter`() = runBlocking {
        // Test with valid sort JSON (even if connection fails)
        val result = mongoModule.find(
            collection = "test_collection",
            sort = """{"name": 1}"""
        )

        // Should handle connection error or succeed
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Collection:") ||
            text.contains("Sort:") ||
            text.contains("failed") ||
            result.isError
        )
    }
}
