package com.finance.tokenservice.exception

/**
 * Base exception for token exchange errors.
 */
open class TokenExchangeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when authorization code is invalid or already used.
 */
class InvalidAuthorizationCodeException(message: String, cause: Throwable? = null) 
    : TokenExchangeException(message, cause)

/**
 * Exception thrown when client authentication fails.
 */
class ClientAuthenticationException(message: String, cause: Throwable? = null) 
    : TokenExchangeException(message, cause)







