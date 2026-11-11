
package com.hoangworks.ai.domain.tool

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>
)

data class ToolParameter(
    val name: String,
    val type: ParameterType,
    val description: String,
    val required: Boolean = true
)

enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN
}
