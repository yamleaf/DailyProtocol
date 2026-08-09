package com.yample.mqttprotocol

object BrokerUtils {
    fun normalizeBroker(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("ssl://") || trimmed.startsWith("tcp://") ||
            trimmed.startsWith("ws://") || trimmed.startsWith("wss://")
        ) return trimmed
        val hostPart = trimmed.substringAfter("://", trimmed)
        // 未带端口：EMQX Cloud 默认走 TLS 8883，避免按 tcp://:1883 明文连接失败
        if (!hostPart.contains(":") && !hostPart.startsWith("[")) return "ssl://$trimmed:8883"
        val port = trimmed.substringAfterLast(':', trimmed).toIntOrNull()
        val scheme = if (port != null && (port == 8883 || port == 8884 || port == 8886)) "ssl://" else "tcp://"
        return "$scheme$trimmed"
    }
}
