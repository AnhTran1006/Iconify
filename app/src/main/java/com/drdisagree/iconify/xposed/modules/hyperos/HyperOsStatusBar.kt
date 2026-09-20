package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class HyperOsStatusBar(context: Context) : ModPack(context) {
    override fun updatePrefs(vararg key: String) = Unit

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(param.packageName)) return
        try {
            val battery = findClass("com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl", suppressError = true)
            val batteryView = findClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView", suppressError = true)
            val batteryController = findClass("com.android.systemui.statusbar.policy.BatteryControllerImpl", suppressError = true)
            val wifi = findClass("com.android.systemui.statusbar.connectivity.WifiSignalController", suppressError = true)
            val mobile = findClass("com.android.systemui.statusbar.connectivity.MobileSignalController", suppressError = true)
            val iconController = findClass("com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl", suppressError = true)
            log("HyperOS SystemUI mapping: battery=" + (battery != null) + ", batteryView=" + (batteryView != null) + ", batteryController=" + (batteryController != null) + ", wifi=" + (wifi != null) + ", mobile=" + (mobile != null) + ", iconController=" + (iconController != null))
        } catch (t: Throwable) {
            log(this, "HyperOS StatusBar discovery failed; fail-open")
            log(this, t)
        }
    }
}