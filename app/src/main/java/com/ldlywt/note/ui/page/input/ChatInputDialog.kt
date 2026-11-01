package com.ldlywt.note.ui.page.input

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.ldlywt.note.R
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.bean.Tag
import com.ldlywt.note.component.PIconButton
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.ui.page.LocalTags
import com.ldlywt.note.utils.handlePickFiles
import com.ldlywt.note.utils.toast
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatInputDialog(
    isShow: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    parentNote: NoteShowBean? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    dismiss: () -> Unit
) {
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf<String?>(null) }
    var photoImageUri by remember { mutableStateOf<Uri?>(null) }
    val tagList = LocalTags.current.filterNot { it.isCityTag }
    val memosViewModel = LocalMemosViewModel.current
    val memoInputViewModel = hiltViewModel<MemoInputViewModel>()

    var isFocused by remember { mutableStateOf(false) }
    val isActive by remember {
        derivedStateOf { isShow || isFocused || text.text.isNotEmpty() || memoInputViewModel.uploadAttachments.isNotEmpty() }
    }
    val isInteracting by remember {
        derivedStateOf { isShow || isFocused }
    }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var lastImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isInteracting) {
        onExpandedChange(isInteracting)
    }

    LaunchedEffect(isImeVisible) {
        if (lastImeVisible && !isImeVisible && isFocused) {
            focusManager.clearFocus()
            dismiss()
        }
        lastImeVisible = isImeVisible
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoImageUri?.let { uri ->
                coroutineScope.launch {
                    handlePickFiles(setOf(uri)) { list ->
                        memoInputViewModel.uploadAttachments.addAll(list)
                    }
                }
            }
        }
    }

    val pickMultipleMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        coroutineScope.launch {
            handlePickFiles(uris.toSet()) { list ->
                memoInputViewModel.uploadAttachments.addAll(list)
            }
        }
    }

    fun submit() = coroutineScope.launch {
        val content = text.text
        if (content.isNotBlank() || memoInputViewModel.uploadAttachments.isNotEmpty()) {
            memosViewModel.insertOrUpdate(Note(content = content, attachments = memoInputViewModel.uploadAttachments.toList(), parentNoteId = parentNote?.note?.noteId))
            text = TextFieldValue("")
            memoInputViewModel.clearAttachments()
        }
        focusManager.clearFocus()
        softwareKeyboardController?.hide()
        dismiss()
    }

    LaunchedEffect(isShow) {
        if (isShow) {
            focusRequester.requestFocus()
            softwareKeyboardController?.show()
        }
    }

    // 处理返回键：一步到位清除焦点、隐藏键盘并收起状态
    BackHandler(enabled = isActive || isFocused) {
        focusManager.clearFocus()
        softwareKeyboardController?.hide()
        dismiss()
    }

    @Composable
    fun TagButton(tagList: List<Tag>) {
        val filteredTags = remember(tagList, tagSearchQuery) {
            if (tagSearchQuery == null) tagList else tagList.filter { it.tag.contains(tagSearchQuery!!, ignoreCase = true) }
        }

        fun insertTagText(tagContent: String) {
            val newText = text.text.replaceRange(text.selection.min, text.selection.max, tagContent)
            text = text.copy(newText, TextRange(text.selection.min + tagContent.length))
        }

        fun replaceTagText(tagContent: String) {
            val cursorPos = text.selection.start
            val textBeforeCursor = text.text.substring(0, cursorPos)
            val lastHashIndex = textBeforeCursor.lastIndexOf('#')
            if (lastHashIndex != -1) {
                val replacement = "#${tagContent.removePrefix("#")} "
                val newText = text.text.replaceRange(lastHashIndex, cursorPos, replacement)
                text = text.copy(newText, TextRange(lastHashIndex + replacement.length))
            }
        }

        val tagIcon = if (tagList.isEmpty()) Icons.Filled.Tag else Icons.Outlined.Tag
        PIconButton(imageVector = tagIcon, contentDescription = stringResource(R.string.tag)) {
            val cursorPos = text.selection.start
            val textBeforeCursor = text.text.substring(0, cursorPos)
            val lastHashIndex = textBeforeCursor.lastIndexOf('#')
            val fragment = if (lastHashIndex != -1) textBeforeCursor.substring(lastHashIndex + 1) else null
            if (fragment != null && !fragment.contains(" ") && !fragment.contains("\n")) {
                tagSearchQuery = fragment
                tagMenuExpanded = !tagMenuExpanded
            } else {
                insertTagText("#")
                tagSearchQuery = ""
                tagMenuExpanded = tagList.isNotEmpty()
            }
        }

        if (filteredTags.isNotEmpty() && tagMenuExpanded) {
            Box {
                DropdownMenu(
                    modifier = Modifier.wrapContentHeight().heightIn(max = 400.dp),
                    expanded = tagMenuExpanded,
                    onDismissRequest = { tagMenuExpanded = false; tagSearchQuery = null },
                    properties = PopupProperties(focusable = false)
                ) {
                    filteredTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag.tag) },
                            onClick = {
                                if (tagSearchQuery != null) replaceTagText(tag.tag) else insertTagText("#${tag.tag.removePrefix("#")} ")
                                tagMenuExpanded = false
                                tagSearchQuery = null
                            },
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 24.dp) // 增加上边缘阴影
            .background(color = SaltTheme.colors.background) // 移除背景圆角以贴合边缘
            .border(width = 1.dp, color = SaltTheme.colors.subText.copy(alpha = 0.05f))
            .imePadding() // 核心：移除 animateContentSize 动画，实现零延迟随键盘升起
            .navigationBarsPadding()
    ) {
        // 1. 引用回复内容 (活跃状态且有引用时显示)
        if (isActive && parentNote != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                    .background(SaltTheme.colors.subBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, SaltTheme.colors.subText.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = parentNote.note.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SaltTheme.textStyles.paragraph.copy(fontSize = 12.sp, color = SaltTheme.colors.subText)
                )
            }
        }

        // 2. 输入框区域
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { it: TextFieldValue ->
                    text = it
                    // 标签自动提示逻辑
                    val cursor = it.selection.start
                    val textBefore = it.text.substring(0, cursor)
                    val lastHash = textBefore.lastIndexOf('#')
                    if (lastHash != -1) {
                        val fragment = textBefore.substring(lastHash + 1)
                        if (fragment.contains(" ") || fragment.contains("\n")) {
                            tagMenuExpanded = false
                            tagSearchQuery = null
                        } else {
                            tagSearchQuery = fragment
                            tagMenuExpanded = tagList.any { it.tag.contains(fragment, ignoreCase = true) }
                        }
                    } else {
                        tagMenuExpanded = false
                        tagSearchQuery = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .heightIn(min = 44.dp, max = 200.dp)
                    .onPreviewKeyEvent { 
                        // 拦截返回键事件，一步到位关闭：在 KeyDown 时直接消费并执行关闭逻辑
                        if (it.key == Key.Back) {
                            if (isFocused || isActive) {
                                if (it.type == KeyEventType.KeyDown) {
                                    focusManager.clearFocus()
                                    softwareKeyboardController?.hide()
                                    dismiss()
                                }
                                true
                            } else false
                        } else false
                    },
                textStyle = SaltTheme.textStyles.paragraph.copy(fontSize = 15.sp),
                placeholder = { Text(stringResource(R.string.any_thoughts), color = SaltTheme.colors.subText, fontSize = 15.sp) },
                shape = RoundedCornerShape(6.dp), // 修改圆角为 6dp
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SaltTheme.colors.subBackground,
                    unfocusedContainerColor = SaltTheme.colors.subBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = SaltTheme.colors.highlight,
                    focusedTextColor = SaltTheme.colors.text,
                    unfocusedTextColor = SaltTheme.colors.text
                ),
                maxLines = 10,
                keyboardOptions = keyboardOptions
            )
        }

        // 3. 附件预览
        if (isActive && memoInputViewModel.uploadAttachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.height(80.dp).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(memoInputViewModel.uploadAttachments.toList(), key = { it.path }) { resource ->
                    InputImage(attachment = resource, isEdit = true, delete = { pat ->
                        memoInputViewModel.deleteResource(pat)
                    })
                }
            }
        }

        // 4. 操作栏：默认始终展示核心按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 核心功能按钮默认显示
                TagButton(tagList)
                PIconButton(imageVector = Icons.Outlined.Image, contentDescription = stringResource(R.string.add_image)) {
                    pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                PIconButton(imageVector = Icons.Outlined.PhotoCamera, contentDescription = stringResource(R.string.take_photo)) {
                    try {
                        val imagesFolder = File(context.cacheDir, "capture_picture").apply { if (!exists()) mkdirs() }
                        val file = File.createTempFile("capture_picture_", ".jpg", imagesFolder)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        photoImageUri = uri
                        takePhoto.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        toast(e.localizedMessage ?: "Unable to take picture.")
                    }
                }
            }

            // 发送按钮
            PIconButton(
                imageVector = Icons.Outlined.Send,
                contentDescription = stringResource(R.string.send),
                tint = if (text.text.isNotEmpty() || memoInputViewModel.uploadAttachments.isNotEmpty()) SaltTheme.colors.highlight else SaltTheme.colors.subText,
                onClick = { if (text.text.isNotEmpty() || memoInputViewModel.uploadAttachments.isNotEmpty()) submit() else focusRequester.requestFocus() }
            )
        }
    }
}
