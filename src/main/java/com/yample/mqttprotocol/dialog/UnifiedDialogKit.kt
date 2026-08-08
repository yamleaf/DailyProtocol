package com.yample.mqttprotocol.dialog

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.yample.mqttprotocol.R

/**
 * 双端统一现代化弹窗组件（控制端 DailyController / 被控端 DailyTask 共用同一实现）。
 *
 * 设计规范（M3 现代审美）：
 *  - 28dp ExtraLarge 圆角，表面采用 md_surfaceContainerHigh（tonal 抬升）
 *  - 44dp 胶囊按钮：双按钮底部等宽均分，单按钮居中自然宽度
 *  - 标准动效（fast_out_slow_in 入场 220ms / linear_out_slow_in 出场 160ms）
 *  - 56dp tonal 圆形图标容器 + 28dp 线条图标
 *
 * 能力：showInfo / showSuccess / showWarning / showPermission / showForm / showMenu / showSingleChoice / showMultiChoice
 */
object UnifiedDialogKit {

    enum class IconType { SUCCESS, WARNING, PERMISSION, INFO }

    private data class IconSpec(val drawable: Int, val containerTint: Int, val iconTint: Int)

    private fun specFor(type: IconType): IconSpec = when (type) {
        IconType.SUCCESS -> IconSpec(R.drawable.ic_dialog_check, R.color.md_tertiaryContainer, R.color.md_tertiary)
        IconType.WARNING -> IconSpec(R.drawable.ic_dialog_warning, R.color.md_errorContainer, R.color.md_error)
        IconType.PERMISSION -> IconSpec(R.drawable.ic_dialog_permission, R.color.md_primaryContainer, R.color.md_primary)
        IconType.INFO -> IconSpec(R.drawable.ic_dialog_info, R.color.md_primaryContainer, R.color.md_primary)
    }

