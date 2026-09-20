package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.view.View
import android.widget.TextView
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.callMethodSilently
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.getFieldSilently
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.math.roundToInt

/**
 * HyperOS 4 Control Center volume adapter.
 *
 * OS4 uses the plugin-side VolumeSliderController rather than the legacy
 * VolumeDialogImpl row model.
 */
class HyperOsVolumeAdapter(context: Context) : ModPack(context) {

    private var showPercentage = false

    override fun updatePrefs(vararg key: String) {
        showPercentage = Xprefs.getBoolean(XposedKey.VOLUME_PANEL_PERCENTAGE)
    }

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesPluginBackend(param.packageName)) return
        if (!showPercentage) return

        val controller = findClass(
            "miui.systemui.controlcenter.panel.main.volume.VolumeSliderController",
            suppressError = true
        ) ?: return

        controller.hookMethod(
            "updateSuperVolume",
            "updateIconProgress"
        ).suppressError().runAfter { hookParam ->
            updateTopText(hookParam.thisObject)
        }
    }

    private fun updateTopText(controller: Any) {
        val holder = controller.callMethodSilently("getHolder") ?: return
        val itemView = holder.getFieldSilently("itemView") as? View ?: return
        val topId = itemView.resources.getIdentifier(
            "top_text", "id", itemView.context.packageName
        )
        if (topId == 0) return

        val topText = itemView.findViewById<TextView>(topId) ?: return
        val max = controller.getFieldSilently("sliderMaxValue") as? Int ?: return
        val value = controller.callMethodSilently("getTargetValue") as? Int ?: return
        if (max <= 0) return

        val percent = (value.toFloat() / max.toFloat() * 100f)
            .roundToInt()
            .coerceIn(0, 100)

        topText.text = "$percent%"
        topText.visibility = View.VISIBLE
    }
}
