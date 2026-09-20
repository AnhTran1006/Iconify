package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.widget.LinearLayout
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class HyperOsStatusBar(context: Context) : ModPack(context) {
    override fun updatePrefs(vararg key: String) = Unit

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(param.packageName)) return
        try {
            val batteryClass = findClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView", suppressError = true)
            if (batteryClass != null) {
                batteryClass.hookMethod("updateAll$1", suppressError = true).runAfter { hookParam ->
                    try {
                        val view = hookParam.thisObject as? LinearLayout
                        val percent = hookParam.thisObject.getField("mBatteryPercentView")
                        val mark = hookParam.thisObject.getField("mBatteryPercentMarkView")
                        val digit = hookParam.thisObject.getField("mBatteryTextDigitView")
                        log("HyperOS battery view discovered: view=" + (view != null) + ", percent=" + (percent != null) + ", mark=" + (mark != null) + ", digit=" + (digit != null))
                    } catch (t: Throwable) {
                        log(this, "HyperOS battery field discovery failed")
                        log(this, t)
                    }
                }
            }
        } catch (t: Throwable) {
            log(this, "HyperOS SystemUI discovery failed; fail-open")
            log(this, t)
        }
    }
}
