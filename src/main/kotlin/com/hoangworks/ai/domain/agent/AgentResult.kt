package com.hoangworks.ai.domain.agent

sealed interface AgentResult {
    data class Success(val answer: String) : AgentResult
    data class Failure(val message: String, val cause: Throwable? = null) : AgentResult
}
