package com.ldlywt.note.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ldlywt.note.R
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.utils.AudioRecorder
import com.ldlywt.note.utils.formatDuration
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * 录音状态控制器，封装权限申请、开始/取消/完成录音。
 */
class AudioRecorderState internal constructor(
    private val recordingState: MutableState<Boolean>,
    private val elapsedState: MutableLongState,
    val start: () -> Unit,
    val cancel: () -> Unit,
    val finish: () -> Unit,
) {
    val isRecording: Boolean
        get() = recordingState.value

    val elapsedMs: Long
        get() = elapsedState.value
}

@Composable
fun rememberAudioRecorderState(
    onRecorded: (Attachment) -> Unit,
    onError: () -> Unit = {},
    onPermissionDenied: () -> Unit = onError,
): AudioRecorderState {
    val context = LocalContext.current
    val recorder = remember(context) { AudioRecorder(context) }
    val recordingState = remember { mutableStateOf(false) }
    val elapsedState = remember { mutableLongStateOf(0L) }
    val currentOnRecorded by rememberUpdatedState(onRecorded)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)

    fun startRecording() {
        // 录音前先停止正在播放的音频，避免把外放声音录进去
        AudioPlayback.stop()
        if (recorder.start()) {
            recordingState.value = true
            elapsedState.longValue = 0L
        } else {
            currentOnError()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else currentOnPermissionDenied()
    }

    LaunchedEffect(recordingState.value) {
        while (recordingState.value) {
            elapsedState.longValue = recorder.elapsedMs
            delay(100L)
        }
    }

    DisposableEffect(recorder) {
        onDispose {
            recorder.cancel()
            recordingState.value = false
            elapsedState.longValue = 0L
        }
    }

    return remember(recorder) {
        AudioRecorderState(
            recordingState = recordingState,
            elapsedState = elapsedState,
            start = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    startRecording()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            cancel = {
                recorder.cancel()
                recordingState.value = false
                elapsedState.longValue = 0L
            },
            finish = {
                val attachment = recorder.stop()
                recordingState.value = false
                elapsedState.longValue = 0L
                if (attachment != null) {
                    currentOnRecorded(attachment)
                } else {
                    currentOnError()
                }
            }
        )
    }
}

/**
 * flomo 风格的录音条：红点 + 计时 + 动态波形 + 取消/完成。
 */
@Composable
fun RecordingBar(
    elapsedMs: Long,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "recording_dot")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = dotAlpha))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDuration(elapsedMs),
            color = SaltTheme.colors.text,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(12.dp))
        RecordingWaveform(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.record_cancel),
                tint = SaltTheme.colors.subText
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SaltTheme.colors.highlight)
                .clickable(onClick = onFinish),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.record_finish),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RecordingWaveform(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "recording_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val color = SaltTheme.colors.highlight

    Canvas(modifier = modifier) {
        val barCount = 22
        val gap = size.width / (barCount * 2f)
        val barWidth = gap
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.92f
        repeat(barCount) { index ->
            val wave = (sin(phase + index * 0.55f) + 1f) / 2f
            val barHeight = (maxBarHeight * (0.18f + 0.82f * wave)).coerceAtMost(size.height)
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = color.copy(alpha = 0.55f + 0.45f * wave),
                topLeft = Offset(left, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
