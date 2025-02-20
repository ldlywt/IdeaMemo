package com.ldlywt.note.backup

import android.content.Context
import android.util.Log
import androidx.lifecycle.asLiveData
import com.ldlywt.note.R
import com.ldlywt.note.backup.api.OnSyncResultListener
import com.ldlywt.note.backup.model.DavData
import com.ldlywt.note.utils.SharedPreferencesUtils
import com.ldlywt.note.utils.toast
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SyncManager(
    private val context: Context,
) {

    private suspend fun getSardine(): OkHttpSardine {
        val sardine = OkHttpSardine()
        sardine.setCredentials(SharedPreferencesUtils.davUserName.first(), SharedPreferencesUtils.davPassword.first(), true)
        return sardine
    }

    private fun ensureDirectoryExists(sardine: OkHttpSardine, dirUrl: String) {
        if (!sardine.exists(dirUrl)) {
            sardine.createDirectory(dirUrl)
        }
    }

    suspend fun uploadFile(fileName: String?, fileDir: String, localFile: File?): String = withContext(Dispatchers.IO) {

        try {
            val sardine = getSardine()
            val serverUrl = SharedPreferencesUtils.davServerUrl.first()
            ensureDirectoryExists(sardine, serverUrl + fileDir)
            val url = "$serverUrl$fileDir/$fileName"
            if (sardine.exists(url)) {
                sardine.delete(url)
            }
            sardine.put(url, localFile, "application/x-www-form-urlencoded")
            "Success：$fileDir/$fileName"
        } catch (e: IOException) {
            e.printStackTrace()
            e.message.toString()
        }
    }

    suspend fun checkConnection(url: String, account: String, pwd: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val sardine = OkHttpSardine()
        sardine.setCredentials(account, pwd, true)
        return@withContext try {
            sardine.exists(url)
            SharedPreferencesUtils.updateDavServerUrl(url)
            SharedPreferencesUtils.updateDavUserName(account)
            SharedPreferencesUtils.updateDavPassword(pwd)
            SharedPreferencesUtils.updateDavLoginSuccess(true)
            Pair(true, context.getString(R.string.webdav_config_success))
        } catch (e: SardineException) {
            e.printStackTrace()
            SharedPreferencesUtils.clearDavConfig()
            Pair(false, e.message.toString())
        }
    }

    suspend fun uploadString(fileName: String?, fileLoc: String?, content: String?, listener: OnSyncResultListener?) = withContext(Dispatchers.IO) {

        try {
            val sardine = getSardine()
            val davServerUrl = SharedPreferencesUtils.davServerUrl.first()
            ensureDirectoryExists(sardine, davServerUrl + fileLoc)
            val data = content!!.toByteArray()
            sardine.put("$davServerUrl$fileLoc/$fileName", data)
            listener!!.onSuccess("$fileLoc/$fileName,上传成功")
        } catch (e: IOException) {
            e.printStackTrace()
            listener!!.onError("出错了$e")
        }
    }

    suspend fun downloadFileByPath(webPath: String, localDir: String): String? = withContext(Dispatchers.IO) {
        try {

            val davServerUrl = SharedPreferencesUtils.davServerUrl.first()
            val sardine = getSardine()
            val fileName = webPath.substringAfterLast("/")
            val localPath = File(localDir, fileName).path
            Log.i("wutao", "downloadFileByPath: $davServerUrl$webPath")
            sardine.get(davServerUrl + webPath).use { inputStream ->
                FileOutputStream(localPath).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        outputStream.flush()
                    }
                }
            }
            localPath
        } catch (e: Exception) {
            toast(e.message.toString())
            e.printStackTrace()
            null
        }
    }


    suspend fun downloadString(fileName: String?, fileLoc: String?, listener: OnSyncResultListener?) = withContext(Dispatchers.IO) {
        try {
            val davServerUrl = SharedPreferencesUtils.davServerUrl.first()
            val sardine = getSardine()
            val inputStream = sardine["$davServerUrl$fileLoc/$fileName"]
            //设置输入缓冲区
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)) // 实例化输入流，并获取网页代
            var s: String? // 依次循环，至到读的值为空
            val sb = StringBuilder()
            while ((reader.readLine().also { s = it }) != null) {
                sb.append(s)
            }
            reader.close()
            inputStream.close()
            val str = sb.toString()
            listener!!.onSuccess(str)
        } catch (e: IOException) {
            e.printStackTrace()
            listener!!.onError("出错了,$e")
        }

    }

    suspend fun listAllFile(dir: String?): List<DavData?> = withContext(Dispatchers.IO) {

        try {
            val davServerUrl = SharedPreferencesUtils.davServerUrl.asLiveData().value
            val sardine = getSardine()
            val resources = sardine.list(davServerUrl + dir) //如果是目录一定别忘记在后面加上一个斜杠
            val davData: MutableList<DavData> = ArrayList()
            for (i: DavResource in resources) {
                davData.add(DavData(i))
            }
            davData
        } catch (e: Exception) {
            e.printStackTrace()
            toast(e.message.toString())
            emptyList()
        }
    }

    suspend fun deleteFile(fileDir: String?, listener: OnSyncResultListener?) = withContext(Dispatchers.IO) {
        try {
            val davServerUrl = SharedPreferencesUtils.davServerUrl.asLiveData().value
            val sardine = getSardine()
            sardine.delete(davServerUrl + fileDir)
            listener!!.onSuccess("删除成功！")
        } catch (e: IOException) {
            e.printStackTrace()
            listener!!.onError("出错了,$e")
        }

    }
}