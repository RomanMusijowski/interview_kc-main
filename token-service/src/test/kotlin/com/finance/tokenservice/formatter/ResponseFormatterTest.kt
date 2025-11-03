package com.finance.tokenservice.formatter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.finance.tokenservice.model.TransactionsResponse
import com.finance.tokenservice.model.Transaction
import com.finance.tokenservice.model.TransactionType
import com.finance.tokenservice.model.TransactionStatus
import java.math.BigDecimal
import java.time.LocalDateTime

class ResponseFormatterTest {

    private fun sampleTransaction(): Transaction = Transaction(
        id = "tx-1",
        accountId = "acc-1",
        amount = BigDecimal("1.00"),
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
    fun `formatSuccessResponse includes expected keys`() {
        val formatter = ResponseFormatter()
        val tr = TransactionsResponse(
            userId = "user-1",
            transactions = listOf(sampleTransaction()),
            count = 1,
            tokenInfo = mapOf()
        )

        val result = formatter.formatSuccessResponse(tr, "topic-x")

        assertEquals("success", result["status"])
        assertEquals("user-1", result["userId"])
        assertEquals(1, result["transactionCount"])
        assertEquals("topic-x", result["kafkaTopic"])
        assertTrue(result.containsKey("timestamp"))
    }

    @Test
    fun `formatErrorResponse returns pair with provided http status and details`() {
        val formatter = ResponseFormatter()
        val (body, status) = formatter.formatErrorResponse("something bad", ResponseFormatter.ProcessingStep.KAFKA_PUBLISH, "detail-x", 502)

        assertEquals(502, status)
        assertEquals("error", body["status"])
        assertEquals("something bad", body["error"])
        assertEquals("kafka_publish", body["step"])
        assertEquals("detail-x", body["details"])
        assertTrue(body.containsKey("timestamp"))
    }
}
