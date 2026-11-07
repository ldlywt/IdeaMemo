package com.ldlywt.note.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.utils.toTime
import com.moriafly.salt.ui.SaltTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

enum class NoteCardFrom {
    SEARCH, TAG_DETAIL, COMMON,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    noteShowBean: NoteShowBean,
    navHostController: NavHostController,
    from: NoteCardFrom = NoteCardFrom.COMMON,
    onCommentClick: ((NoteShowBean) -> Unit)? = null,
    isCanNavigate: Boolean = true,
    highlightText: String = "",
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    autoLink: Boolean = true,
) {

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    val note = noteShowBean.note
    Card(
        colors = CardDefaults.cardColors(containerColor = SaltTheme.colors.subBackground),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CardDefaults.shape)
            .combinedClickable(
                enabled = selectionMode || isCanNavigate,
                onClick = {
                    if (selectionMode) {
                        onSelectedChange?.invoke(!selected)
                    } else if (note.parentNoteId != null) {
                        navHostController.navigate(route = Screen.CommentList(note.parentNoteId!!))
                    } else {
                        navHostController.navigate(route = Screen.MemoPreview(noteShowBean.note.noteId))
                    }
                },
                onLongClick = {
                    if (selectionMode) {
                        onSelectedChange?.invoke(!selected)
                    } else {
                        openBottomSheet = true
                    }
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (!note.noteTitle.isNullOrBlank()) {
                Text(
                    text = note.noteTitle!!,
                    style = SaltTheme.textStyles.paragraph.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SaltTheme.colors.text
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            MarkdownText(
                markdown = note.content,
                maxLines = if (isExpanded) 8 else Int.MAX_VALUE,
                style = SaltTheme.textStyles.paragraph.copy(
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = SaltTheme.colors.text
                ), onTagClick = {
                    if (from == NoteCardFrom.COMMON && isCanNavigate) {
                        navHostController.navigate(Screen.TagDetail(it))
                    }
                },
                highlightText = highlightText,
                autoLink = autoLink
            )
            // 添加展开/收起按钮
            if (note.content.length > 200) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (isExpanded) stringResource(R.string.read_more) else stringResource(R.string.collapse),
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded },
                    color = Color.Blue.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (noteShowBean.parentNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
//                        .border(1.dp, SaltTheme.colors.subText.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
//                        .clickable {
//                            navHostController.navigate(route = Screen.InputDetail(noteShowBean.parentNote.noteId))
//                        }
                        .padding(8.dp)
                ) {
                    Text(
                        text = noteShowBean.parentNote.content,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        style = SaltTheme.textStyles.paragraph.copy(
                            fontSize = 13.sp,
                            color = SaltTheme.colors.subText
                        )
                    )
                }
            }

            if (note.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ImageCard(note, navHostController)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modifier = Modifier.padding(start = 2.dp),
                        text = note.createTime.toTime(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                    if (note.updateTime - note.createTime > 1000) {
                        Text(
                            modifier = Modifier.padding(start = 2.dp),
                            text = stringResource(
                                R.string.updated_at,
                                note.updateTime.toTime()
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                if (selectionMode) {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) SaltTheme.colors.highlight else SaltTheme.colors.subText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
    ActionBottomSheet(
        navHostController = navHostController,
        noteShowBean = noteShowBean,
        show = openBottomSheet,
        onCommentClick = onCommentClick,
        onDismissRequest = {
            openBottomSheet = false
        }
    )

}