
package com.hoangworks.ai.domain.tool

sealed interface ToolResult {
    data class Success(val output: String) : ToolResult
    data class Failure(val message: String) : ToolResult
}
