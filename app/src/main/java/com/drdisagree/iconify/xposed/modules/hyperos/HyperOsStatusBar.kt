package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.drdisagree.iconify.R
import com.drdisagree.iconify.data.common.Const.SYSTEMUI_PACKAGE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_DOTTED_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_FILLED_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_DEFAULT
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.HookRes.Companion.modRes
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.misc.ViewHelper.toPx
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.getField
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.getFieldSilently
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import com.drdisagree.iconify.xposed.modules.statusbar.batterystyles.BatteryDrawable
import com.drdisagree.iconify.xposed.modules.statusbar.batterystyles.CircleBattery
import com.drdisagree.iconify.xposed.modules.statusbar.batterystyles.CircleFilledBattery
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Field

/**
 * HyperOS adapter for Iconify's status-bar battery system.
 *
 * HyperOS uses MiuiBatteryMeterView instead of the AOSP battery pipeline.
 * The adapter deliberately uses optional reflection and fails open on unknown builds.
 */
class HyperOsStatusBar(context: Context) : ModPack(context) {

    private var style = BATTERY_STYLE_DEFAULT
    private var hidePercent = false
    private var hideBattery = false
    private var insidePercent = false
    private var reverseLayout = false
    private var width = 20
    private var height = 20
    private var marginLeft = 0
    private var marginTop = 0
    private var marginRight = 0
    private var marginBottom = 0

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            style = getString(XposedKey.CUSTOM_BATTERY_STYLE).toIntOrNull() ?: BATTERY_STYLE_DEFAULT
            hidePercent = getBoolean(XposedKey.CUSTOM_BATTERY_HIDE_PERCENTAGE)
            hideBattery = getBoolean(XposedKey.CUSTOM_BATTERY_HIDE_BATTERY)
            insidePercent = getBoolean(XposedKey.CUSTOM_BATTERY_INSIDE_PERCENTAGE)
            reverseLayout = getBoolean(XposedKey.CUSTOM_BATTERY_LAYOUT_REVERSE)
            width = getInt(XposedKey.CUSTOM_BATTERY_WIDTH)
            height = getInt(XposedKey.CUSTOM_BATTERY_HEIGHT)
            marginLeft = mContext.toPx(getInt(XposedKey.CUSTOM_BATTERY_MARGIN_LEFT))
            marginTop = mContext.toPx(getInt(XposedKey.CUSTOM_BATTERY_MARGIN_TOP))
            marginRight = mContext.toPx(getInt(XposedKey.CUSTOM_BATTERY_MARGIN_RIGHT))
            marginBottom = mContext.toPx(getInt(XposedKey.CUSTOM_BATTERY_MARGIN_BOTTOM))
        }
    }

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(param.packageName)) return

        val batteryClass = findClass(
            "$SYSTEMUI_PACKAGE.statusbar.views.MiuiBatteryMeterView",
            suppressError = true
        ) ?: return

        batteryClass
            .hookMethod("updateAll$1")
            .suppressError()
            .runAfter { hookParam ->
                try {
                    applyBatteryView(hookParam.thisObject)
                } catch (t: Throwable) {
                    log(this, "HyperOS battery adapter failed; keeping stock battery")
                    log(this, t)
                }
            }
    }

    private fun applyBatteryView(instance: Any) {
        val percent = instance.getFieldSilently("mBatteryPercentView") as? TextView
        val mark = instance.getFieldSilently("mBatteryPercentMarkView") as? TextView
        val digit = instance.getFieldSilently("mBatteryTextDigitView") as? TextView
        val icon = instance.getFieldSilently("mBatteryIconView") as? ImageView
        val charging = instance.getFieldSilently("mBatteryChargingView") as? ImageView

        percent?.visibility = if (hidePercent || insidePercent) View.GONE else View.VISIBLE
        mark?.visibility = if (hidePercent || insidePercent) View.GONE else View.VISIBLE
        digit?.visibility = if (hidePercent || insidePercent) View.GONE else View.VISIBLE

        if (style == BATTERY_STYLE_DEFAULT) {
            icon?.visibility = if (hideBattery) View.GONE else View.VISIBLE
            return
        }

        val level = (instance.getFieldSilently("mLevel") as? Int) ?: 0
        val isCharging = (instance.getFieldSilently("mCharging") as? Boolean) ?: false
        val drawable = createDrawable(style) ?: return

        drawable.setBatteryLevel(level)
        drawable.setChargingEnabled(isCharging)
        drawable.setPowerSavingEnabled(false)

        val fg = percent?.currentTextColor ?: Color.WHITE
        drawable.setColors(fg, Color.TRANSPARENT, fg)
        drawable.setShowPercentEnabled(insidePercent)

        icon?.apply {
            setImageDrawable(drawable)
            visibility = if (hideBattery) View.GONE else View.VISIBLE
            val lp = layoutParams as? ViewGroup.MarginLayoutParams
            lp?.let {
                it.width = mContext.toPx(width)
                it.height = mContext.toPx(height)
                it.setMargins(marginLeft, marginTop, marginRight, marginBottom)
                layoutParams = it
            }
            rotation = if (reverseLayout) 180f else 0f
        }

        charging?.setColorFilter(fg, PorterDuff.Mode.SRC_IN)

        if (insidePercent) {
            // HyperOS already owns the percentage TextViews; keep the drawable responsible
            // for the embedded percentage and hide the external text fields.
            percent?.visibility = View.GONE
            mark?.visibility = View.GONE
            digit?.visibility = View.GONE
        }
    }

    private fun createDrawable(selectedStyle: Int): BatteryDrawable? {
        return when (selectedStyle) {
            BATTERY_STYLE_CIRCLE -> CircleBattery(mContext, Color.WHITE)
            BATTERY_STYLE_DOTTED_CIRCLE -> CircleBattery(mContext, Color.WHITE).apply {
                setMeterStyle(BATTERY_STYLE_DOTTED_CIRCLE)
            }
            BATTERY_STYLE_FILLED_CIRCLE -> CircleFilledBattery(mContext, Color.WHITE)
            else -> null
        }
    }
}
