package com.appyhigh.newsfeedsdk.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.reels
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.ReelsActivity
import com.appyhigh.newsfeedsdk.callbacks.ReelsClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemReelBinding
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.eventsListener
import java.util.*
import kotlin.collections.ArrayList

class FeedReelsAdapter(var feedReels: List<Item>) :
    RecyclerView.Adapter<FeedReelsAdapter.FeedReelsViewHolder>(), ReelsClickListener {
    inner class FeedReelsViewHolder(val view: ItemReelBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedReelsAdapter.FeedReelsViewHolder {
        return FeedReelsViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_reel,
                parent,
                false
            )
        )
    }

    override fun onReelClicked(v: View, position: Int) {
        val intent = Intent(v.context, ReelsActivity::class.java)
        intent.putExtra(POSITION, position)
        val cardList = ArrayList<Card>()
        try {
            eventsListener?.onExploreInteraction("Reels", feedReels[position].publisherName!!, feedReels[position].postId!!)
        } catch (ex:Exception){
            ex.printStackTrace()
        }
        for (reel in feedReels) {
            val itemList = ArrayList<Item>()
            itemList.add(reel)
            val card = Card(itemList)
            card.cardType = Constants.CardType.MEDIA_VIDEO_BIG.toString()
                .lowercase(Locale.getDefault())
            cardList.add(card)
        }
        reels.addAll(cardList)
        v.context.startActivity(intent)
    }

    override fun onBindViewHolder(
        holder: FeedReelsAdapter.FeedReelsViewHolder,
        position: Int
    ) {
        holder.view.item = feedReels[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun getItemCount(): Int {
        return feedReels.size
    }
}