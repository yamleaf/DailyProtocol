package com.yample.mqttprotocol

import androidx.annotation.Keep

@Keep
data class BindingPayload(
    val broker: String,
    val deviceId: String,
    val ctlUser: String,
    val ctlPass: String,
    val pairingToken: String
)
