package com.drdisagree.iconify.hyperos

data class HyperOsCapabilities(
    val wifiIcons: Boolean = false,
    val mobileIcons: Boolean = false,
    val signalIcons: Boolean = false,
    val batteryIcons: Boolean = false,
    val chargingIcons: Boolean = false,
    val statusBarLayout: Boolean = false,
    val quickSettingsCompose: Boolean = false,
    val controlCenterPlugin: Boolean = false,
    val brightnessPlugin: Boolean = false,
    val dynamicIslandPlugin: Boolean = false,
    val keyguardCompose: Boolean = false,
)
