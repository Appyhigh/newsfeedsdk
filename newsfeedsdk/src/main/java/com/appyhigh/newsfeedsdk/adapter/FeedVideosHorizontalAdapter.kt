package com.appyhigh.newsfeedsdk.adapter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.FEED_TYPE
import com.appyhigh.newsfeedsdk.Constants.FROM_APP
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.POST_ID
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.NewsFeedPageActivity
import com.appyhigh.newsfeedsdk.activity.PostNativeDetailActivity
import com.appyhigh.newsfeedsdk.callbacks.FeedVideosHorizontalClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemFeedVideoHorizontalBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.firebase.analytics.FirebaseAnalytics

class FeedVideosHorizontalAdapter(var feedVideos: List<Item>) :
    RecyclerView.Adapter<FeedVideosHorizontalAdapter.FeedVideosHorizontalViewHolder>(),
    FeedVideosHorizontalClickListener {
    inner class FeedVideosHorizontalViewHolder(val view: ItemFeedVideoHorizontalBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedVideosHorizontalAdapter.FeedVideosHorizontalViewHolder {
        return FeedVideosHorizontalViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_feed_video_horizontal,
                parent,
                false
            )
        )
    }

    override fun onFeedVideosHorizontalClicked(v: View, position: Int) {
        try {
            SpUtil.eventsListener?.onExploreInteraction("Explore Videos", feedVideos[position].publisherName!!, feedVideos[position].postId!!)
        } catch (ex: Exception){
            LogDetail.LogEStack(ex)
        }
        val intent = if(feedVideos[position].isNative!!){
            val bundle = Bundle()
            bundle.putString("NativePageOpen","Feed")
            FirebaseAnalytics.getInstance(v.context).logEvent("NativePage",bundle)
            Intent(v.context, PostNativeDetailActivity::class.java)
        } else{
            Intent(v.context, NewsFeedPageActivity::class.java)
        }
        intent.putExtra(POSITION, position)
        intent.putExtra(FROM_APP, true)
        intent.putExtra(POST_ID, feedVideos[position].postId)
        intent.putExtra(FEED_TYPE, "explore_videos")
        intent.putExtra(POST_SOURCE, "explore_videos")
        intent.putExtra(Constants.LANGUAGE, feedVideos[position].languageString)
        intent.putExtra(Constants.SCREEN_TYPE, Constants.EXPLORE)
        v.context.startActivity(intent)
    }

    override fun onBindViewHolder(
        holder: FeedVideosHorizontalAdapter.FeedVideosHorizontalViewHolder,
        position: Int
    ) {
        holder.view.item = feedVideos[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun getItemCount(): Int {
        return feedVideos.size
    }
}