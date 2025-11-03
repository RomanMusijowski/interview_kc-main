package com.finance.tokenservice.exception

/**
 * Base exception for transaction fetching errors.
 */
open class TransactionFetchException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception when transactions service is unavailable.
 */
class TransactionsServiceUnavailableException(message: String, cause: Throwable? = null) 
    : TransactionFetchException(message, cause)

/**
 * Exception when authentication fails with transactions service.
 */
class TransactionAuthenticationException(message: String, cause: Throwable? = null) 
    : TransactionFetchException(message, cause)







