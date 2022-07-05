package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.InterestClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemPreferenceBinding
import com.appyhigh.newsfeedsdk.model.Interest

class PreferenceAdapter(private var interestsList: ArrayList<Interest>) :
    RecyclerView.Adapter<PreferenceAdapter.InterestViewHolder>(), InterestClickListener {
    inner class InterestViewHolder(val view: ItemPreferenceBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PreferenceAdapter.InterestViewHolder {
        return InterestViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_preference,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PreferenceAdapter.InterestViewHolder, position: Int) {
        holder.view.interest = interestsList[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun getItemCount(): Int =
        interestsList.size

    override fun onInterestClicked(v: View, position: Int) {
        interestsList[position].isSelected = !interestsList[position].isSelected
        notifyItemChanged(position)
    }

    fun getItems(): ArrayList<Interest> {
        return interestsList
    }

    fun updateList(interestList: List<Interest>) {
        this.interestsList = interestList as ArrayList<Interest>
        notifyDataSetChanged()
    }
}