
package com.hoangworks.ai.domain.tool

interface Tool {
    val name: String
    val description: String

    suspend fun execute(input: String): ToolResult
}
