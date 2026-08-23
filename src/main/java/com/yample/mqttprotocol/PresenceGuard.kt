package com.yample.mqttprotocol

import androidx.annotation.Keep
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 设备ID占用仲裁（presence 协议）纯逻辑，三仓共享。
 *
 * 规则：挑战者上线前在 dt/{id}/presence 敲门（PRB），在位者实时回 CLM 或以 retained 占位牌（HB）
 * 表明占用；冲突时只有挑战者停服，在位者仅提醒；sid 持久化可跨进程死亡认领旧占位牌；
 * 同时上线竞速按 sid 字典序裁决。任何解析失败一律放行（失败开放，不阻断既有连接能力）。
 */
@Keep
object PresenceGuard {

    const val TOPIC_SUFFIX = "presence"

    /** 发出探测后的决策窗口：等待 CLM / HB / 竞速对手 */
    const val DECISION_WINDOW_MS = 3_000L

    /** 占位牌发布节流间隔（蹭现有 80s 心跳唤醒窗口，距上次 ≥ 此间隔才真正 publish） */
    const val HB_MIN_INTERVAL_MS = 120_000L

    /** 占位牌新鲜度阈值：超过视为前任已退场，允许接管。
     *  必须远大于 HB 发布间隔，且覆盖「深睡在位者来不及应答探测」的最坏情况。 */
    const val HB_FRESH_WINDOW_MS = 15 * 60_000L

    /** 同时上线竞速的随机抖动上限：错开双方探测时刻，降低竞速概率 */
    const val PROBE_JITTER_MS = 800L

    /** 上岗后的监听期：期间收到外来 CLAIM 说明原占用者刚醒（深睡），挑战者让位 */
    const val TAKEOVER_WATCH_MS = 5 * 60_000L

    /** 对同一挑战者回 CLM 的节流窗口 */
    const val CLAIM_REPLY_THROTTLE_MS = 2_000L

    fun presenceTopic(prefix: String, deviceId: String): String =
        "$prefix/$deviceId/$TOPIC_SUFFIX"

    // ══════════ 报文编解码 ══════════

    @Keep
    data class PresenceMsg(val type: String, val sid: String, val ts: Long)

    private fun encode(type: String, sid: String, ts: Long): String {
        val o = JsonObject()
        o.addProperty("t", type)
        o.addProperty(Protocol.FIELD_SID, sid)
        o.addProperty("ts", ts)
        return o.toString()
    }

    fun encodeProbe(sid: String, ts: Long): String = encode(Protocol.CMD_PROBE, sid, ts)

    fun encodeClaim(sid: String, ts: Long): String = encode(Protocol.CMD_CLAIM, sid, ts)

    fun encodeHeartbeat(sid: String, ts: Long): String = encode(Protocol.CMD_HEARTBEAT, sid, ts)

