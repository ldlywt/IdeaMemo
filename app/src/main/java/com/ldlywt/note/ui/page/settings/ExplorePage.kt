package com.ldlywt.note.ui.page.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.bean.Note
import com.ldlywt.note.component.ConfirmDialog
import com.ldlywt.note.component.DraggableCard
import com.ldlywt.note.component.EmptyComponent
import com.ldlywt.note.component.ImageCard
import com.ldlywt.note.ui.page.LocalMemosState
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.ldlywt.note.utils.SettingsPreferences
import com.ldlywt.note.utils.toTime
import kotlinx.coroutines.launch
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class, UnstableSaltApi::class)
@Composable
fun ExplorePage(
    navHostController: NavHostController,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val noteState = LocalMemosState.current
    val memosViewModel = LocalMemosViewModel.current
    val scope = rememberCoroutineScope()
    val autoLink by SettingsPreferences.autoLinkEnabled.collectAsState(true)
    
    // 当从编辑页面返回且数据发生变化时，同步更新 ViewModel 中的对应内容
    LaunchedEffect(noteState.notes) {
        viewModel.shuffledList.forEach { oldNote ->
            noteState.notes.find { it.note.noteId == oldNote.noteId }?.let { updatedNoteBean ->
                if (oldNote.content != updatedNoteBean.note.content || 
                    oldNote.attachments.size != updatedNoteBean.note.attachments.size) {
                    viewModel.updateNote(updatedNoteBean.note.copy())
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
            .statusBarsPadding()
    ) {
        TitleBar(
            onBack = {
                navHostController.debouncedPopBackStack()
            },
            text = stringResource(R.string.random_walk)
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (viewModel.shuffledList.isEmpty()) {
                EmptyComponent()
            } else {
                // 仅渲染最顶层的一张卡片
                val topNote = viewModel.shuffledList.last()
                val currentPos = viewModel.totalCount.intValue - viewModel.shuffledList.size + 1
                
                // 关键点：使用 key 约束。当划走一张后，ID 变化会强制销毁并重建新的 DraggableCard
                // 从而重置 swipeX 的动画位置，解决下一张卡片“跟随划走”导致黑屏的问题
                key(topNote.noteId) {
                    ExploreMemoCardWithDraggable(
                        note = topNote,
                        currentIndex = currentPos,
                        totalCount = viewModel.totalCount.intValue,
                        onSwiped = { 
                            viewModel.removeTop() 
                        },
                        onDelete = {
                            scope.launch {
                                memosViewModel.moveNoteToTrash(topNote)
                                viewModel.removeTop()
                            }
                        },
                        autoLink = autoLink,
                        navHostController = navHostController
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreMemoCardWithDraggable(
    note: Note,
    currentIndex: Int,
    totalCount: Int,
    onSwiped: () -> Unit,
    onDelete: () -> Unit,
    autoLink: Boolean,
    navHostController: NavHostController
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val cardHeight = screenHeight * 2 / 3

    DraggableCard(
        item = note,
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(horizontal = 24.dp),
        onSwiped = { _, _ -> onSwiped() }
    ) {
        ExploreMemoContent(
            note = note,
            currentIndex = currentIndex,
            totalCount = totalCount,
            onDelete = onDelete,
            autoLink = autoLink,
            navHostController = navHostController
        )
    }
}

@Composable
fun ExploreMemoContent(
    note: Note,
    currentIndex: Int,
    totalCount: Int,
    onDelete: () -> Unit,
    autoLink: Boolean,
    navHostController: NavHostController
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.subBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentIndex / $totalCount",
                style = SaltTheme.textStyles.sub.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.size(28.dp),
                onClick = { showDeleteConfirm = true }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = SaltTheme.colors.text.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                modifier = Modifier.size(28.dp),
                onClick = {
                    navHostController.navigate(route = Screen.InputDetail(note.noteId))
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = SaltTheme.colors.text.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (!note.noteTitle.isNullOrBlank()) {
                Text(
                    text = note.noteTitle!!,
                    style = SaltTheme.textStyles.paragraph.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            MarkdownText(
                markdown = note.content,
                style = SaltTheme.textStyles.paragraph.copy(fontSize = 15.sp, lineHeight = 24.sp),
                onTagClick = {
                    navHostController.navigate(Screen.TagDetail(it))
                },
                autoLink = autoLink
            )
            if (note.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ImageCard(note, navHostController)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(start = 2.dp),
            text = note.createTime.toTime(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        if (note.updateTime - note.createTime > 1000) {
            Text(
                modifier = Modifier.padding(start = 2.dp),
                text = stringResource(R.string.updated_at, note.updateTime.toTime()),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }

    ConfirmDialog(
        visible = showDeleteConfirm,
        title = stringResource(R.string.delete_to_trash_title),
        content = stringResource(R.string.delete_to_trash_message),
        onDismissRequest = { showDeleteConfirm = false },
        onConfirmRequest = {
            showDeleteConfirm = false
            onDelete()
        }
    )
}
