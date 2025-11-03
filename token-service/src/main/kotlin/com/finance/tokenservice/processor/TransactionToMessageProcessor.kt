package com.finance.tokenservice.processor

import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.formatter.KafkaMessageFormatter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

@ApplicationScoped
class TransactionToMessageProcessor @Inject constructor(
    private val kafkaMessageFormatter: KafkaMessageFormatter
) : Processor {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TransactionToMessageProcessor::class.java)
    }
    
    /**
     * Header name where TransactionsResponse is stored before Kafka publishing.
     * Made public for use in SuccessResponseProcessor.
     */
    object Headers {
        const val TRANSACTIONS_RESPONSE = "transactionsResponse"
        const val KAFKA_MESSAGES_COUNT = "kafkaMessagesCount"
    }
    
    override fun process(exchange: Exchange) {
        val transactionsResponse = exchange.message.body as? TransactionsResponse
            ?: throw IllegalStateException("Expected TransactionsResponse in exchange body")

        // Store original response in header for final HTTP response
        exchange.message.headers[Headers.TRANSACTIONS_RESPONSE] = transactionsResponse
        
        // Domain transformation: Split transactions into individual messages
        // This encapsulates the business rule: "Each transaction = one Kafka message"
        val kafkaMessages = kafkaMessageFormatter.formatForKafka(transactionsResponse)
        
        // Set list of messages as body - route will iterate and publish each
        exchange.message.body = kafkaMessages
        
        // Store metadata for logging/monitoring
        exchange.message.headers[Headers.KAFKA_MESSAGES_COUNT] = kafkaMessages.size
        
        logger.info("Prepared ${kafkaMessages.size} individual Kafka messages for userId=${transactionsResponse.userId}")
    }
}

