package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.tool.ToolDefinition

class MockModelClient : LlmClient {
    override suspend fun generate(prompt: String, tools: List<ToolDefinition>): LlmResult = LlmResult.Success("Hi")
}
