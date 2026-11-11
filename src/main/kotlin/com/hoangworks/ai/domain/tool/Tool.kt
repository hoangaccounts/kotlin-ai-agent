
package com.hoangworks.ai.domain.tool

interface Tool {
    val name: String
    val description: String
    val definition: ToolDefinition

    suspend fun execute(input: String): ToolResult
}
