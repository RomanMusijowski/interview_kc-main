package com.finance.tokenservice.formatter

import com.finance.tokenservice.model.TransactionsResponse
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.time.Instant

@ApplicationScoped
class KafkaMessageFormatter {
    
    companion object {
        private val logger = LoggerFactory.getLogger(KafkaMessageFormatter::class.java)
    }
    
    /**
     * @param transactionsResponse The response from transactions service
     * @return List of formatted messages (one per transaction), ready for Kafka publishing
     */
    fun formatForKafka(transactionsResponse: TransactionsResponse): List<Map<String, Any>> {
        logger.debug("Formatting ${transactionsResponse.count} transactions into individual Kafka messages")
        
        return transactionsResponse.transactions.mapIndexed { _, transaction ->
            mapOf(
                "userId" to transactionsResponse.userId,
                "transaction" to mapOf(
                    "id" to transaction.id,
                    "accountId" to transaction.accountId,
                    "amount" to transaction.amount,
                    "currency" to transaction.currency,
                    "type" to transaction.type.name,
                    "description" to transaction.description,
                    "merchantName" to transaction.merchantName,
                    "category" to transaction.category,
                    "timestamp" to transaction.timestamp.toString(),
                    "status" to transaction.status.name,
                    "reference" to transaction.reference,
                    "balance" to transaction.balance
                ),
                "messageTimestamp" to Instant.now().toString()
            )
        }
    }
    
}




