package com.payquick.app.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payquick.R

@Immutable
data class TransactionListItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val isCredit: Boolean
)

@Composable
fun TransactionGroupHeader(
    monthLabel: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = monthLabel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(vertical = 8.dp)
    )
}

@Composable
fun TransactionListCard(
    item: TransactionListItemUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (iconBackground, iconTint) = if (item.isCredit) {
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    val iconRes = if (item.isCredit) {
        R.drawable.arrow_cool_down_24px
    } else {
        R.drawable.arrow_warm_up_24px
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = if (item.isCredit) "Received" else "Sent",
                        tint = iconTint
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.amountLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.isCredit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionGroupHeaderSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(vertical = 4.dp)
            .shimmerPlaceholder(shape = RoundedCornerShape(4.dp))
    )
}

@Composable
private fun TransactionCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shimmerPlaceholder(shape = CircleShape)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .shimmerPlaceholder(shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .shimmerPlaceholder(
                                shape = RoundedCornerShape(4.dp),
                                baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(16.dp)
                    .shimmerPlaceholder(shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

fun LazyListScope.transactionListSkeleton(
    groupCount: Int = 2,
    itemsPerGroup: Int = 3
) {
    repeat(groupCount) { groupIndex ->
        item(key = "skeleton-header-$groupIndex") {
            TransactionGroupHeaderSkeleton()
        }
        items(itemsPerGroup, key = { itemIndex -> "skeleton-card-$groupIndex-$itemIndex" }) {
            TransactionCardSkeleton()
        }
    }
}

@Composable
private fun Modifier.shimmerPlaceholder(
    shape: Shape,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    shimmerWidth: Float = 600f,
    durationMillis: Int = 1200
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing)
        ),
        label = "shimmer-progress"
    )

    val startX = -shimmerWidth + shimmerWidth * progress
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(startX, 0f),
        end = Offset(startX + shimmerWidth, 0f)
    )

    return this
        .clip(shape)
        .background(brush)
}
