package com.hoangworks.ai.domain.llm

sealed interface LlmResult {
    data class Success(val message: LlmMessage.AssistantMessage) : LlmResult
    data class Failure(val message: String, val cause: Throwable? = null) : LlmResult
}
