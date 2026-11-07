package com.ldlywt.note.ui.page.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ldlywt.note.R
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.component.RYScaffold
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.ldlywt.note.utils.SettingsPreferences
import com.ldlywt.note.utils.lunchIo
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toTime
import com.moriafly.salt.ui.SaltTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@ExperimentalComposeUiApi
@Composable
fun SharePage(noteId: Long, navController: NavHostController) {

    val context = LocalContext.current
    val noteViewModel = LocalMemosViewModel.current
    val autoLinkEnabled by SettingsPreferences.autoLinkEnabled.collectAsState(true)
    val noteShowBean = remember { mutableStateOf<NoteShowBean?>(null) }
    val captureView = remember { mutableStateOf<View?>(null) }
    val scrollState = remember { ScrollState(0) }
    val imagesLoaded = remember { mutableStateOf(true) } // 跟踪图片是否加载完成
    val totalImages = remember { mutableStateOf(0) }
    val loadedImages = remember { mutableStateOf(0) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) } // 改为状态变量
    val isCapturing = remember { mutableStateOf(false) } // 添加截图状态

    LaunchedEffect(Unit) {
        lunchIo {
            val queriedNote = noteViewModel.getNoteShowBeanById(noteId)
            noteShowBean.value = queriedNote
            totalImages.value = queriedNote?.note?.attachments?.size ?: 0
            if (totalImages.value == 0) {
                imagesLoaded.value = true
            } else {
                imagesLoaded.value = false
                loadedImages.value = 0
            }
        }
    }

    // 当图片加载数量达到总数时，标记所有图片已加载完成
    LaunchedEffect(loadedImages.value, totalImages.value) {
        if (loadedImages.value >= totalImages.value && totalImages.value > 0) {
            imagesLoaded.value = true
            captureView.value?.let { view ->
                isCapturing.value = true
                bitmap.value = captureFullView(view, context)
                isCapturing.value = false
            }
        }
    }

    // 处理没有图片的情况
    LaunchedEffect(imagesLoaded.value, noteShowBean.value) {
        if (imagesLoaded.value && totalImages.value == 0 && noteShowBean.value != null && bitmap.value == null && !isCapturing.value) {
            captureView.value?.let { view ->
                isCapturing.value = true
                bitmap.value = captureFullView(view, context)
                isCapturing.value = false
            }
        }
    }

    RYScaffold(title = R.string.share.str, navController = navController, actions = {
        IconButton(
            onClick = {
                if (!imagesLoaded.value || isCapturing.value) {
                    // 图片还在加载中或正在截图，可以显示提示或延迟截图
                    return@IconButton
                }

                // 如果bitmap为空，尝试重新生成
                if (bitmap.value == null) {
                    captureView.value?.let { view ->
                        isCapturing.value = true
                        bitmap.value = captureFullView(view, context)
                        isCapturing.value = false
                    }
                }

                // 再次检查bitmap是否可用
                bitmap.value?.let {
                    shareImage(context, saveBitmapToFile(context, it))
                }
            },
            content = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    tint = SaltTheme.colors.text,
                    contentDescription = R.string.share.str
                )
            })
    }) {
        Column(
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            // 使用AndroidView包装内容，以便获取完整尺寸
            AndroidView(factory = {
                ComposeView(it).apply {
                    // 设置布局参数，确保宽度匹配屏幕
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setContent {
                        noteShowBean.value?.let { noteBean ->
                            Column(modifier = Modifier.background(SaltTheme.colors.background)) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SaltTheme.colors.popup),
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth() // 确保内容填充卡片宽度
                                            .padding(12.dp)
                                    ) {
                                        val note = noteBean.note
                                        Text(
                                            modifier = Modifier.padding(start = 2.dp),
                                            text = note.createTime.toTime(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        if (!note.noteTitle.isNullOrBlank()) {
                                            Text(
                                                modifier = Modifier.padding(start = 2.dp, top = 4.dp),
                                                text = note.noteTitle!!,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 18.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                                ),
                                                color = SaltTheme.colors.text,
                                            )
                                        }
                                        MarkdownText(
                                            markdown = note.content,
                                            style = SaltTheme.textStyles.paragraph.copy(fontSize = 15.sp, lineHeight = 24.sp),
                                            modifier = Modifier.fillMaxWidth(), // 确保MarkdownText填充可用宽度
                                            autoLink = autoLinkEnabled,
                                        )
                                        if (note.attachments.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            // 自定义图片显示组件，支持图片加载完成通知
                                            CustomImageCard(
                                                note = note,
                                                onImageLoaded = {
                                                    loadedImages.value++
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "By IdeaMemo",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Cursive),
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp)) // 增加内部底部间距
                            }
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), update = {
                // 保存View引用以便后续截图
                captureView.value = it
            })
            Spacer(modifier = Modifier.height(120.dp)) // 增加外部底部间距，确保所有内容都能完整显示
        }
    }
}

// 自定义图片卡片组件，支持图片加载完成通知
@Composable
fun CustomImageCard(note: Note, onImageLoaded: () -> Unit) {
    val context = LocalContext.current
    if (note.attachments.size == 1) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(note.attachments[0].path)
                .bitmapConfig(Bitmap.Config.ARGB_8888) // 强制使用软件位图
                .allowHardware(false) // 禁用硬件位图
                .allowRgb565(false) // 禁用RGB565格式
                .build(),
            contentDescription = null,
            modifier = Modifier
                .width(160.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            onSuccess = { onImageLoaded() }
        )
    } else {
        Row(
            modifier = Modifier
                .height(90.dp)
                .padding(end = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            note.attachments.forEachIndexed { index, attachment ->
                val path: String = attachment.path
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(path)
                        .bitmapConfig(Bitmap.Config.ARGB_8888) // 强制使用软件位图
                        .allowHardware(false) // 禁用硬件位图
                        .allowRgb565(false) // 禁用RGB565格式
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { onImageLoaded() }
                )
            }
        }
    }
}

// 捕获完整View内容的方法，确保图片宽度不超过屏幕宽度
fun captureFullView(view: View, context: Context): Bitmap? {
    // 获取屏幕宽度，减去左右边距
    val screenWidth = getScreenWidth(context)
    val padding = (16 * 2 * context.resources.displayMetrics.density).toInt() // 16dp * 2 转换为像素
    val contentWidth = screenWidth - 0

    // 使用屏幕宽度作为测量宽度，确保内容在测量时就会正确换行
    view.measure(
        View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)

    try {
        // 创建与测量尺寸相同的位图，确保使用软件渲染模式
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 绘制View内容
        view.draw(canvas)

        // 确保返回的是软件位图
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            // 如果是硬件位图，转换为软件位图
            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            softwareBitmap
        } else {
            bitmap
        }
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        // 如果绘制失败，尝试创建一个空的软件位图
        return try {
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        } catch (ex: OutOfMemoryError) {
            // 如果内存不足，尝试创建一个更小的位图
            Bitmap.createBitmap(
                (view.measuredWidth / 2).coerceAtLeast(1),
                (view.measuredHeight / 2).coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
        }
    }
}

// 获取屏幕宽度
fun getScreenWidth(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri {
    val imagesFolder = File(context.cacheDir, "shared_images")
    if (!imagesFolder.exists()) {
        imagesFolder.mkdirs()
    }

    val file = File(imagesFolder, "${System.currentTimeMillis()}.png")
    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)

    val stream: OutputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.flush()
    stream.close()

    return uri
}

fun shareImage(context: Context, imageUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Image"))
}