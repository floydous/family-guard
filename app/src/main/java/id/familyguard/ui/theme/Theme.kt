package id.familyguard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ToggleBlue,
    onPrimary = BgLight,
    primaryContainer = BandLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = AccentOrange,
    onSecondary = BgLight,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BandLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = TagBlocked,
    onError = BgLight
)

private val DarkColorScheme = darkColorScheme(
    primary = ToggleBlue,
    onPrimary = TextPrimaryDark,
    primaryContainer = BandDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentOrange,
    onSecondary = BgDark,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = BandDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = TagBlocked,
    onError = BgDark
)

@Composable
fun FamilyGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
