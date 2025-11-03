package com.finance.tokenservice.client

import com.finance.tokenservice.model.TransactionsResponse
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.core.MediaType

/**
 * REST client interface for transactions service.
 */
@Path("/api/transactions")
@RegisterRestClient(configKey = "transaction-api")
interface TransactionRestClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getTransactions(
        @QueryParam("limit") limit: Int = 20,
        @HeaderParam("Authorization") authorization: String
    ): TransactionsResponse
}
