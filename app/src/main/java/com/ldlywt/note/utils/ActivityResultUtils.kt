package com.ldlywt.note.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract


object None

object ChoseFolderContract : ActivityResultContract<None?, Uri?>() {
    override fun createIntent(context: Context, input: None?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

object ExportTextContract : ActivityResultContract<None?, Uri?>() {
    override fun createIntent(context: Context, input: None?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

object ExportHtmlContract : ActivityResultContract<None?, Uri?>() {
    override fun createIntent(context: Context, input: None?): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, backUpFileName)
        }
        return ExportUriMemory.applyInitialUri(context, intent)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

class ExportMarkDownContract(val name: String) : ActivityResultContract<None?, Uri?>() {
    override fun createIntent(context: Context, input: None?): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
            putExtra(Intent.EXTRA_TITLE, "$name.md")
        }
        return ExportUriMemory.applyInitialUri(context, intent)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

object ImportHtmlZipContract : ActivityResultContract<None?, Uri?>() {
    override fun createIntent(context: Context, input: None?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

/**
 * Picks a Markdown or plain text file for import.
 */
object ImportTextContract : ActivityResultContract<None?, Uri?>() {
    override fun createIntent(context: Context, input: None?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("text/plain", "text/markdown", "text/x-markdown", "application/octet-stream")
            )
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent != null && resultCode == Activity.RESULT_OK) intent.data else null
    }
}

/**
 * Remembers the location of the last export so the system picker opens there next time.
 */
object ExportUriMemory {
    private const val PREFS_NAME = "export_uri_memory"
    private const val KEY_LAST_URI = "last_export_uri"

    fun remember(context: Context, uri: Uri?) {
        if (uri == null) return
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_URI, uri.toString())
                .apply()
        }
    }

    fun lastUri(context: Context): Uri? = runCatching {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_URI, null)
            ?.let(Uri::parse)
    }.getOrNull()

    /**
     * Adds the remembered location as the initial directory hint when supported.
     */
    fun applyInitialUri(context: Context, intent: Intent): Intent {
        val uri = lastUri(context) ?: return intent
        return runCatching {
            intent.putExtra("android.provider.extra.INITIAL_URI", uri)
        }.getOrDefault(intent)
    }
}

