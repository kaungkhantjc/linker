package com.jcoder.linker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcoder.linker.data.Link
import com.jcoder.linker.database.AppDatabase
import com.jcoder.linker.databinding.ActivityMainBinding
import com.jcoder.linker.models.LinkModel
import com.jcoder.linker.utils.ClipboardUtils.getTextFromClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            binding.autoCompleteTextView.text.insert(selectionStart, getTextFromClipboard())
        }
    }

    private fun loadLinks() {
        val links = arrayListOf<LinkModel>()
        val localLinks = resources.getStringArray(R.array.links)
        localLinks.forEach { url -> links.add(LinkModel(true, Link(url, 0))) }

        CoroutineScope(Dispatchers.IO).launch {
            val savedLinks = db.linkDao().getAll()
            savedLinks.forEach { link -> links.add(LinkModel(false, link)) }
            links.sortBy { it.link.url }

            runOnUiThread {
                val adapter =
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, links)
                binding.autoCompleteTextView.setAdapter(adapter)
                binding.autoCompleteTextView.requestFocus()
            }
        }
    }

    private fun launchLink() {
        val url = binding.autoCompleteTextView.text.toString().trim()
        if (url.isEmpty()) {
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
        val icon = R.mipmap.ic_launcher
        val iconUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(icon))
            .appendPath(resources.getResourceTypeName(icon))
            .appendPath(resources.getResourceEntryName(icon))
            .build()

        val shareIntent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=$packageName"
            )
            putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name))
            data = iconUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "text/plain"
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
