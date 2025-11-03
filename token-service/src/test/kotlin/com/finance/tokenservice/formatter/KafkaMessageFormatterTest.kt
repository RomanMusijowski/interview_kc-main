package com.finance.tokenservice.formatter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.model.Transaction
import com.finance.tokenservice.model.TransactionType
import com.finance.tokenservice.model.TransactionStatus
import java.math.BigDecimal
import java.time.LocalDateTime

class KafkaMessageFormatterTest {

    private fun sampleTransaction(id: String): Transaction = Transaction(
        id = id,
        accountId = "acc-1",
        amount = BigDecimal("10.50"),
        currency = "USD",
        type = TransactionType.DEBIT,
        description = "desc",
        merchantName = "merchant",
        category = "cat",
        timestamp = LocalDateTime.of(2020,1,1,12,0),
        status = TransactionStatus.COMPLETED,
        reference = "ref",
        balance = BigDecimal("100.00")
    )

    @Test
    fun `formatForKafka returns one message per transaction with expected structure`() {
        val formatter = KafkaMessageFormatter()

        val tr = TransactionsResponse(
            userId = "user-123",
            transactions = listOf(sampleTransaction("tx-1"), sampleTransaction("tx-2")),
            count = 2,
            tokenInfo = mapOf("k" to "v")
        )

        val messages = formatter.formatForKafka(tr)

        assertEquals(2, messages.size)

        val first = messages[0] as Map<*, *>
        assertEquals("user-123", first["userId"])
        val transaction = first["transaction"] as? Map<*, *>
        assertNotNull(transaction)
        assertEquals("tx-1", transaction!!["id"])
        assertEquals("USD", transaction["currency"])
        assertTrue(first.containsKey("messageTimestamp"))
    }
}
