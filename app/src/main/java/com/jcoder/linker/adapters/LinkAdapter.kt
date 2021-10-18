package com.jcoder.linker.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.jcoder.linker.R
import com.jcoder.linker.databinding.ItemLinkBinding
import com.jcoder.linker.models.LinkModel
import java.util.*


class LinkAdapter(
    private val context: Context,
    private val linkModelList: ArrayList<LinkModel>,
    private val onItemLongClickListener: OnItemLongClickListener
) :
    RecyclerView.Adapter<LinkAdapter.PlaceHolder>(), Filterable {

    private var filteredLinks: ArrayList<LinkModel> = arrayListOf()

    init {
        filteredLinks = linkModelList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceHolder {
        return PlaceHolder(LayoutInflater.from(context).inflate(R.layout.item_link, parent, false))
    }

    override fun onBindViewHolder(holder: PlaceHolder, position: Int) {
        val linkModel = filteredLinks[position]
        val icon =
            if (linkModel.isLocal) R.drawable.ic_baseline_link_24 else R.drawable.ic_baseline_add_link_24
        val iconColor = if (linkModel.isLocal) R.color.pink_500 else R.color.blue_500

        holder.binding.ivIcon.setImageResource(icon)
        ImageViewCompat.setImageTintList(
            holder.binding.ivIcon,
            ColorStateList.valueOf(ContextCompat.getColor(context, iconColor))
        )
        holder.binding.tvLink.text = linkModel.link.url
        holder.binding.root.setOnLongClickListener {
            val linkModel1 = filteredLinks[holder.adapterPosition]
            if (!linkModel1.isLocal) {
                onItemLongClickListener.onItemLongClicked(linkModel1)
            }
            true
        }
    }

    override fun getItemCount(): Int {
        return filteredLinks.size
    }

    class PlaceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemLinkBinding.bind(itemView)
    }

    interface OnItemLongClickListener {
        fun onItemLongClicked(linkModel: LinkModel)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()

                filteredLinks = if (charSearch.isEmpty()) {
                    linkModelList
                } else {
                    val resultList: ArrayList<LinkModel> = arrayListOf()
                    for (linkModel in linkModelList) {
                        if (linkModel.link.url.contains(charSearch, true)) {
                            resultList.add(linkModel)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = filteredLinks
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredLinks = results?.values as ArrayList<LinkModel>
                notifyDataSetChanged()
            }
        }
    }
}
