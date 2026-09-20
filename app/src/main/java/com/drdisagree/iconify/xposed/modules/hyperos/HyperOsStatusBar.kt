package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.drdisagree.iconify.data.common.Const.SYSTEMUI_PACKAGE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_DEFAULT
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.HookRes.Companion.modRes
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.misc.ViewHelper.toPx
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.getFieldSilently
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import com.drdisagree.iconify.xposed.modules.statusbar.batterystyles.BatteryDrawable
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 * HyperOS adapter for Iconify's status-bar battery implementation.
 *
 * HyperOS uses MiuiBatteryMeterView and keeps the battery state in that view,
 * so the adapter consumes the existing Iconify battery drawables and preferences
 * without depending on the AOSP BatteryStyleManager view pipeline.
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
    private var scaledPerimeterAlpha = false
    private var scaledFillAlpha = false
    private var rainbowFill = false
    private var blendColor = false
    private var chargingColor = Color.BLACK
    private var fillColor = Color.BLACK
    private var fillGradColor = Color.BLACK
    private var powerSaveColor = Color.BLACK
    private var powerSaveFillColor = Color.BLACK
    private var chargingStyle = 0
    private var chargingEnabled = false
    private var chargingSize = 14
    private var chargingMarginLeft = 1
    private var chargingMarginRight = 0
    private var swapPercentage = false
    private var hideDefaultBattery = false

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
            scaledPerimeterAlpha = getBoolean(XposedKey.CUSTOM_BATTERY_PERIMETER_ALPHA)
            scaledFillAlpha = getBoolean(XposedKey.CUSTOM_BATTERY_FILL_ALPHA)
            rainbowFill = getBoolean(XposedKey.CUSTOM_BATTERY_RAINBOW_FILL_COLOR)
            blendColor = getBoolean(XposedKey.CUSTOM_BATTERY_BLEND_COLOR)
            chargingColor = getColor(XposedKey.CUSTOM_BATTERY_CHARGING_COLOR)
            fillColor = getColor(XposedKey.CUSTOM_BATTERY_FILL_COLOR)
            fillGradColor = getColor(XposedKey.CUSTOM_BATTERY_FILL_GRAD_COLOR)
            powerSaveColor = getColor(XposedKey.CUSTOM_BATTERY_POWER_SAVE_INDICATOR_COLOR)
            powerSaveFillColor = getColor(XposedKey.CUSTOM_BATTERY_POWER_SAVE_FILL_COLOR)
            chargingEnabled = getBoolean(XposedKey.CUSTOM_BATTERY_CHARGING_ICON_SWITCH)
            chargingStyle = getInt(XposedKey.CUSTOM_BATTERY_CHARGING_ICON_STYLE)
            chargingSize = getInt(XposedKey.CUSTOM_BATTERY_CHARGING_ICON_WIDTH_HEIGHT)
            chargingMarginLeft = getInt(XposedKey.CUSTOM_BATTERY_CHARGING_ICON_MARGIN_LEFT)
            chargingMarginRight = getInt(XposedKey.CUSTOM_BATTERY_CHARGING_ICON_MARGIN_RIGHT)
            swapPercentage = getBoolean(XposedKey.CUSTOM_BATTERY_SWAP_PERCENTAGE)
            hideDefaultBattery = getBoolean(XposedKey.HIDE_DEFAULT_BATTERY_VIEW)
        }
    }

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(param.packageName)) return

        val batteryClass = findClass(
            "$SYSTEMUI_PACKAGE.statusbar.views.MiuiBatteryMeterView",
            suppressError = true
        ) ?: return

        batteryClass.hookMethod("updateAll$1").suppressError().runAfter { hookParam ->
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

        val hideText = hidePercent || insidePercent
        val batteryView = instance as? ViewGroup
        percent?.visibility = if (hideText) View.GONE else View.VISIBLE
        mark?.visibility = if (hideText) View.GONE else View.VISIBLE
        digit?.visibility = if (hideText) View.GONE else View.VISIBLE

        if (style == BATTERY_STYLE_DEFAULT) {
            icon?.visibility = if (hideBattery || hideDefaultBattery) View.GONE else View.VISIBLE
            return
        }

        val drawable = createDrawable(style) ?: return
        val level = (instance.getFieldSilently("mLevel") as? Int) ?: 0
        val isCharging = (instance.getFieldSilently("mCharging") as? Boolean) ?: false
        val isPowerSave = (instance.getFieldSilently("mPowerSave") as? Boolean) ?: false
        val fg = percent?.currentTextColor ?: Color.WHITE

        drawable.customizeBatteryDrawable(
            reverseLayout,
            scaledPerimeterAlpha,
            scaledFillAlpha,
            blendColor,
            rainbowFill,
            fillColor,
            fillGradColor,
            chargingColor,
            powerSaveColor,
            powerSaveFillColor,
            chargingEnabled
        )
        drawable.setBatteryLevel(level)
        drawable.setChargingEnabled(isCharging)
        drawable.setPowerSavingEnabled(isPowerSave)
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

        charging?.apply {
            val chargingDrawable = getChargingDrawable()
            if (chargingDrawable != null) setImageDrawable(chargingDrawable)
            visibility = if (isCharging && chargingEnabled) View.VISIBLE else View.GONE
            setColorFilter(fg, PorterDuff.Mode.SRC_IN)
            val lp = layoutParams as? ViewGroup.MarginLayoutParams
            lp?.let {
                it.width = mContext.toPx(chargingSize)
                it.height = mContext.toPx(chargingSize)
                it.setMargins(mContext.toPx(chargingMarginLeft), 0, mContext.toPx(chargingMarginRight), 0)
                layoutParams = it
            }
        }
    }

    private fun createDrawable(selectedStyle: Int): BatteryDrawable? {
        val className = when (selectedStyle) {
            1 -> "RLandscapeBattery"
            2 -> "LandscapeBattery"
            3 -> "PortraitBatteryCapsule"
            4 -> "PortraitBatteryLorn"
            5 -> "PortraitBatteryMx"
            6 -> "PortraitBatteryAiroo"
            7 -> "RLandscapeBatteryStyleA"
            8 -> "LandscapeBatteryStyleA"
            9 -> "RLandscapeBatteryStyleB"
            10 -> "LandscapeBatteryStyleB"
            11 -> "LandscapeBatteryiOS15"
            12 -> "LandscapeBatteryiOS16"
            13 -> "PortraitBatteryOrigami"
            14 -> "LandscapeBatterySmiley"
            15 -> "LandscapeBatteryMIUIPill"
            16 -> "LandscapeBatteryColorOS"
            17 -> "RLandscapeBatteryColorOS"
            18 -> "LandscapeBatteryA"
            19 -> "LandscapeBatteryB"
            20 -> "LandscapeBatteryC"
            21 -> "LandscapeBatteryD"
            22 -> "LandscapeBatteryE"
            23 -> "LandscapeBatteryF"
            24 -> "LandscapeBatteryG"
            25 -> "LandscapeBatteryH"
            26 -> "LandscapeBatteryI"
            27 -> "LandscapeBatteryJ"
            28 -> "LandscapeBatteryK"
            29 -> "LandscapeBatteryL"
            30 -> "LandscapeBatteryM"
            31 -> "LandscapeBatteryN"
            32 -> "LandscapeBatteryO"
            33 -> "CircleBattery"
            34 -> "CircleBattery"
            35 -> "CircleFilledBattery"
            36 -> "LandscapeBatteryKim"
            37 -> "LandscapeBatteryOneUI7"
            else -> return null
        }

        return try {
            val clazz = Class.forName(
                "com.drdisagree.iconify.xposed.modules.statusbar.batterystyles.$className"
            )
            val drawable = clazz.getConstructor(Context::class.java, Int::class.javaPrimitiveType)
                .newInstance(mContext, Color.WHITE) as BatteryDrawable
            if (selectedStyle == 34) {
                clazz.getMethod("setMeterStyle", Int::class.javaPrimitiveType)
                    .invoke(drawable, selectedStyle)
            }
            drawable
        } catch (t: Throwable) {
            log(this, "Unable to create HyperOS battery style $selectedStyle")
            log(this, t)
            null
        }
    }

    private fun getChargingDrawable(): Drawable? {
        val names = arrayOf(
            "ic_charging_bold", "ic_charging_asus", "ic_charging_buddy", "ic_charging_evplug",
            "ic_charging_idc", "ic_charging_ios", "ic_charging_koplak", "ic_charging_miui",
            "ic_charging_mmk", "ic_charging_moto", "ic_charging_nokia", "ic_charging_plug",
            "ic_charging_powercable", "ic_charging_powercord", "ic_charging_powerstation",
            "ic_charging_realme", "ic_charging_soak", "ic_charging_stres", "ic_charging_strip",
            "ic_charging_usbcable", "ic_charging_xiaomi"
        )
        if (chargingStyle !in names.indices) return null
        val id = modRes.getIdentifier(names[chargingStyle], "drawable", mContext.packageName)
        return if (id != 0) modRes.getDrawable(id, mContext.theme) else null
    }
}
