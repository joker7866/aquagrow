package com.aquagrow.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Renders a preview in both light and dark theme. */
@Preview(name = "Light", showBackground = true, widthDp = 400)
@Preview(name = "Dark", showBackground = true, widthDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class ThemePreviews

/** Full-screen previews (phone size) in light and dark. */
@Preview(name = "Light", showBackground = true, device = "spec:width=411dp,height=891dp")
@Preview(
    name = "Dark",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ScreenPreviews
