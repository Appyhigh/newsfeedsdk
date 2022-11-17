package com.appyhigh.newsfeedsdk.model

import android.widget.LinearLayout
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ConfigModel(
    @SerializedName("plcmnt_id")
    @Expose
    val placementId: String = "",
    @SerializedName("showInterstitialAfterPosts")
    @Expose
    val showInterstitialAfterPosts: Int = 3,
    @SerializedName("showParentAdmobAds")
    @Expose
    val showParentAdmobAds: Boolean = false,
    @SerializedName("showPrivateAds")
    @Expose
    val showPrivateAds: Boolean = false,
    @SerializedName("feed_native")
    @Expose
    val feedNative:ItemAdsModel = ItemAdsModel("", showAdmob = false, showPrivate = true),
    @SerializedName("video_native")
    @Expose
    val videoNative: ItemAdsModel = ItemAdsModel(),
    @SerializedName("search_page_native")
    @Expose
    val searchPageNative: ItemAdsModel = ItemAdsModel(),
    @SerializedName("search_footer_banner")
    @Expose
    val searchFooterBanner: ItemAdsModel = ItemAdsModel(),
    @SerializedName("post_detail_article_top_native")
    @Expose
    val postDetailArticleTopNative: ItemAdsModel = ItemAdsModel(),
    @SerializedName("post_detail_article_end_native")
    @Expose
    val postDetailArticleEndNative: ItemAdsModel = ItemAdsModel(),
    @SerializedName("post_detail_footer_banner")
    @Expose
    val postDetailFooterBanner: ItemAdsModel = ItemAdsModel(),
    @SerializedName("post_detail_interstitial")
    @Expose
    val postDetailInterstitial: ItemAdsModel = ItemAdsModel(),
    @SerializedName("crypto_home_native")
    @Expose
    val cryptoHomeNative: ItemAdsModel = ItemAdsModel(),
    @SerializedName("crypto_home_native_footer")
    @Expose
    val cryptoHomeNativeFooter: ItemAdsModel = ItemAdsModel(),
    @SerializedName("custom_firebase_config")
    @Expose
    val customFirebaseConfig: CustomFirebaseConfig = CustomFirebaseConfig(),
    @SerializedName("report_posts_text")
    @Expose
    val reportPostsText: List<String> = ArrayList(),
    @SerializedName("privacy_policy_url")
    @Expose
    val privacyPolicyUrl: String? = null
)

data class CustomFirebaseConfig(
    @SerializedName("feed_native_ad_interval")
    @Expose
    val feedNativeAdInterval: Long = 60,
    @SerializedName("searchSticky")
    @Expose
    val searchSticky: SearchSticky = SearchSticky(),
    @SerializedName("socket_series")
    @Expose
    val socketSeries: String = ""
)

data class SearchSticky(
    @SerializedName("showStickyOnTop")
    @Expose
    val showStickyOnTop: Boolean = true,
    @SerializedName("timer")
    @Expose
    val timer: Long = 300,
    @SerializedName("workManager")
    @Expose
    val workManager: Long = 6
)

data class ItemAdsModel(
    @SerializedName("admob_id")
    @Expose
    val admobId: String = "",
    @SerializedName("showAdmob")
    @Expose
    val showAdmob: Boolean = false,
    @SerializedName("showPrivate")
    @Expose
    val showPrivate: Boolean = false
)

data class NativeAdItem(
    val view: LinearLayout,
    val layoutId: Int,
    val adUnit:String,
    val showNew: Boolean,
    val screen: String
)