package com.yample.mqttprotocol

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 敏感字段信封加密（AES-256-GCM）。
 *
 * 使用场景：控制端下发「消息渠道配置」时会携带企业微信 Webhook Key、邮箱授权码等明文机密。
 * MQTT 报文只做 HMAC 签名（防篡改），不做保密；若走公共 Broker，明文机密会暴露给中转方。
 * 因此这类字段先用配对期 HKDF 派生的会话密钥再包一层 AES-GCM，Broker 只能看到密文。
 *
 * 密文格式：`v1:<base64(iv12)>:<base64(cipher+tag)>`
 */
object SecretBox {

    private const val PREFIX = "v1"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_INFO = "mqtt-secret-box-v1"

    /** 会话密钥（十六进制字符串）→ 32 字节 AES 密钥 */
    private fun keyOf(sessionSecret: String): SecretKeySpec =
        SecretKeySpec(Hkdf.derive(sessionSecret.toByteArray(), ByteArray(0), KEY_INFO.toByteArray(), 32), "AES")

    fun isSealed(text: String): Boolean = text.startsWith("$PREFIX:")

    /** 加密；失败时回退为明文（保证功能可用，调用方自行权衡） */
    fun seal(sessionSecret: String, plain: String): String {
        if (sessionSecret.isBlank()) return plain
        return try {
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keyOf(sessionSecret), GCMParameterSpec(TAG_BITS, iv))
            val out = cipher.doFinal(plain.toByteArray())
            "$PREFIX:${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(out, Base64.NO_WRAP)}"
        } catch (e: Exception) {
            plain
        }
    }

    /** 解密；非密文格式或解密失败时原样返回，兼容旧版本明文报文 */
    fun open(sessionSecret: String, sealed: String): String {
        if (!isSealed(sealed) || sessionSecret.isBlank()) return sealed
        return try {
            val parts = sealed.split(":")
            if (parts.size != 3) return sealed
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val data = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keyOf(sessionSecret), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(data))
        } catch (e: Exception) {
            sealed
        }
    }
}
