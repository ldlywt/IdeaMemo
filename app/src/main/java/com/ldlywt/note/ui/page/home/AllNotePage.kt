package com.ldlywt.note.ui.page.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.ldlywt.note.R
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.component.NoteCard
import com.ldlywt.note.component.RYScaffold
import com.ldlywt.note.state.NoteState
import com.ldlywt.note.ui.page.LocalMemosState
import com.ldlywt.note.ui.page.SortTime
import com.ldlywt.note.ui.page.input.ChatInputDialog
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.str
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AllNotePage(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    externalShowInputDialog: Boolean = false,
    onExternalShowInputDialogChange: (Boolean) -> Unit = {}
) {
    val noteState: NoteState = LocalMemosState.current
    var parentNoteForComment by rememberSaveable { mutableStateOf<NoteShowBean?>(null) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isGalleryMode by rememberSaveable { mutableStateOf(false) }

    val sortTime by SharedPreferencesUtils.sortTime.collectAsState(SortTime.UPDATE_TIME_DESC)
    var lastScrolledSortTime by rememberSaveable { mutableStateOf<SortTime?>(null) }

    LaunchedEffect(sortTime, noteState.notes) {
        if (lastScrolledSortTime != sortTime) {
            scrollToTop(coroutineScope, listState)
            if (noteState.notes.isNotEmpty()) {
                lastScrolledSortTime = sortTime
            }
        }
    }

    // 状态管理：控制背景遮罩
    var isInputActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        RYScaffold(
            navController = null,
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Menu",
                        tint = SaltTheme.colors.text
                    )
                }
            },
            titleContent = {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeTabTitle(
                        selected = !isGalleryMode,
                        text = stringResource(R.string.home_title_one),
                        onClick = { isGalleryMode = false }
                    )
                    HomeTabTitle(
                        selected = isGalleryMode,
                        text = stringResource(R.string.home_title_two),
                        onClick = { isGalleryMode = true }
                    )
                }
            },
            actions = {
                Toolbar(navController, dateRangeBlock = {
                    showDateRangePicker = true
                }, onSortChanged = {
                    scrollToTop(coroutineScope, listState)
                })
            },
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isGalleryMode) {
                        val galleryNotes = remember(noteState.notes) {
                            noteState.notes.filter { it.note.attachments.isNotEmpty() }
                        }
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            items(galleryNotes, key = { it.note.noteId }) { noteBean ->
                                GalleryItem(noteBean) {
                                    navController.navigate(Screen.MemoPreview(noteBean.note.noteId))
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = noteState.notes.size,
                                key = { index -> noteState.notes[index].note.noteId }
                            ) { index ->
                                NoteCard(
                                    noteShowBean = noteState.notes[index],
                                    navHostController = navController,
                                    onCommentClick = {
                                        parentNoteForComment = it
                                        onExternalShowInputDialogChange(true)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }

                    // 遮罩层：仅在输入框活跃时显示
                    AnimatedVisibility(
                        visible = isInputActive,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
//                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onExternalShowInputDialogChange(false)
                                    parentNoteForComment = null
                                }
                        )
                    }
                }
            }
        )

        // 真实输入框：直接放在 Box 底部，不使用 Scaffold 的 bottomBar 以获得更好的 IME 同步
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ChatInputDialog(
                isShow = externalShowInputDialog,
                parentNote = parentNoteForComment,
                onExpandedChange = { isInputActive = it },
                dismiss = {
                    onExternalShowInputDialogChange(false)
                    parentNoteForComment = null
                }
            )
        }
    }

    if (showDateRangePicker) {
        ModernDateRangePicker(
            onDismissRequest = { showDateRangePicker = false },
            onConfirm = { startTime, endTime ->
                navController.navigate(
                    Screen.DateRangePage(
                        startTime = startTime,
                        endTime = endTime
                    )
                )
                showDateRangePicker = false
            }
        )
    }
}

