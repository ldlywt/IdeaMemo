package com.ldlywt.note.ui.page.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Javascript
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ldlywt.note.App
import com.ldlywt.note.R
import com.ldlywt.note.backup.model.DavData
import com.ldlywt.note.component.ConfirmDialog
import com.ldlywt.note.component.LoadingComponent
import com.ldlywt.note.component.RYDialog
import com.ldlywt.note.ui.page.LocalMemosState
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.ui.page.settings.SettingsBean
import com.ldlywt.note.utils.BackUp
import com.ldlywt.note.utils.ChoseFolderContract
import com.ldlywt.note.utils.ExportNotesJsonContract
import com.ldlywt.note.utils.ExportTextContract
import com.ldlywt.note.utils.RestoreNotesContract
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.backUpFileName
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
    var isShowRestartDialog by remember { mutableStateOf(false) }
    val webDavList = remember { mutableListOf<DavData>() }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current as AppCompatActivity
    val snackbarState = remember { SnackbarHostState() }
    var webInputDialog: Boolean by remember { mutableStateOf(false) }
    val autoBackSwitchState = SharedPreferencesUtils.localAutoBackup.collectAsState(false)
    val jianGuoCloudSwitchState = SharedPreferencesUtils.davLoginSuccess.collectAsState(false)
    val isLogin =viewModel.isLogin

    fun navToWebdavConfigPage() {
        navController.navigate(Screen.DataCloudConfig)
    }

    fun exportToWebdav() {
        if (isLogin) {
            navToWebdavConfigPage()
            return
        }
        lunchMain {
            isLoading = true
            val resultStr = viewModel.exportToWebdav(context)
            isLoading = false
            snackbarState.showSnackbar(resultStr)
        }
    }

    fun restoreForWebdav() {
        if (isLogin) {
            navToWebdavConfigPage()
            return
        }
        lunchMain {
            isLoading = true
            val list: List<DavData> = viewModel.restoreForWebdav()
            webDavList.clear()
            webDavList.addAll(list)
            isLoading = false
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
        lunchMain {
            withContext(Dispatchers.IO) {
                BackUp.exportTXTFile(list = noteState.notes, uri)
            }
            snackbarState.showSnackbar(R.string.excute_success.str)
        }
    }

    // Create an ActivityResultLauncher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                withContext(Dispatchers.IO) {
                    BackUp.export(context, uri)
                }
                isLoading = false
                snackbarState.showSnackbar(R.string.excute_success.str)
            }
        }
    }

    // Create an ActivityResultLauncher
    val encryptedExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                withContext(Dispatchers.IO) {
                    BackUp.exportEncrypted(context, uri)
                }
                isLoading = false
                snackbarState.showSnackbar(R.string.excute_success.str)
            }
        }
    }

    val restoreFromSdLauncher = rememberLauncherForActivityResult(RestoreNotesContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        lunchMain {
            isLoading = true
            BackUp.restoreFromEncryptedZip(App.instance, uri)
            isLoading = false
            isShowRestartDialog = true
        }
    }

    val exportNotesJsonLauncher = rememberLauncherForActivityResult(ExportNotesJsonContract) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        lunchMain {
            withContext(Dispatchers.IO) {
                BackUp.exportJson(list = noteState.notes, uri)
            }
            snackbarState.showSnackbar(R.string.excute_success.str)
        }
    }

    val localDataList = listOf(
        SettingsBean(R.string.data_backup, Icons.Outlined.PrivacyTip) {
            encryptedExportLauncher.launch(backUpFileName)
        },
        SettingsBean(R.string.data_restore, Icons.Outlined.Restore) {
            restoreFromSdLauncher.launch(null)
        },
        SettingsBean(R.string.json_export, Icons.Outlined.Javascript) {
            exportNotesJsonLauncher.launch(null)
        },
        SettingsBean(R.string.txt_export, Icons.Outlined.TextFields) {
            exportTxtLauncher.launch(null)
        },
        SettingsBean(R.string.export_data, Icons.Outlined.SaveAlt) {
            exportLauncher.launch("IdeaMemo.zip")
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
                navController.popBackStack()
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
                    iconPainter = rememberVectorPainter(it.imageVector),
                )
            }
            val localBackUri = SharedPreferencesUtils.localBackupUri.collectAsState("")
            ItemSwitcher(
                state = autoBackSwitchState.value,
                onChange = {
                    if (localBackUri.value.isNullOrEmpty()) {
                        showChoseFolderDialog = true
                    } else {
                        val isChecked = autoBackSwitchState.value
                        if (isChecked) {
                            scope.launch {
                                SharedPreferencesUtils.updateLocalAutoBackup(false)
                                SharedPreferencesUtils.updateLocalBackUri(null)
                            }

                        }
                    }
                },
                iconColor = SaltTheme.colors.highlight,
                text = R.string.title_local_auto_backup.str
            )
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
                    if(jianGuoCloudSwitchState.value) {
                        scope.launch {
                            SharedPreferencesUtils.clearDavConfig()
                        }
                    }else {
                            webInputDialog = true
                        }
                }
            )
            if (jianGuoCloudSwitchState.value) {
                Item(text = R.string.webdav_backup.str, onClick = {
                    exportToWebdav()
                })
                Item(text = R.string.webdav_restore.str, onClick = {
                    restoreForWebdav()
                })
            }
        }
    }
    LoadingComponent(isLoading)
    ChoseFolderDialog(
        visible = showChoseFolderDialog,
        onDismissRequest = {
            showChoseFolderDialog = false
        }, onConfirmRequest = {
            choseFolderLauncher.launch(null)
            showChoseFolderDialog = false
        })

    ConfirmDialog(isShowRestartDialog, title = R.string.restart.str, content = R.string.app_restored.str,
        onDismissRequest = {
            isShowRestartDialog = false
        }, onConfirmRequest = {
            isShowRestartDialog = false
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        })

    WebRestoreBottomSheet(show = openBottomSheet, webDavList, onDismissRequest = {
        openBottomSheet = false
    }, onConfirmRequest = { davData ->
        lunchMain {
            openBottomSheet = false
            isLoading = true
            val resultPath = viewModel.downloadFileByPath(davData)
            if (!resultPath.isNullOrEmpty()) {
                val uri = FileProvider.getUriForFile(context, "com.ldlywt.note.provider", File(resultPath))
                BackUp.restoreFromEncryptedZip(App.instance, uri)
                isLoading = false
                isShowRestartDialog = true
            }
        }
    })
    LoadingComponent(isLoading)
    ConfirmDialog(isShowRestartDialog, title = R.string.restart.str, content = R.string.app_restored.str,
        onDismissRequest = {
            isShowRestartDialog=false
        }, onConfirmRequest = {
            isShowRestartDialog=false
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        })

}

