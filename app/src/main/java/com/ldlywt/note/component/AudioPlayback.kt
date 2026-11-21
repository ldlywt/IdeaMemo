package com.ldlywt.note.component

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ldlywt.note.R
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.utils.formatDuration
import com.ldlywt.note.utils.getAudioDurationMs
import com.moriafly.salt.ui.SaltTheme
import java.io.File

/**
 * 全局音频播放控制，保证同一时间只播放一段录音。
 */
object AudioPlayback {

    var playingPath by mutableStateOf<String?>(null)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            val current = player ?: return
            positionMs = try {
                current.currentPosition.toLong()
            } catch (_: Exception) {
                0L
            }
            handler.postDelayed(this, 200L)
        }
    }

    fun toggle(path: String) {
        if (playingPath == path) {
            stop()
        } else {
            play(path)
        }
    }

    fun play(path: String) {
        stop()
        if (path.isBlank() || !File(path).exists()) return
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            mediaPlayer.setOnCompletionListener { stop() }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            mediaPlayer.start()
            player = mediaPlayer
            playingPath = path
            positionMs = 0L
            handler.post(ticker)
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun stop() {
        handler.removeCallbacks(ticker)
        try {
            player?.stop()
        } catch (_: Exception) {
            // ignore
        }
        try {
            player?.release()
        } catch (_: Exception) {
            // ignore
        }
        player = null
        playingPath = null
        positionMs = 0L
    }

    fun stopIf(path: String) {
        if (playingPath == path) stop()
    }
}

/**
 * 录音附件播放条，点击即可播放/暂停。
 */
@Composable
fun AudioPlayerCard(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val duration = remember(attachment.path) { getAudioDurationMs(attachment.path) }
    val isCurrent = AudioPlayback.playingPath == attachment.path
    val position = if (isCurrent) AudioPlayback.positionMs else 0L
    val progress = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SaltTheme.colors.subBackground)
            .clickable { AudioPlayback.toggle(attachment.path) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SaltTheme.colors.highlight.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrent) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isCurrent) R.string.pause_audio else R.string.play_audio
                ),
                tint = SaltTheme.colors.highlight,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.voice_note),
                    color = SaltTheme.colors.text,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isCurrent) {
                        "${formatDuration(position)} / ${formatDuration(duration)}"
                    } else {
                        formatDuration(duration)
                    },
                    color = SaltTheme.colors.subText,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = SaltTheme.colors.highlight,
                trackColor = SaltTheme.colors.subText.copy(alpha = 0.18f)
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(4.dp))
            trailing()
        }
    }
}
