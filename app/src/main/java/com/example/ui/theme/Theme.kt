package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AegisDarkColorScheme = darkColorScheme(
    primary = AegisHealthy,
    onPrimary = AegisVoid,
    primaryContainer = AegisHealthyContainer,
    onPrimaryContainer = AegisHealthy,
    secondary = AegisTrust,
    onSecondary = AegisTextPrimary,
    secondaryContainer = AegisTrustContainer,
    onSecondaryContainer = AegisTextPrimary,
    tertiary = AegisCyan,
    onTertiary = AegisVoid,
    background = AegisVoid,
    onBackground = AegisTextPrimary,
    surface = AegisSurface,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisSurfaceVariant,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisBorder,
    outlineVariant = AegisBorderFocused,
    error = AegisCritical,
    onError = AegisTextPrimary,
    errorContainer = AegisCriticalContainer,
    onErrorContainer = AegisCritical
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // AegisCore enforces high-contrast privacy dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = AegisDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AegisVoid.toArgb()
            window.navigationBarColor = AegisVoid.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
