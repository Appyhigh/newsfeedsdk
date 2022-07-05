package com.appyhigh.newsfeedsdk.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.NewInterestClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemInterestNewBinding
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.utils.SpUtil
import it.sephiroth.android.library.xtooltip.Tooltip

class NewInterestAdapter(
    private var interestList: List<Interest>,
    private var listener: NewInterestClickListener?
) : RecyclerView.Adapter<NewInterestAdapter.InterestViewHolder>(),
    NewInterestClickListener {
    inner class InterestViewHolder(val view: ItemInterestNewBinding) :
        RecyclerView.ViewHolder(view.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        return InterestViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_interest_new,
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

    override fun getItemCount(): Int = interestList.size

    override fun onInterestPinned(v: View, position: Int, isPinned: Boolean) {
        listener?.onInterestPinned(v,position, !isPinned)
        if(!isPinned){
            interestList[position].userSelected = true
            interestList[position].isPinned = true
        } else{
            interestList[position].isPinned = false
        }
        notifyItemChanged(position)
    }

    override fun onInterestFollowed(v: View, position: Int, isSelected: Boolean) {
        listener?.onInterestFollowed(v,position, !isSelected)
        interestList[position].userSelected = !isSelected
        if(isSelected){
            interestList[position].isPinned = false
        }
        notifyItemChanged(position)
    }

    fun updateData(interestList: ArrayList<Interest>) {
        this.interestList = interestList
        notifyDataSetChanged()
    }

    fun getItems(): ArrayList<Interest>{
        return interestList as ArrayList<Interest>
    }


}