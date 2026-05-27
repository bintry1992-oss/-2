package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = MidnightBlack,
    surface = DarkSurface,
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
  )

private val AmberColorScheme =
  darkColorScheme(
    primary = AmberPrimary,
    secondary = AmberSecondary,
    tertiary = AmberTertiary,
    background = MidnightBlack,
    surface = DarkSurface,
    onBackground = Color(0xFFFFE082),
    onSurface = Color(0xFFFFCC80)
  )

private val EmeraldColorScheme =
  darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = EmeraldTertiary,
    background = MidnightBlack,
    surface = DarkSurface,
    onBackground = Color(0xFFC8E6C9),
    onSurface = Color(0xFFA5D6A7)
  )

private val WarmSandColorScheme =
  lightColorScheme(
    primary = Color(0xFF8D6E63),
    secondary = Color(0xFFA1887F),
    tertiary = Color(0xFFBCAAA4),
    background = WarmSandLight,
    surface = WarmSandSurface,
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF4E342E)
  )

@Composable
fun MyApplicationTheme(
  themeMode: Int = 1, // 0: Default Dark, 1: Warm Amber (Anti-Blue), 2: Forest Emerald, 3: Warm Sand Light
  content: @Composable () -> Unit,
) {
  val colorScheme = when (themeMode) {
    0 -> DarkColorScheme
    1 -> AmberColorScheme
    2 -> EmeraldColorScheme
    3 -> WarmSandColorScheme
    else -> AmberColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
