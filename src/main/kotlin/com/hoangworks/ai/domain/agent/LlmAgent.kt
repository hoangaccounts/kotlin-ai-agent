package com.hoangworks.ai.domain.agent

import com.hoangworks.ai.domain.agent.AgentResult.*
import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.tool.Tool

class LlmAgent( private val llmClient : LlmClient,
    private val tools : List<Tool>) : Agent {

    private val toolRegistry = tools.associateBy { it.definition.name }

    override suspend fun run(goal: String): AgentResult {
        return when (val result = llmClient.generate(goal,
        tools = toolRegistry.values.map { it.definition})) {
            is LlmResult.Success ->
                Success(result.content)

            is LlmResult.Failure ->
                Failure(result.message)

            is LlmResult.ToolCall -> TODO()
        }
    }
}