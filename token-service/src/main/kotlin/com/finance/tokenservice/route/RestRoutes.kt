package com.finance.tokenservice.route

import jakarta.enterprise.context.ApplicationScoped
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestParamType

@ApplicationScoped
class RestRoutes : RouteBuilder() {
    override fun configure() {
        // Configure REST DSL to use platform-http (built-in server) and JSON binding
        restConfiguration()
            .component("platform-http")
            .bindingMode(org.apache.camel.model.rest.RestBindingMode.json)

        // Define the /token endpoint to accept GET with query param 'code'
        rest("/token")
            .get()
            .param().name("code").type(RestParamType.query).dataType("string").required(true).endParam()
            .produces("application/json")
            .routeId("TokenEndpointRoute")
            .to("direct:exchangeToken")
    }
}
