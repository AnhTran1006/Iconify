package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.log
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 * HyperOS 4 plugin-side adapter.
 *
 * HyperOS 4 moved Control Center, brightness and Dynamic Island windows into
 * the miui.systemui.plugin scope. Hooks are resolved structurally and fail
 * closed when a particular device build does not expose the target method.
 */
class HyperOsPluginAdapter(context: Context) : ModPack(context) {

    override fun updatePrefs(vararg key: String) = Unit

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesPluginBackend(param.packageName)) return
        if (HyperOsEnvironment.generation != HyperOsEnvironment.Generation.HYPEROS_4) return

        hookControlCenter()
        hookBrightness()
        hookDynamicIsland()

        val lifecycle = findClass(
            "miui.systemui.plugins.domain.interactor.PluginLifecycleInteractor",
            suppressError = true
        )
        if (lifecycle != null) {
            log(this, "HyperOS 4 PluginLifecycleInteractor detected")
        }
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
}
