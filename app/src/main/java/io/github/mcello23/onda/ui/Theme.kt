package io.github.mcello23.onda.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.mcello23.onda.data.AppSettings
import io.github.mcello23.onda.data.SeedColor
import io.github.mcello23.onda.data.ThemeMode

private fun seedLightScheme(seed: SeedColor): ColorScheme = when (seed) {
    SeedColor.OCEAN -> lightColorScheme(
        primary = Color(0xFF006399),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCDE5FF),
        onPrimaryContainer = Color(0xFF001D32),
        secondary = Color(0xFF51606F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5E4F7),
        onSecondaryContainer = Color(0xFF0E1D2A),
        tertiary = Color(0xFF67587A),
        background = Color(0xFFF7F9FD),
        surface = Color(0xFFF7F9FD),
        surfaceContainerLow = Color(0xFFF1F4FA),
        surfaceContainer = Color(0xFFEBF0F7),
        surfaceContainerHigh = Color(0xFFE5EBF3),
    )
    SeedColor.TERRACOTTA -> lightColorScheme(
        primary = Color(0xFF9C4221),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDBCE),
        onPrimaryContainer = Color(0xFF380D00),
        secondary = Color(0xFF77574B),
        secondaryContainer = Color(0xFFFFDBCE),
        onSecondaryContainer = Color(0xFF2C160D),
        background = Color(0xFFFDF8F6),
        surface = Color(0xFFFDF8F6),
        surfaceContainerLow = Color(0xFFF8F1EE),
        surfaceContainer = Color(0xFFF4EAE6),
        surfaceContainerHigh = Color(0xFFEDE1DC),
    )
    SeedColor.EMERALD -> lightColorScheme(
        primary = Color(0xFF006C4C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF89F8C7),
        onPrimaryContainer = Color(0xFF002114),
        secondary = Color(0xFF4D6357),
        secondaryContainer = Color(0xFFCFE9D9),
        onSecondaryContainer = Color(0xFF092016),
        background = Color(0xFFF5FBF7),
        surface = Color(0xFFF5FBF7),
        surfaceContainerLow = Color(0xFFEEF5F0),
        surfaceContainer = Color(0xFFE6F1EB),
        surfaceContainerHigh = Color(0xFFDDEBE3),
    )
    SeedColor.VIOLET -> lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        background = Color(0xFFFAF8FF),
        surface = Color(0xFFFAF8FF),
        surfaceContainerLow = Color(0xFFF4F1FA),
        surfaceContainer = Color(0xFFEFEBF8),
        surfaceContainerHigh = Color(0xFFE8E2F4),
    )
}

private fun seedDarkScheme(seed: SeedColor): ColorScheme = when (seed) {
    SeedColor.OCEAN -> darkColorScheme(
        primary = Color(0xFF92CCFF),
        onPrimary = Color(0xFF003351),
        primaryContainer = Color(0xFF004B73),
        onPrimaryContainer = Color(0xFFCDE5FF),
        secondary = Color(0xFFB9C8DA),
        secondaryContainer = Color(0xFF394857),
        onSecondaryContainer = Color(0xFFD5E4F7),
        background = Color(0xFF101418),
        surface = Color(0xFF101418),
        surfaceContainerLow = Color(0xFF181C21),
        surfaceContainer = Color(0xFF1C2026),
        surfaceContainerHigh = Color(0xFF262B32),
    )
    SeedColor.TERRACOTTA -> darkColorScheme(
        primary = Color(0xFFFFB59A),
        onPrimary = Color(0xFF5B1B03),
        primaryContainer = Color(0xFF7D2C0D),
        onPrimaryContainer = Color(0xFFFFDBCE),
        secondary = Color(0xFFE7BDB0),
        secondaryContainer = Color(0xFF5D4035),
        onSecondaryContainer = Color(0xFFFFDBCE),
        background = Color(0xFF171210),
        surface = Color(0xFF171210),
        surfaceContainerLow = Color(0xFF1F1917),
        surfaceContainer = Color(0xFF241D1A),
        surfaceContainerHigh = Color(0xFF2F2622),
    )
    SeedColor.EMERALD -> darkColorScheme(
        primary = Color(0xFF6CDBAC),
        onPrimary = Color(0xFF003826),
        primaryContainer = Color(0xFF005138),
        onPrimaryContainer = Color(0xFF89F8C7),
        secondary = Color(0xFFB3CCC0),
        secondaryContainer = Color(0xFF354B40),
        onSecondaryContainer = Color(0xFFCFE9D9),
        background = Color(0xFF0F1512),
        surface = Color(0xFF0F1512),
        surfaceContainerLow = Color(0xFF161D1A),
        surfaceContainer = Color(0xFF1A221E),
        surfaceContainerHigh = Color(0xFF242D28),
    )
    SeedColor.VIOLET -> darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        background = Color(0xFF141218),
        surface = Color(0xFF141218),
        surfaceContainerLow = Color(0xFF1C1B20),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
    )
}

fun SeedColor.swatchColor(): Color = when (this) {
    SeedColor.OCEAN -> Color(0xFF006399)
    SeedColor.TERRACOTTA -> Color(0xFF9C4221)
    SeedColor.EMERALD -> Color(0xFF006C4C)
    SeedColor.VIOLET -> Color(0xFF6750A4)
}

@Composable
fun OndaTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        settings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> seedDarkScheme(settings.seedColor)
        else -> seedLightScheme(settings.seedColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !dark
            insets.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
