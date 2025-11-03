package com.finance.tokenservice.route

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.builder.RouteBuilder
import com.finance.tokenservice.processor.TransactionProcessor

@ApplicationScoped
class FetchTransactionsRoutes @Inject constructor(
    private val transactionProcessor: TransactionProcessor
) : RouteBuilder() {
    override fun configure() {
        from("direct:fetchTransactions")
            .routeId("FetchTransactionsRoute")
            .log("Step 2. Fetching transactions from transactions-service for user")
            .process(transactionProcessor)
            .to("direct:publishToKafka")
    }
}
