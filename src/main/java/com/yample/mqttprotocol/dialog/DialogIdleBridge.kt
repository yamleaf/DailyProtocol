package com.yample.mqttprotocol.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * 弹窗内交互桥接器。
 *
 * 弹窗（AlertDialog）是独立 Window，其上的触摸事件不会经过宿主 Activity 的
 * dispatchTouchEvent / onUserInteraction，导致宿主「前台无操作」倒计时在用户操作弹窗时
 * 仍会被误判为无操作。app 模块在初始化时把 [onInteraction] 接到自己的空闲计时重置逻辑，
 * 使「弹窗内触摸 = 有操作」；而弹窗内无操作超时仍照常计时，不会因弹窗常开而永不触发。
 *
 * 控制端 / 未接线方 [onInteraction] 为空，本类无副作用。
 */
object DialogIdleBridge {
    var onInteraction: ((Context) -> Unit)? = null

    /**
     * 为弹窗内容根布局包一层触摸感知容器：任何子控件（滑块 / 滚轮 / 列表 / 按钮）的
     * ACTION_DOWN 都会先经过 [dispatchTouchEvent]，在此重置空闲计时，随后继续正常分发，
     * 不影响弹窗内交互。
     */
    @SuppressLint("ViewConstructor")
    fun wrap(inner: android.view.View): FrameLayout {
        val context = inner.context
        return object : FrameLayout(context) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                    onInteraction?.invoke(context)
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            addView(
                inner,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }
}
