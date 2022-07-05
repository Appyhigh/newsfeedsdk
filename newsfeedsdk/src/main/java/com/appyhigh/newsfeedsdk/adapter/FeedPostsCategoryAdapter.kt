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
import com.appyhigh.newsfeedsdk.Constants.LANGUAGE
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.POST_ID
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.NewsFeedPageActivity
import com.appyhigh.newsfeedsdk.activity.PostNativeDetailActivity
import com.appyhigh.newsfeedsdk.callbacks.FeedPostCategoryClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemFeedPostCategoryBindingImpl
import com.appyhigh.newsfeedsdk.databinding.ItemPopularAccountBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.google.firebase.analytics.FirebaseAnalytics

class FeedPostsCategoryAdapter(var feedPosts: List<Item>) :
    RecyclerView.Adapter<FeedPostsCategoryAdapter.FeedPostsCategoryViewHolder>(),
    FeedPostCategoryClickListener {
    inner class FeedPostsCategoryViewHolder(val view: ItemFeedPostCategoryBindingImpl) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedPostsCategoryAdapter.FeedPostsCategoryViewHolder {
        return FeedPostsCategoryViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_feed_post_category,
                parent,
                false
            )
        )
    }

    override fun onFeedPostCategoryClicked(v: View, position: Int) {
        val intent = if(feedPosts[position].isNative!!){
            val bundle = Bundle()
            bundle.putString("NativePageOpen","Feed")
            FirebaseAnalytics.getInstance(v.context).logEvent("NativePage",bundle)
            Intent(v.context, PostNativeDetailActivity::class.java)
        } else{
            Intent(v.context, NewsFeedPageActivity::class.java)
        }
        intent.putExtra(POSITION, position)
        intent.putExtra(FROM_APP, true)
        intent.putExtra(POST_ID, feedPosts[position].postId)
        intent.putExtra(FEED_TYPE, "explore_category")
        intent.putExtra(POST_SOURCE, "explore_category")
        intent.putExtra(LANGUAGE, feedPosts[position].languageString)
        intent.putExtra(Constants.SCREEN_TYPE, Constants.EXPLORE)
        v.context.startActivity(intent)
    }

    override fun onBindViewHolder(
        holder: FeedPostsCategoryAdapter.FeedPostsCategoryViewHolder,
        position: Int
    ) {
        holder.view.item = feedPosts[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun getItemCount(): Int {
        return feedPosts.size
    }
}