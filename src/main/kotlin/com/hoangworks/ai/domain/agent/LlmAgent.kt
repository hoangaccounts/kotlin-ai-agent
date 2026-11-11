package com.hoangworks.ai.domain.agent

import com.hoangworks.ai.domain.agent.AgentResult.*
import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.tool.Tool
import com.hoangworks.ai.domain.tool.ToolResult

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

            is LlmResult.ToolCall -> {
                val tool = toolRegistry[result.name]
                    ?: return AgentResult.Failure(
                        "Unknown tool: ${result.name}"
                    )

                val input = result.arguments["input"]
                    ?: return AgentResult.Failure("Missing input")

                when (val toolResult = tool.execute(input)) {
                    is ToolResult.Success ->
                        AgentResult.Success(toolResult.output)

                    is ToolResult.Failure ->
                        AgentResult.Failure(toolResult.message)
                }
            }

        }
    }
}