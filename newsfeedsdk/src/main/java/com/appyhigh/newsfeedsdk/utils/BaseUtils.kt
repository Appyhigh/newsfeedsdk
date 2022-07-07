@file:Suppress("DEPRECATION")

package com.appyhigh.newsfeedsdk.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.model.NativeAdItem
import com.google.android.gms.ads.*
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.mocklets.pluto.PlutoLog
import kotlin.concurrent.fixedRateTimer

const val TAG = "BaseUtils"
private var isTimerOn = false
var unifiedNative: NativeAd? = null
@SuppressLint("StaticFieldLeak")
var unifiedNativeAdView: NativeAdView? = null

var videoUnifiedNative: NativeAd? = null
@SuppressLint("StaticFieldLeak")
var videoUnifiedNativeAdView: NativeAdView? = null

fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // Set the media view.
    adView.mediaView = adView.findViewById(R.id.ad_media)

    // Set other ad assets.
    adView.headlineView = adView.findViewById(R.id.ad_headlines)
    adView.bodyView = adView.findViewById(R.id.ad_body_text)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action_button)
    adView.iconView = adView.findViewById(R.id.ad_icons)
    adView.priceView = adView.findViewById(R.id.ad_price_text)
    adView.starRatingView = adView.findViewById(R.id.ad_stars_bar)
    adView.storeView = adView.findViewById(R.id.ad_store_text)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser_text)

    // The headline and mediaContent are guaranteed to be in every UnifiedNativeAd.
    (adView.headlineView as TextView).text = nativeAd.headline
    (adView.headlineView as TextView).setTypeface(FeedSdk.font, Typeface.BOLD)

    adView.mediaView?.setMediaContent(nativeAd.mediaContent!!)

    // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
    // check before trying to display them.
//    if (nativeAd.body == null) {
//        adView.bodyView?.visibility = View.INVISIBLE
//    } else {
//        adView.bodyView?.visibility = View.VISIBLE
//        (adView.bodyView as TextView?)?.text = nativeAd.body
//    }
    if (nativeAd.callToAction == null) {
        adView.callToActionView?.visibility = View.INVISIBLE
    } else {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as Button?)?.text = nativeAd.callToAction
        (adView.callToActionView as Button?)?.setTypeface(FeedSdk.font, Typeface.BOLD)
    }
    if (nativeAd.icon == null) {
        adView.iconView?.visibility = View.GONE
    } else {
        (adView.iconView as ImageView?)?.setImageDrawable(
            nativeAd.icon?.drawable
        )
        adView.iconView?.visibility = View.VISIBLE
    }
    if (nativeAd.price == null) {
        adView.priceView?.visibility = View.INVISIBLE
    } else {
        adView.priceView?.visibility = View.VISIBLE
        (adView.priceView as TextView?)?.text = nativeAd.price
        (adView.priceView as TextView?)?.setTypeface(FeedSdk.font, Typeface.BOLD)
    }
    if (nativeAd.store == null) {
        adView.storeView?.visibility = View.INVISIBLE
    } else {
        adView.storeView?.visibility = View.VISIBLE
        (adView.storeView as TextView?)?.text = nativeAd.store
        (adView.storeView as TextView?)?.setTypeface(FeedSdk.font, Typeface.BOLD)
    }
    if (nativeAd.starRating == null) {
        adView.starRatingView?.visibility = View.INVISIBLE
    } else {
        (adView.starRatingView as RatingBar?)?.rating = nativeAd.starRating!!.toFloat()
        adView.starRatingView?.visibility = View.VISIBLE
    }
    if (nativeAd.advertiser == null) {
        adView.advertiserView?.visibility = View.INVISIBLE
    } else {
        (adView.advertiserView as TextView?)?.text = nativeAd.advertiser
        adView.advertiserView?.visibility = View.VISIBLE
        (adView.advertiserView as TextView?)?.setTypeface(FeedSdk.font, Typeface.BOLD)
    }

    // This method tells the Google Mobile Ads SDK that you have finished populating your
    // native ad view with this native ad.
    adView.setNativeAd(nativeAd)

    // Get the video controller for the ad. One will always be provided, even if the ad doesn't
    // have a video asset.
    val vc = nativeAd.mediaContent?.videoController

    // Updates the UI to say whether or not this ad has a video asset.
    if (vc?.hasVideoContent() == true) {

        // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
        // VideoController will call methods on this object when events occur in the video
        // lifecycle.
        vc.videoLifecycleCallbacks = object : VideoLifecycleCallbacks() {
        }
    }
}

