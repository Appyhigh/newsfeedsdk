package com.appyhigh.newsfeedsdk.model

import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle

data class
AdsModel(
    var feed_native: String,
    var video_ad_native: String,
    var search_page_native: String,
    var search_footer_banner_intermediate:String,
    var ad_id_between_article_native: String,
    var ad_id_between_article_native_fallback: String,
    var ad_id_article_end_native: String,
    var ad_id_article_end_native_fallback: String,
    var ad_id_post_interstitial: String,
    var native_footer_banner: String
)

data class NativeAdItem(
    val view: LinearLayout,
    val layoutId: Int,
    val adUnit:String,
    val showNew: Boolean,
    val screen: String
)
