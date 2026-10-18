package com.ldlywt.note.ui.page

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ldlywt.note.R
import com.ldlywt.note.ui.page.router.debouncedPopBackStack
import com.ldlywt.note.utils.BlurTransformation
import com.ldlywt.note.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PictureDisplayPage(
    pathList: List<String>, curIndex: Int, timestamps: List<Long>,
    navController: NavHostController
) {
    val pagerState = rememberPagerState(pageCount = { pathList.size }, initialPage = curIndex)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box {
        HorizontalPager(state = pagerState) { page ->
            Surface(modifier = Modifier.fillMaxSize()) {
                DetailContent(
                    imgUrl = pathList[page],
                    requestImage = {
                        Image(pathList[page])
                    }
                )
            }
        }
        IconButton(
            onClick = { navController.debouncedPopBackStack() },
            modifier = Modifier.padding(start = 12.dp, top = 24.dp, end = 0.dp, bottom = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFFFFFFFF)
            )
        }
        // Page indicator in top right corner
        Box(
            modifier = Modifier
                .padding(top = 24.dp, end = 12.dp)
                .align(Alignment.TopEnd)
                .wrapContentSize()
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${pathList.size}",
                color = Color(0xFFFFFFFF),
                fontSize = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
        // Bottom bar containing time display (left) and save button (right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 12.dp, end = 12.dp)
                .align(Alignment.BottomStart)
        ) {
            val safeIndex = min(pagerState.currentPage, timestamps.size - 1)
            val timestamp = if (timestamps.isNotEmpty()) timestamps[safeIndex] else System.currentTimeMillis()
            Text(
                text = dateFormat.format(Date(timestamp)),
                color = Color(0xFFFFFFFF),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp)
            )

            IconButton(
                onClick = {
                    val currentPath = pathList.getOrNull(pagerState.currentPage) ?: return@IconButton
                    coroutineScope.launch {
                        val success = saveImageToGallery(context, currentPath)
                        if (success) {
                            toast(context.getString(R.string.save_image_success))
                        } else {
                            toast(context.getString(R.string.save_image_failed))
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = stringResource(R.string.save_image),
                    tint = Color(0xFFFFFFFF)
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    imgUrl: String?,
    modifier: Modifier = Modifier,
    requestImage: @Composable () -> Unit
) {
    Surface {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imgUrl)
                .transformations(
                    BlurTransformation(
                        LocalContext.current,
                        radius = 25f,
                        sampling = 5f
                    )
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop,
        )

        requestImage()
    }
}

@Composable
private fun Image(imgUrl: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imgUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
    )
}

private suspend fun saveImageToGallery(context: Context, imgUrl: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val fileName = "IdeaMemo_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IdeaMemo")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext false

        var success = false

        val localFile = File(imgUrl)
        if (localFile.exists()) {
            resolver.openOutputStream(imageUri)?.use { output ->
                localFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            success = true
        } else {
            val inputUri = try {
                Uri.parse(imgUrl)
            } catch (e: Exception) {
                null
            }
            if (inputUri != null && (inputUri.scheme == "content" || inputUri.scheme == "file")) {
                try {
                    resolver.openInputStream(inputUri)?.use { input ->
                        resolver.openOutputStream(imageUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    success = true
                } catch (e: Exception) {
                    success = false
                }
            }

            if (!success) {
                val imageLoader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(imgUrl)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        ?: result.drawable.toBitmap()
                    resolver.openOutputStream(imageUri)?.use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    }
                    success = true
                }
            }
        }

        if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        } else if (!success) {
            resolver.delete(imageUri, null, null)
        }

        success
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
