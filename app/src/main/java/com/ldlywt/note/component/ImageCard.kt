package com.ldlywt.note.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.bean.Note
import com.ldlywt.note.ui.page.router.Screen

@Composable
fun ImageCard(note: Note, navHostController: NavHostController?) {
    val images = note.attachments.filter { it.type == Attachment.Type.IMAGE }
    if (images.isEmpty()) return

    if (images.size == 1) {
        AsyncImage(
            model = images[0].path,
            contentDescription = null,
            modifier = Modifier
                .width(160.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    navHostController?.navigate(Screen.PictureDisplay(arrayListOf(images[0].path), 0,  listOf(note.createTime )))
                },
            contentScale = ContentScale.Crop
        )
    } else {
        LazyRow(
            modifier = Modifier.height(90.dp).padding(end = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(count = images.size, key = { index -> images[index].path }) { index ->
                val path: String = images[index].path
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .zIndex(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            navHostController?.navigate(Screen.PictureDisplay(images.map { it.path }, index,  listOf(note.createTime )))
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}