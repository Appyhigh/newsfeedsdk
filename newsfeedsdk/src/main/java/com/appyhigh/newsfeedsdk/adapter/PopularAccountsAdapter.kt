package com.appyhigh.newsfeedsdk.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.FEED_TYPE
import com.appyhigh.newsfeedsdk.Constants.FULL_NAME
import com.appyhigh.newsfeedsdk.Constants.IS_FOLLOWING_PUBLISHER
import com.appyhigh.newsfeedsdk.Constants.POSITION
import com.appyhigh.newsfeedsdk.Constants.POST_SOURCE
import com.appyhigh.newsfeedsdk.Constants.PROFILE_PIC
import com.appyhigh.newsfeedsdk.Constants.PUBLISHER_ID
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.PublisherPageActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiFollowPublihser
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PopularAccountClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemPopularAccountBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil

class PopularAccountsAdapter(var popularAccounts: List<Item>) :
    RecyclerView.Adapter<PopularAccountsAdapter.PopularAccountsViewHolder>(),
    PopularAccountClickListener {
    inner class PopularAccountsViewHolder(val view: ItemPopularAccountBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PopularAccountsAdapter.PopularAccountsViewHolder {
        return PopularAccountsViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_popular_account,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: PopularAccountsAdapter.PopularAccountsViewHolder,
        position: Int
    ) {
        holder.view.item = popularAccounts[position]
        holder.view.listener = this
        holder.view.position = holder.adapterPosition
    }

    override fun getItemCount(): Int {
        return popularAccounts.size
    }

    override fun onPopularAccountClicked(v: View, position: Int) {
        val intent = Intent(v.context, PublisherPageActivity::class.java)
        try {
            SpUtil.eventsListener?.onExploreInteraction("Popular Accounts", popularAccounts[position].fullname!!, popularAccounts[position].publisherId!!)
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
        }
        intent.putExtra(FULL_NAME, popularAccounts[position].fullname)
        intent.putExtra(PROFILE_PIC, popularAccounts[position].profilePic)
        intent.putExtra(PUBLISHER_ID, popularAccounts[position].publisherId)
        intent.putExtra(IS_FOLLOWING_PUBLISHER, popularAccounts[position].isFollowingPublisher)
        intent.putExtra(Constants.PUBLISHER_CONTACT, popularAccounts[position].publisherContactUs)
        intent.putExtra(POSITION, position)
        intent.putExtra(FEED_TYPE, "explore_publisher")
        intent.putExtra(POST_SOURCE, "explore_publisher")
        intent.putExtra(Constants.SCREEN_TYPE, Constants.EXPLORE)
        v.context.startActivity(intent)
    }

    override fun onFollowClicked(v: View, position: Int) {
        if (popularAccounts[position].isFollowingPublisher!!) {
            (v as AppCompatTextView).text = "Follow"
        } else {
            (v as AppCompatTextView).text = "✓Following"
        }
        popularAccounts[position].isFollowingPublisher =
            !popularAccounts[position].isFollowingPublisher!!
        popularAccounts[position].publisherId?.let {
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                ApiFollowPublihser().followPublisherEncrypted(
                    Endpoints.FOLLOW_PUBLISHER_ENCRYPTED,
                    it1,
                    FeedSdk.userId,
                    it
                )
            }
        }
    }
}