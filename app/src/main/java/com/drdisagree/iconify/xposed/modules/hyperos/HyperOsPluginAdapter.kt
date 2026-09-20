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

/**
 * HyperOS 4 plugin-side adapter.
 *
 * HyperOS 4 moved Control Center, brightness and Dynamic Island windows into
 * the miui.systemui.plugin scope. Hooks are resolved structurally and fail
 * closed when a particular device build does not expose the target method.
 */
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
        if (HyperOsEnvironment.generation != HyperOsEnvironment.Generation.HYPEROS_4) return

        hookControlCenter()
        hookBrightness()
        hookDynamicIsland()
        hookPluginLifecycle()
        bridgeBlurResource()
    }

    private fun hookControlCenter() {
        val cls = findClass(
            "miui.systemui.controlcenter.windowview.ControlCenterWindowViewImpl",
            "miui.systemui.controlcenter.windowview.ControlCenterWindowViewController",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "createWindowView",
            "getWindowView",
            "attachToWindow",
            "detachFromWindow"
        ).suppressError().runAfter {
            log(this, "HyperOS 4 Control Center target resolved")
        }
    }

    private fun hookBrightness() {
        val cls = findClass(
            "miui.systemui.plugin.brightness.window.BrightnessWindowViewController",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "onCreate",
            "updateInternalInsets",
            "ensureWindow",
            "closeWindow"
        ).suppressError().runAfter {
            log(this, "HyperOS 4 brightness target resolved")
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
            log(this, "HyperOS 4 Dynamic Island target resolved")
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
            log(this, "HyperOS 4 plugin lifecycle target resolved")
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
