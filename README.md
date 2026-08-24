# DailyProtocol 共享协议库

> DailyTask（被控端）与 DailyController（控制端）之间 MQTT 通信协议的共享封装，
> Kotlin Android Library，以 git 子模块形式引入双端工程。

[![API](https://img.shields.io/badge/API-26%2B-green.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blueviolet.svg)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/JDK-17-orange.svg)](https://www.oracle.com/java)

---

## 目录

- [项目简介](#项目简介)
- [协议版本](#协议版本)
- [核心组件](#核心组件)
- [报文协议](#报文协议)
- [主题规划](#主题规划)
- [设置字段](#设置字段)
- [安全模型](#安全模型)
- [设备 ID 占用仲裁（presence）](#设备-id-占用仲裁presence)
- [集成方式](#集成方式)
- [本地构建](#本地构建)
- [模块结构](#模块结构)
- [许可证](#许可证)

---

## 项目简介

DailyProtocol 为双端双向通信而设计，封装了报文协议、数字签名、密钥派生、信封加密、
配对载荷、Broker 地址规范化等核心能力；同时内置一套 MD3 设计系统资源（状态药丸/呼吸球/
统一弹窗等），供两端 App 直接复用。

| 特性 | 说明 |
|:---|:---|
| **协议统一** | 标准化 JSON 报文（c/f/v/rid/ts/sign），单一常量源 `Protocol` 保证双端一致 |
| **安全可靠** | HKDF-SHA256 会话密钥派生 + HMAC-SHA256 验签 + AES-256-GCM 信封加密 |
| **无状态存储** | 不收集用户任何隐私数据 |
| **子模块分发** | 源码级 git submodule 引入，随宿主工程一起编译，无发布坐标依赖 |
| **设计系统共享** | MD3 tokens、状态组件、统一弹窗体系双端复用，保证 UI 一致性 |

---

## 协议版本

`Protocol.PROTO_VER = "2.0"`，采用语义化版本：

- **次版本（x.y）**：向后兼容的小改动——新增字段/指令、字段含义扩展，双端各自容错默认值即可；
- **主版本（x.0）**：不兼容的大改动——字段语义重定义、指令流程重构、加密封装变更。

历史记录：

- v3（旧编号制）：公共 Broker 加固——UB/PA/ACK 强制验签、解绑 status 走签名信封、
  resp/push/alert 载荷 AES-GCM 密封后再 HMAC；
- 1.0：改用语义化版本制；
- 2.0：设备 ID 占用仲裁（presence：PRB/CLM/HB/sid 上线流程重构）、告警回放 AQ、状态签名信封 ST、
  低电量分段告警与智能预警、id_conflict 异常接入上报。

控制端按版本号做容错：缺字段/未知字段一律取默认值，不抛异常。

---

## 核心组件

| 组件 | 职责 |
|:---|:---|
| `Protocol.kt` | 协议常量总表：指令标识、动作命令、设置字段、主题前缀、配对参数、presence 常量、PROTO_VER |
| `MqttPacket.kt` | JSON 报文结构与 `PacketValue` 负载密封类 + Gson 反序列化适配器 |
| `MqttSigner.kt` | HMAC-SHA256 签名（十六进制输出） |
| `Hkdf.kt` | RFC 5869 HKDF-SHA256 精简实现，配对握手派生会话密钥 |
| `SecretBox.kt` | AES-256-GCM 敏感字段信封加密 |
| `PresenceGuard.kt` | 设备 ID 占用仲裁纯逻辑（PRB/CLM/HB 状态机），三仓共享 |
| `BindingPayload.kt` | 绑定二维码 JSON 载荷 |
| `BrokerUtils.kt` | Broker 地址规范化（协议头与端口推断） |
| `MqttQuota.kt` | 消息收发量/连接时长等流量统计 |
| `ThemeManager.kt` | 双端日夜主题切换管理 |
| `dialog/UnifiedDialogKit.kt` + `DialogIdleBridge.kt` | 双端统一现代化弹窗体系 |

### 使用示例

```kotlin
// 1. 构造报文
val packet = MqttPacket(
    c = MqttPacket.CMD_ACTION,          // X
    f = Protocol.ACTION_PUNCH,          // punch
    v = null,
    rid = UUID.randomUUID().toString(),
    ts = System.currentTimeMillis(),
    sign = MqttSigner.sign(
        sessionSecret, deviceId, ts, rid,
        f = Protocol.ACTION_PUNCH, t = "", v = "", c = MqttPacket.CMD_ACTION
    )
)
val json = Gson().toJson(packet)

// 2. 解析负载（自动区分 Boolean/Int/String 变体）
when (val v = packet.v) {
    is PacketValue.StringValue -> v.toStringValue()
    is PacketValue.IntValue    -> v.toInt()
    is PacketValue.BooleanValue-> v.toBooleanStrict()
    null -> ""
}

// 3. 配对握手成功后派生会话密钥（双端入参一致）
val sessionSecret = Hkdf.deriveHex(pairingToken, deviceId, Protocol.PAIRING_INFO, Protocol.SESSION_KEY_LEN)

// 4. 敏感载荷密封 / 解封
val sealed = SecretBox.seal(sessionSecret, plainJson)
val opened = SecretBox.open(sessionSecret, sealed)   // 兼容明文，失败原样返回

// 5. Broker 地址规范化（无端口默认 ssl://…:8883）
val broker = BrokerUtils.normalizeBroker(rawFromQrCode)
```

绑定二维码载荷（`BindingPayload`，序列化为 JSON 后编码成二维码由控制端扫描）：

```json
{
  "broker": "ssl://broker.example.com:8883",
  "deviceId": "设备ID",
  "ctlUser": "ctl-设备ID",
  "ctlPass": "密码",
  "pairingToken": "一次性配对令牌"
}
```

---

## 报文协议

所有报文共用同一 JSON 结构：

| 字段 | 类型 | 说明 |
|:--:|:---|:---|
| `c` | String | 指令标识（见下表） |
| `f` | String | 功能/动作字段（如 `task`、`alerts`、动作名） |
| `v` | Object? | 负载，`{b}` 布尔 / `{i}` 整数 / `{s}` 字符串 三种变体之一 |
| `rid` | String | 消息 ID（UUID），请求-回执关联与去重键 |
| `ts` | Long | 发送时间戳（毫秒），参与签名与防重放 |
| `sign` | String | HMAC-SHA256 签名（十六进制） |

指令标识总表（`Protocol.CMD_*`）：

| 报文 | 含义 | 方向 |
|:--:|:---|:---|
| `S` CMD_SYNC | 会话探活（验签通过回 ACK ONLINE） | 控制端 → 被控端 |
| `U` CMD_UPDATE | 下发设置项变更 | 控制端 → 被控端 |
| `A` CMD_ACK | 指令确认回执（ONLINE / SIGN_FAIL / TOKEN_MISMATCH …） | 被控端 → 控制端 |
| `N` CMD_NOTIFY | 预留常量（当前双端未使用） | — |
| `P` CMD_PAIR | 发起配对（不带签名，凭 pairingToken 在 PA 中验证） | 控制端 → 被控端 |
| `PA` CMD_PAIR_ACCEPT | 配对接受（pairingToken 验签 + rid 匹配） | 被控端 → 控制端 |
| `UB` CMD_UNBOUND | 解绑 | 双向 |
| `Q` CMD_QUERY | 查询快照 | 控制端 → 被控端 |
| `R` CMD_RESP | 快照应答（`v` 载荷 AES-GCM 密封） | 被控端 → 控制端 |
| `T` CMD_TASK | 打卡任务列表下发（f=task，JSON 载荷密封） | 控制端 → 被控端 |
| `X` CMD_ACTION | 动作命令：`punch` / `start` / `stop` / `attendance` / `screenshot` | 控制端 → 被控端 |
| `D` CMD_PUSH | 状态变更增量推送（密封） | 被控端 → 控制端 |
| `AL` CMD_ALERT | 一次性事件告警（低电量分段 / 开始充电 / id_conflict，密封） | 被控端 → 控制端 |
| `ST` CMD_STATUS | 带会话签名的状态信封（unbound / force_unbound 等，防公共 Broker 伪造明文 status） | 被控端 → 控制端 |
| `AQ` CMD_ALERT_QUERY | 告警回放查询（f=alerts，经 resp 密封回放） | 控制 ↔ 被控 |

其他关键常量：主题前缀 `TOPIC_PREFIX = "dt"`、配对信息串 `PAIRING_INFO = "daily-pairing-v1"`、
会话密钥长度 `SESSION_KEY_LEN = 32`、配对令牌有效期 `PAIRING_TTL_MS = 120_000`。

---

## 主题规划

| 主题（前缀 `dt/{deviceId}/`） | 用途 | QoS |
|:---|:---|:--:|
| `cmd` | 控制端指令下行（Q/U/T/X/AQ） | 1 |
| `ack` | 被控端确认回执上行 | 1 |
| `status` | 在线状态（retained；被控端离线遗嘱置 offline） | 1 |
| `resp` | 快照应答 / 告警回放（密封） | 1 |
| `push` | 变更增量推送（密封） | 1 |
| `alert` | 事件告警（密封；监控服务断线可补收） | 1 |
| `pair` | 配对请求 / 解绑（P/UB） | 1 |
| `pair/accept` | 配对接受（PA） | 1 |
| `presence` | 设备 ID 占用仲裁（PRB/CLM/HB，轻量 JSON） | 0/1 |

---

## 设置字段

`CMD_UPDATE(U)` 支持的设置字段（`Protocol.FIELD_*`），按功能分组：

| 分组 | 字段 |
|:---|:---|
| 屏幕与伪息屏 | `ps` 省电模式 · `pm` 强制伪息屏 · `sm` 屏幕模式(0 伪息屏/1 息屏/2 常亮) · `tm` 伪息屏延时 · `nc` 伪息屏不显示时钟 · `ga` 手势识别 |
| 任务调度 | `ar` 每日循环 · `rt` 随机时间 · `rh` 重置小时 · `tr` 时间范围 · `ot` 超时时长 · `sh` 跳过节假日 · `uh` 更新节假日数据 · `cw` 自定义工作日 · `bo` 开机自动调度 |
| 通知与消息 | `nt` 通知转移 · `fd` 关闭反馈 · `mc` 消息渠道 · `mt` 消息标题 · `mcfg` 渠道配置(AES-GCM 密封下发) |
| 电量告警 | `lb` 低电量阈值(10~80，默认 30) · `ba` 智能预警开关 · `bw` 预警上报时间(分钟 0-1439) · `bs` 告警段数(0-3) · `br` 预警检测区间起始小时 · `bd` 区间时长(1-24 小时) |
| 其他 | `re` 远程开关 · `lg` 运行日志总开关 · `dp` 桌面宠物 |

---

## 安全模型

1. **配对握手**：控制端发 `P`（无签名）→ 被控端回 `PA`（pairingToken HMAC 验签 + rid 匹配）
   → 双端各自以 `HKDF-SHA256(ikm=pairingToken, salt=deviceId, info=daily-pairing-v1, len=32)`
   派生会话密钥，仅保存在本机（Room/EncryptedSharedPreferences）。
2. **全量验签**：握手完成后所有报文执行 HMAC-SHA256 验签，
   签名串为 `deviceId + ts + rid + f + t + v + c` 十六进制摘要。
3. **防重放**：`rid` 滑动窗口去重（最近 200 条）+ ±120 秒时间戳容差。
4. **机密性**：HMAC 只防篡改不防窃听——快照、推送、告警、消息渠道配置等敏感载荷先经
   `SecretBox`（AES-256-GCM，格式 `v1:<base64(iv12)>:<base64(cipher+tag)>`）密封再签名，
   公共 Broker 只能看到密文；解绑等敏感状态走签名信封 `ST`，防止伪造明文 status。
5. **失败开放**：presence 等非关键报文解析失败一律放行，不阻断既有连接能力；
   `SecretBox` 解密失败时原样返回，兼容旧版明文报文。

---

## 设备 ID 占用仲裁（presence）

防止同一 deviceId 的两台被控端先后/同时上线造成互踢错乱。挑战者上线前在
`dt/{id}/presence` 敲门探测，在位者实时应答或以 retained 占位牌表明占用；
冲突时只有挑战者停服并向控制端上报 `id_conflict`，在位者仅提醒。

| 参数 | 值 | 说明 |
|:---|:--:|:---|
| 决策窗口 `DECISION_WINDOW_MS` | 3 s | 发出 PRB 后等待 CLM / HB / 竞速对手 |
| 占位牌节流 `HB_MIN_INTERVAL_MS` | 120 s | 蹭现有心跳唤醒窗口发布 retained HB |
| 新鲜度阈值 `HB_FRESH_WINDOW_MS` | 15 min | 占位牌超时视为前任已退场，允许接管 |
| 接管观察期 `TAKEOVER_WATCH_MS` | 5 min | 上岗初期收到外来 CLAIM 说明前任刚醒，让位 |
| 探测抖动 `PROBE_JITTER_MS` | 800 ms | 错开同时上线竞速时刻 |
| CLAIM 回复节流 | 2 s | 对同一挑战者回 CLM 的最小间隔 |

- 会话标识 `sid` 持久化：进程死亡重启后可认领自己的旧占位牌；同时上线竞速按 sid 字典序裁决。
- 报文为轻量 JSON `{t, sid, ts}`，旧版本处理器天然忽略。

---

## 集成方式

```bash
# 在宿主工程根目录添加子模块
git submodule add https://github.com/yamleaf/DailyProtocol.git protocol
```

```groovy
// settings.gradle
include ':protocol'
```

```groovy
// app/build.gradle
dependencies {
    implementation project(':protocol')
}
```

> 库内样式（`Widget.Daily.*` 等）继承自 Material3 父样式并直接暴露资源给宿主使用，
> 宿主需保持 `android.nonTransitiveRClass=false` 以引用库内 `R` 资源。

---

## 本地构建

本仓库作为子模块随宿主工程构建（无独立 Gradle Wrapper 与 settings.gradle）：

```bash
# 经宿主工程构建 AAR
cd DailyTask   # 或 DailyController
./gradlew :protocol:bundleRelease
# AAR 输出：protocol/build/outputs/aar/
```

也可在本目录用本机 Gradle（8.x，JDK 17 + Android SDK 36）单独编译：

```bash
gradle build
```

配置要点：compileSdk 36 / minSdk 26 / JDK 17；consumer ProGuard 规则已内置；
依赖 gson 2.14.0、material 1.13.0、appcompat 1.7.0。

---

## 模块结构

```
.
├── src/main/java/com/yample/mqttprotocol/
│   ├── Protocol.kt            # 协议常量总表（指令/字段/主题/presence/版本）
│   ├── MqttPacket.kt          # JSON 报文结构 + PacketValue 负载密封类
│   ├── MqttSigner.kt          # HMAC-SHA256 签名
│   ├── Hkdf.kt                # HKDF-SHA256 会话密钥派生
│   ├── SecretBox.kt           # AES-256-GCM 信封加密
│   ├── PresenceGuard.kt       # 设备ID占用仲裁纯逻辑
│   ├── BindingPayload.kt      # 绑定二维码载荷
│   ├── BrokerUtils.kt         # Broker 地址规范化
│   ├── MqttQuota.kt           # 流量统计
│   ├── ThemeManager.kt        # 日夜主题管理
│   └── dialog/                # UnifiedDialogKit 统一弹窗体系
├── src/main/res/              # MD3 设计系统（tokens/styles/type/dimens、状态药丸/呼吸球、
│                              #   统一弹窗布局与动画，含 values-night 夜间变体）
├── build.gradle               # 库构建配置（compileSdk 36 / minSdk 26 / JDK 17）
└── proguard-rules.pro         # consumer ProGuard 规则
```

---

## 许可证

本项目以 **PolyForm Noncommercial License 1.0.0** 发布，仅供非商业用途的学习与研究，禁止任何商业使用、倒卖或二次售卖。