    /** 返回已套用统一弹窗主题的 builder，供需要自绘按钮栏的调用方使用 */
    fun builder(ctx: Context): MaterialAlertDialogBuilder =
        MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_Daily_UnifiedDialog)

    private fun dp(ctx: Context, value: Float): Int =
        (value * ctx.resources.displayMetrics.density + 0.5f).toInt()

    // ===================== 图标型弹窗 =====================

    private fun buildContent(ctx: Context, type: IconType, title: String, message: String): View {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_unified_content, null)
        val spec = specFor(type)
        view.findViewById<ImageView>(R.id.ivDialogIcon).apply {
            backgroundTintList = ContextCompat.getColorStateList(ctx, spec.containerTint)
            setImageResource(spec.drawable)
            imageTintList = ContextCompat.getColorStateList(ctx, spec.iconTint)
        }
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        view.findViewById<TextView>(R.id.tvDialogMessage).text = message
        return view
    }

    private fun configureButtons(
        dlg: AlertDialog,
        ctx: Context,
        content: View,
        positiveText: String,
        negativeText: String?,
        danger: Boolean,
        onPositive: (() -> Unit)?,
        onNegative: (() -> Unit)?
    ) {
        val btnBar = content.findViewById<LinearLayout>(R.id.btnBar)
        val btnPos = content.findViewById<Button>(R.id.btnPositive)
        val btnNeg = content.findViewById<Button>(R.id.btnNegative)

        btnPos.text = positiveText
        btnPos.visibility = View.VISIBLE
        btnPos.setOnClickListener {
            onPositive?.invoke()
            dlg.dismiss()
        }
        if (danger) {
            btnPos.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.md_error)
            btnPos.setTextColor(ContextCompat.getColor(ctx, R.color.md_onError))
        }

        if (negativeText != null) {
            btnNeg.text = negativeText
            btnNeg.visibility = View.VISIBLE
            btnNeg.setOnClickListener {
                onNegative?.invoke()
                dlg.dismiss()
            }
        } else {
            // 单按钮：居中、自然宽度
            val lp = btnPos.layoutParams as LinearLayout.LayoutParams
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
            lp.weight = 0f
            lp.marginStart = 0
            btnPos.layoutParams = lp
        }
        btnBar.gravity = Gravity.CENTER
    }

    private fun createDialog(
        ctx: Context,
        type: IconType,
        title: String,
        message: String,
        positiveText: String,
        negativeText: String?,
        danger: Boolean,
        cancelable: Boolean,
        onPositive: (() -> Unit)?,
        onNegative: (() -> Unit)?
    ): AlertDialog {
        val content = buildContent(ctx, type, title, message)
        val dlg = MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_Daily_UnifiedDialog)
            .setView(DialogIdleBridge.wrap(content))
            .create()
        dlg.setCancelable(cancelable)
        configureButtons(dlg, ctx, content, positiveText, negativeText, danger, onPositive, onNegative)
        dlg.show()
        return dlg
    }

    /** 信息提示（单按钮居中） */
    fun showInfo(
        ctx: Context,
        title: String,
        message: String,
        buttonText: String = "知道了",
        cancelable: Boolean = true
    ): AlertDialog = createDialog(ctx, IconType.INFO, title, message, buttonText, null, false, cancelable, null, null)

    /** 成功 / 提示（双按钮均分） */
    fun showSuccess(
        ctx: Context,
        title: String,
        message: String,
        confirmText: String = ctx.getString(android.R.string.ok),
        cancelText: String = ctx.getString(android.R.string.cancel),
        cancelable: Boolean = true,
        onConfirm: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ): AlertDialog = createDialog(
        ctx, IconType.SUCCESS, title, message, confirmText, cancelText,
        danger = false, cancelable = cancelable, onPositive = onConfirm, onNegative = onCancel
    )

    /** 警告 / 删除确认（主按钮染危险色） */
    fun showWarning(
        ctx: Context,
        title: String,
        message: String,
        confirmText: String = "删除",
        cancelText: String = ctx.getString(android.R.string.cancel),
        cancelable: Boolean = true,
        onCancel: (() -> Unit)? = null,
        onConfirm: (() -> Unit)? = null
    ): AlertDialog = createDialog(
        ctx, IconType.WARNING, title, message, confirmText, cancelText,
        danger = true, cancelable = cancelable, onPositive = onConfirm, onNegative = onCancel
    )

    /** 权限申请（双按钮均分） */
    fun showPermission(
        ctx: Context,
        title: String,
        message: String,
        grantText: String = "允许",
        denyText: String = "拒绝",
        cancelable: Boolean = true,
        onGrant: (() -> Unit)? = null
    ): AlertDialog = createDialog(
        ctx, IconType.PERMISSION, title, message, grantText, denyText,
        danger = false, cancelable = cancelable, onPositive = onGrant, onNegative = null
    )

    // ===================== 表单 / 自定义内容弹窗 =====================

    /**
     * 表单类弹窗：标题 / 消息 + 自定义内容视图（contentHost）+ 底部按钮组（双按钮等宽均分，单按钮居中）。
     * @param onShow 显示后回传 dialog 与两个按钮，供实时校验 / 动态禁用。
     * @param onCancel 返回 false 表示不关闭弹窗。
     * @param onConfirm 返回 false 表示不关闭弹窗（校验失败）。置于参数末尾，支持尾部 lambda 直接绑定确认动作。
     */
    fun showForm(
        ctx: Context,
        contentView: View,
        title: String? = null,
        message: String? = null,
        positiveText: String = "确定",
        negativeText: String? = "取消",
        cancelable: Boolean = true,
        onShow: ((AlertDialog, Button, Button) -> Unit)? = null,
        onCancel: ((AlertDialog) -> Boolean)? = null,
        onConfirm: ((AlertDialog) -> Boolean)? = null
    ): AlertDialog {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_unified_form, null)
        val titleView = view.findViewById<TextView>(R.id.tvDialogTitle)
        val titleGap = view.findViewById<View>(R.id.titleGap)
        if (!title.isNullOrBlank()) {
            titleView.text = title
            titleView.visibility = View.VISIBLE
            titleGap.visibility = View.VISIBLE
        }
        val msgView = view.findViewById<TextView>(R.id.tvDialogMessage)
        val msgGap = view.findViewById<View>(R.id.messageGap)
        if (!message.isNullOrBlank()) {
            msgView.text = message
            msgView.visibility = View.VISIBLE
            msgGap.visibility = View.VISIBLE
        }
        view.findViewById<FrameLayout>(R.id.contentHost).addView(
            contentView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val dlg = MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_Daily_UnifiedDialog)
            .setView(DialogIdleBridge.wrap(view))
            .create()

        val btnBar = view.findViewById<LinearLayout>(R.id.btnBar)
        val btnPos = view.findViewById<Button>(R.id.btnPositive)
        val btnNeg = view.findViewById<Button>(R.id.btnNegative)

        btnPos.text = positiveText
        btnPos.visibility = View.VISIBLE
        btnPos.setOnClickListener {
            if (onConfirm?.invoke(dlg) != false) dlg.dismiss()
        }

        if (negativeText != null) {
            btnNeg.text = negativeText
            btnNeg.visibility = View.VISIBLE
            btnNeg.setOnClickListener {
                if (onCancel?.invoke(dlg) != false) dlg.dismiss()
            }
        } else {
            val lp = btnPos.layoutParams as LinearLayout.LayoutParams
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
            lp.weight = 0f
            lp.marginStart = 0
            btnPos.layoutParams = lp
        }
        btnBar.gravity = Gravity.CENTER

        dlg.setCancelable(cancelable)
        dlg.setOnShowListener { onShow?.invoke(dlg, btnPos, btnNeg) }
        dlg.show()
        return dlg
    }

    // ===================== 列表型弹窗 =====================

    private fun rippleBackground(ctx: Context): Drawable? {
        val outValue = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return if (outValue.resourceId != 0) ContextCompat.getDrawable(ctx, outValue.resourceId) else null
    }

    /** kind: 0=菜单 1=单选 2=多选 */
    private fun buildChoiceRow(ctx: Context, label: String, kind: Int, selected: Boolean): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(ctx, 48f)
            setPadding(dp(ctx, 12f), 0, dp(ctx, 12f), 0)
            background = rippleBackground(ctx)
        }
        row.addView(
            TextView(ctx).apply {
                text = label
                textSize = 15f
                setTextColor(ContextCompat.getColor(ctx, R.color.md_onSurface))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        when (kind) {
            1 -> row.addView(MaterialRadioButton(ctx).apply {
                isClickable = false
                isFocusable = false
                isChecked = selected
            })
            2 -> row.addView(MaterialCheckBox(ctx).apply {
                isClickable = false
                isFocusable = false
                isChecked = selected
            })
        }
        return row
    }

    private fun setListTitle(view: View, title: String?) {
        val tv = view.findViewById<TextView>(R.id.tvDialogTitle)
        val gap = view.findViewById<View>(R.id.titleGap)
        if (!title.isNullOrBlank()) {
            tv.text = title
            tv.visibility = View.VISIBLE
            gap.visibility = View.VISIBLE
        }
    }

    /** 列表弹窗底部单个居中「取消」按钮（自然宽度） */
    private fun bindSingleCancel(view: View, dlg: AlertDialog, text: String) {
        val btnBar = view.findViewById<LinearLayout>(R.id.btnBar)
        val btnPos = view.findViewById<Button>(R.id.btnPositive)
        btnPos.text = text
        btnPos.visibility = View.VISIBLE
        btnPos.setOnClickListener { dlg.dismiss() }
        val lp = btnPos.layoutParams as LinearLayout.LayoutParams
        lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        lp.weight = 0f
        btnPos.layoutParams = lp
        btnBar.gravity = Gravity.CENTER
    }

    /**
     * 菜单弹窗：选项点击即关闭。
     */
    fun showMenu(
        ctx: Context,
        title: String,
        items: List<String>,
        cancelable: Boolean = true,
        onSelect: (Int) -> Unit
    ): AlertDialog {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_unified_list, null)
        setListTitle(view, title)
        val dlg = MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_Daily_UnifiedDialog)
            .setView(DialogIdleBridge.wrap(view))
            .create()
        dlg.setCancelable(cancelable)

        val container = view.findViewById<LinearLayout>(R.id.listContainer)
        items.forEachIndexed { index, label ->
            val row = buildChoiceRow(ctx, label, 0, false)
            row.setOnClickListener {
                onSelect(index)
                dlg.dismiss()
            }
            container.addView(row)
        }
        bindSingleCancel(view, dlg, "取消")
        dlg.show()
        return dlg
    }

    /**
     * 单选弹窗：点击选项即选中并关闭。
     */
    fun showSingleChoice(
        ctx: Context,
        title: String,
        items: List<String>,
        selectedIndex: Int,
        cancelable: Boolean = true,
        onSelect: (Int) -> Unit
    ): AlertDialog {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_unified_list, null)
        setListTitle(view, title)
        val dlg = MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_Daily_UnifiedDialog)
            .setView(DialogIdleBridge.wrap(view))
            .create()
        dlg.setCancelable(cancelable)

        val container = view.findViewById<LinearLayout>(R.id.listContainer)
        val radios = mutableListOf<MaterialRadioButton>()
        items.forEachIndexed { index, label ->
            val row = buildChoiceRow(ctx, label, 1, index == selectedIndex)
            val radio = row.getChildAt(1) as MaterialRadioButton
            radios.add(radio)
            row.setOnClickListener {
                radios.forEach { it.isChecked = false }
                radio.isChecked = true
                onSelect(index)
                dlg.dismiss()
            }
            container.addView(row)
        }
        bindSingleCancel(view, dlg, "取消")
        dlg.show()
        return dlg
    }

    /**
     * 多选弹窗：勾选后点「确定」返回选中集。
     * @param onConfirm 返回 false 表示不关闭弹窗（如校验失败）。
     */
    fun showMultiChoice(
        ctx: Context,
        title: String,
        items: List<String>,
        checked: BooleanArray,
        confirmText: String = "确定",
        cancelText: String = "取消",
        cancelable: Boolean = true,
        onConfirm: ((BooleanArray) -> Boolean)? = null
    ): AlertDialog {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_unified_list, null)
        setListTitle(view, title)
        val dlg = MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_Daily_UnifiedDialog)
            .setView(DialogIdleBridge.wrap(view))
            .create()
        dlg.setCancelable(cancelable)

        val container = view.findViewById<LinearLayout>(R.id.listContainer)
        val boxes = mutableListOf<MaterialCheckBox>()
        items.forEachIndexed { index, label ->
            val row = buildChoiceRow(ctx, label, 2, checked.getOrElse(index) { false })
            val box = row.getChildAt(1) as MaterialCheckBox
            boxes.add(box)
            row.setOnClickListener { box.isChecked = !box.isChecked }
            container.addView(row)
        }

        val btnBar = view.findViewById<LinearLayout>(R.id.btnBar)
        val btnPos = view.findViewById<Button>(R.id.btnPositive)
        val btnNeg = view.findViewById<Button>(R.id.btnNegative)
        btnNeg.text = cancelText
        btnNeg.visibility = View.VISIBLE
        btnNeg.setOnClickListener { dlg.dismiss() }
        btnPos.text = confirmText
        btnPos.visibility = View.VISIBLE
        btnPos.setOnClickListener {
            val result = BooleanArray(boxes.size) { boxes[it].isChecked }
            if (onConfirm?.invoke(result) != false) dlg.dismiss()
        }
        btnBar.gravity = Gravity.CENTER
        dlg.show()
        return dlg
    }
}
