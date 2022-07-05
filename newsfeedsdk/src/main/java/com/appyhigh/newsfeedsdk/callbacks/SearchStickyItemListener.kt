package com.appyhigh.newsfeedsdk.callbacks

import com.appyhigh.newsfeedsdk.model.SearchStickyItemModel

interface SearchStickyItemListener {
    fun onItemSelected(iconModel: SearchStickyItemModel)
    fun onBackgroundClicked(iconModel: SearchStickyItemModel)
}

interface StickyIconListener{
    fun onRefresh()
    fun onUnSelected(iconName: String, type: String)
}