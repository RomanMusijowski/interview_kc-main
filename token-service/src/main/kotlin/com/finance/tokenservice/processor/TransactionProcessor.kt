package com.finance.tokenservice.processor

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.RuntimeCamelException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import com.finance.tokenservice.client.TransactionRestClient
import com.finance.tokenservice.exception.TransactionsServiceUnavailableException
import com.finance.tokenservice.exception.TransactionFetchException

@ApplicationScoped
class TransactionProcessor @Inject constructor(
    @RestClient private val transactionClient: TransactionRestClient
) : Processor {
    
    companion object {
        private val logger = LoggerFactory.getLogger(TransactionProcessor::class.java)
    }
    
    override fun process(exchange: Exchange) {
        // Retrieve the Authorization header containing the JWT
        val authHeader = exchange.message.getHeader("Authorization", String::class.java)
        
        if (authHeader.isNullOrBlank()) {
            logger.error("Missing Authorization header - JWT token not found in exchange")
            throw TransactionFetchException(
                "Missing Authorization header for transactions call. " +
                "Token exchange may have failed or token was not stored properly."
            )
        }

        try {
            // Call the transactions service and set the response body
            val response = transactionClient.getTransactions(20, authHeader)
            logger.info("Step 2. Successfully fetched ${response.count} transactions for user: ${response.userId}")
            
            exchange.message.body = response
            // Keep Authorization header for any downstream use
            exchange.message.headers["Authorization"] = authHeader
            
        } catch (e: jakarta.ws.rs.WebApplicationException) {
            val status = e.response?.status ?: 500
            logger.error("Transactions service returned error: Status $status - ${e.message}")

            
            throw RuntimeCamelException(e.message, e)
            
        } catch (e: jakarta.ws.rs.ProcessingException) {
            logger.error("Failed to process transactions service response", e)
            throw TransactionsServiceUnavailableException(
                "Failed to connect to transactions service. Service may be unavailable."
            )
            
        } catch (e: Exception) {
            logger.error("Unexpected error fetching transactions", e)
            throw TransactionFetchException("Failed to fetch transactions: ${e.message}")
        }
    }
}
