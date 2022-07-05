package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.PodcastsCategoryActivity
import com.appyhigh.newsfeedsdk.callbacks.SeeAllClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemHashtagsPlatformBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.lang.Exception

class HashtagsCircleAdapter(var hashtagsPlatforms: ArrayList<Item>, var backupList: ArrayList<Item>)
    : RecyclerView.Adapter<HashtagsCircleAdapter.HashtagsCircleViewHolder>(), HashtagsPlatformsListener, SeeAllClickListener{
    inner class HashtagsCircleViewHolder(val view: ItemHashtagsPlatformBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HashtagsCircleViewHolder {
        try {
            SpUtil.seeAllClickListener[backupList[0].onHit.toString()] = this
        } catch (ex:Exception){ }
        return HashtagsCircleViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_hashtags_platform,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: HashtagsCircleViewHolder,
        position: Int
    ) {
        holder.view.item = hashtagsPlatforms[position]
        holder.view.listener = this
        holder.view.position = holder.adapterPosition
    }

    override fun getItemCount(): Int {
        return hashtagsPlatforms.size
    }

    override fun onPlatformClicked(v: View, item: Item, position: Int) {
        val intent = Intent(v.context, PodcastsCategoryActivity::class.java)
        intent.putExtra("groupType", item.onHit)
        if(item.onHit == "podcast-publisher"){
            intent.putExtra("publisher_name", item.id)
            intent.putExtra(Constants.PUBLISHER_ID, item.publisherId)
        } else{
            intent.putExtra(Constants.INTEREST, item.id)
        }
        v.context.startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onSeeAllClicked(showMore: Boolean) {
        if(showMore){
            hashtagsPlatforms.addAll(9, backupList.subList(9, backupList.size))
            notifyItemRangeInserted(9, backupList.size-9)
        } else{
            hashtagsPlatforms.removeAll(backupList.subList(9, backupList.size))
            notifyItemRangeRemoved(9, backupList.size-9)
        }
    }
}

interface HashtagsPlatformsListener{
    fun onPlatformClicked(v: View, item: Item, position: Int)
}