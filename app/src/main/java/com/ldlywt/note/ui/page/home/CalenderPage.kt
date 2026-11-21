package com.ldlywt.note.ui.page.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.ldlywt.note.R
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.component.CardCalender
import com.ldlywt.note.component.EmptyComponent
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalenderPage(navController: NavHostController, onOpenDrawer: () -> Unit) {

    val noteViewModel = LocalMemosViewModel.current
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    val daysOfWeek = remember { daysOfWeek() }
    val today = remember { LocalDate.now() }
    var currentLocalDate by remember { mutableStateOf(LocalDate.now()) }
    val filterList: SnapshotStateList<NoteShowBean> = remember { mutableStateListOf<NoteShowBean>() }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val calendarState: CalendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid,
    )

    LaunchedEffect(currentLocalDate) {
        isLoading = true
        filterList.clear()
        val notes = withContext(Dispatchers.IO) {
            noteViewModel.getNotesOnSelectedDate(currentLocalDate)
        }
        filterList.addAll(notes)
        isLoading = false
    }

    // 日历是列表的第一项，当它被完全滑出屏幕后，紧凑的日期栏会吸附在顶部。
    val isCalendarCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    fun backToToday() {
        currentLocalDate = LocalDate.now()
        scope.launch {
            calendarState.animateScrollToMonth(YearMonth.now())
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
            .statusBarsPadding()
    ) {
        CalendarTopBar(
            date = currentLocalDate,
            onOpenDrawer = onOpenDrawer,
            onToday = ::backToToday,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "calendar") {
                CalendarCard(
                    calendarState = calendarState,
                    daysOfWeek = daysOfWeek,
                    today = today,
                    selectedDate = currentLocalDate,
                    onSelectDate = { day -> currentLocalDate = day.date },
                )
            }

            stickyHeader(key = "selected-date") {
                SelectedDateHeader(
                    date = currentLocalDate,
                    noteCount = filterList.size,
                    isLoading = isLoading,
                    collapsed = isCalendarCollapsed,
                    onExpandCalendar = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
            }

            if (filterList.isEmpty()) {
                item(key = "empty") {
                    if (!isLoading) {
                        EmptyComponent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            text = stringResource(R.string.calendar_empty_day),
                        )
                    }
                }
            } else {
                items(
                    items = filterList,
                    key = { bean -> bean.note.noteId },
                    contentType = { "calendar_note" },
                ) { bean ->
                    CardCalender(
                        noteShowBean = bean,
                        navHostController = navController,
                        onNoteDeleted = { deleted ->
                            // 已进入回收站，直接从当前日期列表中移除
                            filterList.removeAll { it.note.noteId == deleted.note.noteId }
                        },
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(if (filterList.isEmpty()) 24.dp else 120.dp))
            }
        }
    }
}

@Composable
private fun CalendarTopBar(
    date: LocalDate,
    onOpenDrawer: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = SaltTheme.colors.text,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.calendar),
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = date.fullDateText(),
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 12.sp,
                    color = SaltTheme.colors.subText,
                ),
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SaltTheme.colors.subBackground)
                .clickable(onClick = onToday)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = SaltTheme.colors.highlight,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.calendar_today),
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SaltTheme.colors.highlight,
                ),
            )
        }
    }
}

@Composable
private fun CalendarCard(
    calendarState: CalendarState,
    daysOfWeek: List<java.time.DayOfWeek>,
    today: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noteViewModel = LocalMemosViewModel.current
    val scope = rememberCoroutineScope()
    val visibleMonth = calendarState.firstVisibleMonth.yearMonth

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SaltTheme.colors.subBackground)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visibleMonth.calendarMonthText(),
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    scope.launch { calendarState.animateScrollToMonth(visibleMonth.minusMonths(1)) }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = SaltTheme.colors.subText,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = {
                    scope.launch { calendarState.animateScrollToMonth(visibleMonth.plusMonths(1)) }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next month",
                    tint = SaltTheme.colors.subText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        HorizontalCalendar(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("Calendar"),
            state = calendarState,
            dayContent = { day: CalendarDay ->
                Day(
                    day = day,
                    today = today,
                    level = noteViewModel.levelMemosMap[day.date],
                    isSelected = selectedDate == day.date,
                    onClick = onSelectDate,
                )
            },
            monthHeader = {
                MonthHeader(daysOfWeek = daysOfWeek)
            },
        )
    }
}

@Composable
private fun SelectedDateHeader(
    date: LocalDate,
    noteCount: Int,
    isLoading: Boolean,
    collapsed: Boolean,
    onExpandCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SaltTheme.colors.background)
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.dateHeaderText(),
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = when {
                    isLoading -> "…"
                    noteCount == 0 -> stringResource(R.string.calendar_empty_day)
                    else -> stringResource(R.string.calendar_note_count, noteCount)
                },
                style = SaltTheme.textStyles.main.copy(
                    fontSize = 12.sp,
                    color = SaltTheme.colors.subText,
                ),
            )
        }

        AnimatedVisibility(
            visible = collapsed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            IconButton(
                onClick = onExpandCalendar,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.calendar),
                    tint = SaltTheme.colors.subText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun LocalDate.fullDateText(): String {
    val locale = Locale.getDefault()
    val dateText = if (locale.language == "zh") {
        "${year}年${monthValue}月${dayOfMonth}日"
    } else {
        "${month.getDisplayName(TextStyle.SHORT, locale)} $dayOfMonth, $year"
    }
    return "$dateText · ${dayOfWeek.getDisplayName(TextStyle.FULL, locale)}"
}

private fun LocalDate.dateHeaderText(): String {
    val locale = Locale.getDefault()
    val dateText = if (locale.language == "zh") {
        "${monthValue}月${dayOfMonth}日"
    } else {
        "${month.getDisplayName(TextStyle.SHORT, locale)} $dayOfMonth"
    }
    return "$dateText · ${dayOfWeek.getDisplayName(TextStyle.SHORT, locale)}"
}
