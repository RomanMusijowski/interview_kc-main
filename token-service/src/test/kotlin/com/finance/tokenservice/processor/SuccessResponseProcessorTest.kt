package com.finance.tokenservice.processor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Exchange
import com.finance.tokenservice.formatter.ResponseFormatter
import com.finance.tokenservice.config.KafkaConfig
import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.model.Transaction
import com.finance.tokenservice.model.TransactionType
import com.finance.tokenservice.model.TransactionStatus
import org.apache.camel.support.DefaultExchange
import java.math.BigDecimal
import java.time.LocalDateTime

class SuccessResponseProcessorTest {

    private fun sampleTransaction(): Transaction = Transaction(
        id = "tx-1",
        accountId = "acc-1",
        amount = BigDecimal("1.23"),
        currency = "USD",
        type = TransactionType.DEBIT,
        description = "desc",
        merchantName = "m",
        category = "cat",
        timestamp = LocalDateTime.now(),
        status = TransactionStatus.COMPLETED,
        reference = "ref",
        balance = BigDecimal("10.00")
    )

    @Test
    fun `process formats success response using ResponseFormatter when transactions present`() {
        val formatter = ResponseFormatter()
        val fakeConfig = object : KafkaConfig {
            override fun bootstrapServers(): String = "localhost:9092"
            override fun topic(): String = "topic-1"
        }

        val processor = SuccessResponseProcessor(formatter, fakeConfig)

        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)

        val tr = TransactionsResponse(
            userId = "user-1",
            transactions = listOf(sampleTransaction()),
            count = 1,
            tokenInfo = mapOf()
        )

        ex.message.setHeader(TransactionToMessageProcessor.Headers.TRANSACTIONS_RESPONSE, tr)

        processor.process(ex)

        val body = ex.message.body as? Map<*, *>
        assertNotNull(body)
        assertEquals("success", body!!["status"])
        assertEquals("topic-1", body["kafkaTopic"])
        assertEquals(200, ex.message.getHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE))
    }

    @Test
    fun `process returns default success when transactions header missing`() {
        val formatter = ResponseFormatter()
        val fakeConfig = object : KafkaConfig {
            override fun bootstrapServers(): String = "localhost:9092"
            override fun topic(): String = "topic-2"
        }

        val processor = SuccessResponseProcessor(formatter, fakeConfig)
        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)

        processor.process(ex)

        val body = ex.message.body as? Map<*, *>
        assertNotNull(body)
        assertEquals("success", body!!["status"])
        assertEquals("topic-2", body["kafkaTopic"])
        assertEquals(200, ex.message.getHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE))
    }
}
