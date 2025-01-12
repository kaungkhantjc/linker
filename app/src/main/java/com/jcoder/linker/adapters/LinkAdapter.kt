package com.jcoder.linker.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.jcoder.linker.R
import com.jcoder.linker.databinding.ItemLinkBinding
import com.jcoder.linker.models.LinkModel

class LinkAdapter(
    private val context: Context,
    private val onItemLongClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<LinkAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLinkBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val linkModel = asyncListDiffer.currentList[position]

        val icon =
            if (linkModel.isLocal) R.drawable.ic_baseline_link_24 else R.drawable.ic_baseline_add_link_24
        val iconColor = if (linkModel.isLocal) R.color.pink_500 else R.color.blue_500

        holder.binding.ivIcon.setImageResource(icon)
        ImageViewCompat.setImageTintList(
            holder.binding.ivIcon,
            ColorStateList.valueOf(ContextCompat.getColor(context, iconColor))
        )
        holder.binding.tvLink.text = linkModel.link.url
    }

    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }

    inner class ViewHolder(val binding: ItemLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnLongClickListener {
                val linkModel = asyncListDiffer.currentList[adapterPosition]
                if (!linkModel.isLocal) {
                    onItemLongClickListener.onItemLongClicked(linkModel)
                }
                true
            }
        }
    }

    interface OnItemLongClickListener {
        fun onItemLongClicked(linkModel: LinkModel)
    }

    private val diffCallback = object : DiffUtil.ItemCallback<LinkModel>() {
        override fun areItemsTheSame(oldItem: LinkModel, newItem: LinkModel): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: LinkModel, newItem: LinkModel): Boolean {
            return oldItem == newItem
        }
    }

    val asyncListDiffer = AsyncListDiffer(this, diffCallback)

}
