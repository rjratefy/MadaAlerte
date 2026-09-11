package mg.etat.madaalerte.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = MalagasyRed,
    secondary = MalagasyGreen,
    tertiary = MalagasyRed,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MalagasyRed,
    secondary = MalagasyGreen,
    tertiary = MalagasyRed,
    background = Color(0xFFF2F2F2),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = MalagasyDark,
    onSurface = MalagasyDark
)

@Composable
fun MadaAlerteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // On désactive le dynamicColor par défaut (false) pour imposer l'identité visuelle malgache
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}