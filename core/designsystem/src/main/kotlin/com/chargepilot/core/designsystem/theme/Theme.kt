package com.chargepilot.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Sky400,
    onPrimary = Slate900,
    secondary = Slate700,
    onSecondary = Slate50,
    tertiary = Green500,
    onTertiary = Slate900,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    error = Red500,
)

private val LightColors = lightColorScheme(
    primary = Slate800,
    onPrimary = Slate50,
    secondary = Slate700,
    onSecondary = Slate50,
    tertiary = Green500,
    onTertiary = Slate900,
    background = Slate50,
    onBackground = Slate900,
    surface = Slate100,
    onSurface = Slate900,
    error = Red500,
)

/**
 * Single entry point for theming Charge Pilot screens.
 *
 * @param darkTheme whether to use dark colors; defaults to following the system.
 * @param dynamicColor whether to opt into Material You wallpaper-derived colors on
 *  Android 12+. Off by default to keep the brand palette intact; toggleable via Settings.
 */
@Composable
fun ChargePilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChargePilotTypography,
        content = content,
    )
}
