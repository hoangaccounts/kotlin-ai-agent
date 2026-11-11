package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.*
import com.hoangworks.ai.domain.tool.ToolDefinition
import com.hoangworks.ai.domain.tool.ToolParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class OllamaClient : LlmClient {
    private val httpClient = HttpClient.newHttpClient()

    override suspend fun generate(messages: List<LlmMessage>, tools: List<ToolDefinition>): LlmResult = try {
        val request = HttpRequest.newBuilder().uri(URI.create("http://localhost:11434/api/chat"))
            .header("Content-Type", "application/json").timeout(Duration.ofMinutes(5))
            .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(messages, tools))).build()
        val response = withContext(Dispatchers.IO) { httpClient.send(request, HttpResponse.BodyHandlers.ofString()) }
        if (response.statusCode() !in 200..299) LlmResult.Failure("Ollama HTTP ${response.statusCode()}")
        else parseResponse(response.body())
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { LlmResult.Failure(e.message ?: "Unknown Ollama error", e) }

    private fun buildRequestBody(messages: List<LlmMessage>, tools: List<ToolDefinition>): String = buildJsonObject {
        put("model", "gpt-oss:20b"); put("stream", false)
        putJsonArray("messages") { messages.forEach { add(toOllamaMessage(it)) } }
        if (tools.isNotEmpty()) putJsonArray("tools") { tools.forEach { add(buildToolDefinition(it)) } }
    }.toString()

    private fun toOllamaMessage(message: LlmMessage): JsonObject = buildJsonObject {
        when (message) {
            is LlmMessage.UserMessage -> { put("role", "user"); put("content", message.content) }
            is LlmMessage.AssistantMessage -> {
                put("role", "assistant"); message.content?.let { put("content", it) }
                if (message.toolRequests.isNotEmpty()) putJsonArray("tool_calls") {
                    message.toolRequests.forEach { request -> add(buildJsonObject {
                        putJsonObject("function") { put("name", request.name); putJsonObject("arguments") {
                            request.arguments.forEach { (key, value) -> put(key, value) }
                        } }
                    }) }
                }
            }
            is LlmMessage.ToolMessage -> { put("role", "tool"); put("name", message.name); put("content", message.content) }
        }
    }

    private fun buildToolDefinition(tool: ToolDefinition): JsonObject = buildJsonObject {
        put("type", "function"); putJsonObject("function") {
            put("name", tool.name); put("description", tool.description); put("parameters", buildToolParameters(tool.parameters))
        }
    }

    private fun buildToolParameters(parameters: List<ToolParameter>): JsonObject = buildJsonObject {
        put("type", "object"); putJsonObject("properties") { parameters.forEach { parameter -> putJsonObject(parameter.name) {
            put("type", parameter.type.name.lowercase()); put("description", parameter.description)
        } } }; putJsonArray("required") { parameters.filter { it.required }.forEach { add(it.name) } }
    }

    private fun parseResponse(body: String): LlmResult {
        val message = Json.parseToJsonElement(body).jsonObject["message"]?.jsonObject
            ?: return LlmResult.Failure("Missing message")
        val requests = message["tool_calls"]?.jsonArray?.map { call ->
            val function = call.jsonObject["function"]?.jsonObject ?: return LlmResult.Failure("Missing function")
            val name = function["name"]?.jsonPrimitive?.contentOrNull ?: return LlmResult.Failure("Missing tool name")
            val arguments = function["arguments"]?.jsonObject?.mapValues { (_, value) -> value.jsonPrimitive.content }
                ?: return LlmResult.Failure("Invalid tool arguments")
            ToolRequest(name, arguments)
        } ?: emptyList()
        val content = message["content"]?.jsonPrimitive?.contentOrNull
        if (content == null && requests.isEmpty()) return LlmResult.Failure("Missing message content")
        return LlmResult.Success(LlmMessage.AssistantMessage(content, requests))
    }
}
