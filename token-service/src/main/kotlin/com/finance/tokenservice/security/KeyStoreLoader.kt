package com.finance.tokenservice.security

import com.finance.tokenservice.config.KeycloakConfig
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.PrivateKey

@ApplicationScoped
class KeyStoreLoader(
    private val keycloakConfig: KeycloakConfig
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(KeyStoreLoader::class.java)
    }

    /**
     * Loads private key from Java KeyStore.
     * Priority:
     * 1. Explicit configuration (finance.keycloak.keystore-path)
     * 2. Environment variable (KEYCLOAK_KEYSTORE_PATH)
     * 3. Auto-detection (searches for keystore.jks or keystore.p12 in keys directory)
     * 
     * @return PrivateKey loaded from KeyStore
     * @throws IllegalStateException if KeyStore not found or cannot be loaded
     */
    fun loadPrivateKey(): PrivateKey {
        logger.info("Loading private key from KeyStore...")
        
        val keystorePath = findKeyStorePath()
            ?: throw IllegalStateException(
                """
                |KeyStore not found!
                |Please configure one of:
                |  1. Set finance.keycloak.keystore-path in application.properties
                |  2. Set KEYCLOAK_KEYSTORE_PATH environment variable
                |  3. Place keystore.jks or keystore.p12 in project root/keys/ directory
                |
                |Expected location: keys/keystore.jks or keys/keystore.p12
                """.trimMargin()
            )

        return loadPrivateKeyFromKeyStore(
            keystorePath,
            keycloakConfig.keystorePassword().orElse("changeit"),
            keycloakConfig.keystoreAlias().orElse("finance-client")
        )
    }
    
    /**
     * Finds KeyStore path using priority order:
     * 1. Explicit config
     * 2. Environment variable
     * 3. Auto-detection
     */
    private fun findKeyStorePath(): String? {
        // Priority 1: Explicit configuration
        keycloakConfig.keystorePath().orElse(null)?.let { path ->
            if (path.isNotBlank()) {
                val pathObj = Paths.get(path)
                if (Files.exists(pathObj)) {
                    logger.debug("Found KeyStore via explicit config: $path")
                    return path
                } else {
                    logger.warn("Configured KeyStore path does not exist: $path")
                }
            }
        }
        
        // Priority 2: Environment variable
        System.getenv("KEYCLOAK_KEYSTORE_PATH")?.let { envPath ->
            if (envPath.isNotBlank()) {
                val pathObj = Paths.get(envPath)
                if (Files.exists(pathObj)) {
                    logger.debug("Found KeyStore via environment variable: $envPath")
                    return envPath
                } else {
                    logger.warn("Environment KeyStore path does not exist: $envPath")
                }
            }
        }
        
        // Priority 3: Auto-detection (simplified - check keys directory)
        logger.debug("Attempting KeyStore auto-detection...")
        return autoDetectKeyStore()
    }
    
    /**
     * Simplified auto-detection: searches for keystore files in keys/ directory
     * relative to project root.
     */
    private fun autoDetectKeyStore(): String? {
        val currentDir = System.getProperty("user.dir") ?: "."
        val currentDirPath = Paths.get(currentDir).toAbsolutePath()

        val keystoreNames = listOf("keystore.jks", "keystore.p12")

        // Strategy 1: Check keys/ directory relative to current directory
        keystoreNames.forEach { name ->
            val path = currentDirPath.resolve("keys").resolve(name)
            if (Files.exists(path)) {
                logger.info("Auto-detected KeyStore: ${path.toAbsolutePath()}")
                return path.toAbsolutePath().toString()
            }
        }

        // Strategy 2: Walk up to find project root with keys/ directory
        var searchPath = currentDirPath
        var levels = 0
        while (levels < 5 && searchPath.parent != null) {
            keystoreNames.forEach { name ->
                val path = searchPath.resolve("keys").resolve(name)
                if (Files.exists(path)) {
                    logger.info("Auto-detected KeyStore: ${path.toAbsolutePath()}")
                    return path.toAbsolutePath().toString()
                }
            }
            searchPath = searchPath.parent ?: break
            levels++
        }

        logger.debug("KeyStore auto-detection failed after checking $levels levels")
        return null
    }
    
    /**
     * Loads private key from Java KeyStore (PKCS12 or JKS format).
     */
    private fun loadPrivateKeyFromKeyStore(
        keystorePath: String,
        password: String,
        alias: String
    ): PrivateKey {
        return try {
            val keystorePathObj = Paths.get(keystorePath).toAbsolutePath()
            if (!Files.exists(keystorePathObj)) {
                throw IllegalStateException("KeyStore file not found: $keystorePath")
            }
            
            val keystore = KeyStore.getInstance(
                if (keystorePath.endsWith(".p12") || keystorePath.endsWith(".pkcs12")) {
                    "PKCS12"
                } else {
                    "JKS"
                }
            )
            
            Files.newInputStream(keystorePathObj).use { input ->
                keystore.load(input, password.toCharArray())
            }
            
            val privateKeyEntry = keystore.getEntry(
                alias,
                KeyStore.PasswordProtection(password.toCharArray())
            ) as? KeyStore.PrivateKeyEntry
                ?: throw IllegalStateException("Private key entry '$alias' not found in KeyStore")
            
            logger.info("Successfully loaded private key from KeyStore")
            privateKeyEntry.privateKey
        } catch (e: Exception) {
            logger.error("Failed to load private key from KeyStore: $keystorePath", e)
            throw IllegalStateException("Failed to load private key from KeyStore: ${e.message}")
        }
    }
}



