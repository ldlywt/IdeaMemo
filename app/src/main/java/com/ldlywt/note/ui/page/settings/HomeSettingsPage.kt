@file:JvmName("SettingsPageKt")

package com.ldlywt.note.ui.page.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.android.material.color.DynamicColors
import com.ldlywt.note.R
import com.ldlywt.note.component.ItemPopup
import com.ldlywt.note.component.LoadingComponent
import com.ldlywt.note.component.RYScaffold
import com.ldlywt.note.ui.page.LocalMemosState
import com.ldlywt.note.ui.page.data.DataManagerViewModel
import com.ldlywt.note.ui.page.main.MainActivity
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.utils.DonateUtils
import com.ldlywt.note.utils.HttpServer
import com.ldlywt.note.utils.LanguageUtils
import com.ldlywt.note.utils.SettingsPreferences
import com.ldlywt.note.utils.openUrl
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toYYMMDD
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTitle
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlinx.coroutines.launch
import java.util.Locale


@Composable
fun SettingsPage(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    RYScaffold(
        title = R.string.settings.str,
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    tint = SaltTheme.colors.text
                )
            }
        },
        content = {
            SettingsPreferenceScreen(navController)
        }
    )
}

data class SettingsBean(val title: Int, val imageVector: ImageVector, val onClick: () -> Unit)

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(UnstableSaltApi::class)
@Composable
fun SettingsPreferenceScreen(navController: NavHostController) {
    val noteState = LocalMemosState.current
    val dataViewModel = hiltViewModel<DataManagerViewModel>()

    val context = LocalContext.current
    val themeModePopupMenuState = rememberPopupState()
    val languagePopupMenuState = rememberPopupState()
    val fontScalePopupMenuState = rememberPopupState()
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val biometricAuthState by settingsViewModel.biometricAuthState.collectAsState()
    val dynamicColor by SettingsPreferences.dynamicColor.collectAsState(false)
    val themeMode by SettingsPreferences.themeMode.collectAsState(SettingsPreferences.ThemeMode.SYSTEM)
    val fontScale by SettingsPreferences.fontScale.collectAsState(100)
    val autoLinkEnabled by SettingsPreferences.autoLinkEnabled.collectAsState(true)
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    var isHttpServerRunning by remember { mutableStateOf(HttpServer.isRunning()) }
    var serverIp by remember { mutableStateOf(HttpServer.getIpAddress()) }

    LoadingComponent(
        isLoading = isLoading,
        isSuccess = isSuccess,
        onFinished = {
            isLoading = false
            isSuccess = false
        }
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {

            item {
                RoundedColumn {
                    ItemTitle(text = stringResource(R.string.user_interface))
                    if (DynamicColors.isDynamicColorAvailable()) {
                        ItemSwitcher(
                            state = dynamicColor,
                            onChange = { checked ->
                                scope.launch {
                                    SettingsPreferences.changeDynamicColor(checked)
                                }
                            },
                            text = stringResource(R.string.dynamic_color_switcher_text),
                            sub = stringResource(R.string.dynamic_color_switcher_sub),
                            iconPainter = painterResource(id = R.drawable.color),
                            iconPaddingValues = PaddingValues(all = 1.7.dp),
                            iconColor = SaltTheme.colors.text,
                        )
                    }
                    ItemPopup(
                        state = themeModePopupMenuState,
                        iconPainter = painterResource(id = R.drawable.app_theme),
                        iconPaddingValues = PaddingValues(all = 1.8.dp),
                        iconColor = SaltTheme.colors.text,
                        text = stringResource(R.string.theme_mode_switcher_text),
                        selectedItem = stringResource(id = themeMode.resId),
                        popupWidth = 140
                    ) {

                        val options =
                            SettingsPreferences.ThemeMode.entries.map { stringResource(id = it.resId) }
                        val selectedIndex = SettingsPreferences.ThemeMode.entries.indexOf(themeMode)

                        options.forEachIndexed { index, label ->
                            PopupMenuItem(
                                onClick = {
                                    scope.launch {
                                        SettingsPreferences.changeThemeMode(SettingsPreferences.ThemeMode.entries[index])
                                    }
                                    themeModePopupMenuState.dismiss()
                                },
                                selected = selectedIndex == index,
                                text = label,
                                iconColor = SaltTheme.colors.text
                            )
                        }
                    }

                    // 语言切换 Item
                    val currentLocale = LanguageUtils.getAppLocale(context)
                    val languageOptions = listOf(
                        stringResource(R.string.language_chinese) to Locale.SIMPLIFIED_CHINESE,
                        stringResource(R.string.language_english) to Locale.ENGLISH,
                        stringResource(R.string.language_traditional_chinese) to Locale.TRADITIONAL_CHINESE
                    )

                    val currentLanguageName = when (currentLocale.language) {
                        "zh" -> if (currentLocale.country == "TW" || currentLocale.country == "HK") stringResource(R.string.language_traditional_chinese) else stringResource(R.string.language_chinese)
                        "en" -> stringResource(R.string.language_english)
                        else -> stringResource(R.string.language_english)
                    }

                    ItemPopup(
                        state = languagePopupMenuState,
                        iconPainter = rememberVectorPainter(Icons.Outlined.Translate),
                        iconColor = SaltTheme.colors.text,
                        text = stringResource(R.string.language_switcher_text),
                        selectedItem = currentLanguageName,
                        popupWidth = 160
                    ) {
                        languageOptions.forEach { (name, locale) ->
                            PopupMenuItem(
                                onClick = {
                                    LanguageUtils.setLanguage(context, locale)
                                    languagePopupMenuState.dismiss()
                                },
                                selected = currentLocale.language == locale.language &&
                                        (if (locale.language == "zh") currentLocale.country == locale.country else true),
                                text = name,
                                iconColor = SaltTheme.colors.text
                            )
                        }
                    }

                    ItemPopup(
                        state = fontScalePopupMenuState,
                        iconPainter = rememberVectorPainter(Icons.Outlined.FormatSize),
                        iconColor = SaltTheme.colors.text,
                        text = stringResource(R.string.font_scale),
                        selectedItem = fontScaleLabel(fontScale),
                        popupWidth = 160
                    ) {
                        SettingsPreferences.fontScaleOptions.forEach { option ->
                            PopupMenuItem(
                                onClick = {
                                    scope.launch {
                                        SettingsPreferences.changeFontScale(option)
                                    }
                                    fontScalePopupMenuState.dismiss()
                                },
                                selected = fontScale == option,
                                text = fontScaleLabel(option),
                                iconColor = SaltTheme.colors.text
                            )
                        }
                    }

                    ItemSwitcher(
                        state = autoLinkEnabled,
                        onChange = { checked ->
                            scope.launch {
                                SettingsPreferences.changeAutoLink(checked)
                            }
                        },
                        text = stringResource(R.string.auto_link_switcher_text),
                        sub = stringResource(R.string.auto_link_switcher_sub),
                        iconPainter = rememberVectorPainter(Icons.Outlined.Link),
                        iconColor = SaltTheme.colors.text,
                    )
                }
            }

            item {
                RoundedColumn {
                    ItemTitle(text = stringResource(R.string.safe))
                    ItemSwitcher(
                        state = biometricAuthState,
                        iconPainter = rememberVectorPainter(Icons.Outlined.Fingerprint),
                        iconColor = SaltTheme.colors.text,
                        onChange = {
                            settingsViewModel.showBiometricPrompt(context as MainActivity)
                        },
                        text = R.string.biometric.str
                    )
                    Item(
                        onClick = {
                            navController.navigate(Screen.DataManager)
                        },
                        text = R.string.local_data_manager.str,
                        iconPainter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_database))
                    )

                    Item(
                        onClick = {
                            navController.navigate(Screen.RecycleBin)
                        },
                        text = R.string.recycle_bin.str,
                        iconPainter = rememberVectorPainter(Icons.Outlined.DeleteOutline)
                    )

                    Item(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                isSuccess = false
                                dataViewModel.fixTag()
                                isSuccess = true
                                isLoading = false
                            }
                        },
                        text = R.string.tag_fix.str,
                        iconPainter = rememberVectorPainter(Icons.Outlined.Label),
                    )

                    ItemSwitcher(
                        text = R.string.lan_share.str,
                        sub = if (isHttpServerRunning) "http://$serverIp:8080" else R.string.lan_share_sub.str,
                        state = isHttpServerRunning,
                        iconPainter = rememberVectorPainter(Icons.Outlined.Wifi),
                        iconColor = SaltTheme.colors.text,
                        onChange = {
                            if (it) {
                                HttpServer.start(noteState.notes)
                                serverIp = HttpServer.getIpAddress()
                            } else {
                                HttpServer.stop()
                            }
                            isHttpServerRunning = HttpServer.isRunning()
                        }
                    )

                }
            }

            item {
                RoundedColumn {
                    ItemTitle(text = stringResource(R.string.other))
                    Item(
                        onClick = {
                            navController.navigate(Screen.DonatePage)
                        },
                        text = R.string.donate_app.str,
                        iconPainter = rememberVectorPainter(Icons.Outlined.LocalCafe),
                    )
                    Item(
                        onClick = {
                            DonateUtils.openGooglePlay(
                                context
                            )
                        },
                        text = R.string.new_version.str,
                        iconPainter = rememberVectorPainter(Icons.Outlined.Download),
                    )
                    Item(
                        onClick = {
                            context.openUrl("https://xhslink.com/m/6eJ9xE368Ja")
                        },
                        text = R.string.xiaohongshu.str,
                        iconPainter = painterResource(id = R.drawable.ic_xiaohongshu),
                    )
                    Item(
                        onClick = {
                            navController.navigate(Screen.MoreInfo) { launchSingleTop = true }
                        },
                        text = stringResource(id = R.string.other),
                        iconPainter = rememberVectorPainter(image = Icons.Outlined.Info),
                        iconColor = SaltTheme.colors.text,
                        iconPaddingValues = PaddingValues(all = 1.5.dp)
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        })
}

