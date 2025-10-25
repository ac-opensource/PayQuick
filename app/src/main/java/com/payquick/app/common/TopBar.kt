package com.payquick.app.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.payquick.R
import com.payquick.app.designsystem.PayQuickTheme

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    leftIcon: ImageVector? = null,
    onLeftIconClick: (() -> Unit)? = null,
    leftIconEnabled: Boolean = true,
    rightIcon: ImageVector? = null,
    onRightIconClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leftIcon != null && onLeftIconClick != null) {
            Icon(
                imageVector = leftIcon,
                contentDescription = stringResource(R.string.top_bar_back),
                modifier = Modifier
                    .clickable(
                        enabled = leftIconEnabled,
                        onClick = onLeftIconClick
                    )
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.weight(1f))

        if (rightIcon != null && onRightIconClick != null) {
            IconButton(onClick = onRightIconClick) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Top Bar")
@Composable
private fun TopBarPreview() {
    PayQuickTheme {
        TopBar(
            title = "Wallet",
            leftIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onLeftIconClick = {},
            rightIcon = Icons.Rounded.MoreVert,
            onRightIconClick = {}
        )
    }
}
