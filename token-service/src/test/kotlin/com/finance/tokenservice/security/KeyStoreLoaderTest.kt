package com.finance.tokenservice.security

import com.finance.tokenservice.config.KeycloakConfig
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.nio.file.Files
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*
import java.util.Date

class KeyStoreLoaderTest {

    init {
        // ensure BouncyCastle provider is available for certificate generation
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private fun generateSelfSignedCert(): Pair<KeyPair, X509Certificate> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.genKeyPair()

        val dn = X500Name("CN=Test, O=Test, L=Test, C=US")
        val now = Date()
        val notAfter = Date(now.time + 365L * 24 * 60 * 60 * 1000)

        val builder = JcaX509v3CertificateBuilder(
            dn,
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            notAfter,
            dn,
            kp.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(kp.private)
        val holder = builder.build(signer)
        val cert = JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder)

        return Pair(kp, cert)
    }

    @Test
    fun `loadPrivateKey loads key from explicit keystore path`() {
        val (kp, cert) = generateSelfSignedCert()

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)

        val alias = "test-alias"
        val password = "changeit".toCharArray()
        ks.setKeyEntry(alias, kp.private, password, arrayOf(cert))

        val temp = Files.createTempFile("test-keystore", ".p12")
        temp.toFile().deleteOnExit()
        temp.toFile().outputStream().use { fos -> ks.store(fos, password) }

        val fakeConfig = object : KeycloakConfig {
            override fun baseUrl(): String = "http://localhost"
            override fun realm(): String = "realm"
            override fun clientId(): String = "client"
            override fun redirectUri(): String = "http://localhost/cb"
            override fun keystorePath(): Optional<String> = Optional.of(temp.toAbsolutePath().toString())
            override fun keystorePassword(): Optional<String> = Optional.of(String(password))
            override fun keystoreAlias(): Optional<String> = Optional.of(alias)
        }

        val loader = KeyStoreLoader(fakeConfig)
        val privateKey = loader.loadPrivateKey()
        assertNotNull(privateKey)
        assertArrayEquals(kp.private.encoded, privateKey.encoded)

        // cleanup
        Files.deleteIfExists(temp)
    }

    @Test
    fun `loadPrivateKey throws when keystore missing`() {
        val fakeConfig = object : KeycloakConfig {
            override fun baseUrl(): String = "http://localhost"
            override fun realm(): String = "realm"
            override fun clientId(): String = "client"
            override fun redirectUri(): String = "http://localhost/cb"
            override fun keystorePath(): Optional<String> = Optional.of("/non/existent/keystore.p12")
            override fun keystorePassword(): Optional<String> = Optional.of("pw")
            override fun keystoreAlias(): Optional<String> = Optional.of("alias")
        }

        val loader = KeyStoreLoader(fakeConfig)
        val ex = try {
            loader.loadPrivateKey()
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertNotNull(ex)
        assertTrue(ex!!.message!!.contains("KeyStore file not found") || ex.message!!.contains("Failed to load private key"))
    }
}
