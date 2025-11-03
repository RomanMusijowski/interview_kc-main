package com.finance.tokenservice.config

import io.smallrye.config.ConfigMapping
import java.util.Optional

/**
 * Configuration properties for Keycloak integration.
 * Uses JWT client assertion (signed with private key from Java KeyStore) for authentication.
 * 
 * Security: Only Java KeyStore (JKS/PKCS12) is supported. PEM files are not supported.
 */
@ConfigMapping(prefix = "finance.keycloak")
interface KeycloakConfig {
    /**
     * Base URL of the Keycloak server (e.g. http://localhost:8080).
     */
    fun baseUrl(): String

    /**
     * Keycloak realm to use for authentication.
     */
    fun realm(): String

    /**
     * OAuth2 client ID registered in Keycloak.
     * Used as both issuer (iss) and subject (sub) in JWT client assertion.
     */
    fun clientId(): String

    /**
     * Redirect URI registered in Keycloak for the authorization code.
     */
    fun redirectUri(): String

    /**
     * Optional path to Java KeyStore file (PKCS12 or JKS format).
     * If not specified, will auto-detect from keys/ directory.
     * 
     * Example: /secure/keystore.jks
     * Can be set via environment variable: KEYCLOAK_KEYSTORE_PATH
     */
    fun keystorePath(): Optional<String>

    /**
     * Keystore password (required if keystorePath is specified).
     * For production, use environment variable or secret management service.
     * Can be set via environment variable: KEYCLOAK_KEYSTORE_PASSWORD
     */
    fun keystorePassword(): Optional<String>

    /**
     * Keystore alias for the private key entry.
     * Default: "finance-client"
     * Can be set via environment variable: KEYCLOAK_KEYSTORE_ALIAS
     */
    fun keystoreAlias(): Optional<String>
}