    /** 解析失败返回 null（非 presence 报文 / 损坏载荷一律忽略） */
    fun decode(json: String): PresenceMsg? {
        return try {
            if (!json.startsWith("{")) return null
            val o = JsonParser.parseString(json).asJsonObject
            val type = o.get("t")?.asString ?: return null
            val sid = o.get(Protocol.FIELD_SID)?.asString ?: return null
            if (sid.isBlank()) return null
            when (type) {
                Protocol.CMD_PROBE,
                Protocol.CMD_CLAIM,
                Protocol.CMD_HEARTBEAT -> PresenceMsg(type, sid, o.get("ts")?.asLong ?: 0L)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    // ══════════ 判定工具 ══════════

    /** 占位牌是否新鲜（对方可能仍在运行） */
    fun isHeartbeatFresh(hbTs: Long, nowMs: Long): Boolean =
        hbTs > 0 && nowMs >= hbTs && (nowMs - hbTs) <= HB_FRESH_WINDOW_MS

    /** 同时上线竞速裁决：sid 字典序小者胜 */
    fun raceWinnerIsMe(mySid: String, foreignSid: String): Boolean = mySid < foreignSid

    // ══════════ 冲突原因与用户文案 ══════════

    /** 冲突原因码（持久化到被控端本地，UI/通知按原因显示不同文案） */
    @Keep
    enum class ConflictReason {
        /** 探测窗口内收到对方实时应答：对方此刻正在运行 */
        LIVE_REPLY,
        /** 无实时应答但占位牌新鲜：对方最近还在（可能深睡未醒），或本机重装前的残留记录 */
        FRESH_HB,
        /** 双方同时上线互相看见，sid 竞速落败 */
        RACE_LOST,
        /** 上岗后被原占用者抗议（监听期内收到 CLAIM），让位 */
        CONTESTED,
        /** 断线重连后发现占位牌已被他人接管，回归者此时是挑战者 */
        RECLAIM;

        fun userMessage(deviceId: String, foreignTs: Long = 0): String = when (this) {
            LIVE_REPLY ->
                "设备ID $deviceId 正被另一台在线设备使用，远程服务已停止。\n请修改设备ID后再开启，或先在另一台设备上关闭远程服务。"
            FRESH_HB -> {
                if (foreignTs <= 0) {
                    "设备ID $deviceId 最近有其他设备的使用记录（对方暂未应答），为避免干扰已暂停本机服务。\n若这是本机重装前的残留记录，请稍候再开启；若另一台设备仍在使用该ID，请修改本机设备ID。"
                } else {
                    val remaining = HB_FRESH_WINDOW_MS - (System.currentTimeMillis() - foreignTs)
                    if (remaining > 60_000) {
                        val minutes = (remaining + 59_999L) / 60_000L
                        "设备ID $deviceId 最近有其他设备使用记录，对方暂未应答，为避免干扰已暂停本机服务。\n等待约 ${minutes} 分钟后即可尝试再次开启，或可修改本机设备ID再次尝试。"
                    } else if (remaining > 0) {
                        val seconds = remaining / 1000L
                        "设备ID $deviceId 最近有其他设备使用记录，对方暂未应答，为避免干扰已暂停本机服务。\n等待约 ${seconds} 秒后即可尝试再次开启，或可修改本机设备ID再次尝试。"
                    } else {
                        "设备ID $deviceId 此前有其他设备使用记录（已过期），现已可尝试再次开启；若仍冲突请修改本机设备ID。"
                    }
                }
            }
            RACE_LOST ->
                "检测到另一台设备与本机同时以相同设备ID上线，按规则本机已让位。\n如需启用本机，请先停用另一台设备或更换设备ID后重新开启服务。"
            CONTESTED ->
                "检测到该设备ID的原占用设备仍在运行，本机已让位并停止远程服务。"
            RECLAIM ->
                "断线重连后发现设备ID $deviceId 已被其他设备接管，本机已停止远程服务。"
        }

        companion object {
            fun fromName(name: String?): ConflictReason? =
                name?.let { n -> entries.firstOrNull { it.name == n } }
        }
    }
}

/**
 * 单次上线仲裁的决策窗口状态机（纯逻辑、可单测）：
 * 订阅 presence 后把窗口内收到的外来消息喂进来，窗口结束调 [evaluate] 得出胜负。
 */
class PresenceArbitration(private val mySid: String) {

    private val claims = mutableMapOf<String, Long>()   // 外来应答（对方声明占用且活着）
    private val probes = mutableMapOf<String, Long>()   // 外来探测（同时上线的竞速对手）
    private var freshHbSid: String? = null
    private var freshHbTs: Long = 0L

    fun onForeignHeartbeat(sid: String, hbTs: Long) {
        if (sid == mySid || sid.isBlank()) return
        if (PresenceGuard.isHeartbeatFresh(hbTs, System.currentTimeMillis())) {
            freshHbSid = sid
            freshHbTs = hbTs
        }
    }

    fun onForeignMessage(msg: PresenceGuard.PresenceMsg) {
        if (msg.sid == mySid || msg.sid.isBlank()) return
        when (msg.type) {
            Protocol.CMD_CLAIM -> claims[msg.sid] = msg.ts
            Protocol.CMD_PROBE -> probes[msg.sid] = msg.ts
            Protocol.CMD_HEARTBEAT -> onForeignHeartbeat(msg.sid, msg.ts)
        }
    }

    /**
     * 裁决：
     * 1. 收到外来 CLM → LIVE_REPLY（对方明确活着且声明占用）；
     * 2. 否则存在新鲜外来 HB → FRESH_HB（对方最近还在，可能只是深睡没来得及应答）；
     * 3. 只有外来 PRB → 同时上线竞速，按 sid 字典序裁决；
     * 4. 全无 → WIN 放行上岗。
     */
    fun evaluate(): Outcome {
        claims.maxByOrNull { it.value }?.let { (sid, _) ->
            return Outcome.Occupied(PresenceGuard.ConflictReason.LIVE_REPLY, sid)
        }
        freshHbSid?.let { sid ->
            return Outcome.Occupied(PresenceGuard.ConflictReason.FRESH_HB, sid, freshHbTs)
        }
        probes.keys.firstOrNull { rival -> !PresenceGuard.raceWinnerIsMe(mySid, rival) }?.let { rival ->
            return Outcome.Occupied(PresenceGuard.ConflictReason.RACE_LOST, rival)
        }
        return Outcome.Win
    }

    sealed class Outcome {
        object Win : Outcome()
        data class Occupied(val reason: PresenceGuard.ConflictReason, val foreignSid: String, val foreignTs: Long = 0L) : Outcome()
    }
}
