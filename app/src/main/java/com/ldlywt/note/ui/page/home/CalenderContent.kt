package com.ldlywt.note.ui.page.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.settings.Level
import com.moriafly.salt.ui.SaltTheme
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
            .testTag("MonthHeader"),
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = SaltTheme.colors.subText,
                text = dayOfWeek.displayText(),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun Day(
    day: CalendarDay,
    today: LocalDate,
    level: Level?,
    isSelected: Boolean,
    onClick: (CalendarDay) -> Unit,
) {
    val noteViewModel = LocalMemosViewModel.current
    val firstImagePath = noteViewModel.dateFirstImageMap[day.date]
    val isToday = day.date == today
    val isCurrentMonth = day.position == DayPosition.MonthDate

    val shape = RoundedCornerShape(12.dp)
    val containerColor = when {
        isSelected -> SaltTheme.colors.highlight
        isToday -> SaltTheme.colors.highlight.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .testTag("MonthDay")
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .clip(shape)
            .background(color = containerColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = SaltTheme.colors.highlight.copy(alpha = 0.35f),
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(showRipple = !isSelected, onClick = { onClick(day) }),
        contentAlignment = Alignment.Center,
    ) {
        if (firstImagePath != null) {
            AsyncImage(
                model = File(firstImagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = if (isSelected) 0.6f else 0.92f),
            )
            // 底部渐变遮罩，保证有图片时日期依然清晰可读
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                            )
                        )
                    )
            )
        }

        val textColor = when {
            isSelected -> Color.White
            !isCurrentMonth -> SaltTheme.colors.subText.copy(alpha = 0.45f)
            firstImagePath != null -> Color.White
            isToday -> SaltTheme.colors.highlight
            else -> SaltTheme.colors.text
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            style = if (firstImagePath != null) {
                MaterialTheme.typography.bodyMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        blurRadius = 6f,
                    )
                )
            } else {
                MaterialTheme.typography.bodyMedium
            },
        )

        if (level != null && level != Level.Zero) {
            val dotColor = when {
                isSelected || firstImagePath != null -> Color.White
                else -> SaltTheme.colors.highlight
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = level.dotAlpha())),
            )
        }
    }
}

private fun Level.dotAlpha(): Float = when (this) {
    Level.Zero -> 0.4f
    Level.One -> 0.55f
    Level.Two -> 0.7f
    Level.Three -> 0.85f
    Level.Four -> 1f
}

fun DayOfWeek.displayText(uppercase: Boolean = false): String {
    return getDisplayName(TextStyle.SHORT, Locale.getDefault()).let { value ->
        if (uppercase) value.uppercase(Locale.getDefault()) else value
    }
}

fun YearMonth.displayText(short: Boolean = false): String {
    return "${this.month.displayText(short = short)} ${this.year}"
}

fun Month.displayText(short: Boolean = true): String {
    val style = if (short) TextStyle.SHORT else TextStyle.FULL
    return getDisplayName(style, Locale.getDefault())
}

/** 日历卡片顶部使用的月份标题，例如「2026年11月」/「November 2026」。 */
fun YearMonth.calendarMonthText(): String {
    val locale = Locale.getDefault()
    return if (locale.language == "zh") {
        "${year}年${monthValue}月"
    } else {
        "${month.getDisplayName(TextStyle.FULL, locale)} $year"
    }
}

fun Modifier.clickable(
    enabled: Boolean = true,
    showRipple: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = if (showRipple) LocalIndication.current else null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
}
