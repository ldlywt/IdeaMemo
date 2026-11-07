package com.ldlywt.note.ui.page.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.component.ConfirmDialog
import com.ldlywt.note.component.EmptyComponent
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toast
import com.ldlywt.note.utils.toTime
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlinx.coroutines.launch

@OptIn(UnstableSaltApi::class)
@Composable
fun RecycleBinPage(navController: NavHostController) {
    val viewModel = LocalMemosViewModel.current
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    val scope = rememberCoroutineScope()

    var permanentDeleteTarget by remember { mutableStateOf<NoteShowBean?>(null) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
            .statusBarsPadding()
    ) {
        TitleBar(
            onBack = { navController.debouncedPopBackStack() },
            text = R.string.recycle_bin.str
        )

        if (deletedNotes.isEmpty()) {
            EmptyComponent(text = stringResource(R.string.recycle_bin_empty))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showEmptyConfirm = true }) {
                    Text(
                        text = stringResource(R.string.empty_recycle_bin),
                        color = SaltTheme.colors.subText
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deletedNotes, key = { it.note.noteId }) { bean ->
                    RecycleBinItem(
                        bean = bean,
                        onRestore = {
                            scope.launch {
                                viewModel.restoreNote(bean.note)
                                toast(R.string.restore_success.str)
                            }
                        },
                        onDeletePermanently = { permanentDeleteTarget = bean }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    ConfirmDialog(
        visible = permanentDeleteTarget != null,
        title = stringResource(R.string.delete_permanently_title),
        content = stringResource(R.string.delete_permanently_message),
        onDismissRequest = { permanentDeleteTarget = null },
        onConfirmRequest = {
            val target = permanentDeleteTarget
            permanentDeleteTarget = null
            if (target != null) {
                scope.launch {
                    viewModel.deleteNotePermanently(target.note, target.tagList)
                }
            }
        }
    )

    ConfirmDialog(
        visible = showEmptyConfirm,
        title = stringResource(R.string.empty_recycle_bin_title),
        content = stringResource(R.string.empty_recycle_bin_message),
        onDismissRequest = { showEmptyConfirm = false },
        onConfirmRequest = {
            showEmptyConfirm = false
            val snapshot = deletedNotes
            scope.launch {
                viewModel.emptyRecycleBin(snapshot)
            }
        }
    )
}

@Composable
private fun RecycleBinItem(
    bean: NoteShowBean,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val note = bean.note
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SaltTheme.colors.subBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!note.noteTitle.isNullOrBlank()) {
                Text(
                    text = note.noteTitle!!,
                    style = SaltTheme.textStyles.paragraph.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = note.content,
                style = SaltTheme.textStyles.paragraph.copy(fontSize = 15.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.updateTime.toTime(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onRestore) {
                    Text(text = stringResource(R.string.restore), color = SaltTheme.colors.highlight)
                }
                TextButton(onClick = onDeletePermanently) {
                    Text(text = stringResource(R.string.delete_permanently), color = SaltTheme.colors.subText)
                }
            }
        }
    }
}
