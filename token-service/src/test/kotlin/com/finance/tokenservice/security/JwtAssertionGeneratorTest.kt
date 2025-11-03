package com.finance.tokenservice.security

import com.finance.tokenservice.config.KeycloakConfig
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.util.Optional

class JwtAssertionGeneratorTest {

    @Test
    fun `generateClientAssertion produces signed JWT with expected claims`() {
        // generate RSA keypair for signing
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.genKeyPair()

        val fakeConfig = object : KeycloakConfig {
            override fun baseUrl(): String = "http://localhost:8080"
            override fun realm(): String = "finance"
            override fun clientId(): String = "client-123"
            override fun redirectUri(): String = "http://localhost/callback"
            override fun keystorePath() = Optional.empty<String>()
            override fun keystorePassword() = Optional.empty<String>()
            override fun keystoreAlias() = Optional.empty<String>()
        }

        val keyStoreLoader = object : KeyStoreLoader(fakeConfig) {
            override fun loadPrivateKey() = kp.private
        }

        val gen = JwtAssertionGenerator(fakeConfig, keyStoreLoader)
        // call init to load private key
        gen.init()

        val jwt = gen.generateClientAssertion()
        assertNotNull(jwt)

        // parse/verify JWT using public key (use Jwts.parser() factory API)
        val parsed = Jwts.parser()
            .verifyWith(kp.public)
            .build()
            .parseSignedClaims(jwt)


        val claims = parsed.payload
        assertEquals("client-123", claims.issuer)
        assertEquals("client-123", claims.subject)
        val expectedAud = "http://localhost:8080/realms/finance"
        // 'aud' may be returned as a String or a Collection depending on JJWT/runtime
        val audClaim = try { claims.get("aud") } catch (_: Exception) { null } ?: claims.audience
        val audStr = when (audClaim) {
            is String -> audClaim
            is Collection<*> -> audClaim.firstOrNull()?.toString()
            else -> audClaim?.toString()
        }

        assertEquals(expectedAud, audStr)

        // iat/exp sanity
        assertTrue(claims.issuedAt.before(claims.expiration) || claims.issuedAt == claims.expiration)
        assertNotNull(parsed.header["alg"])
        assertNotNull(parsed.header["typ"])
    }
}
