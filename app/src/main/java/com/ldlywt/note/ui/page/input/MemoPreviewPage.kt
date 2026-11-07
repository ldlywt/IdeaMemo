package com.ldlywt.note.ui.page.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ldlywt.note.R
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.router.LocalRootNavController
import com.ldlywt.note.ui.page.router.Screen
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.ldlywt.note.utils.SettingsPreferences
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toTime
import com.moriafly.salt.ui.SaltTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoPreviewPage(memoId: Long) {
    val memosViewModel = LocalMemosViewModel.current
    val memo by remember(memoId) { memosViewModel.getNoteShowBeanByIdFlow(memoId) }.collectAsState(initial = null)
    val navController = LocalRootNavController.current
    val autoLinkEnabled by SettingsPreferences.autoLinkEnabled.collectAsState(true)

    if (memo == null) {
        return
    }

    val localDate = Instant.ofEpochMilli(memo?.note?.createTime ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SaltTheme.colors.background),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localDate.dayOfMonth.toString(),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold).copy(color = SaltTheme.colors.text)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = localDate.dayOfWeek.getDisplayName(
                                    TextStyle.SHORT, Locale.getDefault()
                                ), style = MaterialTheme.typography.bodySmall.copy(color = SaltTheme.colors.text), fontSize = 11.sp
                            )

                            Row {
                                Text(
                                    text = localDate.year.toString() + "/" + localDate.month.getDisplayName(
                                        TextStyle.SHORT, Locale.getDefault()
                                    ), style = MaterialTheme.typography.bodySmall.copy(color = SaltTheme.colors.text), fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.debouncedPopBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SaltTheme.colors.text)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Share(memoId))
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = R.string.share.str, tint = SaltTheme.colors.text)
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.InputDetail(memoId))
                    }) {
                        Icon(Icons.Outlined.Edit, contentDescription = R.string.edit.str, tint = SaltTheme.colors.text)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SaltTheme.colors.background)
                .verticalScroll(rememberScrollState())
        ) {
            val attachments = memo?.note?.attachments ?: emptyList()
            if (attachments.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { attachments.size })
                val configuration = LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight / 2)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = attachments[page].path,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    if (attachments.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${attachments.size}",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                val note = memo?.note
                if (!note?.noteTitle.isNullOrBlank()) {
                    Text(
                        text = note!!.noteTitle!!,
                        style = SaltTheme.textStyles.paragraph.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaltTheme.colors.text
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (note != null && note.updateTime > note.createTime) {
                    Text(
                        text = stringResource(R.string.updated_at, note.updateTime.toTime()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SaltTheme.colors.subText
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                MarkdownText(
                    markdown = note?.content ?: "",
                    style = SaltTheme.textStyles.paragraph.copy(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = SaltTheme.colors.text
                    ),
                    isTextSelectable = true,
                    autoLink = autoLinkEnabled,
                )
            }
        }
    }
}