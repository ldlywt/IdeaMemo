package com.ldlywt.note.ui.page.data

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.ldlywt.note.App
import com.ldlywt.note.backup.SyncManager
import com.ldlywt.note.backup.model.DavData
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.db.repo.TagNoteRepo
import com.ldlywt.note.getAppName
import com.ldlywt.note.utils.BackUp
import com.ldlywt.note.utils.backUpFileName
import com.ldlywt.note.utils.withIO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


@HiltViewModel
class DataManagerViewModel @Inject constructor(
    private val tagNoteRepo: TagNoteRepo,
    private val syncManager: SyncManager
) : ViewModel() {

    suspend fun restoreForWebdav(): List<DavData> = withIO {
        val dataList = syncManager.listAllFile(getAppName() + "/")
            .filterNotNull()
            .filter { it.name.endsWith(".zip") }
            .sortedByDescending { it.name }
        dataList
    }

    suspend fun downloadFileByPath(davData: DavData): String? = withIO {
        val fileName = davData.name
        val destFile = File(App.instance.filesDir, "backup/$fileName")
        destFile.parentFile?.mkdirs()
        val result = syncManager.downloadFileByPath(
            davData.path.substringAfterLast("/dav/"),
            destFile.parentFile!!.absolutePath
        )
        if (result != null) destFile.absolutePath else null
    }


    suspend fun exportToWebdav(context: Context, notes: List<NoteShowBean>): String = withIO {
        val file = generateZipFile(context, backUpFileName, notes)
        // 2. 上传到 WebDAV
        val resultStr = syncManager.uploadFile(generateBackupFileName(), getAppName(), file)
        // 3. 上传成功或失败后，可以根据需要决定是否删除私有目录文件
        // 这里的逻辑是保留在私有目录，或者你可以选择删除
        if (resultStr.startsWith("Success")) {
            file.delete()
        }
        resultStr
    }

    fun generateBackupFileName(): String {
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH")
        val currentTime = java.time.LocalDateTime.now()
        return "IdeaMemoHtml${dateFormatter.format(currentTime)}.zip"
    }

    private suspend fun generateZipFile(
        context: Context,
        fileName: String,
        notes: List<NoteShowBean>
    ): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        val uri = FileProvider.getUriForFile(context, "com.ldlywt.note.provider", file)
        // 1. 先导出到私有目录
        BackUp.exportHtmlZip(notes, uri)
        file
    }


    suspend fun fixTag() = withContext(Dispatchers.IO) {
        val dataList: List<Note> = tagNoteRepo.queryAllNoteList()
        dataList.forEach(tagNoteRepo::insertOrUpdate)
        tagNoteRepo.queryAllTagList().forEach { tag ->
            val count = tagNoteRepo.countNoteListWithByTag(tag.tag)
            tag.count = count
            tagNoteRepo.updateTag(tag)
        }
        // 对应有效笔记数为 0 的标签直接删除
        tagNoteRepo.recountAllTagsAndCleanup()
    }
}
