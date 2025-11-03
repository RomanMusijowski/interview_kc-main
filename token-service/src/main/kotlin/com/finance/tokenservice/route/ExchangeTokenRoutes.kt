package com.finance.tokenservice.route

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.builder.RouteBuilder
import com.finance.tokenservice.processor.KeycloakProcessor

@ApplicationScoped
class ExchangeTokenRoutes @Inject constructor(
    private val keycloakProcessor: KeycloakProcessor
) : RouteBuilder() {
    override fun configure() {
        from("direct:exchangeToken")
            .routeId("ExchangeTokenRoute")
            .log("Step 1. Exchanging authorization code for JWT token")
            .process(keycloakProcessor)
            .log("Step 1. Completed: JWT token obtained, proceeding to fetch transactions")
            .to("direct:fetchTransactions")
    }
}
