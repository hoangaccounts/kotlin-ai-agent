package com.hoangworks.ai.domain.llm

interface LlmClient {
    suspend fun generate(prompt: String): LlmResult
}
