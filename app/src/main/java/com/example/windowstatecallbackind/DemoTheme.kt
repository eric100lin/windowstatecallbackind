package com.example.windowstatecallbackind

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

@Composable
fun DemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            darkColorScheme(
                primary = colorResource(R.color.colorPrimaryDark),
                secondary = colorResource(R.color.colorAccent),
            )
        } else {
            lightColorScheme(
                primary = colorResource(R.color.colorPrimary),
                secondary = colorResource(R.color.colorAccent),
            )
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
