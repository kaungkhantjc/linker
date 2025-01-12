package com.jcoder.linker.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat

object ClipboardUtils {

    fun Context.clipboardManager() =
        ContextCompat.getSystemService(this, ClipboardManager::class.java)!!

    fun ClipboardManager.getTextCompat(context: Context): String {
        val clipData = this.primaryClip
        return if (clipData != null && clipData.itemCount > 0) {
            val firstItem = clipData.getItemAt(0)
            firstItem.coerceToText(context).toString()
        } else ""
    }

    fun ClipboardManager.setPlainText(text: String) {
        val clip = ClipData.newPlainText("text", text)
        setPrimaryClip(clip)
    }

    fun ClipboardManager.setUri(contentResolver: ContentResolver, uri: Uri) {
        val clip = ClipData.newUri(contentResolver, "URI", uri)
        this.setPrimaryClip(clip)
    }

}