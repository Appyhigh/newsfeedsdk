package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.databinding.ItemSearchStickyHeaderBinding
import com.appyhigh.newsfeedsdk.databinding.ItemSearchStickyIconBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.SearchStickyItemModel
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*

class StickyGridAdapter(var backupList: ArrayList<SearchStickyItemModel>,
                        var iconsList: ArrayList<SearchStickyItemModel>,
                        var type: String, var cardType: String,
                        var expandedMap: HashMap<String, Int>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ICON_TYPE = 0
    private val BACKGROUND_TYPE = 1
    private val HEADER = 2

    inner class StickyIconViewHolder(val view: ItemSearchStickyIconBinding) :
        RecyclerView.ViewHolder(view.root)

    inner class StickyHeaderViewHolder(val view: ItemSearchStickyHeaderBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            ICON_TYPE  -> StickyIconViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_search_sticky_icon,
                    parent,
                    false
                )
            )
            HEADER -> StickyHeaderViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_search_sticky_header,
                    parent,
                    false
                )
            )
            else -> StickyIconViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_search_sticky_icon,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            ICON_TYPE  -> onIconBindViewHolder(holder as StickyIconViewHolder, position)
            HEADER -> onHeaderBindViewHolder(holder as StickyHeaderViewHolder, position)
            else -> onBackgroundViewHolder(holder as StickyIconViewHolder, position)
        }
    }

    private fun onClickOfHeader(type:String, isExpanded: Boolean){
        if(cardType == "ICON"){
            if(type=="solid"){
                changeDataView(1, 12, isExpanded, type)
            } else{
                changeDataView(13, 24, isExpanded, type)
            }
        } else {
            when (type) {
                "gaming" -> changeDataView(22, 27, isExpanded, type)
                "fashion" -> changeDataView(28, 32, isExpanded, type)
                "beauty" -> changeDataView(33, 38, isExpanded, type)
                "education" -> changeDataView(39, 44, isExpanded, type)
                "tech" -> changeDataView(45, 49, isExpanded, type)
                "glass" -> changeDataView(50, 55, isExpanded, type)
                "miscellaneous" -> changeDataView(56, 62, isExpanded, type)
                else -> changeDataView(1, 21, isExpanded, type)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun changeDataView(startPosition: Int, endPosition: Int, isExpanded: Boolean, type: String){
        try {
            val headerPosition = expandedMap[type]!!+1
            if (isExpanded) {
                iconsList.addAll(headerPosition, backupList.subList(startPosition, endPosition))
                notifyDataSetChanged()
            } else {
                iconsList.removeAll(backupList.subList(startPosition, endPosition))
                notifyDataSetChanged()
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun onHeaderBindViewHolder(holder: StickyHeaderViewHolder, position: Int){
        try{
            val iconHeader = iconsList[position]
            val nameInSmall = iconHeader.iconName.lowercase(Locale.getDefault())
            expandedMap[nameInSmall] = position
            if(iconHeader.isSelected){
                holder.view.lineView.visibility = View.GONE
                holder.view.expandIcon.setImageResource(R.drawable.ic_settings_collapsed)
            } else{
                holder.view.lineView.visibility = View.VISIBLE
                holder.view.expandIcon.setImageResource(R.drawable.ic_settings_expand)
            }
            holder.view.title.text = iconHeader.iconName
            holder.itemView.setOnClickListener {
                try{
                    holder.itemView.isEnabled = false
                    if(!iconHeader.isSelected){
                        iconHeader.isSelected = true
                        holder.view.lineView.visibility = View.GONE
                        holder.view.expandIcon.setImageResource(R.drawable.ic_settings_collapsed)
                        onClickOfHeader(nameInSmall, true)
                    } else{
                        iconHeader.isSelected = false
                        holder.view.lineView.visibility = View.VISIBLE
                        holder.view.expandIcon.setImageResource(R.drawable.ic_settings_expand)
                        onClickOfHeader(nameInSmall, false)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({holder.itemView.isEnabled = true}, 1000)
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }



    private fun onIconBindViewHolder(holder: StickyIconViewHolder, position: Int){
        try{
            val iconItem = iconsList[position]
            holder.view.icon.setImageResource(0)
            if(iconItem.isSelected){
                holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_icon_selected)
                holder.view.check.visibility = View.VISIBLE
            } else{
                holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_icon_not_selected)
                holder.view.check.visibility = View.GONE
            }
            val scale: Float = holder.itemView.context.resources.displayMetrics.density
            holder.view.icon.setImageResource(iconItem.icon)
            val padding = (8 * scale + 0.5f).toInt()
            holder.view.icon.setPadding(padding)
            holder.view.title.visibility = View.VISIBLE
            holder.view.title.text = iconItem.iconName
            holder.view.iconLayout.setOnClickListener {
                try {
                    if (!iconItem.isSelected) {
                        iconItem.isSelected = true
                        holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_icon_selected)
                        holder.view.check.visibility = View.VISIBLE
                        iconItem.isSelected = true
                        holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_icon_selected)
                        holder.view.check.visibility = View.VISIBLE
                    } else {
                        iconItem.isSelected = false
                        holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_icon_not_selected)
                        holder.view.check.visibility = View.GONE
                    }
                    SpUtil.searchStickyItemListener?.onItemSelected(iconItem)
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun onBackgroundViewHolder(holder: StickyIconViewHolder, position: Int){
        try {
            val iconItem = iconsList[position]
            if(iconItem.isSelected){
                Constants.stickyBackgroundSelected = position
                holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_bg_selected)
                holder.view.check.visibility = View.VISIBLE
            } else{
                holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_bg_not_selected)
                holder.view.check.visibility = View.GONE
            }
            val scale: Float = holder.itemView.context.resources.displayMetrics.density
            if(iconItem.type == "solid"){
                holder.view.icon.setImageResource(0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.view.iconLayout.background.setTint(Constants.getStickyBackground(iconItem.type, iconItem.iconName))
                } else{
                    holder.view.iconLayout.background.setColorFilter(Constants.getStickyBackground(iconItem.type, iconItem.iconName), PorterDuff.Mode.SRC_ATOP)
                }
            } else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.view.iconLayout.background.setTint(0)
                } else{
                    holder.view.iconLayout.background.setColorFilter(0, PorterDuff.Mode.SRC_ATOP)
                }
                holder.view.icon.setImageResource(Constants.getStickyBackground(iconItem.type, iconItem.iconName))
            }
            val padding = (2 * scale + 0.5f).toInt()
            holder.view.icon.setPadding(padding)
            holder.view.title.visibility = View.GONE
            holder.view.iconLayout.setOnClickListener{
                try{
                    if(!iconItem.isSelected){
                        iconsList[Constants.stickyBackgroundSelected].isSelected = false
                        notifyItemChanged(Constants.stickyBackgroundSelected)
                        Constants.stickyBackgroundSelected = position
                        iconItem.isSelected = true
                        holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_bg_selected)
                        if(iconItem.type == "solid"){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                holder.view.iconLayout.background.setTint(Constants.getStickyBackground(iconItem.type, iconItem.iconName))
                            } else{
                                holder.view.iconLayout.background.setColorFilter(Constants.getStickyBackground(iconItem.type, iconItem.iconName), PorterDuff.Mode.SRC_ATOP)
                            }
                        }
                        holder.view.check.visibility = View.VISIBLE
                    } else {
                        iconItem.isSelected = false
                        holder.view.iconLayout.setBackgroundResource(R.drawable.bg_sticky_bg_not_selected)
                        if(iconItem.type == "solid"){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                holder.view.iconLayout.background.setTint(Constants.getStickyBackground(iconItem.type, iconItem.iconName))
                            } else{
                                holder.view.iconLayout.background.setColorFilter(Constants.getStickyBackground(iconItem.type, iconItem.iconName), PorterDuff.Mode.SRC_ATOP)
                            }
                        }
                        holder.view.check.visibility = View.GONE
                    }
                    SpUtil.searchStickyItemListener?.onBackgroundClicked(iconItem)
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun getItemCount(): Int {
        return iconsList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(iconsList[position].type == "HEADER"){
            HEADER
        } else if(cardType == "ICON"){
            ICON_TYPE
        } else{
            BACKGROUND_TYPE
        }
    }
}