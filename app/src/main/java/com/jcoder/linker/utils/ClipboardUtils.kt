package com.jcoder.linker.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

object ClipboardUtils {
    fun Context.getTextFromClipboard(): CharSequence {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        return if (clipData != null && clipData.itemCount > 0) {
            val firstItem = clipData.getItemAt(0)
            if (firstItem.text != null) firstItem.coerceToText(this)
            else ""
        } else ""
    }

    fun Context.copyText(text: String) {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun Context.copyImage(imageUri: Uri) {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(this.contentResolver, "URI", imageUri)
        clipboard.setPrimaryClip(clip)
    }
}
