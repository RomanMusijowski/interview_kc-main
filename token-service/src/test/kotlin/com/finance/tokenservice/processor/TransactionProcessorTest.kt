package com.finance.tokenservice.processor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Exchange
import com.finance.tokenservice.client.TransactionRestClient
import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.model.Transaction
import com.finance.tokenservice.model.TransactionType
import com.finance.tokenservice.model.TransactionStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.apache.camel.RuntimeCamelException
import com.finance.tokenservice.exception.TransactionFetchException
import org.apache.camel.support.DefaultExchange

class TransactionProcessorTest {

    private fun sampleTransaction(): Transaction {
        return Transaction(
            id = "tx-1",
            accountId = "acc-1",
            amount = BigDecimal("12.34"),
            currency = "USD",
            type = TransactionType.DEBIT,
            description = "Test",
            merchantName = "Store",
            category = "Shopping",
            timestamp = LocalDateTime.now(),
            status = TransactionStatus.COMPLETED,
            reference = "ref-1",
            balance = BigDecimal("100.00")
        )
    }

    @Test
    fun `process sets body and preserves Authorization header on success`() {
        val fakeClient = object : TransactionRestClient {
            override fun getTransactions(limit: Int, authorization: String): TransactionsResponse {
                return TransactionsResponse(
                    userId = "user-1",
                    transactions = listOf(sampleTransaction()),
                    count = 1,
                    tokenInfo = mapOf("auth" to authorization)
                )
            }
        }

        val processor = TransactionProcessor(fakeClient)

        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)
        ex.message.setHeader("Authorization", "Bearer token-xyz")

        processor.process(ex)

        val body = ex.message.body as? TransactionsResponse
        assertNotNull(body)
        assertEquals(1, body?.count)
        assertEquals("Bearer token-xyz", ex.message.getHeader("Authorization"))
    }

    @Test
    fun `process throws TransactionFetchException when missing Authorization header`() {
        val fakeClient = object : TransactionRestClient {
            override fun getTransactions(limit: Int, authorization: String): TransactionsResponse {
                throw IllegalStateException("should not be called")
            }
        }

        val processor = TransactionProcessor(fakeClient)

        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)

        val thrown = assertThrows(TransactionFetchException::class.java) {
            processor.process(ex)
        }

        assertTrue(thrown.message?.contains("Missing Authorization header") == true)
    }

    @Test
    fun `process wraps WebApplicationException into RuntimeCamelException`() {
        val response = Response.status(500).entity("server error").build()
        val fakeClient = object : TransactionRestClient {
            override fun getTransactions(limit: Int, authorization: String): TransactionsResponse {
                throw WebApplicationException(response)
            }
        }

        val processor = TransactionProcessor(fakeClient)
        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)
        ex.message.setHeader("Authorization", "Bearer token-xyz")

        assertThrows(RuntimeCamelException::class.java) {
            processor.process(ex)
        }
    }
}
