package com.yample.mqttprotocol

import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import androidx.annotation.Keep

@Keep
sealed class PacketValue {
    data class BooleanValue(val b: Boolean) : PacketValue()
    data class IntValue(val i: Int) : PacketValue()
    data class StringValue(val s: String) : PacketValue()

    fun toBooleanStrict(): Boolean = (this as BooleanValue).b
    fun toInt(): Int = (this as IntValue).i
    fun toStringValue(): String = (this as StringValue).s
}

@Keep
object PacketValueAdapter : JsonDeserializer<PacketValue> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: com.google.gson.JsonDeserializationContext
    ): PacketValue {
        if (json.isJsonNull || !json.isJsonObject) {
            throw JsonParseException("PacketValue 期望 JSON 对象，实际: $json")
        }
        val obj = json.asJsonObject
        return when {
            obj.has("b") -> PacketValue.BooleanValue(obj["b"].asBoolean)
            obj.has("i") -> PacketValue.IntValue(obj["i"].asInt)
            obj.has("s") -> PacketValue.StringValue(obj["s"].asString)
            else -> throw JsonParseException("无法识别的 PacketValue 变体: $obj")
        }
    }
}

@Keep
data class MqttPacket(
    val c: String,
    val f: String,
    val v: PacketValue?,
    val rid: String,
    val ts: Long,
    val sign: String
) {
    companion object {
        // 命令常量引用 Protocol
        const val CMD_SYNC = Protocol.CMD_SYNC
        const val CMD_UPDATE = Protocol.CMD_UPDATE
        const val CMD_ACK = Protocol.CMD_ACK
        const val CMD_NOTIFY = Protocol.CMD_NOTIFY
        const val CMD_PAIR = Protocol.CMD_PAIR
        const val CMD_PAIR_ACCEPT = Protocol.CMD_PAIR_ACCEPT
        const val CMD_UNBOUND = Protocol.CMD_UNBOUND
        const val CMD_QUERY = Protocol.CMD_QUERY
        const val CMD_RESP = Protocol.CMD_RESP
        const val CMD_TASK = Protocol.CMD_TASK
        const val CMD_ACTION = Protocol.CMD_ACTION
        const val CMD_PUSH = Protocol.CMD_PUSH
        const val CMD_STATUS = Protocol.CMD_STATUS

        // 动作常量引用 Protocol
        const val ACTION_PUNCH = Protocol.ACTION_PUNCH
        const val ACTION_START = Protocol.ACTION_START
        const val ACTION_STOP = Protocol.ACTION_STOP
        const val ACTION_ATTENDANCE = Protocol.ACTION_ATTENDANCE
        const val ACTION_SCREENSHOT = Protocol.ACTION_SCREENSHOT

        // 主题与配对常量引用 Protocol
        const val TOPIC_PREFIX = Protocol.TOPIC_PREFIX
        const val PAIRING_INFO = Protocol.PAIRING_INFO
        const val SESSION_KEY_LEN = Protocol.SESSION_KEY_LEN
    }
}