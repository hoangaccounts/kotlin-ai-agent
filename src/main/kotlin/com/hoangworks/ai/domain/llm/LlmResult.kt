package com.hoangworks.ai.domain.llm

sealed interface LlmResult {
    data class Success(val content: String) : LlmResult
    data class ToolCall(val name: String, val arguments: Map<String, String>
    ) : LlmResult    data class Failure(val message: String, val cause: Throwable? = null) : LlmResult
}
