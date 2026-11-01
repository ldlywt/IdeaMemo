package com.ldlywt.note.utils

import android.content.Context
import android.net.Uri
import com.ldlywt.note.App
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun File.newName(): String {
    var index = 1
    var candidate: String
    val split = nameWithoutExtension.split(' ').toMutableList()
    val last = split.last()
    if ("""^\(\d+\)$""".toRegex().matches(last)) {
        split.removeAt(split.lastIndex)
    }
    val name = split.joinToString(" ")
    while (true) {
        candidate = if (extension.isEmpty()) "$name ($index)" else "$name ($index).$extension"
        if (!File("$parent/$candidate").exists()) {
            return candidate
        }
        index++
    }
}

fun File.newPath(): String {
    return "$parent/" + newName()
}

/**
 * 删除附件文件。只允许删除应用私有目录下的文件，避免误删用户外部文件。
 */
fun deleteAttachmentFile(path: String) {
    if (path.isBlank()) return
    try {
        val file = File(path)
        if (!file.exists()) return
        val app = App.instance
        val appDirs = listOfNotNull(
            app.getExternalFilesDir(null),
            app.filesDir,
            app.cacheDir,
        )
        val canonicalFile = file.canonicalFile
        if (appDirs.any { canonicalFile.startsWith(it.canonicalFile) }) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun copyFile(context: Context, pathFrom: Uri, pathTo: String) {
    context.contentResolver.openInputStream(pathFrom).use { input ->
        var bis: BufferedInputStream? = null
        var bos: BufferedOutputStream? = null

        try {
            bis = BufferedInputStream(input)
            bos = BufferedOutputStream(FileOutputStream(pathTo, false))
            val buf = ByteArray(1024)
            bis.read(buf)
            do {
                bos.write(buf)
            } while (bis.read(buf) != -1)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                bis?.close()
                bos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}