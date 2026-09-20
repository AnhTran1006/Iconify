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
        hookRootBinder()
        hookClockProvider()
    }

    private fun hookRootView() {
        val cls = findClass(
            "com.android.keyguard.widget.HyperOSKeyguardRootView",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "onFinishInflate",
            "onAttachedToWindow",
            "onLayout"
        ).suppressError().runAfter { hookParam ->
            if (hideLockIcon) hideKnownLockIcon(hookParam.thisObject)
        }
    }

    private fun hookRootBinder() {
        val cls = findClass(
            "com.android.systemui.keyguard.ui.binder.KeyguardRootViewBinder",
            "com.android.keyguard.ui.binder.KeyguardPanelViewBinder",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "bind",
            "onViewAttached",
            "update"
        ).suppressError().runAfter { hookParam ->
            if (hideLockIcon) hideKnownLockIcon(hookParam.thisObject)
        }
    }

    private fun hookClockProvider() {
        val cls = findClass(
            "com.miui.keyguard.clock.HyperOSDefaultClockProvider",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "getClockView",
            "createClockView",
            "provideClock"
        ).suppressError().runAfter { }
    }

    private fun hideKnownLockIcon(target: Any) {
        val view = target as? View ?: return
        for (name in listOf("lock_icon", "keyguard_lock_icon", "lock_icon_view")) {
            val id = view.resources.getIdentifier(name, "id", view.context.packageName)
            if (id != 0) view.findViewById<View>(id)?.visibility = View.GONE
        }
    }
}
