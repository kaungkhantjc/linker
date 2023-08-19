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
import com.jcoder.linker.utils.ClipboardUtils.getTextFromClipboard
import com.jcoder.linker.views.SpacingItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LinkSuggestionsActivity : AppCompatActivity(), SearchView.OnQueryTextListener,
    LinkAdapter.OnItemLongClickListener {

    private lateinit var binding: ActivityLinkSuggestionsBinding
    private val linkModelList by lazy { arrayListOf<LinkModel>() }
    private val adapter by lazy { LinkAdapter(this, linkModelList, this) }
    private val db by lazy { AppDatabase.getInstance(this) }
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkSuggestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(!isTaskRoot)
        setupViews()
    }

    private fun Context.toPx(dp: Int): Int {
        val scale = this.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun setupViews() {
        binding.recycler.adapter = adapter
        binding.recycler.addItemDecoration(
            SpacingItemDecoration(1, toPx(15), true)
        )

        val localLinks = resources.getStringArray(R.array.links)
        localLinks.forEach { url -> linkModelList.add(LinkModel(true, Link(url, 0))) }

        CoroutineScope(Dispatchers.IO).launch {
            val savedLinks = db.linkDao().getAll()
            savedLinks.forEach { link -> linkModelList.add(LinkModel(false, link)) }
            sortLinkModelList()
        }

        binding.fabAdd.setOnClickListener { showEnterLinkDialog() }
    }

    private fun sortLinkModelList() {
        linkModelList.sortBy { it.link.url }
    }

    private fun showEnterLinkDialog() {
        val dialogBinding = DialogLinkBinding.inflate(layoutInflater)
        dialogBinding.textInputLayout.setEndIconOnClickListener {
            dialogBinding.edt.text?.insert(0, getTextFromClipboard())
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
            linkModelList.add(linkModel)
            sortLinkModelList()
            runOnUiThread {
                val position = linkModelList.indexOf(linkModel)
                if (position != -1) {
                    adapter.notifyItemInserted(position)
                    setResult(RESULT_OK)
                }
            }
        }
    }

    private fun deleteLink(linkModel: LinkModel) {
        searchView?.setQuery("", true)
        CoroutineScope(Dispatchers.IO).launch {
            db.linkDao().delete(linkModel.link)
        }
        val position = linkModelList.indexOf(linkModel)
        if (position != -1) {
            linkModelList.removeAt(position)
            adapter.notifyItemRemoved(position)
            setResult(RESULT_OK)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.link_suggestions_menu, menu)
        val searchMenu = menu?.findItem(R.id.menu_search)
        searchView = searchMenu?.let { (it.actionView as SearchView) }
        searchView?.setOnQueryTextListener(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        query?.let { adapter.filter.filter(it) }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let { adapter.filter.filter(it) }
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
