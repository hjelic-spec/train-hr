package hr.trainhr.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF1F5E8C),
    secondary = Color(0xFF3D6B4F),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CCBF2),
    secondary = Color(0xFFA6D3B3),
)

/** Nešto veća slova od zadanih – čitljivost u teretani. */
private val BigTypography = Typography().let { t ->
    t.copy(
        bodyLarge = t.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelLarge = t.labelLarge.copy(fontSize = 16.sp),
        titleMedium = t.titleMedium.copy(fontSize = 19.sp),
    )
}

@Composable
fun TrainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = BigTypography,
        content = content,
    )
}
