package com.drdisagree.iconify.hyperos

import android.os.Build

/**
 * Conservative HyperOS runtime detection.
 *
 * HyperOS private APIs vary between Android generation, device and build.
 * Feature adapters should fail open when a target is not known.
 */
object HyperOsEnvironment {
    const val SYSTEMUI_PACKAGE = "com.android.systemui"
    const val SYSTEMUI_PLUGIN_PACKAGE = "miui.systemui.plugin"
    const val SETTINGS_PACKAGE = "com.android.settings"

    enum class Generation { HYPEROS_1, HYPEROS_2, HYPEROS_3, HYPEROS_4, UNKNOWN }

    val isXiaomiFamily: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", true) ||
            Build.BRAND.equals("Redmi", true) ||
            Build.BRAND.equals("POCO", true)

    val isHyperOs: Boolean
        get() = isXiaomiFamily &&
            (Build.DISPLAY.contains("HyperOS", true) ||
                systemProperty("ro.mi.os.version.name").isNotBlank() ||\n                systemProperty("ro.mi.os.version.code").isNotBlank())

    val generation: Generation
        get() {
            val version = systemProperty("ro.mi.os.version.name")
            val major = Regex("(\\d+)").find(version)?.groupValues?.getOrNull(1)
                ?: return Generation.UNKNOWN
            return when (major) {
                "4" -> Generation.HYPEROS_4
                "3" -> Generation.HYPEROS_3
                "2" -> Generation.HYPEROS_2
                "1" -> Generation.HYPEROS_1
                else -> Generation.UNKNOWN
            }
        }

    fun systemProperty(name: String): String {
        return try {
            @Suppress("PrivateApi")
            val systemProperties = Class.forName("android.os.SystemProperties")
            systemProperties.getMethod("get", String::class.java)
                .invoke(null, name) as? String ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun usesSystemUiBackend(packageName: String): Boolean =
        isHyperOs && packageName == SYSTEMUI_PACKAGE

    fun usesPluginBackend(packageName: String): Boolean =
        isHyperOs && packageName == SYSTEMUI_PLUGIN_PACKAGE
}
