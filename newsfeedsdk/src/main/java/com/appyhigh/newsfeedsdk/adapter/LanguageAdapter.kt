package com.appyhigh.newsfeedsdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.callbacks.LanguageClickListener
import com.appyhigh.newsfeedsdk.databinding.ItemLanguagesBinding
import com.appyhigh.newsfeedsdk.model.Language

class LanguageAdapter(private var languageList: ArrayList<Language>) :
    RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>(), LanguageClickListener {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageAdapter.LanguageViewHolder {
        return LanguageViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_languages,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LanguageAdapter.LanguageViewHolder, position: Int) {
        holder.view.language = languageList[position]
        holder.view.listener = this
        holder.view.position = position
    }

    override fun getItemCount(): Int {
        return languageList.size
    }

    inner class LanguageViewHolder(val view: ItemLanguagesBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onLanguageClicked(v: View, position: Int) {
        languageList[position].isSelected = !languageList[position].isSelected
        notifyItemChanged(position)
    }

    fun getItems(): ArrayList<Language> {
        return languageList
    }

    fun updateList(languageResponseModel: List<Language>) {
        this.languageList = languageResponseModel as ArrayList<Language>
        notifyDataSetChanged()
    }
}