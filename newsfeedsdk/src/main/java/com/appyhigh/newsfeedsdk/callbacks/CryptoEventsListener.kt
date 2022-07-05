package com.appyhigh.newsfeedsdk.callbacks

interface CryptoEventsListener {
    fun onCoinDetailDateChanged(coinName: String, dateChangedTo: String)
    fun onCoinDetailTabChanged(coinName: String, tabChangedTo: String)
    fun onCoinDetailLinkChanged(coinName: String, clickedONLink: String)
    fun openCoinFromCryptoHome(section:String, coinName: String)
    fun onCryptoHomeCTAClicked(section:String, clickedON: String)
    fun onAddWatchlist(coinName: String, addedThrough: String, isSelected: Boolean)
    fun openCoinFromListPage(section:String, coinName: String)
    fun onListSearch(searchQuery: String)
    fun onNewsAction(category:String, postId: String, action:String, publisher:String, postTitle:String)
    fun onLearnPageClick(category:String, section:String, postId: String, postTitle:String)
    fun onActionOfPostDetailPage(postId: String, action:String, publisher:String, postTitle:String)
}