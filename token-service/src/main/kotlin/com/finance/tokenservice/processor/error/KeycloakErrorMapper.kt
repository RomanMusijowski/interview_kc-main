package com.finance.tokenservice.processor.error

import com.finance.tokenservice.exception.ClientAuthenticationException
import com.finance.tokenservice.exception.InvalidAuthorizationCodeException
import com.finance.tokenservice.exception.TokenExchangeException
import org.slf4j.LoggerFactory

class KeycloakErrorMapper {

    companion object {
        private val logger = LoggerFactory.getLogger(KeycloakErrorMapper::class.java)
    }

    fun map(errorBody: String?): TokenExchangeException {
        val body = errorBody ?: ""

        return when {
            body.contains("invalid_code", ignoreCase = true) || body.contains("CODE_TO_TOKEN_ERROR", ignoreCase = true) -> {
                logger.warn("Mapped Keycloak response to InvalidAuthorizationCodeException")
                InvalidAuthorizationCodeException(
                    "Authorization code has already been used or is invalid. OAuth2 authorization codes are single-use. Please get a new authorization code from Keycloak."
                )
            }

            body.contains("invalid_client", ignoreCase = true) || body.contains("invalid_grant", ignoreCase = true) ||
            body.contains("client_assertion", ignoreCase = true) || body.contains("unauthorized_client", ignoreCase = true) -> {
                logger.error("Mapped Keycloak response to ClientAuthenticationException")
                ClientAuthenticationException(
                    "Client authentication failed. Check JWT assertion and client configuration. Verify that the keystore certificate matches Keycloak configuration."
                )
            }

            else -> {
                logger.error("Mapped Keycloak response to generic TokenExchangeException: ${body.take(200)}")
                TokenExchangeException("Keycloak token exchange failed: $body")
            }
        }
    }
}
