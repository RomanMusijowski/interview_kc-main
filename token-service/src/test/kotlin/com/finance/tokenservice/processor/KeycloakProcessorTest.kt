package com.finance.tokenservice.processor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Exchange
import com.finance.tokenservice.config.KeycloakConfig
import com.finance.tokenservice.client.KeycloakRestClient
import com.finance.tokenservice.model.KeycloakTokenResponse
import com.finance.tokenservice.security.KeyStoreLoader
import com.finance.tokenservice.security.JwtAssertionGenerator
import java.util.Optional
import java.security.KeyPairGenerator
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.apache.camel.RuntimeCamelException
import org.apache.camel.support.DefaultExchange

class KeycloakProcessorTest {

    private val testConfig = object : KeycloakConfig {
        override fun baseUrl(): String = "http://localhost:8080"
        override fun realm(): String = "test"
        override fun clientId(): String = "client-id"
        override fun redirectUri(): String = "http://localhost/callback"
        override fun keystorePath(): Optional<String> = Optional.empty()
        override fun keystorePassword(): Optional<String> = Optional.empty()
        override fun keystoreAlias(): Optional<String> = Optional.empty()
    }

    private fun createJwtGenerator(): JwtAssertionGenerator {
        val keyStoreLoader = KeyStoreLoader(testConfig)
        val gen = JwtAssertionGenerator(testConfig, keyStoreLoader)

        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048)
        val kp = keyPairGen.generateKeyPair()

        val field = JwtAssertionGenerator::class.java.getDeclaredField("privateKey")
        field.isAccessible = true
        field.set(gen, kp.private)

        return gen
    }

    @Test
    fun `process sets headers and body on success`() {
        val fakeClient = object : KeycloakRestClient {
            override fun exchangeCode(realm: String, grantType: String, code: String, redirectUri: String, clientId: String, clientAssertionType: String, clientAssertion: String): KeycloakTokenResponse {
                return KeycloakTokenResponse(
                    accessToken = "access-123",
                    refreshToken = "refresh-123",
                    expiresIn = 3600,
                    refreshExpiresIn = 7200,
                    tokenType = "Bearer",
                    scope = "openid",
                    idToken = "id-789"
                )
            }
        }

        val jwtGen = createJwtGenerator()
        val processor = KeycloakProcessor(testConfig, fakeClient, jwtGen)

        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)
        ex.message.setHeader("code", "auth-code-1")

        processor.process(ex)

        val body = ex.message.body as? KeycloakTokenResponse
        assertNotNull(body)
        assertEquals("access-123", body?.accessToken)
        assertEquals("Bearer access-123", ex.message.getHeader("Authorization"))
        assertEquals("access-123", ex.message.getHeader("accessToken"))
        assertEquals("refresh-123", ex.message.getHeader("refreshToken"))
    }

    @Test
    fun `process maps Keycloak error and throws RuntimeCamelException`() {
        val response = Response.status(400).entity("invalid_code").build()
        val fakeClient = object : KeycloakRestClient {
            override fun exchangeCode(realm: String, grantType: String, code: String, redirectUri: String, clientId: String, clientAssertionType: String, clientAssertion: String): KeycloakTokenResponse {
                throw WebApplicationException(response)
            }
        }

        val jwtGen = createJwtGenerator()
        val processor = KeycloakProcessor(testConfig, fakeClient, jwtGen)

        val ctx = DefaultCamelContext()
        val ex: Exchange = DefaultExchange(ctx)
        ex.message.setHeader("code", "bad-code")

        val thrown = assertThrows(RuntimeCamelException::class.java) {
            processor.process(ex)
        }

        val status = ex.message.getHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE, Int::class.java)
        assertEquals(400, status)
        val body = ex.message.body as? Map<*, *>
        assertNotNull(body)
        assertTrue((body!!["error"] as? String)?.contains("Authorization code") == true)
    }
}
