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

    // 动作命令字段（配合 CMD_ACTION）
    const val ACTION_PUNCH = "punch"
    const val ACTION_START = "start"
    const val ACTION_STOP = "stop"
    const val ACTION_ATTENDANCE = "attendance"
    const val ACTION_SCREENSHOT = "screenshot"

    const val FIELD_POWER_SAVE = "ps"
    const val FIELD_FORCE_PSEUDO_MASK = "pm"
    const val FIELD_PSEUDO_MASK_TIMEOUT = "tm"
    const val FIELD_PSEUDO_MASK_NO_CLOCK = "nc"
    const val FIELD_NOTIFICATION_TRANSFER = "nt"
    const val FIELD_FEEDBACK_DISABLED = "fd"
    const val FIELD_SKIP_HOLIDAY = "sh"
    const val FIELD_TASK_AUTO_RECYCLE = "ar"
    const val FIELD_RANDOM_TIME = "rt"
    const val FIELD_GESTURE_DETECT = "ga"
    const val FIELD_BACK_TO_HOME = "bh"
    const val FIELD_KEEP_ALIVE = "ka"
    const val FIELD_RESET_HOUR = "rh"
    const val FIELD_TIME_RANGE = "tr"
    const val FIELD_STAY_OVERTIME = "ot"
    const val FIELD_MSG_CHANNEL = "mc"
    const val FIELD_MESSAGE_TITLE = "mt"
    const val FIELD_REMOTE_ENABLED = "re"
    const val FIELD_MSG_CONFIG = "mcfg"

    const val TOPIC_PREFIX = "dt"
    const val PAIRING_INFO = "daily-pairing-v1"
    const val SESSION_KEY_LEN = 32
    const val PAIRING_TTL_MS = 120_000L
}