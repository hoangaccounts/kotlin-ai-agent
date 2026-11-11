package com.hoangworks.ai.domain.llm

import com.hoangworks.ai.domain.tool.ToolDefinition

interface LlmClient {
    suspend fun generate(
        messages: List<LlmMessage>,
        tools: List<ToolDefinition> = emptyList()
    ): LlmResult
}
