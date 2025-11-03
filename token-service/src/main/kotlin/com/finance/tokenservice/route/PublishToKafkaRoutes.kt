package com.finance.tokenservice.route

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.builder.RouteBuilder
import com.finance.tokenservice.processor.TransactionToMessageProcessor
import com.finance.tokenservice.processor.TransactionIdLoggingProcessor
import com.finance.tokenservice.config.KafkaConfig
import com.finance.tokenservice.processor.SuccessResponseProcessor

@ApplicationScoped
class PublishToKafkaRoutes : RouteBuilder() {

    @Inject
    lateinit var kafkaConfig: KafkaConfig

    @Inject
    lateinit var transactionToMessageProcessor: TransactionToMessageProcessor

    @Inject
    lateinit var transactionIdLoggingProcessor: TransactionIdLoggingProcessor

    @Inject
    lateinit var successResponseProcessor: SuccessResponseProcessor

    override fun configure() {
        from("direct:publishToKafka")
            .routeId("PublishToKafkaRoute")
            .log("Step 3. Publishing transactions to Kafka topic '${kafkaConfig.topic()}'")
            .process(transactionToMessageProcessor) // Splits into list of messages (domain logic)
            .split(body()) // Infrastructure: Iterate over messages
                .process(transactionIdLoggingProcessor)
                .marshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)
                .toD("kafka:${kafkaConfig.topic()}?brokers=${kafkaConfig.bootstrapServers()}")
                .log("Published transaction message to Kafka - transactionId: \${header.transactionId}")
            .end()
            .process(successResponseProcessor)
    }
}
