# DailyProtocol 协议类使用 Gson 反射序列化/反序列化（MqttPacket 载荷），
# 且 PacketValueAdapter 依赖子类字段名（b/i/s）作为 JSON 键。
# R8 混淆会重命名字段导致协议不兼容，必须 keep 字段名。
-keep class com.yample.mqttprotocol.** { *; }

# PacketValue sealed class 由 Gson 多态序列化，子类构造器与字段名必须保留
-keep class com.yample.mqttprotocol.PacketValue { *; }
-keep class com.yample.mqttprotocol.PacketValue$* { *; }