@SuppressLint("InflateParams")
fun requestFeedAd(view: LinearLayout, @LayoutRes layoutId: Int, adUnit:String, showNew: Boolean = false,
                  screen: String="category", contentUrls: ArrayList<String>,  fromTimer: Boolean = false){
    if(!ApiConfig().checkShowAds()){
        return
    }
    Log.d("AdUtils", "requestFeedAd: "+ adUnit+"  screen "+screen)
    if(!fromTimer)
        checkAndStartTimer(contentUrls)
    try {
        Constants.nativeAdLifecycleCallbacks[view] = NativeAdItem(view, layoutId, adUnit, showNew, screen)
        if (unifiedNative != null && !showNew) {
            unifiedNativeAdView = LayoutInflater.from(view.context)
                .inflate(layoutId, null) as NativeAdView
            try {
                populateUnifiedNativeAdView(unifiedNative!!, unifiedNativeAdView!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            view.removeAllViews()
            view.addView(unifiedNativeAdView)
            unifiedNativeAdView?.visibility = View.VISIBLE
            if(screen=="liveMatches"){
                SpUtil.adShownListener?.onAdShown("liveMatches")
            }
        } else {
            try {
                val adLoader = AdLoader.Builder(
                    view.context,
                    adUnit
                )
                    .forNativeAd { unifiedNativeAd ->
                        unifiedNative = unifiedNativeAd
                        if (unifiedNative != null) {
                            unifiedNativeAdView = LayoutInflater.from(view.context)
                                .inflate(
                                    layoutId,
                                    null
                                ) as NativeAdView
                            try {
                                populateUnifiedNativeAdView(
                                    unifiedNative!!,
                                    unifiedNativeAdView!!
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            view.removeAllViews()
                            view.addView(unifiedNativeAdView)
                            unifiedNativeAdView!!.visibility = View.VISIBLE
                            if(screen=="liveMatches"){
                                SpUtil.adShownListener?.onAdShown("liveMatches")
                            }
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            if(!fromTimer){
                                requestFeedAd(view, layoutId, adUnit, showNew, screen, contentUrls, true)
                            }
                        }
                        override fun onAdClicked() {
                            super.onAdClicked()
                            SpUtil.eventsListener?.onAdClickedEvent()
                        }
                    })
                    .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
                if(contentUrls.isNotEmpty()){
                    PlutoLog.d("ContentMapping","requestFeedAd")
                    for (url in contentUrls) {
                        PlutoLog.d("ContentMapping","$url")
                    }
                    adLoader.loadAd(
                        AdRequest.Builder()
                            .setNeighboringContentUrls(contentUrls)
                            .build())
                } else {
                    PlutoLog.d("ContentMapping","empty")
                    adLoader.loadAd(AdRequest.Builder().build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        Log.d("FeedNativeAd", "requestFeedAd: crashed "+screen)
        e.printStackTrace()
    }
}

@SuppressLint("InflateParams")
fun requestVideoAd(view: LinearLayout, @LayoutRes layoutId: Int, adUnit:String, showNew: Boolean = false,
                   screen: String="videofeed", contentUrls: ArrayList<String>, fromTimer: Boolean = false){
    if(!ApiConfig().checkShowAds()){
        return
    }
    if(!fromTimer)
        checkAndStartTimer(contentUrls)
    try {
        Constants.nativeAdLifecycleCallbacks[view] = NativeAdItem(view, layoutId, adUnit, true, "videofeed")
        if (videoUnifiedNative != null && !showNew) {
            videoUnifiedNativeAdView = LayoutInflater.from(view.context)
                .inflate(layoutId, null) as NativeAdView
            try {
                populateUnifiedNativeAdView(videoUnifiedNative!!, videoUnifiedNativeAdView!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            view.removeAllViews()
            view.addView(videoUnifiedNativeAdView)
            videoUnifiedNativeAdView?.visibility = View.VISIBLE
        } else {
            try {
                val adLoader = AdLoader.Builder(
                    view.context,
                    adUnit
                )
                    .forNativeAd { unifiedNativeAd ->
                        videoUnifiedNative = unifiedNativeAd
                        if (videoUnifiedNative != null) {
                            videoUnifiedNativeAdView = LayoutInflater.from(view.context)
                                .inflate(
                                    layoutId,
                                    null
                                ) as NativeAdView
                            try {
                                populateUnifiedNativeAdView(
                                    videoUnifiedNative!!,
                                    videoUnifiedNativeAdView!!
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            view.removeAllViews()
                            view.addView(videoUnifiedNativeAdView)
                            videoUnifiedNativeAdView!!.visibility = View.VISIBLE
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            if(!fromTimer){
                                requestVideoAd(view, layoutId, adUnit, showNew, screen, contentUrls, true)
                            }
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            SpUtil.eventsListener?.onAdClickedEvent()
                        }
                    })
                    .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
                if(contentUrls.isNotEmpty()){
                    PlutoLog.d("ContentMapping","requestVideoFeedAd")
                    for (url in contentUrls) {
                        PlutoLog.d("ContentMapping","$url")
                    }
                    adLoader.loadAd(
                        AdRequest.Builder()
                            .setNeighboringContentUrls(contentUrls)
                            .build())
                } else {
                    PlutoLog.d("ContentMapping","empty")
                    adLoader.loadAd(AdRequest.Builder().build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val kotlin.Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val kotlin.Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun loadInterstitialAd(
    context: Context,
    adUnit: String,
    interstitialAdUtilLoadCallback: InterstitialAdUtilLoadCallback?
) {
    var mInterstitialAd: InterstitialAd?
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(
        context,
        adUnit,
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
                interstitialAdUtilLoadCallback?.onAdFailedToLoad(adError, mInterstitialAd)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
                interstitialAdUtilLoadCallback?.onAdLoaded(interstitialAd)

                mInterstitialAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad was dismissed.")
                            interstitialAdUtilLoadCallback?.onAdDismissedFullScreenContent()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                            Log.d(TAG, "Ad failed to show.")
                            interstitialAdUtilLoadCallback?.onAdFailedToShowFullScreenContent(
                                adError,
                            )
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad showed fullscreen content.")
                            mInterstitialAd = null
                            interstitialAdUtilLoadCallback?.onAdShowedFullScreenContent()
                        }
                    }
            }
        },
    )
}

fun showAdaptiveBanner(context: Context, adUnit: String, bannerAd: LinearLayout) {
    try{
        val adView = AdView(context)
        adView.adUnitId = adUnit
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                bannerAd.visibility = View.VISIBLE
//                refreshNativeAdHandler.postDelayed(refreshNativeAdRunnable, refreshRate)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
            }
        }
        bannerAd.removeAllViews()
        bannerAd.addView(adView)
        val adRequest: AdRequest = AdRequest.Builder().build()
        val adSize = getAdSize(context)
        adView.adSize = adSize
        adView.loadAd(adRequest)
    } catch (ex:java.lang.Exception){
        ex.printStackTrace()
    }
}

private fun getAdSize(context: Context): AdSize? {
    try{
        val display = (context as Activity).windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    } catch (ex:Exception){
        ex.printStackTrace()
        return null
    }
}

private fun checkAndStartTimer(contentUrls: ArrayList<String>){
    try{
        if(!isTimerOn){
            isTimerOn = true
            var interval:Long = (FeedSdk.nativeAdInterval)*1000
            if(interval<=0){
                interval = 60000L
            }
            fixedRateTimer("nativeAdTimer", false, interval, interval) {
                try{
                    Log.d(TAG, "checkAndStartTimer: ")
                    for(ad in Constants.nativeAdLifecycleCallbacks.values){
                        Handler(Looper.getMainLooper()).post {
                            try{
                                if(ad.view.isAttachedToWindow){
                                    if(ad.screen == "videofeed"){
                                        requestVideoAd(ad.view, ad.layoutId, ad.adUnit, true, ad.screen, contentUrls, true)
                                    } else{
                                        requestFeedAd(ad.view, ad.layoutId, ad.adUnit, true, ad.screen, contentUrls, true)
                                    }
                                } else if((ad.view.context is Activity) && (ad.view.context as Activity).isDestroyed){
                                    Constants.nativeAdLifecycleCallbacks.remove(ad.view)
                                }
                            } catch (ex:Exception){
                                ex.printStackTrace()
                            }
                        }
                    }
                } catch (ex:Exception){
                    ex.printStackTrace()
                }
            }
        }
    } catch (ex:Exception){
        ex.printStackTrace()
    }
}

interface InterstitialAdUtilLoadCallback {
    fun onAdFailedToLoad(adError: LoadAdError, ad: InterstitialAd?)
    fun onAdLoaded(ad: InterstitialAd?)
    fun onAdDismissedFullScreenContent()
    fun onAdFailedToShowFullScreenContent(adError: AdError?)
    fun onAdShowedFullScreenContent()
}