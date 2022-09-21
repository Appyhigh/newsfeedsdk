package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.callbacks.BlockPublisherClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemPublisherBlockBinding
import com.appyhigh.newsfeedsdk.model.PublisherDetail
import com.appyhigh.newsfeedsdk.model.feeds.Card

class PublisherBlocklistAdapter(var blockList: ArrayList<PublisherDetail>, var listener: BlockPublisherClickListener):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isChanged = false
    private var showSelector = false

    fun showSelector(show: Boolean){
        showSelector = show
        blockList.forEach { it.isBlocked = !show }
        notifyDataSetChanged()
    }

    inner class BlocklistViewHolder(val view: ItemPublisherBlockBinding) :  RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BlocklistViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_publisher_block,
                parent,
                false
            ))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as BlocklistViewHolder
        mainHolder.view.card = blockList[position]
        mainHolder.view.showSelector = showSelector
        mainHolder.view.tvBlock.setOnClickListener {
            try{
                isChanged = true
                ApiCreateOrUpdateUser().updateBlockPublisher(blockList[position].publisherId!!, "unblock", false)
                notifyItemRemoved(position)
                listener.onRemove(blockList[position])
                listener.onRefresh()
            } catch (ex:Exception){ }
        }
        mainHolder.view.selector.setOnClickListener {
            blockList[position].isBlocked = !blockList[position].isBlocked
            if(blockList[position].isBlocked){
                mainHolder.view.selector.setImageResource(R.drawable.ic_checkbox_unselected)
            }
            else{
                mainHolder.view.selector.setImageResource(R.drawable.ic_checkbox_selected)
            }
        }
        if(showSelector){
            mainHolder.view.tvBlock.visibility = View.GONE
            mainHolder.view.selector.visibility = View.VISIBLE
            if(blockList[position].isBlocked){
                mainHolder.view.selector.setImageResource(R.drawable.ic_checkbox_unselected)
            }
            else{
                mainHolder.view.selector.setImageResource(R.drawable.ic_checkbox_selected)
            }
        } else{
            mainHolder.view.tvBlock.visibility = View.VISIBLE
            mainHolder.view.selector.visibility = View.GONE
        }
        Card.setFontFamily(mainHolder.view.tvPublisherImage, true)
        Card.setFontFamily(mainHolder.view.tvPublisherName, )
        Card.setFontFamily(mainHolder.view.tvBlock)
    }

    fun updateBlockList() {
        isChanged = true
        blockList.filter { !it.isBlocked }.forEach { publisherDetail ->
            ApiCreateOrUpdateUser().updateBlockPublisher(publisherDetail.publisherId!!, "unblock", false)
            listener.onRemove(publisherDetail)
            blockList.remove(publisherDetail)
        }
        notifyDataSetChanged()
        listener.onRefresh()
    }

    fun updateData(newList: ArrayList<PublisherDetail>){
        blockList = newList
        notifyDataSetChanged()
    }

    fun getItems(): ArrayList<PublisherDetail>{
        return blockList
    }

    override fun getItemCount(): Int {
        return blockList.size
    }

}