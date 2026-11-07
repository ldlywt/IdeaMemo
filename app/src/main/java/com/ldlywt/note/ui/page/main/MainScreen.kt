package com.ldlywt.note.ui.page.main

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.component.ConfirmDialog
import com.ldlywt.note.ui.page.home.AllNotePage
import com.ldlywt.note.ui.page.home.CalenderPage
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.ui.page.settings.HeatContent
import com.ldlywt.note.ui.page.settings.SettingsHeadLayout
import com.ldlywt.note.ui.page.settings.SettingsPage
import com.ldlywt.note.ui.page.tag.TagListPage
import com.ldlywt.note.utils.str
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val destinations = NavigationBarPath.entries
    var currentDestination by rememberSaveable { mutableStateOf(destinations[0].route) }
    val pagerState = rememberPagerState(initialPage = 0) { destinations.size }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val view = LocalView.current
    val activity = LocalContext.current as? Activity

    var showInputDialog by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            showExitDialog = true
        }
    }

    ConfirmDialog(
        visible = showExitDialog,
        title = stringResource(R.string.exit_confirm_title),
        content = stringResource(R.string.exit_confirm_message),
        onDismissRequest = { showExitDialog = false },
        onConfirmRequest = {
            showExitDialog = false
            activity?.finish()
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val configuration = LocalConfiguration.current
            val drawerWidth = (configuration.screenWidthDp * 0.8f).dp
            ModalDrawerSheet(
                modifier = Modifier.width(drawerWidth),
                drawerContainerColor = SaltTheme.colors.background,
                drawerContentColor = SaltTheme.colors.text
            ) {
                Spacer(Modifier.height(24.dp))
                SettingsHeadLayout()
                Spacer(Modifier.height(12.dp))
                HeatContent()
                Spacer(Modifier.height(12.dp))
                destinations.forEachIndexed { index, destination ->
                    val selected = destination.route == currentDestination
                    NavigationDrawerItem(
                        label = { Text(destination.route, fontSize = 18.sp) },
                        selected = selected,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            currentDestination = destinations[index].route
                            scope.launch {
                                pagerState.scrollToPage(index)
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        shape = RoundedCornerShape(4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF0ECF66),
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = SaltTheme.colors.text,
                            unselectedTextColor = SaltTheme.colors.text
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                val extraItems = listOf(
                    Triple(R.string.random_walk, Icons.Outlined.Explore, Screen.RandomWalk),
                    Triple(R.string.gallery, Icons.Outlined.Photo, Screen.Gallery),
                    Triple(R.string.statistics, Icons.Outlined.BarChart, Screen.Statistics),
                    Triple(R.string.recycle_bin, Icons.Outlined.DeleteOutline, Screen.RecycleBin)
                )

                extraItems.forEach { (titleRes, icon, screen) ->
                    NavigationDrawerItem(
                        label = { Text(titleRes.str, fontSize = 18.sp, color = SaltTheme.colors.text) },
                        selected = false,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            scope.launch {
                                drawerState.close()
                                navController.navigate(screen)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        shape = RoundedCornerShape(4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = SaltTheme.colors.text,
                            unselectedTextColor = SaltTheme.colors.text
                        )
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SaltTheme.colors.background)
                .displayCutoutPadding()
        ) {
            MainPager(
                pagerState = pagerState,
                navController = navController,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                showInputDialog = showInputDialog,
                onShowInputDialogChange = { showInputDialog = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainPager(
    pagerState: PagerState,
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    showInputDialog: Boolean,
    onShowInputDialogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        modifier = modifier
    ) { page ->
        when (page) {
            0 -> AllNotePage(
                navController = navController,
                onOpenDrawer = onOpenDrawer,
                externalShowInputDialog = showInputDialog,
                onExternalShowInputDialogChange = onShowInputDialogChange
            )

            1 -> TagListPage(navController = navController, onOpenDrawer = onOpenDrawer)
            2 -> CalenderPage(navController = navController, onOpenDrawer = onOpenDrawer)
            3 -> SettingsPage(navController = navController, onOpenDrawer = onOpenDrawer)
        }
    }
}

enum class NavigationBarPath(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    AllNote(
        route = R.string.nav_home.str,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    Tag(
        route = R.string.nav_tag.str,
        selectedIcon = Icons.AutoMirrored.Rounded.Label,
        unselectedIcon = Icons.AutoMirrored.Outlined.Label
    ),
    Calendar(
        route = R.string.nav_calendar.str,
        selectedIcon = Icons.Rounded.Event,
        unselectedIcon = Icons.Outlined.Event
    ),
    Settings(
        route = R.string.nav_settings.str,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}
