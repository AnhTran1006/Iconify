package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/** HyperOS 4 plugin-side capability discovery. */
class HyperOsPluginAdapter(context: Context) : ModPack(context) {
    override fun updatePrefs(vararg key: String) = Unit

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesPluginBackend(param.packageName)) return

        val targets = listOf(
            "miui.systemui.plugin.brightness.window.BrightnessWindowViewController",
            "miui.systemui.controlcenter.windowview.ControlCenterWindowViewController",
            "miui.systemui.dynamicisland.window.DynamicIslandWindowViewController",
            "miui.systemui.plugins.domain.interactor.PluginLifecycleInteractor"
        )
        val present = targets.count { findClass(it, suppressError = true) != null }
        log(this, "HyperOS plugin capabilities: " + present + "/" + targets.size + " core targets present")
    }
}
