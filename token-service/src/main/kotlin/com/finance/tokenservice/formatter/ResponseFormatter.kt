package com.finance.tokenservice.formatter

import com.finance.tokenservice.model.TransactionsResponse
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant

/**
 * Domain service for formatting HTTP responses.
 * 
 * Single Responsibility: Format responses for HTTP endpoints.
 * Separates response formatting from route logic.
 */
@ApplicationScoped
class ResponseFormatter {
    
    /**
     * Formats a success response after transactions are published to Kafka.
     */
    fun formatSuccessResponse(transactionsResponse: TransactionsResponse, kafkaTopic: String): Map<String, Any> {
        return mapOf(
            "status" to "success",
            "message" to "Transactions published to Kafka successfully",
            "userId" to transactionsResponse.userId,
            "transactionCount" to transactionsResponse.count,
            "kafkaTopic" to kafkaTopic,
            "timestamp" to Instant.now().toString()
        )
    }
    
    /**
     * Formats an error response with step information.
     */
    fun formatErrorResponse(
        error: String,
        step: ProcessingStep,
        details: String? = null,
        httpStatus: Int = 500
    ): Pair<Map<String, Any>, Int> {
        return Pair(
            mapOf(
                "status" to "error",
                "error" to error,
                "step" to step.name.lowercase(),
                "details" to (details ?: "No additional details"),
                "timestamp" to Instant.now().toString()
            ),
            httpStatus
        )
    }
    
    enum class ProcessingStep {
        TOKEN_EXCHANGE,
        TRANSACTION_FETCH,
        KAFKA_PUBLISH
    }
}







