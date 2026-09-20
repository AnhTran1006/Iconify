package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.view.View
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class HyperOsKeyguardAdapter(context: Context) : ModPack(context) {

    private var hideLockIcon = false

    override fun updatePrefs(vararg key: String) {
        hideLockIcon = Xprefs.getBoolean(XposedKey.HIDE_LOCKSCREEN_LOCK_ICON)
    }

    override fun handleLoadPackage(param: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(param.packageName)) return
        hookRootView()
        hookBlueprint()
        hookClockProvider()
    }

    private fun hookRootView() {
        val cls = findClass(
            "com.android.keyguard.widget.HyperOSKeyguardRootView",
            "com.android.systemui.keyguard.ui.view.KeyguardRootView",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "onFinishInflate",
            "onAttachedToWindow",
            "onLayout"
        ).suppressError().runAfter { hookParam ->
            if (hideLockIcon) (hookParam.thisObject as? View)?.let(::hideKnownLockIcon)
        }
    }

    private fun hookBlueprint() {
        val cls = findClass(
            "com.android.keyguard.blueprint.HyperOsKeyguardBlueprint",
            "com.android.keyguard.blueprint.KeyguardPanelViewSection",
            "com.android.keyguard.ui.binder.KeyguardPanelViewBinder",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "bind",
            "start",
            "onViewAttached",
            "update"
        ).suppressError().runAfter { hookParam ->
            if (hideLockIcon) {
                (hookParam.thisObject as? View)?.let(::hideKnownLockIcon)
                hookParam.args.filterIsInstance<View>().forEach(::hideKnownLockIcon)
            }
        }
    }

    private fun hookClockProvider() {
        val cls = findClass(
            "com.miui.keyguard.clock.HyperOSDefaultClockProvider",
            "com.android.keyguard.clock.MiuiKeyguardSingleClock",
            "com.android.keyguard.clock.MiuiKeyguardDualClock",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "getClockView",
            "createClockView",
            "provideClock",
            "onFinishInflate"
        ).suppressError().runAfter { }
    }

    private fun hideKnownLockIcon(view: View) {
        for (name in listOf("lock_icon", "keyguard_lock_icon", "lock_icon_view")) {
            val id = view.resources.getIdentifier(name, "id", view.context.packageName)
            if (id != 0) view.findViewById<View>(id)?.visibility = View.GONE
        }
    }
}
