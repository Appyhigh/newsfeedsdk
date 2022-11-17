package com.appyhigh.newsfeedsdk.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.callbacks.TabSelectedListener
import com.appyhigh.newsfeedsdk.databinding.ItemCricketTabBinding
import com.appyhigh.newsfeedsdk.databinding.ItemCryptoLearnTabBinding
import com.appyhigh.newsfeedsdk.databinding.ItemCryptoTabBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil

class TabsAdapter(
    private var tabList: List<Item>,
    private var tabSelectedListener: TabSelectedListener,
    private var type:String?="cricket",
    private var selectedTab: Int = 0
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), TabSelectedListener {
    inner class CricketTabViewHolder(val view: ItemCricketTabBinding) :
        RecyclerView.ViewHolder(view.root)
    inner class CryptoLearnTabViewHolder(val view: ItemCryptoLearnTabBinding) :
        RecyclerView.ViewHolder(view.root)
    inner class CryptoTabViewHolder(val view: ItemCryptoTabBinding) :
        RecyclerView.ViewHolder(view.root)

    private var currentPosition = 0
    private val CRYPTO_ITEM = "crypto"
    private var alreadyCalled = -1
    private val CRYPTO_LEARN_ITEM = "crypto_learn"
    init {
        tabList[selectedTab].selected = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(type){
            CRYPTO_ITEM ->
                return CryptoTabViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_crypto_tab,
                        parent,
                        false
                    )
                )
            CRYPTO_LEARN_ITEM ->
                return CryptoLearnTabViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_crypto_learn_tab,
                        parent,
                        false
                    )
                )
            else ->
                return CricketTabViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_cricket_tab,
                        parent,
                        false
                    )
                )
        }
    }

    override fun onBindViewHolder(mainHolder: RecyclerView.ViewHolder, position: Int) {
        when(type){
            CRYPTO_ITEM -> {
                val holder = mainHolder as CryptoTabViewHolder
                holder.view.tab = tabList[holder.absoluteAdapterPosition]
                holder.view.position = holder.absoluteAdapterPosition
                holder.view.listener = this
                val scale: Float = holder.itemView.context.resources.displayMetrics.density
                val padding5 = (5 * scale + 0.5f).toInt()
                val padding10 = (10 * scale + 0.5f).toInt()
                val padding15 = (15 * scale + 0.5f).toInt()
                if(tabList[position].selected){
                    holder.view.cardLayout.cardElevation = 5f
                    holder.view.tvInterest.setPadding(padding15, padding10, padding15, padding10)
                    holder.view.tvInterest.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.feedSecondaryBackground))
                } else{
                    holder.view.cardLayout.cardElevation = 0f
                    holder.view.tvInterest.setPadding(padding5, padding10, padding5, padding10)
                    holder.view.tvInterest.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.feedBackground))
                }
            }
            CRYPTO_LEARN_ITEM -> {
                val holder = mainHolder as CryptoLearnTabViewHolder
                holder.view.tab = tabList[holder.absoluteAdapterPosition]
                holder.view.position = holder.absoluteAdapterPosition
                holder.view.listener = this
            }
            else -> {
                val holder = mainHolder as CricketTabViewHolder
                holder.view.tab = tabList[holder.absoluteAdapterPosition]
                holder.view.position = holder.absoluteAdapterPosition
                holder.view.listener = this
                if(tabList[holder.absoluteAdapterPosition].key_id=="upcoming_matches" && tabList[holder.absoluteAdapterPosition].selected){
                    hitCricketPostImpression(holder.itemView.context, 0, Constants.cricketUpcomingMatchURI, 0)
                } else if(tabList[holder.absoluteAdapterPosition].key_id=="results" && tabList[holder.absoluteAdapterPosition].selected){
                    hitCricketPostImpression(holder.itemView.context, 0, Constants.cricketPastMatchURI, 1)
                } else if(tabList[holder.absoluteAdapterPosition].key_id=="live_matches" && tabList[holder.absoluteAdapterPosition].selected){
                    ApiPostImpression().addCricketPostImpression(Constants.cricketLiveMatchURI)
                }
            }
        }
    }

    private fun hitCricketPostImpression(context: Context, tryCount: Int, url: String, type: Int){
        if(tryCount>1 || alreadyCalled==type){
            return
        } else{
            alreadyCalled = type
            Handler(Looper.getMainLooper()).postDelayed({ alreadyCalled = -1 }, 2000)
        }
        if(url.isNotEmpty()){
            ApiPostImpression().addCricketPostImpression(url)
        } else{
            Handler(Looper.getMainLooper()).postDelayed({ hitCricketPostImpression(context, tryCount+1, if(type==0) Constants.cricketUpcomingMatchURI else Constants.cricketPastMatchURI, type) }, 5000)
        }
    }

    override fun getItemCount(): Int =
        tabList.size

    override fun onTabClicked(v: View, position: Int) {
        tabSelectedListener.onTabClicked(v, position)
    }

    fun onTabCanged(position: Int){
        try{
            for (tab in tabList) {
                tab.selected = false
            }
            tabList[position].selected = true
            currentPosition = position
            notifyDataSetChanged()
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        if(type=="crypto_learn") {
            tabList[position].id?.let { SpUtil.cryptoEventsListener?.onCryptoNewsTabClicked(it) }
        }
    }

    fun updatePositionData(position: Int, value: String){
        tabList[position].value = value
        notifyItemChanged(position)
    }

    fun getCurrentPosition():Int{
        return currentPosition
    }
}