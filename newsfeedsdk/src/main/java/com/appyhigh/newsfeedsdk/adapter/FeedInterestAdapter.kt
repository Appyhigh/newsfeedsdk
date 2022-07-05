package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.InterestsCardClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemFeedInterestBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item

class FeedInterestAdapter(
    private var interestList: List<Item>,
    private var listener: InterestsCardClickListener?
) :
    RecyclerView.Adapter<FeedInterestAdapter.InterestViewHolder>() {
    inner class InterestViewHolder(val view: ItemFeedInterestBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        return InterestViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_feed_interest,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        holder.view.item = interestList[position]
        holder.itemView.setOnClickListener {
            listener?.onInterestCardClicked(holder.itemView)
        }
    }

    override fun getItemCount(): Int =
        interestList.size
}