package com.hoangworks.ai.domain.agent

import com.hoangworks.ai.domain.agent.AgentResult.*
import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.llm.LlmMessage
import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.tool.Tool
import com.hoangworks.ai.domain.tool.ToolResult

class LlmAgent( private val llmClient : LlmClient,
    private val tools : List<Tool>,
    private val maxIterations: Int = 10) : Agent {

    private val toolRegistry = tools.associateBy { it.definition.name }

    override suspend fun run(goal: String): AgentResult {
        if (maxIterations <= 0) return Failure("Iteration limit must be greater than zero")

        val messages = mutableListOf<LlmMessage>(LlmMessage.UserMessage(goal))
        for (iteration in 0 until maxIterations) {
            when (val result = llmClient.generate(messages.toList(), toolRegistry.values.map { it.definition })) {
                is LlmResult.Failure -> return Failure(result.message, result.cause)
                is LlmResult.Success -> {
                    val assistant = result.message
                    messages += assistant
                    if (assistant.toolRequests.isEmpty()) {
                        return assistant.content?.let(::Success)
                            ?: Failure("Assistant response did not contain an answer")
                    }

                    assistant.toolRequests.forEach { request ->
                        messages += executeTool(request)
                    }
                }
            }
        }

        return Failure("Agent iteration limit reached")
    }

    private suspend fun executeTool(request: com.hoangworks.ai.domain.llm.ToolRequest): LlmMessage.ToolMessage {
        val tool = toolRegistry[request.name]
        if (tool == null) return LlmMessage.ToolMessage(request.name, "Unknown tool: ${request.name}")

        val input = request.arguments["input"]
            ?: return LlmMessage.ToolMessage(request.name, "Malformed arguments: missing input")

        return when (val result = tool.execute(input)) {
            is ToolResult.Success -> LlmMessage.ToolMessage(request.name, result.output)
            is ToolResult.Failure -> LlmMessage.ToolMessage(request.name, result.message)
        }
    }
}
