package com.ldlywt.note.ui.page.data

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.backup.model.DavData
import com.ldlywt.note.component.LoadingComponent
import com.ldlywt.note.component.RYDialog
import com.ldlywt.note.ui.page.LocalMemosState
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.ldlywt.note.ui.page.settings.SettingsBean
import com.ldlywt.note.utils.BackUp
import com.ldlywt.note.utils.ChoseFolderContract
import com.ldlywt.note.utils.ExportHtmlContract
import com.ldlywt.note.utils.ExportMarkDownContract
import com.ldlywt.note.utils.ExportTextContract
import com.ldlywt.note.utils.ExportUriMemory
import com.ldlywt.note.utils.ImportHtmlZipContract
import com.ldlywt.note.utils.ImportTextContract
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.lunchIo
import com.ldlywt.note.utils.lunchMain
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toast
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.ItemOutHalfSpacer
import com.moriafly.salt.ui.ItemOutSpacer
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.BasicDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableSaltApi::class)
@Composable
fun DataManagerPage(
    navController: NavHostController, viewModel: DataManagerViewModel = hiltViewModel()
) {
    val noteState = LocalMemosState.current
    val scope = rememberCoroutineScope()
    var showChoseFolderDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val webDavList = remember { mutableListOf<DavData>() }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current as AppCompatActivity
    var webInputDialog: Boolean by remember { mutableStateOf(false) }
    val autoBackSwitchState = SharedPreferencesUtils.localAutoBackup.collectAsState(false)
    val jianGuoCloudSwitchState = SharedPreferencesUtils.davLoginSuccess.collectAsState(false)

    fun exportToWebdav() {
        lunchMain {
            isLoading = true
            val resultStr = viewModel.exportToWebdav(context, noteState.notes)
            isLoading = false
            isSuccess = resultStr.startsWith("Success")
            if (!isSuccess) {
                toast(resultStr)
            }
        }
    }

    fun restoreForWebdav() {
        lunchMain {
            isLoading = true
            val list: List<DavData> = viewModel.restoreForWebdav()
            webDavList.clear()
            webDavList.addAll(list)
            isLoading = false
            isSuccess = true
            openBottomSheet = true
        }
    }

    val choseFolderLauncher = rememberLauncherForActivityResult(ChoseFolderContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            SharedPreferencesUtils.updateLocalBackUri(uri.toString())
            SharedPreferencesUtils.updateLocalAutoBackup(true)
        }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(ExportTextContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        ExportUriMemory.remember(context, uri)
        lunchMain {
            withContext(Dispatchers.IO) {
                BackUp.exportTXTFile(list = noteState.notes, uri)
            }
        }
    }

    val exportHtmlLauncher = rememberLauncherForActivityResult(ExportHtmlContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        ExportUriMemory.remember(context, uri)
        lunchMain {
            isLoading = true
            withContext(Dispatchers.IO) {
                BackUp.exportHtmlZip(list = noteState.notes, uri)
            }
            isLoading = false
            isSuccess = true
        }
    }

    val exportMarkDownLauncher =
        rememberLauncherForActivityResult(ExportMarkDownContract("IdeaMemo")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            ExportUriMemory.remember(context, uri)
            lunchIo {
                BackUp.exportMarkDownFile(list = noteState.notes, uri)
                toast(R.string.excute_success.str)
            }
        }

    val importHtmlZipLauncher = rememberLauncherForActivityResult(ImportHtmlZipContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        lunchMain {
            isLoading = true
            val result = BackUp.importFromHtmlZip(context, uri)
            isLoading = false
            result.onSuccess { count ->
                isSuccess = true
                Toast.makeText(
                    context,
                    context.getString(R.string.import_success_count, count),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                isSuccess = false
                Toast.makeText(
                    context,
                    context.getString(R.string.import_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importTextLauncher = rememberLauncherForActivityResult(ImportTextContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        lunchMain {
            isLoading = true
            val result = BackUp.importFromMarkdownOrText(context, uri)
            isLoading = false
            result.onSuccess { count ->
                isSuccess = true
                Toast.makeText(
                    context,
                    context.getString(R.string.import_success_count, count),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                isSuccess = false
                Toast.makeText(
                    context,
                    context.getString(R.string.import_failed, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val localDataList = listOf(
        SettingsBean(R.string.txt_export, Icons.Outlined.TextFields) {
            exportTxtLauncher.launch(null)
        },
        SettingsBean(R.string.mk_export, Icons.Outlined.FormatColorText) {
            exportMarkDownLauncher.launch(null)
        },
        SettingsBean(R.string.html_export, Icons.Outlined.FileDownload) {
            exportHtmlLauncher.launch(null)
        },
        SettingsBean(R.string.html_import, Icons.Outlined.FileUpload) {
            importHtmlZipLauncher.launch(null)
        },
        SettingsBean(R.string.import_markdown_txt, Icons.Outlined.FileUpload) {
            importTextLauncher.launch(null)
        },
        SettingsBean(R.string.data_help_title, Icons.Outlined.Info) {
            showHelpDialog = true
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SaltTheme.colors.background)
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        TitleBar(
            onBack = {
                navController.debouncedPopBackStack()
            },
            text = R.string.local_data_manager.str
        )
        RoundedColumn {
            localDataList.forEachIndexed { index, it ->
                Item(
                    onClick = {
                        it.onClick()
                    },
                    text = it.title.str,
                )
            }
        }

        if (webInputDialog) {
            AccountInputDialog(
                onDismissRequest = {
                    webInputDialog = false
                },
                onConfirm = {
                    webInputDialog = false
                    scope.launch {
                        SharedPreferencesUtils.updateDavLoginSuccess(true)
                    }
                },
            )
        }

        RoundedColumn {
            ItemSwitcher(
                text = R.string.webdav_auth.str,
                state = jianGuoCloudSwitchState.value,
                onChange = {
                    if (jianGuoCloudSwitchState.value) {
                        scope.launch {
                            SharedPreferencesUtils.clearDavConfig()
                        }
                    } else {
                        webInputDialog = true
                    }
                }
            )
            if (jianGuoCloudSwitchState.value) {
                Item(text = R.string.edit_webdav_config.str, onClick = {
                    webInputDialog = true
                })
                Item(text = R.string.webdav_backup.str, onClick = {
                    exportToWebdav()
                })
                Item(text = R.string.webdav_restore.str, onClick = {
                    restoreForWebdav()
                })
            }
        }
    }

    LoadingComponent(isLoading = isLoading, isSuccess = isSuccess) {
        isSuccess = false
    }

    ChoseFolderDialog(
        visible = showChoseFolderDialog,
        onDismissRequest = {
            showChoseFolderDialog = false
        }, onConfirmRequest = {
            choseFolderLauncher.launch(null)
            showChoseFolderDialog = false
        })

    WebRestoreBottomSheet(show = openBottomSheet, list = webDavList, onDismissRequest = {
        openBottomSheet = false
    }, onConfirmRequest = { davData ->
        lunchMain {
            openBottomSheet = false
            isLoading = true
            val resultPath = viewModel.downloadFileByPath(davData)
            if (!resultPath.isNullOrEmpty()) {
                val uri = Uri.fromFile(File(resultPath))
                val result = BackUp.importFromHtmlZip(context, uri)
                isLoading = false
                result.onSuccess { count ->
                    isSuccess = true
                }.onFailure { e ->
                    isSuccess = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.import_failed, e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                isLoading = false
                isSuccess = false
            }
        }
    })

    if (showHelpDialog) {
        RYDialog(
            visible = true,
            properties = DialogProperties(),
            title = {
                Text(text = stringResource(R.string.data_help_title))
            },
            text = {
                Text(text = stringResource(R.string.data_help_message))
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            },
        )
    }
}

@Composable
fun ChoseFolderDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    RYDialog(
        visible = visible,
        properties = DialogProperties(),
        title = {
            Text(text = R.string.choose_folder.str)
        },
        text = {
            Text(text = R.string.notes_will_be.str)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmRequest()
                }
            ) {
                Text(stringResource(R.string.choose))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebRestoreBottomSheet(
    show: Boolean,
    list: List<DavData>,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (data: DavData) -> Unit
) {

    if (show) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                LazyColumn {
                    items(list.size) {
                        ListItem(
                            headlineContent = { Text(list[it].displayName) },
                            modifier = Modifier.clickable {
                                onConfirmRequest(list[it])
                            })
                    }
                }
            }
        }
    }
}


@UnstableSaltApi
@Composable
fun AccountInputDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    properties: DialogProperties = DialogProperties(),
) {

    val scope = rememberCoroutineScope()

    val initialServerUrl by SharedPreferencesUtils.davServerUrl.collectAsState("")
    val initialUsername by SharedPreferencesUtils.davUserName.collectAsState(null)
    val initialPassword by SharedPreferencesUtils.davPassword.collectAsState(null)

    var serverUrl by remember(initialServerUrl) { mutableStateOf(initialServerUrl ?: "") }
    var username by remember(initialUsername) { mutableStateOf(initialUsername ?: "") }
    var password by remember(initialPassword) { mutableStateOf(initialPassword ?: "") }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        ItemOutSpacer()
//        DialogTitle(text = R.string.webdav_config.str)
        ItemOutHalfSpacer()

        ItemTitle(text = R.string.webdav_config.str)

        ItemEdit(
            text = serverUrl,
            onChange = {
                serverUrl = it
            },
            hint = stringResource(R.string.server_url)
        )

        ItemEdit(
            text = username,
            onChange = {
                username = it
            },
            hint = R.string.username.str
        )

        ItemEditPassword(
            text = password,
            onChange = {
                password = it
            },
            hint = R.string.password.str
        )
        LaunchedEffect(Unit) {
//            focusRequester.requestFocus()
        }
        ItemOutHalfSpacer()
        com.moriafly.salt.ui.TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {
                scope.launch {
                    SharedPreferencesUtils.updateDavServerUrl(serverUrl)
                    SharedPreferencesUtils.updateDavUserName(username)
                    SharedPreferencesUtils.updateDavPassword(password)
                    onConfirm()
                }
            },
            text = stringResource(id = R.string.confirm)
        )
        ItemOutSpacer()
    }
}
