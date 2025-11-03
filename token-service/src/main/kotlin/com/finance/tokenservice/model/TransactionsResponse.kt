package com.finance.tokenservice.model

/**
 * Model class representing the response from the transactions service.
 */
data class TransactionsResponse(
    val userId: String,
    val transactions: List<Transaction>,
    val count: Int,
    val tokenInfo: Map<String, Any>
)
