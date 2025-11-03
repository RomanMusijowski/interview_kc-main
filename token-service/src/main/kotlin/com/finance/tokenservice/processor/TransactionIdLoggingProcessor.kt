package com.finance.tokenservice.processor

import org.apache.camel.Exchange
import org.apache.camel.Processor
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory

@ApplicationScoped
class TransactionIdLoggingProcessor : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(TransactionIdLoggingProcessor::class.java)
    }

    override fun process(exchange: Exchange) {
        @Suppress("UNCHECKED_CAST")
        val messageMap = exchange.message.body as? Map<String, Any>
        val transactionId = (messageMap?.get("transaction") as? Map<String, Any>)?.get("id")?.toString() ?: "unknown"
        exchange.message.setHeader("transactionId", transactionId)
        logger.debug("Set transactionId header: $transactionId")
    }
}
