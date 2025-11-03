package com.finance.tokenservice.security

import com.finance.tokenservice.config.KeycloakConfig
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.security.PrivateKey
import java.time.Instant
import java.util.*

@ApplicationScoped
class JwtAssertionGenerator @Inject constructor(
    private val keycloakConfig: KeycloakConfig,
    private val keyStoreLoader: KeyStoreLoader
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(JwtAssertionGenerator::class.java)
    }

    private lateinit var privateKey: PrivateKey

    @PostConstruct
    fun init() {
        logger.info("Loading private key from KeyStore...")
        privateKey = keyStoreLoader.loadPrivateKey()
    }

    fun generateClientAssertion(): String {
        val now = Instant.now()
        val expiration = now.plusSeconds(300)
        val jti = UUID.randomUUID().toString()
        
        val realmUrl = "${keycloakConfig.baseUrl()}/realms/${keycloakConfig.realm()}"
        
        logger.debug("Generating JWT client assertion for client: ${keycloakConfig.clientId()}, audience: $realmUrl")
        
        return Jwts.builder()
            .setHeaderParam("alg", "RS256")
            .setHeaderParam("typ", "JWT")
            .setIssuer(keycloakConfig.clientId()) // iss: client_id
            .setSubject(keycloakConfig.clientId()) // sub: client_id
            .setAudience(realmUrl) // aud: realm URL
            .setExpiration(Date.from(expiration)) // exp: expiration time
            .setIssuedAt(Date.from(now)) // iat: issued at
            .setId(jti) // jti: unique JWT ID
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact()
    }
}
