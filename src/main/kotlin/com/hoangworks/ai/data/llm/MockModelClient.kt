package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.llm.LlmMessage
import com.hoangworks.ai.domain.tool.ToolDefinition

class MockModelClient : LlmClient {
    override suspend fun generate(messages: List<LlmMessage>, tools: List<ToolDefinition>): LlmResult =
        LlmResult.Success(LlmMessage.AssistantMessage(content = "Hi"))
}
