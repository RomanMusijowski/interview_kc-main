package com.finance.tokenservice.processor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Exchange
import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.model.Transaction
import com.finance.tokenservice.model.TransactionType
import com.finance.tokenservice.model.TransactionStatus
import com.finance.tokenservice.formatter.KafkaMessageFormatter
import org.apache.camel.support.DefaultExchange
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionToMessageProcessorTest {

    private fun sampleTransaction(id: String): Transaction = Transaction(
        id = id,
        accountId = "acc-1",
        amount = BigDecimal("5.00"),
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
    fun `process transforms TransactionsResponse into kafka messages and sets headers`() {
        val formatter = KafkaMessageFormatter()
        val processor = TransactionToMessageProcessor(formatter)

        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)

        val tr = TransactionsResponse(
            userId = "user-1",
            transactions = listOf(sampleTransaction("tx-1"), sampleTransaction("tx-2")),
            count = 2,
            tokenInfo = mapOf()
        )

        ex.message.body = tr

        processor.process(ex)

        val body = ex.message.body as? List<*>
        assertNotNull(body)
        assertEquals(2, body?.size)
        val header = ex.message.getHeader(TransactionToMessageProcessor.Headers.KAFKA_MESSAGES_COUNT)
        assertEquals(2, header)
        val stored = ex.message.getHeader(TransactionToMessageProcessor.Headers.TRANSACTIONS_RESPONSE) as? TransactionsResponse
        assertNotNull(stored)
        assertEquals("user-1", stored?.userId)
    }
}
