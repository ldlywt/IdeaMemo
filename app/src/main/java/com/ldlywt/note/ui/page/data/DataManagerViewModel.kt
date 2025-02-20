package com.ldlywt.note.ui.page.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.ldlywt.note.App
import com.ldlywt.note.backup.SyncManager
import com.ldlywt.note.backup.model.DavData
import com.ldlywt.note.bean.Note
import com.ldlywt.note.db.repo.TagNoteRepo
import com.ldlywt.note.getAppName
import com.ldlywt.note.utils.BackUp
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.backUpFileName
import com.ldlywt.note.utils.withIO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


@HiltViewModel
class DataManagerViewModel @Inject constructor(private val tagNoteRepo: TagNoteRepo, private val syncManager: SyncManager) : ViewModel() {

    suspend fun restoreForWebdav(): List<DavData> = withIO {
        val dataList = syncManager.listAllFile(getAppName() + "/").filterNotNull().filter { it.name.endsWith(".zip") }.sortedByDescending { it.name }
        dataList
    }

    suspend fun downloadFileByPath(davData: DavData): String? = withIO {
        syncManager.downloadFileByPath(davData.path.substringAfterLast("/dav/"), App.instance.cacheDir.absolutePath)
    }


    val isLogin = SharedPreferencesUtils.davLoginSuccess.asLiveData().value?:false



    suspend fun exportToWebdav(context: Context): String = withIO {
        val (filename, file, _) = generateZipFile(context, backUpFileName)
        val resultStr = syncManager.uploadFile(filename, getAppName(), file)
        if (resultStr.startsWith("Success")) {
            File(filename).delete()
        }
        resultStr
    }

    private suspend fun generateZipFile(context: Context, fileName: String): Triple<String, File, Uri> = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        val uri = FileProvider.getUriForFile(context, "com.ldlywt.note.provider", file)
        val fileName = BackUp.exportEncrypted(context, uri)
        Triple(fileName, file, uri)
    }

    suspend fun fixTag() = withContext(Dispatchers.IO) {
        val dataList: List<Note> = tagNoteRepo.queryAllNoteList()
        dataList.forEach(tagNoteRepo::insertOrUpdate)
        tagNoteRepo.queryAllTagList().forEach { tag ->
            val count = tagNoteRepo.countNoteListWithByTag(tag.tag)
            tag.count = count
            tagNoteRepo.updateTag(tag)
        }
    }

    suspend fun checkConnection(url: String, account: String, pwd: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        syncManager.checkConnection(url, account, pwd)
    }
}