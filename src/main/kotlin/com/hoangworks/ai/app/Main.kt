package com.hoangworks.ai.app

import com.hoangworks.ai.data.llm.OllamaClient
import com.hoangworks.ai.domain.llm.LlmResult

suspend fun main() {
    println("Agent and Harness!")

    val client = OllamaClient()
    while (true) {
        print("\nPlease enter your prompt (Ctrl-D to exit): ")
        val input = readlnOrNull() ?: break
        if (input.isBlank()) continue

        when (val result = client.generate(input)) {
            is LlmResult.Success -> println(result.content)
            is LlmResult.Failure -> println("Request failed: ${result.message}")
        }
    }

    println("\nGoodbye!")
}
