package com.ldlywt.note.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.utils.BackUp
import com.ldlywt.note.utils.ExportMarkDownContract
import com.ldlywt.note.utils.copy
import com.ldlywt.note.utils.lunchIo
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toast
import com.ldlywt.note.utils.withIO
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.launch


/**
 * 长按笔记后底部弹框里可用的操作项。
 * 允许调用方按页面场景裁剪，例如日历页只需要「复制 / 分享 / 删除」。
 */
enum class NoteSheetAction {
    Comment, Copy, Share, Delete, Export;

    companion object {
        /** 默认展示全部操作项，保持原有行为。 */
        val All: Set<NoteSheetAction> = enumValues<NoteSheetAction>().toSet()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBottomSheet(
    navHostController: NavHostController,
    noteShowBean: NoteShowBean,
    show: Boolean,
    onCommentClick: ((NoteShowBean) -> Unit)? = null,
    onNoteDeleted: (() -> Unit)? = null,
    actions: Set<NoteSheetAction> = NoteSheetAction.All,
    onDismissRequest: () -> Unit
) {

    val viewModel = LocalMemosViewModel.current
    val skipPartiallyExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )
    val fileName = if (noteShowBean.note.content.length > 4) noteShowBean.note.content.take(4) else noteShowBean.note.content

    val exportMarkDownLauncher = rememberLauncherForActivityResult(ExportMarkDownContract(fileName)) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        lunchIo {
            BackUp.exportMarkDownFile(list = arrayListOf(noteShowBean), uri)
            toast(R.string.excute_success.str)
        }
    }

    // Sheet content
    if (show) {
        ModalBottomSheet(
            containerColor = SaltTheme.colors.popup,
            onDismissRequest = onDismissRequest,
            sheetState = bottomSheetState,
            dragHandle = { SaltDragHandle() }
        ) {
            LazyColumn(Modifier.fillMaxWidth()) {
                if (NoteSheetAction.Comment in actions) {
                    item {
                        Item(
                            onClick = {
                                onCommentClick?.invoke(noteShowBean)
                                onDismissRequest()
                            },
                            text = stringResource(R.string.comment)
                        )
                    }
                }

                if (NoteSheetAction.Copy in actions) {
                    item {
                        Item(
                            onClick = {
                                copy(noteShowBean.note)
                                onDismissRequest()
                            },
                            text = stringResource(R.string.copy)
                        )
                    }
                }

                if (NoteSheetAction.Share in actions) {
                    item {
                        Item(
                            onClick = {
                                navHostController.navigate(Screen.Share(noteShowBean.note.noteId))
                                onDismissRequest()
                            },
                            text = stringResource(R.string.share)
                        )
                    }
                }

                if (NoteSheetAction.Delete in actions) {
                    item {
                        Item(
                            onClick = {
                                showDeleteConfirm = true
                                onDismissRequest()
                            },
                            text = stringResource(R.string.delete)
                        )
                    }
                }

                if (NoteSheetAction.Export in actions) {
                    item {
                        Item(
                            onClick = {
                                scope.launch {
                                    withIO {
                                        exportMarkDownLauncher.launch(null)
                                    }
                                    onDismissRequest()
                                }
                            },
                            text = stringResource(R.string.export)
                        )
                    }
                }
            }
        }
    }

    ConfirmDialog(
        visible = showDeleteConfirm,
        title = stringResource(R.string.delete_to_trash_title),
        content = stringResource(R.string.delete_to_trash_message),
        onDismissRequest = { showDeleteConfirm = false },
        onConfirmRequest = {
            showDeleteConfirm = false
            scope.launch {
                // 软删除：标记 is_deleted，数据进入回收站
                viewModel.deleteNote(noteShowBean.note, noteShowBean.tagList)
                onNoteDeleted?.invoke()
            }
        }
    )
}