@Composable
fun ChoseFolderDialog(visible: Boolean, onDismissRequest: () -> Unit, onConfirmRequest: () -> Unit) {
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
fun WebRestoreBottomSheet(show: Boolean, list: List<DavData>, onDismissRequest: () -> Unit, onConfirmRequest: (data: DavData) -> Unit) {

    if (show) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                LazyColumn {
                    items(list.size) {
                        ListItem(headlineContent = { Text(list[it].displayName) }, modifier = Modifier.clickable {
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
    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        ItemOutSpacer()
//        DialogTitle(text = R.string.webdav_config.str)
        ItemOutHalfSpacer()

        ItemTitle(text = R.string.webdav_config.str)

        val serverUrl = SharedPreferencesUtils.davServerUrl.collectAsState("")
        val username =SharedPreferencesUtils.davUserName.collectAsState(null)
        val password =SharedPreferencesUtils.davPassword.collectAsState(null)
        val dataManagerViewMode: DataManagerViewModel = hiltViewModel()
//        val focusRequester = remember { FocusRequester() }
        ItemEdit(
            text = serverUrl.value?:"",
            onChange = {
                scope.launch {
                   SharedPreferencesUtils.updateDavServerUrl(it)
                }
            },
            hint = stringResource(R.string.server_url)
        )

        ItemEdit(
            text = username.value?:"" ,
            onChange = {
                scope.launch {
                    SharedPreferencesUtils.updateDavUserName(it)
                }
            },
            hint = R.string.username.str
        )

        ItemEditPassword(
            text = password.value ?: "",
            onChange = {
                scope.launch {
                    SharedPreferencesUtils.updateDavPassword(it)
                }
            },
            hint = R.string.password.str
        )
        LaunchedEffect(Unit) {
//            focusRequester.requestFocus()
        }
        ItemOutHalfSpacer()
        Row(
            modifier = Modifier.padding(horizontal = SaltTheme.dimens.outerHorizontalPadding)
        ) {
            com.moriafly.salt.ui.TextButton(
                onClick = {
                    onDismissRequest()
                },
                modifier = Modifier
                    .weight(1f),
                text = R.string.cancel.str,
                textColor = SaltTheme.colors.subText,
                backgroundColor = SaltTheme.colors.subBackground
            )
            Spacer(modifier = Modifier.width(SaltTheme.dimens.contentPadding))
            com.moriafly.salt.ui.TextButton(
                onClick = {
                    lunchIo {
                        val pair = dataManagerViewMode.checkConnection(serverUrl.value!!, username.value!!, password.value!!)
                        withContext(Dispatchers.Main) {
                            toast(pair.second)
                            scope.launch {
                                SharedPreferencesUtils.updateDavLoginSuccess(pair.first)
                                if (pair.first) {
                                    onConfirm()
                                }
                            }

                        }
                    }
                },
                modifier = Modifier
                    .weight(1f),
                text = R.string.submit.str
            )
        }
        ItemOutSpacer()
    }
}

