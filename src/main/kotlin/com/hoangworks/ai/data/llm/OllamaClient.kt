package com.hoangworks.ai.data.llm

import com.hoangworks.ai.domain.llm.LlmResult
import com.hoangworks.ai.domain.llm.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class OllamaClient : LlmClient {
    private val httpClient = HttpClient.newHttpClient()

    override suspend fun generate(prompt: String): LlmResult = try {
        val requestBody = buildJsonObject {
            put("model", "gpt-oss:20b")
            put("prompt", prompt)
            put("stream", false)
        }.toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/generate"))
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

        val content = Json.parseToJsonElement(response.body()).jsonObject["response"]
            ?.jsonPrimitive?.content
            ?: return LlmResult.Failure("Missing response field")

        LlmResult.Success(content)
    } catch (exception: Exception) {
        LlmResult.Failure(
            message = exception.message ?: "Unknown Ollama error",
            cause = exception
        )
    }
}
