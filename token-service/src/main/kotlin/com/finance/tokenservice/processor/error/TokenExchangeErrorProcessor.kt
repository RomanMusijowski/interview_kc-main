package com.finance.tokenservice.processor.error

import org.apache.camel.Exchange
import org.apache.camel.Processor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import com.finance.tokenservice.formatter.ResponseFormatter
import com.finance.tokenservice.exception.TokenExchangeException

@ApplicationScoped
class TokenExchangeErrorProcessor @Inject constructor(
    private val responseFormatter: ResponseFormatter
) : Processor {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenExchangeErrorProcessor::class.java)
    }

    override fun process(exchange: Exchange) {
        val exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, TokenExchangeException::class.java)
        logger.error("Token exchange failed: ${exception?.message}", exception)

        val (errorResponse, httpStatus) = responseFormatter.formatErrorResponse(
            exception?.message ?: "Token exchange failed",
            ResponseFormatter.ProcessingStep.TOKEN_EXCHANGE,
            exception?.cause?.message,
            exchange.message.getHeader(Exchange.HTTP_RESPONSE_CODE, 500, Int::class.java)
        )

        exchange.message.headers[Exchange.HTTP_RESPONSE_CODE] = httpStatus
        exchange.message.body = errorResponse
    }
}
