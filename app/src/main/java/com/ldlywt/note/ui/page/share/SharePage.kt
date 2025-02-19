package com.ldlywt.note.ui.page.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.ldlywt.note.R
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.component.ImageCard
import com.ldlywt.note.component.RYScaffold
import com.ldlywt.note.component.locationAndTimeText
import com.ldlywt.note.component.showLocationInfoContent
import com.ldlywt.note.utils.lunchIo
import com.ldlywt.note.utils.str
import com.ldlywt.note.utils.toTime
import com.ldlywt.note.ui.page.LocalMemosViewModel
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.smarttoolfactory.screenshot.ScreenshotBox
import com.smarttoolfactory.screenshot.rememberScreenshotState
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@ExperimentalComposeUiApi
@Composable
fun SharePage(noteId: Long, navController: NavHostController) {

    val screenshotState = rememberScreenshotState()
    val context = LocalContext.current
    val noteViewModel = LocalMemosViewModel.current
    val noteShowBean = remember { mutableStateOf<NoteShowBean?>(null) }

    LaunchedEffect(Unit) {
        lunchIo {
            val queriedNote = noteViewModel.getNoteShowBeanById(noteId)
            noteShowBean.value = queriedNote
        }
    }

    RYScaffold(title = R.string.share.str, navController = navController, actions = {
        IconButton(onClick = {
            screenshotState.capture()
        }, content = {
            Icon(imageVector = Icons.AutoMirrored.Outlined.Send, tint = SaltTheme.colors.text, contentDescription = R.string.share.str)
        })
    }) {
        Column {
            ScreenshotBox(screenshotState = screenshotState) {
                noteShowBean.value?.let {
                    Column {
                        Spacer(modifier = Modifier.height(40.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SaltTheme.colors.popup),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                val note = it.note
                                MarkdownText(markdown = note.content, style = SaltTheme.textStyles.paragraph.copy(fontSize = 15.sp, lineHeight = 24.sp)){}
                                if (note.attachments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ImageCard(note, null)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                locationAndTimeText(note.createTime.toTime(), modifier = Modifier.padding(start = 2.dp))
                                showLocationInfoContent(note)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "By IdeaMemo",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Cursive),
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
            screenshotState.imageBitmap?.let {
                shareImage(context, saveBitmapToFile(context, it.asAndroidBitmap()))
            }
        }
    }

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
