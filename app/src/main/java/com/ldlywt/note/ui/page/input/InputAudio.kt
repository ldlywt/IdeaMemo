package com.ldlywt.note.ui.page.input

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ldlywt.note.R
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.component.AudioPlayback
import com.ldlywt.note.component.AudioPlayerCard
import com.moriafly.salt.ui.SaltTheme

/**
 * 输入页里的录音附件条目，可播放/暂停，也可以删除。
 */
@Composable
fun InputAudio(
    attachment: Attachment,
    isEdit: Boolean = true,
    delete: (path: String) -> Unit,
) {
    AudioPlayerCard(
        attachment = attachment,
        modifier = Modifier
            .width(230.dp)
            .height(56.dp),
        trailing = if (isEdit) {
            {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        AudioPlayback.stopIf(attachment.path)
                        delete(attachment.path)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = SaltTheme.colors.subText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            null
        }
    )
}
