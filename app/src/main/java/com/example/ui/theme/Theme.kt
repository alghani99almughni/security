package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = Blue500,
  onPrimary = Color.White,
  primaryContainer = Navy800,
  onPrimaryContainer = Blue100,
  secondary = Emerald500,
  onSecondary = Color.White,
  secondaryContainer = Slate800,
  onSecondaryContainer = Emerald100,
  background = Slate900,
  onBackground = Color.White,
  surface = Slate800,
  onSurface = Color.White,
  surfaceVariant = Slate700,
  onSurfaceVariant = Slate200,
  error = Rose500,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Blue600,
  onPrimary = Color.White,
  primaryContainer = Blue100,
  onPrimaryContainer = Navy900,
  secondary = Emerald600,
  onSecondary = Color.White,
  secondaryContainer = Emerald100,
  onSecondaryContainer = Slate900,
  background = Slate50,
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate700,
  error = Rose600,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
