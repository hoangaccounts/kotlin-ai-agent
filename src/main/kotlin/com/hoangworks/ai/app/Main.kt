
package com.hoangworks.ai.app

import com.hoangworks.ai.data.llm.OllamaClient
import com.hoangworks.ai.data.tool.CalculatorTool
import com.hoangworks.ai.domain.agent.AgentResult
import com.hoangworks.ai.domain.agent.LlmAgent

suspend fun main() {
    val agent = LlmAgent(OllamaClient(), tools = listOf(CalculatorTool()))

    println("Agent and Harness!")

    while (true) {
        print("\nEnter a goal (or 'exit'): ")
        val goal = readlnOrNull() ?: break

        if (goal.equals("exit", ignoreCase = true)) break

        when (val result = agent.run(goal)) {
            is AgentResult.Success -> println(result.answer)
            is AgentResult.Failure -> println("Error: ${result.message}")
        }
    }
}
