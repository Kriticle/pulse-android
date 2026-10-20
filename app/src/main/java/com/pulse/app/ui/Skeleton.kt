package com.pulse.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant
    )
    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 200f, 0f),
            end = Offset(translateAnim, 0f)
        )
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmer()
    )
}

@Composable
fun LevelSelectSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(width = 100.dp, height = 28.dp)
            SkeletonBox(width = 40.dp, height = 40.dp, cornerRadius = 20.dp)
        }

        // Category tabs skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                SkeletonBox(width = 80.dp, height = 32.dp, cornerRadius = 16.dp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Level cards skeleton
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .width(220.dp)
                        .height(220.dp),
                    cornerRadius = 16.dp
                )
            }
        }
    }
}

@Composable
fun ProfileSkeleton() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel skeleton
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(40.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                SkeletonBox(
                    width = 80.dp,
                    height = 80.dp,
                    cornerRadius = 40.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonBox(width = 140.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(width = 180.dp, height = 14.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(height = 44.dp)
                SkeletonBox(height = 44.dp)
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline)
        )

        // Right panel skeleton
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            verticalArrangement = Arrangement.Center
        ) {
            SkeletonBox(width = 60.dp, height = 12.dp)
            Spacer(modifier = Modifier.height(24.dp))
            SkeletonBox(height = 60.dp)
            Spacer(modifier = Modifier.height(16.dp))
            SkeletonBox(height = 60.dp)
        }
    }
}

@Composable
fun ResultsSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 80.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    SkeletonBox(width = 160.dp, height = 14.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    SkeletonBox(width = 160.dp, height = 96.dp, cornerRadius = 12.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        SkeletonBox(width = 80.dp, height = 20.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(width = 80.dp, height = 12.dp)
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        SkeletonBox(width = 80.dp, height = 20.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBox(width = 80.dp, height = 12.dp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBox(modifier = Modifier.weight(1f), height = 44.dp)
                    SkeletonBox(modifier = Modifier.weight(1f), height = 44.dp)
                }
            }
        }
    }
}