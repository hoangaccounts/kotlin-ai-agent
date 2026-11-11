package com.hoangworks.ai.domain.agent

import com.hoangworks.ai.domain.llm.*
import com.hoangworks.ai.domain.tool.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LlmAgentTest {
    @Test
    fun `calculator request completes through tool message`() = runBlocking {
        val client = FakeClient(
            LlmResult.Success(LlmMessage.AssistantMessage(toolRequests = listOf(ToolRequest("calculator", mapOf("input" to "2,3"))))),
            LlmResult.Success(LlmMessage.AssistantMessage("5.0"))
        )
        val result = LlmAgent(client, listOf(CalculatorTestTool())).run("What is 2 + 3?")

        assertEquals(AgentResult.Success("5.0"), result)
        assertEquals(listOf(LlmMessage.UserMessage("What is 2 + 3?")), client.calls[0])
        assertEquals(LlmMessage.ToolMessage("calculator", "5.0"), client.calls[1].last())
    }

    @Test
    fun `multiple tool requests are all executed`() = runBlocking {
        val client = FakeClient(
            LlmResult.Success(LlmMessage.AssistantMessage(toolRequests = listOf(
                ToolRequest("echo", mapOf("input" to "one")), ToolRequest("echo", mapOf("input" to "two"))
            ))), LlmResult.Success(LlmMessage.AssistantMessage("done"))
        )
        val result = LlmAgent(client, listOf(EchoTool())).run("go")
        assertEquals(AgentResult.Success("done"), result)
        assertEquals(listOf(LlmMessage.ToolMessage("echo", "one"), LlmMessage.ToolMessage("echo", "two")), client.calls[1].takeLast(2))
    }

    @Test
    fun `unknown tool and iteration limit are reported in the conversation`() = runBlocking {
        val client = FakeClient(
            LlmResult.Success(LlmMessage.AssistantMessage(toolRequests = listOf(ToolRequest("missing", emptyMap())))),
            LlmResult.Success(LlmMessage.AssistantMessage("recovered"))
        )
        val result = LlmAgent(client, emptyList(), maxIterations = 2).run("go")
        assertEquals(AgentResult.Success("recovered"), result)
        assertEquals(LlmMessage.ToolMessage("missing", "Unknown tool: missing"), client.calls[1].last())
    }

    private class FakeClient(private vararg val results: LlmResult) : LlmClient {
        val calls = mutableListOf<List<LlmMessage>>()
        private var index = 0
        override suspend fun generate(messages: List<LlmMessage>, tools: List<ToolDefinition>): LlmResult {
            calls += messages
            return results[index++]
        }
    }

    private class CalculatorTestTool : Tool {
        override val name = "calculator"; override val description = "calculator"
        override val definition = ToolDefinition(name, description, listOf(ToolParameter("input", ParameterType.STRING, "input")))
        override suspend fun execute(input: String) = ToolResult.Success(input.split(",").sumOf { it.toDouble() }.toString())
    }

    private class EchoTool : Tool {
        override val name = "echo"; override val description = "echo"
        override val definition = ToolDefinition(name, description, listOf(ToolParameter("input", ParameterType.STRING, "input")))
        override suspend fun execute(input: String) = ToolResult.Success(input)
    }
}
