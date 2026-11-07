package com.ldlywt.note.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.Keep
import androidx.documentfile.provider.DocumentFile
import com.ldlywt.note.App
import com.ldlywt.note.AppEntryPoint
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.bean.Tag
import dagger.hilt.android.EntryPointAccessors
import dalvik.system.ZipPathValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.zeroturnaround.zip.ZipUtil
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackUp {
    suspend fun exportTXTFile(list: List<NoteShowBean>, uri: Uri) = withContext(Dispatchers.IO) {
        val context = App.instance
        val root = DocumentFile.fromTreeUri(context, uri) ?: return@withContext
        val ideaMemoDir = root.findFile("IdeaMemo") ?: root.createDirectory("IdeaMemo") ?: root

        list.forEach { noteShowBean ->
            val title = noteShowBean.note.noteTitle
            val content = noteShowBean.note.content
            var fileName = if (!title.isNullOrBlank()) {
                title.trim().take(50)
            } else {
                val snippet = if (content.length > 20) content.take(20) else content
                snippet.trim().ifBlank { "note" }
            }
            fileName = fileName.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            val finalFileName = "${fileName}_${noteShowBean.note.createTime}.txt"

            val file = ideaMemoDir.createFile("text/plain", finalFileName) ?: return@forEach
            context.contentResolver.openOutputStream(file.uri)?.use { stream ->
                val writer = BufferedWriter(OutputStreamWriter(stream))
                writer.append("${noteShowBean.note.createTime.toTime()}\n")
                if (!title.isNullOrBlank()) {
                    writer.append("Title: ${title.trim()}\n")
                }
                if (!noteShowBean.note.locationInfo.isNullOrEmpty()) {
                    writer.append(noteShowBean.note.locationInfo.plus("\n"))
                }
                writer.append(noteShowBean.note.content)
                writer.close()
            }

            // Export image attachments
            val attachments =
                noteShowBean.note.attachments.filter { it.type == Attachment.Type.IMAGE }
            if (attachments.isNotEmpty()) {
                val fileDir = ideaMemoDir.findFile("file") ?: ideaMemoDir.createDirectory("file")
                ?: ideaMemoDir
                val dateDirName = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.ENGLISH
                ).format(Date(noteShowBean.note.createTime))
                val dateDir =
                    fileDir.findFile(dateDirName) ?: fileDir.createDirectory(dateDirName) ?: fileDir
                val noteDir = dateDir.findFile(noteShowBean.note.noteId.toString())
                    ?: dateDir.createDirectory(noteShowBean.note.noteId.toString()) ?: dateDir

                attachments.forEach { attachment ->
                    val sourceFile = File(attachment.path)
                    if (sourceFile.exists()) {
                        val destFile =
                            noteDir.createFile("image/*", sourceFile.name) ?: return@forEach
                        context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                            FileInputStream(sourceFile).use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
    }

    fun exportMarkDownFile(list: List<NoteShowBean>, uri: Uri) {
        App.instance.contentResolver.openOutputStream(uri)?.use { stream ->
            BufferedWriter(OutputStreamWriter(stream)).use { writer ->
                list.forEach { noteShowBean ->
                    val note = noteShowBean.note
                    writer.append("### ${note.createTime.toTime()}\n")
                    if (!note.noteTitle.isNullOrBlank()) {
                        writer.append("<!-- title: ${note.noteTitle!!.trim().replace("\n", " ")} -->\n")
                    }
                    if (!note.locationInfo.isNullOrEmpty()) {
                        writer.append(note.locationInfo.plus("\n"))
                    }
                    writer.append(note.content)
                    writer.append("\n\n")
                }
            }
        }
    }

    suspend fun exportHtmlZip(list: List<NoteShowBean>, uri: Uri) = withContext(Dispatchers.IO) {
        val context = App.instance
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            val zipOut = ZipOutputStream(BufferedOutputStream(stream))
            try {
                // 1. 生成 HTML 内容
                val htmlContent = generateHtml(list)
                val htmlEntry = ZipEntry("IdeaMemo.html")
                zipOut.putNextEntry(htmlEntry)
                zipOut.write(htmlContent.toByteArray())
                zipOut.closeEntry()

                // 2. 导出图片附件
                list.forEach { noteShowBean ->
                    val dateDirName = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.ENGLISH
                    ).format(Date(noteShowBean.note.createTime))
                    noteShowBean.note.attachments.filter { it.type == Attachment.Type.IMAGE }
                        .forEach { attachment ->
                            val file = File(attachment.path)
                            if (file.exists()) {
                                val entryPath =
                                    "file/$dateDirName/${noteShowBean.note.noteId}/${file.name}"
                                val entry = ZipEntry(entryPath)
                                zipOut.putNextEntry(entry)
                                FileInputStream(file).use { input ->
                                    input.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                        }
                }
            } finally {
                zipOut.close()
            }
        }
    }

    fun generateHtml(list: List<NoteShowBean>): String {
        val userName = "@Lucky"
        val exportDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val count = list.size

        val dateOptions = list.map {
            SimpleDateFormat(
                "yyyy-MM",
                Locale.ENGLISH
            ).format(Date(it.note.createTime))
        }
            .distinct()
            .sortedDescending()
            .joinToString("") {
                "<option value=\"$it\">$it</option>"
            }

        val tags = list.flatMap { it.tagList }.map { it.tag }.distinct().sorted().joinToString("") {
            "<option value=\"$it\">$it</option>"
        }

        val memosHtml = list.joinToString("") { noteShowBean ->
            val timeStr = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.ENGLISH
            ).format(Date(noteShowBean.note.createTime))
            val dateMonth = SimpleDateFormat(
                "yyyy-MM",
                Locale.ENGLISH
            ).format(Date(noteShowBean.note.createTime))
            val dateDirName = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.ENGLISH
            ).format(Date(noteShowBean.note.createTime))
            val contentHtml =
                noteShowBean.note.content.split("\n").joinToString("") { "<p>$it</p>" }
            val titleHtml = noteShowBean.note.noteTitle
                ?.takeIf { it.isNotBlank() }
                ?.let { "<div class=\"title\">${it.trim()}</div>" }
                ?: ""
            val tagListStr = noteShowBean.tagList.joinToString(" ") { "#${it.tag}" }
            val filesHtml =
                noteShowBean.note.attachments.filter { it.type == Attachment.Type.IMAGE }
                    .joinToString("") { attachment ->
                        val fileName = File(attachment.path).name
                        "<img src=\"file/$dateDirName/${noteShowBean.note.noteId}/$fileName\" alt=\"memo image\" />"
                    }

            """
    <div class="memo" data-date="$dateMonth" data-tags="$tagListStr">
      <div class="time">$timeStr</div>
      $titleHtml
      <div class="content">
        $contentHtml
      </div>
      <div class="files">
        $filesHtml
      </div>
    </div>
            """.trimIndent()
        }

        return """
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <title>IdeaMemo · 笔记导出</title>
        <style type="text/css">
          * { margin: 0; padding: 0; }
          body { background: #fafafa; }
          header .logo { text-align: center; border-bottom: 1px solid #efefef; }
          header .top { display: flex; justify-content: space-between; align-items: center; }
          header .logo, header .top .user { padding: 40px 0; }
          header .top .user .name { color: #454545; font-size: 16px; }
          header .top .user .date { font-size: 12px; color: #9d9d9d; }
          header .top .filter { display: none; justify-content: space-between; align-items: center; }
          header .top .filter button { padding: 9px 10px; background: #30cf79; border-radius: 3px; color: #fff; font-size: 14px; border: none; cursor: pointer; }
          header, .memos { width: 600px; margin: 0 auto; }
          .memos h1 { margin-top: 30px; }
          .memo { margin: 20px 0; background: #fff; padding: 20px; border-radius: 6px; word-wrap: break-word; }
          .memo:hover { box-shadow: 0px 2px 10px #dddddd; }
          .memo .time { color: #8d8d8d; font-size: 12px; }
          .memo .title { color: #454545; font-size: 16px; font-weight: 600; margin: 6px 0 2px; }
          .memo .content { color: #323232; font-size: 14px; }
          .memo .content p { line-height: 1.8; min-height: 20px; margin: 0; }
          .memo .content ul, .memo .content ol { padding-inline-start: 22px; margin: 0; }
          .memo .content li { line-height: 1.8; }
          .memo .files img { max-width: 100%; border: 1px solid #e6e6e6; border-radius: 4px; margin: 6px 0; }
          .custom-select { position: relative; font-size: 14px; color: #9d9d9d; background: #efefef; border-radius: 3px; border: none; margin-right: 10px; }
          .custom-select select { display: none; }
          .select-selected { background: #efefef; color: #9d9d9d; }
          .select-selected:after { position: absolute; content: ""; top: 14px; right: 10px; width: 0; height: 0; border: 6px solid transparent; border-color: #7d7d7d transparent transparent transparent; }
          .select-selected.select-arrow-active:after { border-color: transparent transparent #7d7d7d transparent; top: 7px; }
          .select-items div, .select-selected { color: rgba(157, 157, 157, 1); padding: 9px 12px; font-size: 14px; color: #9d9d9d; border: none; cursor: pointer; border-radius: 3px; border: none; width: 120px; user-select: none; }
          .select-items { position: absolute; background-color: white; top: 100%; left: 0; right: 0; z-index: 99; max-height: 200px; overflow-y: auto; overflow-x: hidden; box-shadow: 0px 4px 2px #dddddd; }
          .select-items::-webkit-scrollbar { width: 5px; }
          .select-items::-webkit-scrollbar-track { background: #f1f1f1; }
          .select-items::-webkit-scrollbar-thumb { background: #888; }
          .select-hide { display: none; }
          .select-items div:hover, .same-as-selected { background-color: rgba(0, 0, 0, 0.1); }
          </style>
      </head>
      <body>
        <header>
          <div class="top">
            <div class="user">
              <div class="name">$userName</div>
              <div class="date">于 $exportDate 导出 $count 条 MEMO</div>
            </div>
            <div class="filter">
              <div class='custom-select'>
                <select name="date" id="date">
                  <option value="">选择年月</option>
                  $dateOptions
                </select>
              </div>
              <div class='custom-select'>
                <select name="tag" id="tag">
                  <option value="">选择标签</option>
                  $tags
                </select>
              </div>
              <button>筛选</button>
            </div>
          </div>
        </header>
        <div class="memos">
          $memosHtml
        </div>
        <script>
          (function() {
            try {
              const filterEl = document.querySelector(".filter");
              if (filterEl) {
                filterEl.style.display = "flex";
              }
            } catch (e) {}
          })();
          
          const filterBtn = document.querySelector("button");
          filterBtn.addEventListener("click", () => {
            const dateVal = document.querySelector("#date").value;
            const tagVal = document.querySelector("#tag").value;
            const memos = document.querySelectorAll(".memo");
            
            memos.forEach((memo) => {
              const memoDate = memo.getAttribute("data-date") || "";
              const memoTags = memo.getAttribute("data-tags") || "";
              const contentText = memo.querySelector(".content").innerText;
              
              const isDateMatch = !dateVal || memoDate === dateVal;
              const isTagMatch = !tagVal || memoTags.includes(tagVal) || contentText.includes(tagVal);
              
              memo.style.display = (isDateMatch && isTagMatch) ? "block" : "none";
            });
          });
          
          var x, i, j, l, ll, selElmnt, a, b, c;
          x = document.getElementsByClassName("custom-select");
          l = x.length;
          for (i = 0; i < l; i++) {
            selElmnt = x[i].getElementsByTagName("select")[0];
            ll = selElmnt.length;
            a = document.createElement("DIV");
            a.setAttribute("class", "select-selected");
            a.innerHTML = selElmnt.options[selElmnt.selectedIndex].innerHTML;
            x[i].appendChild(a);
            b = document.createElement("DIV");
            b.setAttribute("class", "select-items select-hide");
            for (j = 0; j < ll; j++) {
              c = document.createElement("DIV");
              c.innerHTML = selElmnt.options[j].innerHTML;
              c.addEventListener("click", function (e) {
                var y, i, k, s, h, sl, yl;
                s = this.parentNode.parentNode.getElementsByTagName("select")[0];
                sl = s.length;
                h = this.parentNode.previousSibling;
                for (i = 0; i < sl; i++) {
                  if (s.options[i].innerHTML == this.innerHTML) {
                    s.selectedIndex = i;
                    h.innerHTML = this.innerHTML;
                    y = this.parentNode.getElementsByClassName("same-as-selected");
                    yl = y.length;
                    for (k = 0; k < yl; k++) {
                      y[k].removeAttribute("class");
                    }
                    this.setAttribute("class", "same-as-selected");
                    break;
                  }
                }
                h.click();
              });
              b.appendChild(c);
            }
            x[i].appendChild(b);
            a.addEventListener("click", function (e) {
              e.stopPropagation();
              closeAllSelect(this);
              this.nextSibling.classList.toggle("select-hide");
              this.classList.toggle("select-arrow-active");
            });
          }
          
          function closeAllSelect(elmnt) {
            var x, y, i, xl, yl, arrNo = [];
            x = document.getElementsByClassName("select-items");
            y = document.getElementsByClassName("select-selected");
            xl = x.length;
            yl = y.length;
            for (i = 0; i < yl; i++) {
              if (elmnt == y[i]) {
                arrNo.push(i);
              } else {
                y[i].classList.remove("select-arrow-active");
              }
            }
            for (i = 0; i < xl; i++) {
              if (arrNo.indexOf(i) === -1) {
                x[i].classList.add("select-hide");
              }
            }
          }
          document.addEventListener("click", closeAllSelect);
        </script>
      </body>
    </html>
        """.trimIndent()
    }

    @Keep
    suspend fun importFromHtmlZip(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            var tempDir: File? = null
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    runCatching { ZipPathValidator.clearCallback() }
                }
                val dir = File(context.cacheDir, "html_import_${System.currentTimeMillis()}")
                tempDir = dir
                dir.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipUtil.unpack(inputStream, dir)
                } ?: return@withContext Result.failure(Exception("Unable to open zip file"))
                val htmlFile =
                    findFirstHtmlFileAndPrintAll(dir) ?: return@withContext Result.failure(
                        Exception("HTML file not found in zip")
                    )
                val htmlContent = htmlFile.readText()
                val document = Jsoup.parse(htmlContent)
                val memoElements = document.select(".memo")

                val notes = mutableListOf<Pair<Note, List<Tag>>>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

                memoElements.forEach { memoElement ->
                    try {
                        val timeElement = memoElement.select(".time").first()
                        val titleElement = memoElement.select(".title").first()
                        val contentElement = memoElement.select(".content").first()
                        val filesElement = memoElement.select(".files").first()
                        val tagsAttr = memoElement.attr("data-tags")

                        val timeStr = timeElement?.text() ?: ""

                        // 处理内容中的换行：
                        // 导出时是用 <p> 标签包裹每一行，所以还原时需要将 <p> 标签转换回换行符
                        val contentText = contentElement?.let { element ->
                            val sb = StringBuilder()
                            element.children().forEach { child ->
                                if (child.tagName() == "p") {
                                    if (sb.isNotEmpty()) sb.append("\n")
                                    sb.append(child.text())
                                }
                            }
                            // 如果没有 p 标签，则回退到普通文本处理
                            if (sb.isEmpty()) element.text() else sb.toString()
                        } ?: ""

                        val createTime = try {
                            dateFormat.parse(timeStr)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        val attachments = mutableListOf<Attachment>()
                        filesElement?.select("img")?.forEach { imgElement ->
                            val imgSrc = imgElement.attr("src")
                            // 使用相对于 HTML 文件的路径来寻找图片
                            val imgFile = File(htmlFile.parentFile, imgSrc)
                            if (imgFile.exists()) {
                                val destDir = File(
                                    context.getExternalFilesDir(null),
                                    "file/${
                                        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(
                                            Date(createTime)
                                        )
                                    }"
                                )
                                destDir.mkdirs()
                                val destFile = File(destDir, imgFile.name)
                                imgFile.copyTo(destFile, overwrite = true)
                                attachments.add(
                                    Attachment(
                                        path = destFile.absolutePath,
                                        type = Attachment.Type.IMAGE
                                    )
                                )
                            }
                        }

                        val tagList = tagsAttr.split(" ")
                            .filter { it.startsWith("#") }
                            .map { it.removePrefix("#") }
                            .filter { it.isNotBlank() }
                            .map { Tag(tag = it) }

                        val note = Note(
                            noteTitle = titleElement?.text()?.trim()?.ifBlank { null },
                            content = contentText,
                            createTime = createTime,
                            updateTime = createTime,
                            attachments = attachments
                        )

                        notes.add(note to tagList)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val entryPoint =
                    EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java)
                val tagNoteRepo = entryPoint.tagNoteRepo()

                var importedCount = 0
                notes.forEach { (note, tagList) ->
                    try {
                        tagNoteRepo.insertWithExplicitTags(note, tagList)
                        importedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Result.success(importedCount)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            } finally {
                runCatching { tempDir?.deleteRecursively() }
            }
        }

    /**
     * Imports a single Markdown or plain text file. Markdown files exported by
     * IdeaMemo contain one `### yyyy/MM/dd HH:mm` header per note; other files
     * are imported as a single note.
     */
    @Keep
    suspend fun importFromMarkdownOrText(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: return@withContext Result.failure(Exception("Unable to read file"))

                val fileName = queryDisplayName(context, uri) ?: uri.lastPathSegment.orEmpty()
                val isMarkdown = fileName.endsWith(".md", true) ||
                        fileName.endsWith(".markdown", true)

                val notes = parseImportedNotes(text, isMarkdown)
                if (notes.isEmpty()) {
                    return@withContext Result.failure(Exception("No notes found"))
                }

                val entryPoint =
                    EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java)
                val tagNoteRepo = entryPoint.tagNoteRepo()

                var importedCount = 0
                notes.forEach { (note, tagList) ->
                    try {
                        tagNoteRepo.insertWithExplicitTags(note, tagList)
                        importedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Result.success(importedCount)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    private val markdownHeaderRegex =
        Regex("(?m)^###\\s+(\\d{4}[/-]\\d{2}[/-]\\d{2} \\d{2}:\\d{2}(?::\\d{2})?)\\s*$")

    private val markdownTitleRegex =
        Regex("(?m)^<!--\\s*title:\\s*(.*?)\\s*-->\\s*\\n?")

    private fun parseImportedNotes(text: String, isMarkdown: Boolean): List<Pair<Note, List<Tag>>> {
        val headers = markdownHeaderRegex.findAll(text).toList()
        if (headers.isEmpty()) {
            return listOf(parseSingleNote(text, isMarkdown))
        }

        val result = mutableListOf<Pair<Note, List<Tag>>>()
        headers.forEachIndexed { index, header ->
            val bodyStart = header.range.last + 1
            val bodyEnd = if (index + 1 < headers.size) headers[index + 1].range.first else text.length
            val body = text.substring(bodyStart, bodyEnd).trim('\n')
            val createTime = parseImportedTime(header.groupValues[1]) ?: System.currentTimeMillis()

            var title: String? = null
            var content = body
            markdownTitleRegex.find(body)?.let { match ->
                title = match.groupValues[1].trim().ifBlank { null }
                content = body.removeRange(match.range).trim('\n')
            }
            if (content.isBlank() && title == null) return@forEachIndexed

            result.add(
                Note(
                    noteTitle = title,
                    content = content,
                    createTime = createTime,
                    updateTime = createTime
                ) to TopicUtils.getTopicListByString(content)
            )
        }
        return result
    }

    private fun parseSingleNote(text: String, isMarkdown: Boolean): Pair<Note, List<Tag>> {
        val lines = text.trim('\n').lines().toMutableList()
        var createTime = System.currentTimeMillis()
        var title: String? = null

        if (lines.isNotEmpty()) {
            parseImportedTime(lines.first().trim())?.let {
                createTime = it
                lines.removeAt(0)
            }
        }
        if (lines.isNotEmpty() && lines.first().startsWith("Title: ")) {
            title = lines.first().removePrefix("Title: ").trim().ifBlank { null }
            lines.removeAt(0)
        }
        if (title == null && isMarkdown && lines.firstOrNull()?.startsWith("# ") == true) {
            title = lines.first().removePrefix("# ").trim().ifBlank { null }
            lines.removeAt(0)
        }

        val content = lines.joinToString("\n").trim('\n')
        return Note(
            noteTitle = title,
            content = content,
            createTime = createTime,
            updateTime = createTime
        ) to TopicUtils.getTopicListByString(content)
    }

    private fun parseImportedTime(raw: String): Long? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val patterns = listOf(
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss"
        )
        patterns.forEach { pattern ->
            runCatching {
                val format = SimpleDateFormat(pattern, Locale.ENGLISH)
                format.isLenient = false
                val parsed = format.parse(value)
                if (parsed != null && format.format(parsed) == value) {
                    return parsed.time
                }
            }
        }
        return null
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }


    fun findFirstHtmlFileAndPrintAll(dir: File): File? {
        if (!dir.exists() || !dir.isDirectory) {
            Log.i("BackUp","目录不存在或不是文件夹: ${dir.absolutePath}")
            return null
        }

        var result: File? = null

        fun dfs(current: File) {
            // 如果已经找到，就停止递归
            if (result != null) return

            Log.i("BackUp","访问: ${current.absolutePath}")

            if (current.isFile) {
                if (current.name.endsWith(".html", ignoreCase = true)) {
                    result = current
                    return
                }
            } else if (current.isDirectory) {
                current.listFiles()?.forEach { file ->
                    dfs(file)
                    if (result != null) return
                }
            }
        }

        dfs(dir)

        if (result != null) {
            Log.i("BackUp","找到HTML文件: ${result!!.absolutePath}")
        } else {
            Log.i("BackUp","未找到HTML文件")
        }

        return result
    }
}
