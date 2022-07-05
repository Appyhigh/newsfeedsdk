package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.LocationClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemLocationBinding

class ChangeLocationAdapter(private var itemList: ArrayList<String>, private var listener: LocationClickListener) : RecyclerView.Adapter<ChangeLocationAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_location,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.locationText.text = itemList[position]
        holder.binding.listener = listener
        holder.binding.position = position
    }

    override fun getItemCount(): Int = itemList.size

    fun updateData(list: ArrayList<String>){
        itemList = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root)
}