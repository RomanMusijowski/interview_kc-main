package com.finance.tokenservice.processor.error

import org.apache.camel.Exchange
import org.apache.camel.Processor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import com.finance.tokenservice.formatter.ResponseFormatter

@ApplicationScoped
class KafkaPublishErrorProcessor @Inject constructor(
    private val responseFormatter: ResponseFormatter
) : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaPublishErrorProcessor::class.java)
    }

    override fun process(exchange: Exchange) {
        val exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)
        logger.error("Kafka publishing failed: ${exception?.message}", exception)

        val (errorResponse, httpStatus) = responseFormatter.formatErrorResponse(
            "Failed to publish transactions to Kafka",
            ResponseFormatter.ProcessingStep.KAFKA_PUBLISH,
            exception?.message ?: "Unknown Kafka error",
            502
        )

        exchange.message.headers[Exchange.HTTP_RESPONSE_CODE] = httpStatus
        exchange.message.body = errorResponse
    }
}
