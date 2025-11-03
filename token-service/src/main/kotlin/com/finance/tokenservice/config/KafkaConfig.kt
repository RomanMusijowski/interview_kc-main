package com.finance.tokenservice.config

import io.smallrye.config.ConfigMapping

/**
 * Configuration properties for Kafka integration.
 */
@ConfigMapping(prefix = "finance.kafka")
interface KafkaConfig {
    /**
     * Kafka bootstrap servers (e.g. localhost:9092).
     */
    fun bootstrapServers(): String
    
    /**
     * Kafka topic name for user transactions.
     */
    fun topic(): String
}