@Composable
private fun fontScaleLabel(percent: Int): String = when (percent) {
    85 -> stringResource(R.string.font_scale_small)
    115 -> stringResource(R.string.font_scale_large)
    130 -> stringResource(R.string.font_scale_extra_large)
    else -> stringResource(R.string.font_scale_standard)
}

@Composable
fun SettingsHeadLayout() {
    val noteState = LocalMemosState.current
    val memos = noteState.notes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(SaltTheme.colors.subBackground, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val daysCount = memos.map { it.note.createTime.toYYMMDD() }.toSet().size
        val notesCount = memos.size
        val charactersCount = memos.fastSumBy { it.note.noteTitle?.length ?: 0 + it.note.content.length }
        val mediaCount = memos.fastSumBy { it.note.attachments.size }

        StatItem(R.string.dyas.str, daysCount.toString(), Modifier.weight(1f))
        StatItem(R.string.all_note.str, notesCount.toString(), Modifier.weight(1f))
        StatItem(R.string.characters.str, charactersCount.toString(), Modifier.weight(1f))
        StatItem(R.string.picture.str, mediaCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = SaltTheme.textStyles.sub.copy(
                fontSize = 12.sp,
                color = SaltTheme.colors.subText,
                fontWeight = FontWeight.Normal
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = SaltTheme.textStyles.main.copy(
                fontSize = 18.sp,
                color = SaltTheme.colors.text,
                fontWeight = FontWeight.Bold
            )
        )
    }
}