package com.pulse.app.ui.screens.levelselect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pulse.app.data.LevelDto
import kotlinx.coroutines.launch

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val allCategories = listOf("all") + categories
    LazyRow(
        contentPadding = PaddingValues(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(allCategories) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
@Composable
fun LevelSelectScreen(
    modifier: Modifier = Modifier,
    onLevelClick: (LevelDto) -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val viewModel: LevelSelectViewModel = viewModel()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            viewModel.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadData() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                val scrollState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PULSE",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 4.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Select a level",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            viewModel.currentUser?.let { user ->
                                FilledIconButton(
                                    onClick = onProfileClick,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        text = user.displayName.first().uppercaseChar().toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    // Category tabs
                    val allCategories = remember(viewModel.levels) {
                        viewModel.levels
                            .flatMap { it.categories }
                            .distinct()
                            .sorted()
                    }

                    CategoryTabs(
                        categories = allCategories,
                        selectedCategory = viewModel.selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxSize()) {

                        LazyRow(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 56.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(viewModel.filteredLevels) { level ->
                                val isExpanded = viewModel.expandedLevelId == level.id
                                LevelCard(
                                    level = level,
                                    isExpanded = isExpanded,
                                    cardIndex = viewModel.filteredLevels.indexOf(level),
                                    bestAttempt = viewModel.bestAttempts[level.id],
                                    onClick = { viewModel.toggleExpanded(level.id) },
                                    onStartClick = { onLevelClick(level) }
                                )
                            }
                        }

                        val showLeftArrow by remember {
                            derivedStateOf {
                                scrollState.firstVisibleItemIndex > 0 ||
                                        scrollState.firstVisibleItemScrollOffset > 0
                            }
                        }

                        if (showLeftArrow) {
                            FilledIconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val current = scrollState.firstVisibleItemIndex
                                        scrollState.animateScrollToItem(
                                            (current - 1).coerceAtLeast(0)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                )
                            ) {
                                Text(
                                    text = "‹",
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val showRightArrow by remember {
                            derivedStateOf {
                                val layoutInfo = scrollState.layoutInfo
                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                                lastVisible != null &&
                                        lastVisible.index < layoutInfo.totalItemsCount - 1
                            }
                        }

                        if (showRightArrow) {
                            FilledIconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val current = scrollState.firstVisibleItemIndex
                                        scrollState.animateScrollToItem(current + 1)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                )
                            ) {
                                Text(
                                    text = "›",
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}