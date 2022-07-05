package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.AddInterestsActivity
import com.appyhigh.newsfeedsdk.callbacks.InterestClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemAddInterestBinding
import com.appyhigh.newsfeedsdk.model.Interest
import java.util.ArrayList

class AddInterestAdapter(
    private var interestList: List<Interest>,
    private var onInterestSelected: AddInterestsActivity.OnInterestRemoved
) :
    RecyclerView.Adapter<AddInterestAdapter.InterestViewHolder>(), InterestClickListener {
    inner class InterestViewHolder(val view: ItemAddInterestBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        return InterestViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_add_interest,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        holder.view.interest = interestList[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun getItemCount(): Int =
        interestList.size

    fun updateList(interestList: List<Interest>) {
        this.interestList = interestList
    }

    override fun onInterestClicked(v: View, position: Int) {
        onInterestSelected.onRemoved(position)
    }

    fun getItems(): ArrayList<Interest> {
        return interestList as ArrayList<Interest>
    }

    fun updateData(interestList: ArrayList<Interest>) {
        this.interestList = interestList
        notifyDataSetChanged()
    }
}