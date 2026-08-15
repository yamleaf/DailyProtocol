package com.yample.mqttprotocol

import androidx.annotation.Keep

@Keep
object Protocol {
    const val CMD_SYNC = "S"
    const val CMD_UPDATE = "U"
    const val CMD_ACK = "A"
    const val CMD_NOTIFY = "N"
    const val CMD_PAIR = "P"
    const val CMD_PAIR_ACCEPT = "PA"
    const val CMD_UNBOUND = "UB"
    const val CMD_QUERY = "Q"
    const val CMD_RESP = "R"
    const val CMD_TASK = "T"
    const val CMD_ACTION = "X"
    const val CMD_PUSH = "D"
    const val CMD_ALERT = "AL" // 被控端 → 控制端 一次性事件告警（低电量分段 / 开始充电）
    /** 被控端 → 控制端：带会话签名的状态信封（解绑等敏感状态用，防公共 Broker 伪造 plain status） */
    const val CMD_STATUS = "ST"

    // 动作命令字段（配合 CMD_ACTION）
    const val ACTION_PUNCH = "punch"
    const val ACTION_START = "start"
    const val ACTION_STOP = "stop"
    const val ACTION_ATTENDANCE = "attendance"
    const val ACTION_SCREENSHOT = "screenshot"

    const val FIELD_POWER_SAVE = "ps"
    const val FIELD_FORCE_PSEUDO_MASK = "pm"
    const val FIELD_SCREEN_MODE = "sm" // 屏幕模式（int：0 伪息屏 / 1 息屏 / 2 常亮）
    const val FIELD_PSEUDO_MASK_TIMEOUT = "tm"
    const val FIELD_PSEUDO_MASK_NO_CLOCK = "nc"
    const val FIELD_NOTIFICATION_TRANSFER = "nt"
    const val FIELD_FEEDBACK_DISABLED = "fd"
    const val FIELD_SKIP_HOLIDAY = "sh"
    const val FIELD_UPDATE_HOLIDAY = "uh" // 更新节假日（bool：true 触发被控端重新拉取节假日/补班数据）
    const val FIELD_CUSTOM_WORKDAYS = "cw" // 自定义工作日（string：星期值集合，如 "1,2,3,4,5"）
    const val FIELD_TASK_AUTO_RECYCLE = "ar"
    const val FIELD_RANDOM_TIME = "rt"
    const val FIELD_GESTURE_DETECT = "ga"
    const val FIELD_BACK_TO_HOME = "bh"
    const val FIELD_RESET_HOUR = "rh"
    const val FIELD_TIME_RANGE = "tr"
    const val FIELD_STAY_OVERTIME = "ot"
    const val FIELD_MSG_CHANNEL = "mc"
    const val FIELD_MESSAGE_TITLE = "mt"
    const val FIELD_REMOTE_ENABLED = "re"
    const val FIELD_MSG_CONFIG = "mcfg"
    const val FIELD_LOW_BATTERY_THRESHOLD = "lb" // 低电量告警阈值（%，默认 30，范围 10~80）
    const val FIELD_BATTERY_SMART_ALERT = "ba" // 智能预警上报开关（bool）
    const val FIELD_BATTERY_WARNING_HOUR = "bw" // 预警上报时间（int, 分钟数，0-1439，兼容旧版仅小时0-23）
    const val FIELD_BATTERY_ALERT_STAGES = "bs" // 低电量告警段数（int, 0-3）
    const val FIELD_BATTERY_ALERT_RANGE_START = "br" // 预警检测区间起始小时（int, 0-23）
    const val FIELD_BATTERY_ALERT_RANGE_DURATION = "bd" // 预警检测区间时长小时（int, 1-24）
    const val FIELD_BOOT_AUTO_SCHEDULE = "bo" // 开机自动调度（bool）
    const val FIELD_DESKTOP_PET = "dp" // 桌面宠物（bool）
    const val FIELD_LOG_ENABLED = "lg" // 运行日志总开关（bool）

    const val TOPIC_PREFIX = "dt"
    const val PAIRING_INFO = "daily-pairing-v1"
    const val SESSION_KEY_LEN = 32
    const val PAIRING_TTL_MS = 120_000L

    /**
     * 快照/协议版本号。双端版本号从 1.0 起步，采用语义化版本：
     * - 次版本（1.x）：向后兼容的小改动——新增字段/指令、字段含义扩展等，双端各自容错默认值即可；
     * - 主版本（2.0...）：不兼容的大改动——字段语义重定义、指令流程重构、加密封装变更等。
     *
     * 控制端按此版本号做容错（缺字段/未知字段一律默认值，不抛异常）。
     * 历史主版本（旧 v3/v4 编号制）：v3 公共 Broker 加固——UB/PA/ACK 强制验签、解绑 status 走签名信封、
     * resp/push/alert 载荷 AES-GCM（SecretBox）密封后再 HMAC。
     */
    const val PROTO_VER = "1.0"
}