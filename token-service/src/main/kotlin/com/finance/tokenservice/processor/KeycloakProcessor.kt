package com.finance.tokenservice.processor

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.RuntimeCamelException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import com.finance.tokenservice.config.KeycloakConfig
import com.finance.tokenservice.client.KeycloakRestClient
import com.finance.tokenservice.security.JwtAssertionGenerator
import com.finance.tokenservice.exception.TokenExchangeException
import com.finance.tokenservice.processor.error.KeycloakErrorMapper

@ApplicationScoped
class KeycloakProcessor @Inject constructor(
    private val keycloakConfig: KeycloakConfig,
    @RestClient private val keycloakClient: KeycloakRestClient,
    private val jwtAssertionGenerator: JwtAssertionGenerator
) : Processor {
    
    companion object {
        private val logger = LoggerFactory.getLogger(KeycloakProcessor::class.java)
    }

    override fun process(exchange: Exchange) {
        // Extract the authorization code from query parameters
        // In Camel REST DSL, query parameters are available via the header with the parameter name
        val code = exchange.message.getHeader("code", String::class.java)
            ?: exchange.message.getHeader(Exchange.HTTP_QUERY, String::class.java)
                ?.split("&")
                ?.find { it.startsWith("code=") }
                ?.substringAfter("code=")
                ?.split("&")
                ?.first()

        if (code.isNullOrBlank()) {
            logger.error("Missing 'code' parameter in request")
            throw RuntimeCamelException("Missing 'code' parameter. Please provide an authorization code.")
        }

        try {
            // Generate JWT client assertion signed with private key
            val clientAssertion = jwtAssertionGenerator.generateClientAssertion()
            logger.debug("Generated JWT client assertion (length: ${clientAssertion.length})")
            
            // Call Keycloak token endpoint to exchange code for tokens using JWT assertion
            val tokenResponse = keycloakClient.exchangeCode(
                realm = keycloakConfig.realm(),
                grantType = "authorization_code",
                code = code,
                redirectUri = keycloakConfig.redirectUri(),
                clientId = keycloakConfig.clientId(),
                clientAssertionType = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                clientAssertion = clientAssertion
            )


            // Store the full token response in the exchange body
            exchange.message.body = tokenResponse
            // Set Authorization header for downstream use (transactions service)
            exchange.message.headers["Authorization"] = "Bearer ${tokenResponse.accessToken}"
            
            // Also store the access token separately for easy access
            exchange.message.headers["accessToken"] = tokenResponse.accessToken
            exchange.message.headers["refreshToken"] = tokenResponse.refreshToken
            
            logger.debug("Access token stored in exchange headers for downstream processors")

        } catch (e: jakarta.ws.rs.WebApplicationException) {
            val status = e.response?.status ?: 500
            logger.error("Keycloak returned error response: Status $status - ${e.message}", e)
            
            // Read response body if available for better error messages
            val errorBody = try {
                e.response?.readEntity(String::class.java) ?: "No error details available"
            } catch (ex: Exception) {
                "Could not read error response"
            }

            // Delegate mapping/parsing to KeycloakErrorMapper to keep this block concise
            val mapper = KeycloakErrorMapper()
            val exception = mapper.map(errorBody)

            exchange.message.headers[Exchange.HTTP_RESPONSE_CODE] = status
            exchange.message.body = mapOf(
                "error" to exception.message,
                "status" to status,
                "details" to errorBody
            )

            // Preserve stacktrace by wrapping original exception
            throw RuntimeCamelException(exception.message, e)
            
        } catch (e: jakarta.ws.rs.ProcessingException) {
            logger.error("Processing error during Keycloak token exchange: ${e.message}", e)
            if (e.message?.contains("MessageBodyReader") == true) {
                logger.error("JSON deserialization error. Check that KeycloakTokenResponse model matches Keycloak response format.")
            }
            throw TokenExchangeException(
                "Failed to process Keycloak response: ${e.message}. " +
                "This might be a JSON deserialization issue."
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during Keycloak token exchange", e)
            throw TokenExchangeException("Keycloak token exchange failed: ${e.message}")
        }
    }
}
