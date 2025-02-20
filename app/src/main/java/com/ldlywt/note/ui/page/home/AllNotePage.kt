package com.ldlywt.note.ui.page.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.component.NoteCard
import com.ldlywt.note.component.RYScaffold

import com.ldlywt.note.state.NoteState
import com.ldlywt.note.ui.page.LocalMemosState
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.NoteViewModel
import com.ldlywt.note.ui.page.SortTime
import com.ldlywt.note.ui.page.input.ChatInput
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.utils.FirstTimeWarmDialog
import com.ldlywt.note.utils.SettingsPreferences
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.lunchMain
import com.ldlywt.note.utils.str
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AllNotesPage(
    navController: NavHostController,
    hideBottomNavBar: ((Boolean) -> Unit)
) {
    val noteState: NoteState = LocalMemosState.current
    var openFilterBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showWarnDialog by rememberSaveable { mutableStateOf(false) }
    var showInputDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showWarnDialog = SettingsPreferences.firstLaunch.first()
    }

    RYScaffold(
        title = R.string.all_note.str, navController = null,
        actions = {
            toolbar(navController) {
                openFilterBottomSheet = true
            }
        },
        floatingActionButton = {
            if (!showInputDialog) {
                FloatingActionButton(onClick = {
                    hideBottomNavBar.invoke(true)
                    showInputDialog = true
                }, modifier = Modifier.padding(end = 16.dp, bottom = 32.dp)) {
                    Icon(
                        Icons.Rounded.Edit, stringResource(R.string.edit)
                    )
                }
            }
        },
    ) {

        Box {
            LazyColumn(
                Modifier
                    .fillMaxSize()
            ) {
                items(count = noteState.notes.size, key = { it }) { index ->
                    NoteCard(noteShowBean = noteState.notes[index], navController)
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            if (showInputDialog) {
                BackHandler(enabled = true) {
                    hideBottomNavBar.invoke(false)
                    showInputDialog = false
                }
            }

            ChatInput(
                isShow = showInputDialog,
                modifier =
                Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            ) {
                hideBottomNavBar.invoke(false)
                showInputDialog = false
            }
        }
    }

    HomeFilterBottomSheet(
        show = openFilterBottomSheet,
        onDismissRequest = {
            openFilterBottomSheet = false
        }, onConfirmRequest = {
            openFilterBottomSheet = false
        })

    if (showWarnDialog) {
        FirstTimeWarmDialog {
            lunchMain {
                SettingsPreferences.changeFirstLaunch(false)
                showWarnDialog = false
            }
        }
    }


}

@Composable
private fun toolbar(navController: NavHostController, filterBlock: () -> Unit) {
    IconButton(
        onClick = {
            navController.navigate(route = Screen.TagList) {
                launchSingleTop = true
            }
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.Tag,
            contentDescription = R.string.tag.str,
            tint = SaltTheme.colors.text
        )
    }

    IconButton(
        onClick = {
            navController.navigate(route = Screen.Search) {
                launchSingleTop = true
            }
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.Search, contentDescription = R.string.search_hint.str, tint = SaltTheme.colors.text
        )
    }

    IconButton(
        onClick = {
            filterBlock()
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.FilterList, contentDescription = "sort", tint = SaltTheme.colors.text
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFilterBottomSheet(show: Boolean, onDismissRequest: () -> Unit, onConfirmRequest: () -> Unit) {

    val viewModel: NoteViewModel = LocalMemosViewModel.current
    val sortTime = SharedPreferencesUtils.sortTime.collectAsState(SortTime.UPDATE_TIME_DESC)
    val scope = rememberCoroutineScope()

    if (show) {
        ModalBottomSheet(onDismissRequest = onDismissRequest) {
            Column(Modifier.fillMaxWidth()) {
                TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        SharedPreferencesUtils.updateSortTime(SortTime.UPDATE_TIME_DESC)
                    }
                    onConfirmRequest()
                }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.update_time_desc))
                        Spacer(modifier = Modifier.weight(1f))
                        Checkbox(checked = sortTime.value == SortTime.UPDATE_TIME_DESC, null)
                    }
                }
                TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        SharedPreferencesUtils.updateSortTime(SortTime.UPDATE_TIME_ASC)
                        onConfirmRequest()
                    }

                }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.update_time_asc))
                        Spacer(modifier = Modifier.weight(1f))
                        Checkbox(checked = sortTime.value == SortTime.UPDATE_TIME_ASC, null)
                    }
                }
                TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        SharedPreferencesUtils.updateSortTime(SortTime.UPDATE_TIME_ASC)
                        onConfirmRequest()
                    }
                }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.create_time_desc))
                        Spacer(modifier = Modifier.weight(1f))
                        Checkbox(checked = sortTime.value == SortTime.CREATE_TIME_DESC, null)
                    }
                }
                TextButton(onClick = {
                    scope.launch {
                        SharedPreferencesUtils.updateSortTime(SortTime.UPDATE_TIME_ASC)
                        onConfirmRequest()
                    }
                }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.create_time_asc))
                        Spacer(modifier = Modifier.weight(1f))
                        Checkbox(checked = sortTime.value == SortTime.CREATE_TIME_ASC, null)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}