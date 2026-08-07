package com.yample.mqttprotocol

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * 双端共享的主题管理器：深色 / 浅色 / 跟随系统。
 *
 * - 单一源存放在 :protocol 库，被控端与控制端共用同一套持久化 key 与文案；
 * - 在 Application.onCreate 里调用 [apply] 完成冷启动恢复；
 * - 设置页调用 [setMode] 即时切换（AppCompat 会自动重建当前 Activity）。
 */
object ThemeManager {

    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    private const val PREFS = "daily_theme"
    private const val KEY_MODE = "theme_mode"

    /** 设置页单选项文案，索引与 MODE_* 常量一一对应 */
    val LABELS = arrayOf("跟随系统", "浅色", "深色")

    fun labelOf(mode: Int): String = LABELS.getOrElse(mode) { LABELS[0] }

    fun getMode(context: Context): Int = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_MODE, MODE_SYSTEM)

    /** 冷启动时按已保存的偏好恢复主题 */
    fun apply(context: Context) = applyMode(getMode(context))

    /** 持久化并立即生效 */
    fun setMode(context: Context, mode: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MODE, mode)
            .apply()
        applyMode(mode)
    }

    private fun applyMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
