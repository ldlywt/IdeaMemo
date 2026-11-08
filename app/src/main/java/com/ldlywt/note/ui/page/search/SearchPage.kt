package com.ldlywt.note.ui.page.search

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.component.NoteCard
import com.ldlywt.note.component.NoteCardFrom
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.delay

/** 搜索历史最多展示的行数。 */
private const val HISTORY_MAX_ROWS = 4

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    navController: NavHostController,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    // 进入搜索页即展开，直接显示光标、键盘与搜索历史
    var searchBarExpanded by rememberSaveable { mutableStateOf(true) }
    val searchQuery by searchViewModel.query.collectAsState()
    val searchHistory by searchViewModel.searchHistory.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val filterList by searchViewModel.dataFlow.collectAsState(initial = emptyList())

    Box(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = {
                        searchViewModel.onQuery(it)
                        // 输入内容时展开搜索面板，用于展示搜索历史
                        if (!searchBarExpanded) searchBarExpanded = true
                    },
                    onSearch = {
                        searchBarExpanded = true
                        searchViewModel.onSearch(it)
                        keyboardController?.hide()
                    },
                    expanded = searchBarExpanded,
                    onExpandedChange = { },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.search_hint),
                            color = SaltTheme.colors.subText
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = { navController.debouncedPopBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.back),
                                tint = SaltTheme.colors.text
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                searchViewModel.clearSearchQuery()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "",
                                    tint = SaltTheme.colors.text
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = SaltTheme.colors.text,
                        unfocusedTextColor = SaltTheme.colors.text,
                        cursorColor = SaltTheme.colors.text,
                        focusedContainerColor = SaltTheme.colors.subBackground,
                        unfocusedContainerColor = SaltTheme.colors.subBackground
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .focusRequester(focusRequester),
            expanded = searchBarExpanded,
            onExpandedChange = { if (!it) navController.debouncedPopBackStack() },
            colors = SearchBarDefaults.colors(
                containerColor = SaltTheme.colors.subBackground,
                dividerColor = SaltTheme.colors.stroke
            )
        ) {
            LazyColumn(
                Modifier
                    .background(SaltTheme.colors.background)
                    .fillMaxSize()
                    .padding()
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (searchQuery.isBlank()) {
                    item {
                        SearchHistorySection(
                            history = searchHistory,
                            onSearch = { keyword ->
                                searchViewModel.onQuery(keyword)
                                searchViewModel.onSearch(keyword)
                                keyboardController?.hide()
                            },
                            onRemove = searchViewModel::removeSearchHistory,
                            onClear = searchViewModel::clearSearchHistory
                        )
                    }
                } else {
                    items(count = filterList.size, key = { index -> filterList[index].note.noteId }) { index ->
                        NoteCard(
                            noteShowBean = filterList[index],
                            navHostController = navController,
                            from = NoteCardFrom.SEARCH,
                            highlightText = searchQuery // 传入搜索关键词以实现高亮
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onSearch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    if (history.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.search_history),
                color = SaltTheme.colors.subText,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(id = R.string.clear),
                color = SaltTheme.colors.subText,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClear() }
                    .padding(4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SearchHistoryChips(
            history = history,
            onSearch = onSearch,
            onRemove = onRemove
        )
    }
}

/**
 * 搜索历史胶囊，流式布局，最多展示 [HISTORY_MAX_ROWS] 行。
 */
@Composable
private fun SearchHistoryChips(
    history: List<String>,
    onSearch: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        // 测量用的文字样式需要和胶囊里的 Text 保持一致，保证行数计算准确
        val chipTextStyle = SaltTheme.textStyles.main.copy(fontSize = 14.sp)
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val rows = remember(history, maxWidthPx, chipTextStyle) {
            val chipHorizontalPadding = with(density) { 10.dp.roundToPx() } * 2
            val chipIconSpace = with(density) { (14.dp + 2.dp).roundToPx() }
            val spacing = with(density) { 8.dp.roundToPx() }
            val indices = mutableListOf<MutableList<Int>>()
            var currentRow: MutableList<Int>? = null
            var currentWidth = 0
            history.forEachIndexed { index, keyword ->
                val textWidth =
                    textMeasurer.measure(keyword, chipTextStyle, maxLines = 1).size.width
                val chipWidth = textWidth + chipHorizontalPadding + chipIconSpace
                if (currentRow == null) {
                    currentRow = mutableListOf(index)
                    currentWidth = chipWidth
                } else if (currentWidth + spacing + chipWidth <= maxWidthPx) {
                    currentRow!!.add(index)
                    currentWidth += spacing + chipWidth
                } else {
                    indices.add(currentRow!!)
                    currentRow = mutableListOf(index)
                    currentWidth = chipWidth
                }
            }
            currentRow?.let { indices.add(it) }
            indices.take(HISTORY_MAX_ROWS)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { index ->
                        SearchHistoryChip(
                            keyword = history[index],
                            onSearch = onSearch,
                            onRemove = onRemove
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryChip(
    keyword: String,
    onSearch: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SaltTheme.colors.subBackground)
            .clickable { onSearch(keyword) }
            .padding(start = 10.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = keyword,
            color = SaltTheme.colors.text,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = SaltTheme.colors.subText,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove(keyword) }
        )
    }
}
