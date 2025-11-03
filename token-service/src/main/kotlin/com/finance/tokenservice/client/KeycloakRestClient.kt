package com.finance.tokenservice.client

import com.finance.tokenservice.model.KeycloakTokenResponse
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.Produces
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.MediaType

/**
 * REST client interface for Keycloak token endpoint.
 * Uses JWT client assertion for authentication (RFC 7523).
 */
@Path("")
@RegisterRestClient(configKey = "keycloak-api")
interface KeycloakRestClient {

    @POST
    @Path("/realms/{realm}/protocol/openid-connect/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    fun exchangeCode(
        @PathParam("realm") realm: String,
        @FormParam("grant_type") grantType: String,
        @FormParam("code") code: String,
        @FormParam("redirect_uri") redirectUri: String,
        @FormParam("client_id") clientId: String,
        @FormParam("client_assertion_type") clientAssertionType: String,
        @FormParam("client_assertion") clientAssertion: String
    ): KeycloakTokenResponse
}
