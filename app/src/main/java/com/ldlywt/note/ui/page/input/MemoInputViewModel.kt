package com.ldlywt.note.ui.page.input

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.db.repo.TagNoteRepo
import com.ldlywt.note.utils.deleteAttachmentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(private val tagNoteRepo: TagNoteRepo) : ViewModel() {

    var uploadAttachments = mutableStateListOf<Attachment>()
        private set

    /** 已保存到数据库的附件路径。编辑时移除这些附件不立即删文件，取消编辑后仍可恢复。 */
    private val persistedPaths = mutableSetOf<String>()

    /** 编辑已有笔记时初始化附件列表。 */
    fun setAttachments(attachments: List<Attachment>) {
        uploadAttachments.clear()
        uploadAttachments.addAll(attachments)
        persistedPaths.clear()
        persistedPaths.addAll(attachments.map { it.path })
    }

    fun deleteResource(path: String) {
        uploadAttachments.removeAll { it.path == path }
        // 只有本次新选、尚未保存的附件才能立即删除文件
        if (path !in persistedPaths) {
            deleteAttachmentFile(path)
        }
    }

    /** 保存或放弃草稿后清空，避免退出时误删已保存的附件。 */
    fun clearAttachments() {
        uploadAttachments.clear()
        persistedPaths.clear()
    }

    override fun onCleared() {
        // 页面退出时清理本次新选但没有保存的附件
        uploadAttachments
            .filter { it.path !in persistedPaths }
            .forEach { deleteAttachmentFile(it.path) }
        super.onCleared()
    }
}
