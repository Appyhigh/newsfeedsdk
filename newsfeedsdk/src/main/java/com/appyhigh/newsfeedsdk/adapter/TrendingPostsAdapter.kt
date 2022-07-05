package com.appyhigh.newsfeedsdk.adapter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.PostNativeDetailActivity
import com.appyhigh.newsfeedsdk.callbacks.FeedPostCategoryClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemTrendingPostCardBinding
import com.appyhigh.newsfeedsdk.databinding.ItemTrendingPostsBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.google.firebase.analytics.FirebaseAnalytics

class TrendingPostsAdapter(var trendingPosts: List<Item>):
    RecyclerView.Adapter<TrendingPostsAdapter.ViewHolder>(),
    FeedPostCategoryClickListener {

    inner class ViewHolder(val view: ItemTrendingPostCardBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_trending_post_card,
                parent,
                false
            )
        )
    }


    override fun getItemCount(): Int {
        return trendingPosts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.item = trendingPosts[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun onFeedPostCategoryClicked(v: View, position: Int) {
        val bundle = Bundle()
        bundle.putString("NativePageOpen","Feed")
        FirebaseAnalytics.getInstance(v.context).logEvent("NativePage",bundle)
        val intent = Intent(v.context, PostNativeDetailActivity::class.java)
        intent.putExtra(Constants.POSITION, position)
        intent.putExtra(Constants.FROM_APP, true)
        intent.putExtra(Constants.POST_ID, trendingPosts[position].postId)
//        intent.putExtra(Constants.FEED_TYPE, "explore_category")
//        intent.putExtra(Constants.POST_SOURCE, "explore_category")
        intent.putExtra(Constants.LANGUAGE, trendingPosts[position].languageString)
        intent.putExtra(Constants.SCREEN_TYPE, Constants.EXPLORE)
        v.context.startActivity(intent)
    }
}