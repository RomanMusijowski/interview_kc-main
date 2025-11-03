package com.finance.tokenservice.route

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.Exchange
import org.apache.camel.http.base.HttpOperationFailedException
import com.finance.tokenservice.processor.error.TokenExchangeErrorProcessor
import com.finance.tokenservice.processor.error.TransactionFetchErrorProcessor
import com.finance.tokenservice.processor.error.KafkaPublishErrorProcessor
import com.finance.tokenservice.exception.TokenExchangeException
import com.finance.tokenservice.exception.TransactionFetchException
import com.finance.tokenservice.exception.KafkaPublishException

@ApplicationScoped
class ErrorHandlersRoutes @Inject constructor(
    private val tokenExchangeErrorProcessor: TokenExchangeErrorProcessor,
    private val transactionFetchErrorProcessor: TransactionFetchErrorProcessor,
    private val kafkaPublishErrorProcessor: KafkaPublishErrorProcessor
) : RouteBuilder() {
    override fun configure() {
        // Error handler for token exchange failures
        onException(TokenExchangeException::class.java)
            .handled(true)
            .maximumRedeliveries(0)
            .process(tokenExchangeErrorProcessor)
            .marshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)

        // Error handler for transaction fetching failures
        onException(TransactionFetchException::class.java)
            .handled(true)
            .maximumRedeliveries(0)
            .process(transactionFetchErrorProcessor)
            .marshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)

        // Error handler for Kafka publishing failures
        onException(KafkaPublishException::class.java)
            .handled(true)
            .maximumRedeliveries(0)
            .process(kafkaPublishErrorProcessor)
            .marshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)

        // Additional handler for general Kafka connection issues
        onException(Exception::class.java)
            .onWhen { exchange ->
                val exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)
                exception?.javaClass?.name?.contains("kafka", ignoreCase = true) == true ||
                exception?.message?.contains("kafka", ignoreCase = true) == true
            }
            .handled(true)
            .maximumRedeliveries(0)
            .process(kafkaPublishErrorProcessor)
            .marshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)

        // Global error handler for HTTP operation failures
        onException(HttpOperationFailedException::class.java)
            .handled(true)
            .process(transactionFetchErrorProcessor)
            .marshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)
    }
}
