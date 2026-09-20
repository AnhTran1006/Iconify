package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.view.ViewGroup
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.math.roundToInt

class HyperOsNotificationAdapter(context: Context) : ModPack(context) {

    private var iconLimit = -1

    override fun updatePrefs(vararg key: String) {
        iconLimit = Xprefs.getFloat(XposedKey.NOTIFICATION_ICONS_LIMIT).roundToInt()
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(loadPackageParam.packageName)) return
        if (iconLimit < 0) return

        val cls = findClass(
            "com.android.systemui.statusbar.phone.NotificationIconContainer",
            "com.android.systemui.statusbar.notification.icon.ui.viewbinder.NotificationIconContainerViewBinder",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "onLayout",
            "updateState",
            "bind"
        ).suppressError().runAfter { hookParam ->
            val root = hookParam.thisObject as? ViewGroup ?: return@runAfter
            applyLimit(root)
        }
    }

    private fun applyLimit(root: ViewGroup) {
        var visible = 0
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child.visibility == android.view.View.GONE) continue

            if (visible++ < iconLimit) {
                child.visibility = android.view.View.VISIBLE
            } else {
                child.visibility = android.view.View.GONE
            }
        }
    }
}
