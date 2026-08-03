# DailyProtocol 共享模块

> DailyTask 与 DailyController 之间的 MQTT 协议封装实现

[![API](https://img.shields.io/badge/API-21%2B-green.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)](https://developer.android.com)
[![JDK](https://img.shields.io/badge/JDK-17-orange.svg)](https://www.oracle.com/java)
[![Build](https://github.com/yamleaf/DailyProtocol/workflows/build/badge.svg)](https://github.com/yamleaf/DailyProtocol/actions/workflows/build.yml)

---

## 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [主要组件](#主要组件)
- [使用示例](#使用示例)
- [快速上手](#快速上手)
- [本地构建](#本地构建)
- [模块结构](#模块结构)

---

## 项目简介

DailyProtocol 是一个为 DailyTask 与 DailyController 之间双向通信而设计的轻量级 Android 库。它封装了基于 MQTT 协议的消息传输、数字签名验证、报文协议定义等核心功能，确保两个 App 之间的数据交换安全可靠。

| 特性 | 说明 |
|:---|:---|
| **协议统一** | 标准化的 JSON 报文格式，统一 CMD/ACK/status 消息结构 |
| **安全可靠** | HMAC-SHA256 数字签名，MQTT QoS2 保证消息传输安全 |
| **无状态存储** | 不收集用户任何隐私数据 |
| **模块化设计** | 独立的 AAR 库，方便集成到各个项目 |
| **跨平台支持** | 支持 Android 5.0+ 设备 |

## 核心功能

### 1. 报文协议 (MqttPacket)

- **支持多种数据类型**：字符串、整数、布尔值
- **统一的命令结构**：包含指令标识 (`c`)、负载 (`v`)、消息 ID (`rid`)、时间戳 (`ts`)、签名 (`sign`)
- **快速序列化**：基于 Gson 的高效 JSON 编解码

### 2. 密钥管理 (MqttSigner)

- **派生密钥**：基于 HKDF 算法从配对令牌派生会话密钥
- **双向签名**：控制端和被控端均可生成和验证报文签名
- **密钥安全存储**：Android 安全存储，支持加密密钥备份

### 3. MQTT 辅助 (BrokerUtils, MqttQuota)

- **Broker 地址管理**：支持 HTTP 请求获取、静态配置
- **流量统计**：消息发送/接收量、连接时间等指标监控
- **连接状态管理**：自动重连、异常重试、连接质量检测

### 4. 配对流程

- **二维码生成**：被控端生成包含配对信息的二维码
- **安全握手**：控制端扫描二维码，双向认证并派生会话密钥
- **配对令牌管理**：带有效期的配对令牌，支持重复使用

### 5. 指令生命周期管理

- **指令下发**：支持批量、单次指令下发
- **ACK 确认**：智能 ACK 处理，支持重试机制
- **状态订阅**：设备在线/离线、配对状态、解绑通知等

## 主要组件

### 1. MqttPacket

报文协议的 POJO 类，包含所有 MQTT 报文字段，支持多种负载类型。

```kotlin
// 示例：创建 ping 指令报文
val packet = MqttPacket(
    c = MqttPacket.CMD_PING,
    f = "",
    v = PacketValue.StringValue("ping"),
    rid = UUID.randomUUID().toString(),
    ts = System.currentTimeMillis(),
    sign = ""
)
```

### 2. MqttSigner

数字签名工具类，提供密钥派生、签名和验签功能。

```kotlin
// 派生会话密钥
val session = MqttSigner.deriveSession(
    pairingToken = "配对令牌",
    deviceId = "设备ID"
)

// 签名报文
val sign = MqttSigner.sign(
    session = "派生出的会话密钥",
    deviceId = "设备ID",
    ts = System.currentTimeMillis(),
    rid = "消息ID",
    f = "", // 功能字段
    type = "s", // 负载类型：字符串
    vStr = "负载内容",
    cmd = "CMD_PING" // 指令标识
)
```

### 3. BrokerUtils

Broker 地址管理工具，支持动态获取和静态配置。

```kotlin
val broker = BrokerUtils.getBroker(deviceId)
```

### 4. MqttQuota

流量统计和管理，用于监控 MQTT 连接状态和使用量。

```kotlin
// 获取流量统计信息
val stats = MqttQuota.getStats(deviceId)
```

### 5. 配对工具

支持生成配对令牌和二维码。

```kotlin
// 生成配对信息
val payload = PairingPayload(
    broker = "broker.emqx.io",
    deviceId = "设备ID",
    ctlUser = "ctl-设备ID",
    ctlPass = "密码",
    pairingToken = "配对令牌"
)

// 生成二维码（可序列化为 JSON）
val json = Gson().toJson(payload)
// 通过系统分享或 Intent 启动二维码生成器
```

## 使用示例

### 1. 解析报文

```kotlin
fun handleMessage(payload: String) {
    try {
        val packet = Gson().fromJson(payload, MqttPacket::class.java)
        when (packet.c) {
            MqttPacket.CMD_PAIR -> {
                // 处理配对指令
                handlePair(packet)
            }
            MqttPacket.CMD_UNBOUND -> {
                // 处理解绑指令
                handleUnbind(packet)
            }
            MqttPacket.CMD_PING -> {
                // 发送 ACK
                sendAck(packet.rid)
            }
        }
    } catch (e: Exception) {
        Log.e("MqttProtocol", "Failed to parse message", e)
    }
}
```

### 2. 发送指令

```kotlin
fun sendCommand(cmd: String, v: PacketValue, rid: String? = null) {
    val packet = MqttPacket(
        c = cmd,
        f = "",
        v = v,
        rid = rid ?: UUID.randomUUID().toString(),
        ts = System.currentTimeMillis(),
        sign = ""
    )
    
    val json = Gson().toJson(packet)
    mqttClient.publish(topic, MqttMessage(json.toByteArray()))
}
```

### 3. 配对流程

```kotlin
// 控制端：扫描二维码
fun processQrCode(qrCode: String) {
    try {
        val payload = Gson().fromJson(qrCode, BindingPayload::class.java)
        // 验证必要字段
        if (payload.broker.isNotBlank() && payload.deviceId.isNotBlank()) {
            // 开始配对
            startPair(payload)
        }
    } catch (e: Exception) {
        Log.e("MqttProtocol", "Invalid QR code", e)
    }
}

// 被控端：生成配对信息
fun generatePairing() {
    val payload = PairingPayload(
        broker = BrokerUtils.getBroker("deviceId"),
        deviceId = "deviceId",
        ctlUser = "ctl-deviceId",
        ctlPass = "password",
        pairingToken = generatePairingToken()
    )
    
    // 复制到剪贴板或生成二维码
    sharePayload(Gson().toJson(payload))
}
```

## 快速上手

1. **添加依赖**

```gradle
dependencies {
    implementation 'com.yample.mqttprotocol:mqttprotocol:1.0.0'
}
```

2. **初始化**

```kotlin
// 在 Application 中初始化
MqttProtocol.init(applicationContext)
```

3. **配置 MQTT 客户端**

```kotlin
val options = MqttConnectOptions().apply {
    userName = device.ctlUser
    password = device.ctlPass.toCharArray()
    isCleanSession = false
    connectionTimeout = 10
    isAutomaticReconnect = true
    keepAliveInterval = 240
}

val client = MqttClient(brokerUrl, clientId, MemoryPersistence())
client.setCallback(mqttCallback)
```

4. **订阅主题**

```kotlin
val topics = listOf(
    "${MqttPacket.TOPIC_PREFIX}/$deviceId/status",
    "${MqttPacket.TOPIC_PREFIX}/$deviceId/ack",
    "${MqttPacket.TOPIC_PREFIX}/$deviceId/pair/accept",
    "${MqttPacket.TOPIC_PREFIX}/$deviceId/resp",
    "${MqttPacket.TOPIC_PREFIX}/$deviceId/push"
)

for (topic in topics) {
    client.subscribe(topic, 1, null, object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            Log.d("MqttProtocol", "Subscribed to $topic")
        }
        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            Log.e("MqttProtocol", "Failed to subscribe $topic", exception)
        }
    })
}
```

## 本地构建

```bash
# 需要 JDK 17
./gradlew build --offline

# 构建 AAR 包
./gradlew bundleRelease

# 构建示例 App（可选）
./gradlew :sample:assembleDebug
```

## 模块结构

```
.
├── src/
│   └── main/
│       ├── java/com/yample/mqttprotocol/
│       │   ├── MqttPacket.java           # 报文协议
│       │   ├── MqttSigner.java           # 签名工具
│       │   ├── BrokerUtils.java         # Broker 管理
│       │   ├── MqttQuota.java           # 流量统计
│       │   ├── Protocol.java            # 常量定义
│       │   └── PacketValue*.java        # 负载数据类
│       ├── res/
│       │   ├── drawable/               # 资源文件
│       │   └── values/                 # 语言资源
│       └── AndroidManifest.xml
├── build.gradle                    # Gradle 配置
├── proguard-rules.pro              # ProGuard 规则
└── .gitignore                      # Git 忽略
```

## 许可证

本项目以 **PolyForm Noncommercial License 1.0.0** 发布，仅供非商业用途的学习与研究，禁止任何商业使用、倒卖或二次售卖。
