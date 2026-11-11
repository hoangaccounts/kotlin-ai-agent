
package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.LlmClient
import com.hoangworks.ai.domain.llm.LlmResult
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

    override suspend fun generate(
        prompt: String,
        tools: List<ToolDefinition>
    ): LlmResult = try {
        val request = buildHttpRequest(
            buildRequestBody(prompt, tools)
        )

        val response = withContext(Dispatchers.IO) {
            httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )
        }

        if (response.statusCode() !in 200..299) {
            LlmResult.Failure(
                "Ollama HTTP ${response.statusCode()}"
            )
        } else {
            parseResponse(response.body())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LlmResult.Failure(
            message = e.message ?: "Unknown Ollama error",
            cause = e
        )
    }

    private fun buildHttpRequest(body: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/chat"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(5))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

    private fun buildRequestBody(
        prompt: String,
        tools: List<ToolDefinition>
    ): String = buildJsonObject {
        put("model", "gpt-oss:20b")
        put("stream", false)

        putJsonArray("messages") {
            add(buildUserMessage(prompt))
        }

        if (tools.isNotEmpty()) {
            putJsonArray("tools") {
                tools.forEach { tool ->
                    add(buildToolDefinition(tool))
                }
            }
        }
    }.toString()

    private fun buildUserMessage(prompt: String): JsonObject =
        buildJsonObject {
            put("role", "user")
            put("content", prompt)
        }

    private fun buildToolDefinition(
        tool: ToolDefinition
    ): JsonObject = buildJsonObject {
        put("type", "function")

        putJsonObject("function") {
            put("name", tool.name)
            put("description", tool.description)
            put(
                "parameters",
                buildToolParameters(tool.parameters)
            )
        }
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
            parameters
                .filter { it.required }
                .forEach { add(it.name) }
        }
    }

    private fun parseResponse(body: String): LlmResult {
        val json = Json.parseToJsonElement(body).jsonObject

        val message = json["message"]?.jsonObject
            ?: return LlmResult.Failure("Missing message")

        val toolCall = message["tool_calls"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject

        if (toolCall != null) {
            val function = toolCall["function"]?.jsonObject
                ?: return LlmResult.Failure("Missing function")

            val name = function["name"]
                ?.jsonPrimitive
                ?.content
                ?: return LlmResult.Failure("Missing tool name")

            val arguments = parseToolArguments(function)
                ?: return LlmResult.Failure("Invalid tool arguments")

            return LlmResult.ToolCall(name, arguments)
        }

        val content = message["content"]
            ?.jsonPrimitive
            ?.content
            ?: return LlmResult.Failure("Missing message content")

        return LlmResult.Success(content)
    }


    private fun parseToolArguments(
        function: JsonObject
    ): Map<String, String>? {
        val arguments = function["arguments"]?.jsonObject
            ?: return null

        return arguments.mapValues { (_, value) ->
            value.jsonPrimitive.content
        }
    }

}
