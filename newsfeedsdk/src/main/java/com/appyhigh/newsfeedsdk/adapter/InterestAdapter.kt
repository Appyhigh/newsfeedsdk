package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.callbacks.InterestClickListener
import com.appyhigh.newsfeedsdk.callbacks.InterestSelectedListener
import com.appyhigh.newsfeedsdk.databinding.ItemInterestBinding
import com.appyhigh.newsfeedsdk.model.Interest
import java.lang.Exception

class InterestAdapter(
    private var interestList: ArrayList<Interest>,
    private var onInterestSelected: InterestSelectedListener
) :
    RecyclerView.Adapter<InterestAdapter.InterestViewHolder>(), InterestClickListener {
    inner class InterestViewHolder(val view: ItemInterestBinding) :
        RecyclerView.ViewHolder(view.root)

    init {
        try {
            interestList[0].isSelected = true
        } catch (ex:Exception){}
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        return InterestViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_interest,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        Log.d("INTERESTLISTT", interestList.toString())
        holder.view.interest = interestList[position]
        holder.view.listener = this
        holder.view.position = position
        if(interestList[position].keyId=="cricket" && interestList[position].isSelected){
            hitCricketPostImpression(holder.itemView.context, 0)
        }
    }

    private fun hitCricketPostImpression(context: Context, tryCount: Int){
        if(tryCount>1){
            return
        }
        if(Constants.cricketLiveMatchURI.isNotEmpty()){
            FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
                ApiPostImpression().addCricketPostImpression(it, Constants.cricketLiveMatchURI)
            }
        } else{
            Handler(Looper.getMainLooper()).postDelayed({ hitCricketPostImpression(context, tryCount+1) }, 5000)
        }
    }

    override fun getItemCount(): Int =
        interestList.size

    fun updateList(interestList: ArrayList<Interest>){
        this.interestList = ArrayList()
        this.interestList = interestList
    }

    fun updateItem(interest: Interest, index: Int){
        interestList[index] = interest
    }

    fun clearData(){
        stateRestorationPolicy = StateRestorationPolicy.PREVENT
        interestList = ArrayList()
    }

    override fun onInterestClicked(v: View, position: Int) {
        onInterestSelected.onInterestClicked(v, position)
    }
}