package com.hoangworks.ai.domain.agent

interface Agent {
    suspend fun run(goal : String): AgentResult
}