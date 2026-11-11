
package com.hoangworks.ai.data.tool

import com.hoangworks.ai.domain.tool.Tool
import com.hoangworks.ai.domain.tool.ToolResult

class CalculatorTool : Tool {
    override val name = "calculator"
    override val description = "Adds two numbers separated by a comma"

    override suspend fun execute(input: String): ToolResult {
        val numbers = input.split(",")

        if (numbers.size != 2) {
            return ToolResult.Failure("Expected two numbers")
        }

        val a = numbers[0].trim().toDoubleOrNull()
        val b = numbers[1].trim().toDoubleOrNull()

        if (a == null || b == null) {
            return ToolResult.Failure("Invalid numbers")
        }

        return ToolResult.Success((a + b).toString())
    }
}
