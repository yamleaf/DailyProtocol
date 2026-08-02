package com.yample.mqttprotocol

object BrokerUtils {
    fun normalizeBroker(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("ssl://") || trimmed.startsWith("tcp://") ||
            trimmed.startsWith("ws://") || trimmed.startsWith("wss://")
        ) return trimmed
        val port = trimmed.substringAfterLast(':', trimmed).toIntOrNull()
        val scheme = if (port != null && (port == 8883 || port == 8884 || port == 8886)) "ssl://" else "tcp://"
        return "$scheme$trimmed"
    }
}
