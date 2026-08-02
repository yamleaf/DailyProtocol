package com.yample.mqttprotocol

import android.content.Context
import android.content.SharedPreferences

object MqttQuota {
    private const val FREE_QUOTA = 1_000_000L
    private const val PREF = "mqtt_quota"
    private const val KEY_SENT = "sent"
    private const val KEY_RECEIVED = "received"
    private const val KEY_TOTAL_CONNECTED_MS = "totalConnectedMs"

    data class Stats(
        val sent: Long,
        val received: Long,
        val total: Long,
        val totalConnectedMs: Long,
        val sessionConnectedMs: Long,
        val freeQuota: Long
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun onConnect(ctx: Context) {
        prefs(ctx).edit().putLong("sessionStartMs", System.currentTimeMillis()).apply()
    }

    fun onDisconnect(ctx: Context) {
        val p = prefs(ctx)
        val start = p.getLong("sessionStartMs", 0L)
        if (start > 0L) {
            val delta = System.currentTimeMillis() - start
            p.edit()
                .putLong(KEY_TOTAL_CONNECTED_MS, p.getLong(KEY_TOTAL_CONNECTED_MS, 0L) + delta)
                .putLong("sessionStartMs", 0L)
                .apply()
        }
    }

    fun add(ctx: Context, published: Int, received: Int) {
        if (published <= 0 && received <= 0) return
        val p = prefs(ctx)
        p.edit()
            .putLong(KEY_SENT, p.getLong(KEY_SENT, 0L) + published)
            .putLong(KEY_RECEIVED, p.getLong(KEY_RECEIVED, 0L) + received)
            .apply()
    }

    fun get(ctx: Context): Stats {
        val p = prefs(ctx)
        val sent = p.getLong(KEY_SENT, 0L)
        val received = p.getLong(KEY_RECEIVED, 0L)
        val totalConnected = p.getLong(KEY_TOTAL_CONNECTED_MS, 0L)
        val start = p.getLong("sessionStartMs", 0L)
        val session = if (start > 0L) System.currentTimeMillis() - start else 0L
        return Stats(
            sent = sent,
            received = received,
            total = sent + received,
            totalConnectedMs = totalConnected,
            sessionConnectedMs = session,
            freeQuota = FREE_QUOTA
        )
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0秒"
        val sec = ms / 1000
        val days = sec / 86_400
        val hours = (sec % 86_400) / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}时")
            if (minutes > 0) append("${minutes}分")
            if (seconds > 0 || isEmpty()) append("${seconds}秒")
        }
    }
}
