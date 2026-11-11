package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.tool.ToolDefinition
import com.hoangworks.ai.domain.tool.ToolParameter
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

    override suspend fun generate(prompt: String, tools: List<ToolDefinition>): LlmResult = try {


        val requestBody = buildRequestBody(prompt, tools)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/chat"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(5))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = withContext(Dispatchers.IO) {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }

        if (response.statusCode() !in 200..299) {
            return LlmResult.Failure("Ollama HTTP ${response.statusCode()}")
        }

        val json = Json.parseToJsonElement(response.body()).jsonObject
        val content = json["message"]
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            ?: return LlmResult.Failure("Missing message content")

        LlmResult.Success(content)
    } catch (exception: Exception) {
        LlmResult.Failure(
            message = exception.message ?: "Unknown Ollama error",
            cause = exception
        )
    }


    private fun buildRequestBody(
        prompt: String,
        tools: List<ToolDefinition>
    ): String = buildJsonObject {
        put("model", "gpt-oss:20b")
        put("stream", false)
        putJsonArray("messages") {
            add(buildUserMessage(prompt))
        }
        putJsonArray("tools") {
            tools.forEach { add(buildToolDefinition(it)) }
        }
    }.toString()


    private fun buildUserMessage(prompt: String): JsonObject =
        buildJsonObject {
            put("role", "user")
            put("content", prompt)
        }


    private fun buildToolDefinition(tool: ToolDefinition): JsonObject =
        buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", tool.name)
                put("description", tool.description)
                put("parameters", buildToolParameters(tool.parameters))            }
        }

    private fun buildToolParameters(
        parameters: List<ToolParameter>
    ): JsonObject = buildJsonObject {
        put("type", "object")

        putJsonObject("properties") {
            parameters.forEach { parameter ->
                putJsonObject(parameter.name) {
                    put("type", parameter.type.name.lowercase())
                    put("description", parameter.description)
                }
            }
        }

        putJsonArray("required") {
            parameters.filter { it.required }.forEach {
                add(it.name)
            }
        }
    }




}

