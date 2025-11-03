package com.finance.tokenservice.processor.error

import org.apache.camel.Exchange
import org.apache.camel.Processor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import com.finance.tokenservice.formatter.ResponseFormatter
import com.finance.tokenservice.exception.TransactionFetchException

@ApplicationScoped
class TransactionFetchErrorProcessor @Inject constructor(
    private val responseFormatter: ResponseFormatter
) : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(TransactionFetchErrorProcessor::class.java)
    }

    override fun process(exchange: Exchange) {
        val exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, TransactionFetchException::class.java)
        logger.error("Transaction fetch failed: ${exception?.message}", exception)

        val (errorResponse, httpStatus) = responseFormatter.formatErrorResponse(
            exception?.message ?: "Failed to fetch transactions",
            ResponseFormatter.ProcessingStep.TRANSACTION_FETCH,
            exception?.cause?.message,
            502
        )

        exchange.message.headers[Exchange.HTTP_RESPONSE_CODE] = httpStatus
        exchange.message.body = errorResponse
    }
}
