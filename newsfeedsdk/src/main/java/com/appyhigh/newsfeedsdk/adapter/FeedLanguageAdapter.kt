package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.LanguageCardClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemFeedLanguageBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item

class FeedLanguageAdapter(
    private var interestList: List<Item>,
    private var listener: LanguageCardClickListener?
) :
    RecyclerView.Adapter<FeedLanguageAdapter.LanguageViewHolder>() {
    inner class LanguageViewHolder(val view: ItemFeedLanguageBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        return LanguageViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_feed_language,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.view.item = interestList[position]
        holder.itemView.setOnClickListener {
            listener?.onLanguageCardClicked(holder.itemView)
        }
    }

    override fun getItemCount(): Int =
        interestList.size
}