package com.hoangworks.ai.domain.llm

sealed interface LlmMessage {
    data class UserMessage(val content: String) : LlmMessage

    data class AssistantMessage(
        val content: String? = null,
        val toolRequests: List<ToolRequest> = emptyList()
    ) : LlmMessage

    data class ToolMessage(
        val name: String,
        val content: String
    ) : LlmMessage
}

data class ToolRequest(
    val name: String,
    val arguments: Map<String, String>
)
