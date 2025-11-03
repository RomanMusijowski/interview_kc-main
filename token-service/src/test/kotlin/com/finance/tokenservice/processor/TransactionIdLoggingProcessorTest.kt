package com.finance.tokenservice.processor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Exchange
import org.apache.camel.support.DefaultExchange

class TransactionIdLoggingProcessorTest {

    @Test
    fun `process extracts transaction id and sets header`() {
        val processor = TransactionIdLoggingProcessor()
        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)

        val msg = mapOf("transaction" to mapOf("id" to "tx-123", "other" to "x"))
        ex.message.body = msg

        processor.process(ex)

        val header = ex.message.getHeader("transactionId") as? String
        assertEquals("tx-123", header)
    }

    @Test
    fun `process sets unknown when id not present`() {
        val processor = TransactionIdLoggingProcessor()
        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)

        ex.message.body = mapOf("foo" to "bar")
        processor.process(ex)

        val header = ex.message.getHeader("transactionId") as? String
        assertEquals("unknown", header)
    }
}
