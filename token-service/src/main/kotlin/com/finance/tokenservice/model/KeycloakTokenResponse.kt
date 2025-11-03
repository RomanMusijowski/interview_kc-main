package com.finance.tokenservice.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Model class representing the JSON response from Keycloak token endpoint.
 */
data class KeycloakTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("refresh_expires_in")
    val refreshExpiresIn: Int,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("scope")
    val scope: String?,
    @JsonProperty("id_token")
    val idToken: String?
)
