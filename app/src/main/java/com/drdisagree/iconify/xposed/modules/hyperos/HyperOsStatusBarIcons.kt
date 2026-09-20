package com.drdisagree.iconify.xposed.modules.hyperos

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.drdisagree.iconify.data.keys.XposedKey
import com.drdisagree.iconify.hyperos.HyperOsEnvironment
import com.drdisagree.iconify.xposed.ModPack
import com.drdisagree.iconify.xposed.modules.extras.utils.misc.ViewHelper.reAddView
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.drdisagree.iconify.xposed.modules.extras.utils.toolkit.hookMethod
import com.drdisagree.iconify.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class HyperOsStatusBarIcons(context: Context) : ModPack(context) {
    private var swapNetworkType = false

    override fun updatePrefs(vararg key: String) {
        swapNetworkType = Xprefs.getBoolean(XposedKey.STATUSBAR_SWAP_CELLULAR_NETWORK_TYPE)
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (!HyperOsEnvironment.usesSystemUiBackend(loadPackageParam.packageName) || !swapNetworkType) return

        val cls = findClass(
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView",
            suppressError = true
        ) ?: return

        cls.hookMethod(
            "configureLayoutForNewStatusBarIcons",
            "constructAndBind"
        ).suppressError().runAfter { hookParam ->
            val root = hookParam.thisObject as? ViewGroup ?: return@runAfter
            reorderNetworkType(root)
        }
    }

    private fun reorderNetworkType(root: ViewGroup) {
        val containerId = root.resources.getIdentifier(
            "mobile_type_container", "id", root.context.packageName
        )
        if (containerId == 0) return

        val container = root.findViewById<ViewGroup>(containerId) ?: return
        val typeId = root.resources.getIdentifier(
            "mobile_type", "id", root.context.packageName
        )
        val type = if (typeId != 0) container.findViewById<View>(typeId) else null
        val parent = container.parent as? ViewGroup ?: return

        if (parent.indexOfChild(container) > 0) {
            parent.reAddView(container, 0)
        }
        type?.requestLayout()
    }
}
