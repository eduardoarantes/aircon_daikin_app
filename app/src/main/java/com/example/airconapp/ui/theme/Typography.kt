package com.example.airconapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.airconapp.R

// Both fonts are variable fonts: each weight sets the weight axis explicitly, otherwise
// Android draws the default instance and fakes bold.
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: Int) = Font(
    resId,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Body and UI text. */
val Inter = FontFamily(
    variableFont(R.font.inter, 400),
    variableFont(R.font.inter, 500),
    variableFont(R.font.inter, 600),
    variableFont(R.font.inter, 700)
)

/** Temperature numbers. */
val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, 400),
    variableFont(R.font.space_grotesk, 500),
    variableFont(R.font.space_grotesk, 600)
)

private fun TextStyle.inter() = copy(fontFamily = Inter)

/** Material's type scale, set in Inter, so the existing screens match the new ones. */
val Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.inter(),
        displayMedium = displayMedium.inter(),
        displaySmall = displaySmall.inter(),
        headlineLarge = headlineLarge.inter(),
        headlineMedium = headlineMedium.inter(),
        headlineSmall = headlineSmall.inter(),
        titleLarge = titleLarge.inter(),
        titleMedium = titleMedium.inter(),
        titleSmall = titleSmall.inter(),
        bodyLarge = bodyLarge.inter(),
        bodyMedium = bodyMedium.inter(),
        bodySmall = bodySmall.inter(),
        labelLarge = labelLarge.inter(),
        labelMedium = labelMedium.inter(),
        labelSmall = labelSmall.inter()
    )
}
