package com.jcoder.linker

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jcoder.linker.adapters.LinkAdapter
import com.jcoder.linker.data.Link
import com.jcoder.linker.database.AppDatabase
import com.jcoder.linker.databinding.ActivityLinkSuggestionsBinding
import com.jcoder.linker.databinding.DialogLinkBinding
import com.jcoder.linker.models.LinkModel
import com.jcoder.linker.utils.ClipboardUtils.clipboardManager
import com.jcoder.linker.utils.ClipboardUtils.getTextCompat
import com.jcoder.linker.views.SpacingItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinkSuggestionsActivity : AppCompatActivity(), SearchView.OnQueryTextListener,
    LinkAdapter.OnItemLongClickListener {

    private val binding by lazy { ActivityLinkSuggestionsBinding.inflate(layoutInflater) }
    private val linkAdapter by lazy { LinkAdapter(this, this) }
    private val db by lazy { AppDatabase.getInstance(this) }
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(!isTaskRoot)
        setupViews()
        loadLinks()
    }

    private fun Context.toPx(dp: Int): Int {
        val scale = this.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun setupViews() {
        binding.recycler.apply {
            setHasFixedSize(true)
            adapter = linkAdapter
            addItemDecoration(
                SpacingItemDecoration(1, toPx(15), true)
            )
        }

        binding.fabAdd.setOnClickListener { showEnterLinkDialog() }
    }

    private fun loadLinks() {
        CoroutineScope(Dispatchers.IO).launch {
            val links = mutableListOf<LinkModel>()

            val localLinks = resources.getStringArray(R.array.links)
            val savedLinks = db.linkDao().getAll()

            links.addAll(localLinks.map { LinkModel(true, Link(it, 0)) })
            links.addAll(savedLinks.map { LinkModel(false, it) })
            links.sortBy { it.link.url }

            updateLinkList(links)
        }
    }

    private suspend fun updateLinkList(links: List<LinkModel>, callback: (() -> Unit)? = null) {
        withContext(Dispatchers.Main) {
            linkAdapter.asyncListDiffer.submitList(links) {
                callback?.invoke()
            }
        }
    }

    private fun showEnterLinkDialog() {
        val dialogBinding = DialogLinkBinding.inflate(layoutInflater)
        dialogBinding.textInputLayout.setEndIconOnClickListener {
            dialogBinding.edt.text?.insert(0, clipboardManager().getTextCompat(this))
        }
        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val url = dialogBinding.edt.text.toString().trim()
                if (url.isNotBlank()) {
                    insertLink(url)
                }
            }
            .show()
    }

    private fun insertLink(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val link = Link(url, System.currentTimeMillis())
            val linkId = db.linkDao().insertAll(link)[0]
            link.id = linkId

            val linkModel = LinkModel(false, link)
            val links = linkAdapter.asyncListDiffer.currentList.toMutableList()
            links.add(linkModel)
            links.sortBy { it.link.url }

            val insertedPosition = links.indexOf(linkModel)
            updateLinkList(links) {
                setResult(RESULT_OK)
                binding.recycler.scrollToPosition(insertedPosition)
            }
        }
    }

    private fun deleteLink(linkModel: LinkModel) {
        searchView.setQuery("", true)

        CoroutineScope(Dispatchers.IO).launch {
            db.linkDao().delete(linkModel.link)

            val links = linkAdapter.asyncListDiffer.currentList.toMutableList()
            links.remove(linkModel)
            links.sortBy { it.link.url }

            updateLinkList(links) {
                setResult(RESULT_OK)
            }
        }
    }

    private fun searchLink(query: String?) {
        if (query == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val links = linkAdapter.asyncListDiffer.currentList.toMutableList()
            links.filter { it.link.url.contains(query) }
            updateLinkList(links)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.link_suggestions_menu, menu)
        val searchMenu = menu.findItem(R.id.menu_search)
        searchView = searchMenu.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchLink(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchLink(newText)
        return true
    }

    override fun onItemLongClicked(linkModel: LinkModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_delete_confirmation)
            .setMessage(getString(R.string.msg_delete_confirmation, linkModel.link.url))
            .setNegativeButton(R.string.btn_no, null)
            .setPositiveButton(R.string.btn_yes) { _, _ ->
                deleteLink(linkModel)
            }
            .show()
    }

}
