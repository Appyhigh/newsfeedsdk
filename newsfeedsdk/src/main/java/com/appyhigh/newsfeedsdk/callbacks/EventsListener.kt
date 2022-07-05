package com.appyhigh.newsfeedsdk.callbacks

interface EventsListener {
    fun onPersonalizePopup(infoGiven: Boolean, interests: String, languageName: String)
    fun onFeedCategoryClick(name: String)
    fun onFeedInteraction(source: String, category: String, postId: String, postPlatform: String, action: String)
    fun onVideoInteraction(source: String, category: String, postId: String, postPlatform: String, action: String)
    fun onExploreInteraction(category: String, name: String, postId: String)
    fun onAdClickedEvent()
}