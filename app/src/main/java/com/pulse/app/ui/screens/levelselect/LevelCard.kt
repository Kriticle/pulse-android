package com.pulse.app.ui.screens.levelselect

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.app.data.BestAttemptDto
import com.pulse.app.data.LevelDto

@Composable
fun LevelCard(
    level: LevelDto,
    isExpanded: Boolean,
    cardIndex: Int,
    bestAttempt: BestAttemptDto?,
    onClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val cardWidth by animateDpAsState(
        targetValue = if (isExpanded) 440.dp else 220.dp,
        label = "cardWidth"
    )

    val accentColor = when (cardIndex % 3) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth)
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
            ) {
                LevelGraphic(
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    DifficultyBadge(level.difficulty, accentColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = level.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${level.segments.count { it.type == "active" }} exercises · ${
                            level.segments.filter { it.type == "active" }.sumOf { it.loopCount }
                        } total sets",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bestAttempt != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Best: ${bestAttempt.score} pts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                    }
                    // Category chips
                    if (level.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            level.categories.take(2).forEach { category ->
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = category.replaceFirstChar { it.uppercase() },
                                        fontSize = 9.sp,
                                        color = accentColor,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SEQUENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        level.segments.forEach { segment ->
                            val isRest = segment.type == "rest"
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (segment.loopCount > 1)
                                        "${segment.exercise.name} ×${segment.loopCount}"
                                    else
                                        segment.exercise.name,
                                    fontSize = 12.sp,
                                    color = if (isRest)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isRest) FontWeight.Normal else FontWeight.Medium
                                )
                                Text(
                                    text = "${segment.durationSeconds}s",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        )
                    ) {
                        Text(
                            text = "Start",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
