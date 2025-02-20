package com.ldlywt.note.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ldlywt.note.utils.BackUp
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.backUpFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @Inject
    lateinit var syncManager: SyncManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 执行数据备份逻辑
        // 这里是每天备份数据的具体操作

        SharedPreferencesUtils.localBackupUri.first()?.let {
            val uri = Uri.parse(it)
            val folder = requireNotNull(DocumentFile.fromTreeUri(context, uri))
            val file = requireNotNull(folder.createFile("application/zip", "Auto".plus(backUpFileName)))
            BackUp.exportEncrypted(context, file.uri)
            Result.success()
        } ?: Result.failure()
    }
}
