package com.ldlywt.note.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.utils.SettingsPreferences
import com.ldlywt.note.utils.toMinute
import com.moriafly.salt.ui.SaltTheme
import dev.jeziellago.compose.markdowntext.MarkdownText


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardCalender(
    noteShowBean: NoteShowBean,
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
    onNoteDeleted: (NoteShowBean) -> Unit = {},
) {

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val note = noteShowBean.note
    val autoLinkEnabled by SettingsPreferences.autoLinkEnabled.collectAsState(true)

    Card(
        colors = CardDefaults.cardColors(containerColor = SaltTheme.colors.subBackground),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    navHostController.navigate(route = Screen.MemoPreview(noteShowBean.note.noteId))
                },
                onLongClick = {
                    openBottomSheet = true
                },
            ),
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = note.createTime.toMinute(),
                style = SaltTheme.textStyles.paragraph.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (!note.noteTitle.isNullOrEmpty()) {
                Text(
                    text = note.noteTitle ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            MarkdownText(
                markdown = note.content, 
                style = SaltTheme.textStyles.paragraph.copy(fontSize = 15.sp, lineHeight = 24.sp),
                maxLines = 5, 
                onTagClick = {
                    navHostController.navigate(Screen.TagDetail(it))
                },
                autoLink = autoLinkEnabled,
            )
            
            if (note.attachments.any { it.type == Attachment.Type.IMAGE }) {
                Spacer(modifier = Modifier.height(8.dp))
                CalenderImageCard(note, navHostController)
            }

            // 录音附件：与首页 NoteCard 保持一致，点击即可播放/暂停
            val audioAttachments = note.attachments.filter { it.type == Attachment.Type.AUDIO }
            if (audioAttachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    audioAttachments.forEach { attachment ->
                        AudioPlayerCard(
                            attachment = attachment,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // 日历页长按只保留「复制 / 分享 / 删除」，删除后数据进回收站并从当前列表移除
    ActionBottomSheet(
        navHostController = navHostController,
        noteShowBean = noteShowBean,
        show = openBottomSheet,
        onNoteDeleted = { onNoteDeleted(noteShowBean) },
        actions = CalendarSheetActions,
        onDismissRequest = {
            openBottomSheet = false
        }
    )
}

private val CalendarSheetActions = setOf(
    NoteSheetAction.Copy,
    NoteSheetAction.Share,
    NoteSheetAction.Delete,
)

@Composable
fun CalenderImageCard(note: Note, navHostController: NavHostController?) {
    val images = note.attachments.filter { it.type == Attachment.Type.IMAGE }
    if (images.isEmpty()) return

    if (images.size == 1) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    navHostController?.navigate(Screen.PictureDisplay(arrayListOf(images[0].path), 0, listOf(note.createTime)))
                }
        ) {
            AsyncImage(
                model = images[0].path,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        LazyRow(
            modifier = Modifier.height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(items = images, key = { _, attachment -> attachment.path }) { index, attachment ->
                AsyncImage(
                    model = attachment.path,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            navHostController?.navigate(Screen.PictureDisplay(images.map { it.path }, index, listOf(note.createTime)))
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
