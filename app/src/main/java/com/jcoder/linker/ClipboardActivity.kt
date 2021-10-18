package com.jcoder.linker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.annotation.StringRes
import com.jcoder.linker.utils.ClipboardUtils.copyImage
import com.jcoder.linker.utils.ClipboardUtils.copyText

class ClipboardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                }
            }
            else -> {
                finishApp()
            }
        }
    }

    private fun finishApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else finish()
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            copyText(it)
            showCopiedToast(R.string.title_text)
            finishApp()
        }
    }

    private fun handleSendImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            copyImage(it)
            showCopiedToast(R.string.title_image)
            finishApp()
        }
    }

    private fun showCopiedToast(@StringRes resId: Int) {
        Toast.makeText(
            this,
            getString(R.string.msg_item_copied_to_clipboard, getString(resId)),
            Toast.LENGTH_SHORT
        ).show()
    }
}
