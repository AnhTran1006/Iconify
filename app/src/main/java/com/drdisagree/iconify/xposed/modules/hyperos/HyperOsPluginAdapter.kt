package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.ResourceHookManager
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class HyperOsPluginAdapter(context: Context) : ModPack(context) {

    private var blurEnabled = false
    private var blurRadius = 23

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            blurEnabled = getBoolean(XposedKey.QUICK_SETTINGS_BLUR)
            blurRadius = getInt(XposedKey.QUICK_SETTINGS_BLUR_RADIUS)
        }
    }

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesPluginBackend(param.packageName)) return

        hookControlCenter()
        hookBrightness()
        hookDynamicIsland()
        hookPluginLifecycle()
        bridgeBlurResource()
    }

    private fun hookControlCenter() {
        val cls = findClass(
            "com.android.systemui.controlcenter.controls.HyperOSControlsManager",
            "com.android.systemui.controlcenter.controls.HyperOSControlsRepository",
            "miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl",
            "miui.systemui.controlcenter.windowview.ControlCenterWindowViewController",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "createWindowView",
            "getWindowView",
            "attachToWindow",
            "detachFromWindow",
            "onCreate",
            "onDestroy"
        ).suppressError().runAfter {
            log(this, "HyperOS Control Center target resolved")
        }
    }

    private fun hookBrightness() {
        val cls = findClass(
            "com.android.systemui.controlcenter.policy.HyperOSBrightnessPolicyV1",
            "miui.systemui.plugin.brightness.window.BrightnessWindowViewController",
            "miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "onCreate",
            "update",
            "updateBrightness",
            "updateInternalInsets",
            "ensureWindow",
            "closeWindow"
        ).suppressError().runAfter {
            log(this, "HyperOS brightness target resolved")
        }
    }

    private fun hookDynamicIsland() {
        val cls = findClass(
            "miui.systemui.dynamicisland.window.DynamicIslandWindowViewController",
            "miui.systemui.dynamicisland.window.DynamicIslandWindowViewRefactor",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "sendWindowAnimEventForLinkage",
            "onUserChanged",
            "attachToWindow",
            "detachFromWindow"
        ).suppressError().runAfter {
            log(this, "HyperOS Dynamic Island target resolved")
        }
    }

    private fun hookPluginLifecycle() {
        val lifecycle = findClass(
            "miui.systemui.plugins.domain.interactor.PluginLifecycleInteractor",
            suppressError = true
        ) ?: return

        lifecycle.hookMethod(
            "start",
            "stop",
            "onStart",
            "onStop"
        ).suppressError().runAfter {
            log(this, "HyperOS plugin lifecycle target resolved")
        }
    }

    private fun bridgeBlurResource() {
        if (!blurEnabled) return

        ResourceHookManager
            .hookDimen()
            .whenCondition { true }
            .forPackageName(HyperOsEnvironment.SYSTEMUI_PLUGIN_PACKAGE)
            .addResource("max_window_blur_radius") { blurRadius }
            .apply()
    }
}
