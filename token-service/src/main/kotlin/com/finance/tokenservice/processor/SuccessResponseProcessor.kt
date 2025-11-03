package com.finance.tokenservice.processor

import com.finance.tokenservice.config.KafkaConfig
import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.processor.TransactionToMessageProcessor.Headers
import com.finance.tokenservice.formatter.ResponseFormatter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

@ApplicationScoped
class SuccessResponseProcessor @Inject constructor(
    private val responseFormatter: ResponseFormatter,
    private val kafkaConfig: KafkaConfig
) : Processor {
    
    companion object {
        private val logger = LoggerFactory.getLogger(SuccessResponseProcessor::class.java)
    }
    
    override fun process(exchange: Exchange) {
        // Retrieve stored TransactionsResponse from header
        val transactionsResponse = exchange.message.getHeader(Headers.TRANSACTIONS_RESPONSE) 
            as? TransactionsResponse
        
        val topicName = kafkaConfig.topic()
        
        if (transactionsResponse == null) {
            logger.warn("TransactionsResponse not found in headers, using default response")
            exchange.message.body = mapOf(
                "status" to "success",
                "message" to "Transactions published to Kafka successfully",
                "kafkaTopic" to topicName
            )
        } else {
            logger.info("Formatting success response for user: ${transactionsResponse.userId}")
            val successResponse = responseFormatter.formatSuccessResponse(transactionsResponse, topicName)
            exchange.message.body = successResponse
        }
        
        exchange.message.headers[Exchange.HTTP_RESPONSE_CODE] = 200
    }
}

