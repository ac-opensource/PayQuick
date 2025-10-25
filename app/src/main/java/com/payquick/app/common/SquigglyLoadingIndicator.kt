package com.payquick.app.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.payquick.app.designsystem.PayQuickTheme

@Composable
fun SquigglyLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
    )
}

@Preview(showBackground = true, name = "Loading Indicator")
@Composable
private fun SquigglyLoadingIndicatorPreview() {
    PayQuickTheme {
        SquigglyLoadingIndicator()
    }
}
