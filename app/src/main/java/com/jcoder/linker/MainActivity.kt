package com.jcoder.linker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcoder.linker.database.AppDatabase
import com.jcoder.linker.databinding.ActivityMainBinding
import com.jcoder.linker.utils.ClipboardUtils.clipboardManager
import com.jcoder.linker.utils.ClipboardUtils.getTextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.subtitle = BuildConfig.VERSION_NAME

        setupViews()
        loadLinks()
    }

    private fun setupViews() {
        binding.tvAppDetails.text = HtmlCompat.fromHtml(
            getString(R.string.msg_app_details),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.autoCompleteTextView.threshold = 1
        binding.btnClearAll.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                binding.autoCompleteTextView.setText("", true)
            } else {
                binding.autoCompleteTextView.setText("")
            }
        }

        binding.btnLicences.setOnClickListener {
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }

        binding.btnGithub.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/kaungkhantjc/linker")
                )
            )
        }

        binding.btnLaunch.setOnClickListener { launchLink() }
        binding.autoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                launchLink()
                handled = true
            }
            handled
        }

        binding.autoCompleteTextView.addTextChangedListener { binding.textInputLayout.error = null }

        binding.textInputLayout.setEndIconOnClickListener {
            val selectionStart = binding.autoCompleteTextView.selectionStart
            binding.autoCompleteTextView.text.insert(
                selectionStart,
                clipboardManager().getTextCompat(this)
            )
        }
    }

    private fun loadLinks() {
        CoroutineScope(Dispatchers.IO).launch {
            val items = mutableListOf<String>()
            val localLinks = resources.getStringArray(R.array.links)
            val savedLinks = db.linkDao().getAll()

            items.addAll(localLinks)
            items.addAll(savedLinks.map { it.url })
            items.sort()

            withContext(Dispatchers.Main) {
                binding.autoCompleteTextView.apply {
                    setSimpleItems(items.toTypedArray())
                    requestFocus()
                }
            }
        }
    }

    private fun launchLink() {
        val url = binding.autoCompleteTextView.text.toString()
        if (url.isBlank()) {
            binding.textInputLayout.error = getString(R.string.err_empty_link)
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(e.javaClass.simpleName)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun shareApp() {
        val shareIntent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=$packageName"
            )
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }, getString(R.string.menu_share_app))
        startActivity(shareIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> shareApp()
            R.id.menu_link_suggestions -> {
                resultLauncher.launch(Intent(this, LinkSuggestionsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadLinks()
            }
        }
}