@Composable
fun HomeTabTitle(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (selected) SaltTheme.colors.text else SaltTheme.colors.subText,
        animationSpec = tween(durationMillis = 300), label = "colorAnim"
    )
    val fontSize by animateFloatAsState(
        targetValue = if (selected) 24f else 18f,
        animationSpec = tween(durationMillis = 300), label = "fontSizeAnim"
    )

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = text,
            style = SaltTheme.textStyles.main.copy(
                fontSize = fontSize.sp,
                color = color,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

@Composable
fun GalleryItem(noteBean: NoteShowBean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SaltTheme.colors.subBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column {
            val firstImage = noteBean.note.attachments.firstOrNull()
            if (firstImage != null) {
                AsyncImage(
                    model = File(firstImage.path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            }
            Text(
                text = noteBean.note.content,
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

private fun scrollToTop(
    coroutineScope: CoroutineScope,
    listState: LazyListState
) {
    coroutineScope.launch {
        delay(200)
        listState.scrollToItem(0)
    }
}

@Composable
private fun Toolbar(
    navController: NavHostController,
    dateRangeBlock: () -> Unit,
    onSortChanged: () -> Unit
) {
    IconButton(
        onClick = {
            dateRangeBlock()
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.AvTimer,
            contentDescription = R.string.date_range.str,
            tint = SaltTheme.colors.text
        )
    }
    IconButton(
        onClick = {
            navController.navigate(route = Screen.Search) {
                launchSingleTop = true
            }
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = R.string.search_hint.str,
            tint = SaltTheme.colors.text
        )
    }

    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "more",
                tint = SaltTheme.colors.text
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SaltTheme.colors.popup)
        ) {
            SortFilterMenuContent(onSortChanged = {
                expanded = false
                onSortChanged()
            })
        }
    }
}

@Composable
fun SortFilterMenuContent(onSortChanged: () -> Unit = {}) {
    val sortTime by SharedPreferencesUtils.sortTime.collectAsState(SortTime.UPDATE_TIME_DESC)
    val scope = rememberCoroutineScope()

    SortMenuItem(
        text = stringResource(R.string.update_time_desc),
        selected = sortTime == SortTime.UPDATE_TIME_DESC,
        onClick = {
            scope.launch {
                SharedPreferencesUtils.updateSortTime(SortTime.UPDATE_TIME_DESC)
                onSortChanged()
            }
        }
    )
    SortMenuItem(
        text = stringResource(R.string.update_time_asc),
        selected = sortTime == SortTime.UPDATE_TIME_ASC,
        onClick = {
            scope.launch {
                SharedPreferencesUtils.updateSortTime(SortTime.UPDATE_TIME_ASC)
                onSortChanged()
            }
        }
    )
    SortMenuItem(
        text = stringResource(R.string.create_time_desc),
        selected = sortTime == SortTime.CREATE_TIME_DESC,
        onClick = {
            scope.launch {
                SharedPreferencesUtils.updateSortTime(SortTime.CREATE_TIME_DESC)
                onSortChanged()
            }
        }
    )
    SortMenuItem(
        text = stringResource(R.string.create_time_asc),
        selected = sortTime == SortTime.CREATE_TIME_ASC,
        onClick = {
            scope.launch {
                SharedPreferencesUtils.updateSortTime(SortTime.CREATE_TIME_ASC)
                onSortChanged()
            }
        }
    )
}

@Composable
fun SortMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    color = if (selected) SaltTheme.colors.highlight else SaltTheme.colors.text,
                    fontSize = 14.sp
                )
                if (selected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = SaltTheme.colors.highlight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDateRangePicker(
    onDismissRequest: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var startYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var startMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var startDay by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }

    var endYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var endMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var endDay by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }

    var selectingStartDate by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SaltTheme.colors.background,
        dragHandle = { DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.cancel), color = SaltTheme.colors.subText)
                }
                Text(
                    text = stringResource(R.string.date_range),
                    style = SaltTheme.textStyles.main.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = {
                    val start = LocalDate.of(startYear, startMonth, startDay)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val end = LocalDate.of(endYear, endMonth, endDay)
                        .atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(start, end)
                }) {
                    Text(text = stringResource(R.string.confirm), color = SaltTheme.colors.highlight)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SaltTheme.colors.subBackground)
                    .padding(4.dp)
            ) {
                val tabModifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectingStartDate = !selectingStartDate }
                    .padding(vertical = 8.dp)

                Box(
                    modifier = tabModifier.then(
                        if (selectingStartDate) Modifier.background(SaltTheme.colors.background) else Modifier
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$startYear-$startMonth-$startDay",
                        style = SaltTheme.textStyles.main.copy(
                            fontSize = 14.sp,
                            fontWeight = if (selectingStartDate) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }

                Box(
                    modifier = tabModifier.then(
                        if (!selectingStartDate) Modifier.background(SaltTheme.colors.background) else Modifier
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$endYear-$endMonth-$endDay",
                        style = SaltTheme.textStyles.main.copy(
                            fontSize = 14.sp,
                            fontWeight = if (!selectingStartDate) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectingStartDate) {
                WheelDatePicker(
                    year = startYear,
                    month = startMonth,
                    day = startDay,
                    onYearChange = { startYear = it },
                    onMonthChange = { startMonth = it },
                    onDayChange = { startDay = it }
                )
            } else {
                WheelDatePicker(
                    year = endYear,
                    month = endMonth,
                    day = endDay,
                    onYearChange = { endYear = it },
                    onMonthChange = { endMonth = it },
                    onDayChange = { endDay = it }
                )
            }
        }
    }
}

@Composable
fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(SaltTheme.colors.subText.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun WheelDatePicker(
    year: Int,
    month: Int,
    day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    val years = remember { (currentYear - 20..currentYear + 5).toList() }
    val months = remember { (1..12).toList() }
    val days = remember(year, month) {
        val daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth()
        (1..daysInMonth).toList()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f)) {
            WheelPicker<Int>(items = years, initialItem = year, onItemSelected = onYearChange)
        }
        Box(modifier = Modifier.weight(1f)) {
            WheelPicker<Int>(items = months, initialItem = month, onItemSelected = onMonthChange)
        }
        Box(modifier = Modifier.weight(1f)) {
            WheelPicker<Int>(items = days, initialItem = day, onItemSelected = onDayChange)
        }
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialItem: T,
    onItemSelected: (T) -> Unit
) {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(initialItem).coerceAtLeast(0)
    )

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val centerIndex = lazyListState.firstVisibleItemIndex
            if (centerIndex < items.size) {
                onItemSelected(items[centerIndex])
                lazyListState.animateScrollToItem(centerIndex)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(SaltTheme.colors.highlight.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == index } }
                Text(
                    text = item.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = SaltTheme.textStyles.main.copy(
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SaltTheme.colors.text else SaltTheme.colors.subText
                    )
                )
            }
        }
    }
}
