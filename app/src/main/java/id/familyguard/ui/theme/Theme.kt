package id.familyguard.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Slate950,
    primaryContainer = Slate850,
    onPrimaryContainer = Emerald400,
    secondary = Cyan400,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Cyan400,
    tertiary = Indigo400,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = TextPrimary,
    surface = Slate900,
    onSurface = TextPrimary,
    surfaceVariant = Slate800,
    onSurfaceVariant = TextSecondary,
    outline = Slate700,
    outlineVariant = Slate800,
    error = Rose400,
    onError = Slate950,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Rose400
)

@Composable
fun FamilyGuardTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Slate950.toArgb()
            window.navigationBarColor = Slate950.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
