package id.familyguard.ui.theme

import androidx.compose.ui.graphics.Color

// Pure Monochromatic Palette (OLED Black / Clean Paper White)
val BgDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF000000)
val BandDark = Color(0xFF121214)
val BorderDark = Color(0xFF1C1C1E)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF8E8E93)

val BgLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFFFFFFF)
val BandLight = Color(0xFFF4F4F6)
val BorderLight = Color(0xFFE5E5EA)
val TextPrimaryLight = Color(0xFF000000)
val TextSecondaryLight = Color(0xFF8E8E93)

// Accents
val ToggleBlue = Color(0xFF3B57FA)
val AccentOrange = Color(0xFFF38020)
val TagQuarantine = Color(0xFFF59E0B)
val TagBlocked = Color(0xFFEF4444)

// Backward Compatibility Aliases
val BackgroundLight = BgLight
val SurfaceSubtleLight = BandLight
val BorderLightAlias = BorderLight
val TextMutedLight = TextSecondaryLight

val BackgroundDark = BgDark
val SurfaceSubtleDark = BandDark
val BorderDarkAlias = BorderDark
val TextMutedDark = TextSecondaryDark

val EmeraldPrimary = ToggleBlue
val EmeraldLight = ToggleBlue
val EmeraldSoftBg = BandLight
val EmeraldDarkBg = BandDark

val RosePrimary = TagBlocked
val RoseSoftBg = BandLight
val RoseDarkBg = BandDark

val BluePrimary = ToggleBlue
val BlueSoftBg = BandLight
val BlueDarkBg = BandDark
