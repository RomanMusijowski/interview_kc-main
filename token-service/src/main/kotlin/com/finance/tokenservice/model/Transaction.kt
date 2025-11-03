package com.finance.tokenservice.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.finance.tokenservice.decerialiser.LocalDateTimeDeserializer
import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: String,
    val accountId: String,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val description: String,
    val merchantName: String?,
    val category: String,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val timestamp: LocalDateTime,
    val status: TransactionStatus,
    val reference: String,
    val balance: BigDecimal?
)

enum class TransactionType {
    DEBIT, CREDIT, TRANSFER, FEE, INTEREST
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, CANCELLED
}