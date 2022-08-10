package com.appyhigh.newsfeedsdk.callbacks

import com.appyhigh.newsfeedsdk.model.PublisherDetail


interface BlockPublisherClickListener {
    fun onRefresh()
    fun onRemove(publisherDetail: PublisherDetail)
}

