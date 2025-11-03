package com.finance.tokenservice.processor.error

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.finance.tokenservice.exception.InvalidAuthorizationCodeException
import com.finance.tokenservice.exception.ClientAuthenticationException
import com.finance.tokenservice.exception.TokenExchangeException

class KeycloakErrorMapperTest {

    private val mapper = KeycloakErrorMapper()

    @Test
    fun `maps invalid_code to InvalidAuthorizationCodeException`() {
        val ex = mapper.map("{\"error\":\"invalid_code\"}")
        assertTrue(ex is InvalidAuthorizationCodeException)
        assertTrue(ex.message?.contains("Authorization code") == true)
    }

    @Test
    fun `maps invalid_client to ClientAuthenticationException`() {
        val ex = mapper.map("error=invalid_client&detail=bad_assertion")
        assertTrue(ex is ClientAuthenticationException)
        assertTrue(ex.message?.contains("Client authentication failed") == true)
    }

    @Test
    fun `maps unknown body to generic TokenExchangeException`() {
        val ex = mapper.map("some unexpected body")
        assertTrue(ex is TokenExchangeException)
        assertTrue(ex.message?.contains("Keycloak token exchange failed") == true)
    }
}
