package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.llm.LlmClient

class MockModelClient : LlmClient {
    override suspend fun generate(prompt: String): LlmResult = LlmResult.Success("Hi")
}
