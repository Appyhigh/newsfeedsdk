package com.appyhigh.newsfeedsdk.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.LoadNativeAdListener
import com.appyhigh.newsfeedsdk.model.NativeAdItem
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlin.concurrent.fixedRateTimer

class AdUtilsSDK {

    private var isTimerOn = false
    val TAG = "AdUtilsSDK"

    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
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
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
            }
        }
    }

    private var nativeAdsList = HashMap<String, NativeAd>()

    @SuppressLint("InflateParams")
    fun requestFeedAdWithoutInbuiltTimer(view: LinearLayout, @LayoutRes layoutId: Int, adUnit:String, screen: String="category", loadNativeAdListener: LoadNativeAdListener?){
        if(!FeedSdk.showAds && screen!="searchSticky"){
            return
        }
        var unifiedNative: NativeAd? = null
        var unifiedNativeAdView: NativeAdView? = null
        Log.d("AdUtilsSDK", "requestFeedAd: "+ adUnit+"  screen "+screen)
        try {
            Constants.nativeAdLifecycleCallbacks[view] = NativeAdItem(view, layoutId, adUnit, false, screen)
            try {
                if(nativeAdsList.containsKey("searchSticky") && nativeAdsList["searchSticky"]!=null){
                    unifiedNativeAdView = LayoutInflater.from(view.context).inflate(layoutId, null) as NativeAdView
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
                }
                val adLoader = AdLoader.Builder(
                    view.context, adUnit)
                    .forNativeAd { unifiedNativeAd ->
                        unifiedNative = unifiedNativeAd
                        nativeAdsList[screen] = unifiedNativeAd
                        if (unifiedNative != null) {
                            unifiedNativeAdView = LayoutInflater.from(view.context).inflate(layoutId, null) as NativeAdView
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
                            loadNativeAdListener?.onAdLoadFailed()
                        }

                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            loadNativeAdListener?.onAdLoadSuccess()
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            SpUtil.eventsListener?.onAdClickedEvent()
                        }
                    })
                    .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            Log.d("FeedNativeAd", "requestFeedAd: crashed "+screen)
            e.printStackTrace()
        }
    }

    @SuppressLint("InflateParams")
    fun requestFeedAd(view: LinearLayout, @LayoutRes layoutId: Int, adUnit:String, screen: String="category", fromTimer:Boolean = false){
        if(!FeedSdk.showAds && screen!="searchSticky"){
            return
        }
        var unifiedNative: NativeAd? = null
        var unifiedNativeAdView: NativeAdView? = null
        Log.d("AdUtilsSDK", "requestFeedAd: "+ adUnit+"  screen "+screen)
        if (!fromTimer)
            checkAndStartTimer()
        try {
            Constants.nativeAdLifecycleCallbacks[view] = NativeAdItem(view, layoutId, adUnit, false, screen)
            try {
                val adLoader = AdLoader.Builder(
                    view.context,
                    adUnit
                )
                    .forNativeAd { unifiedNativeAd ->
                        unifiedNative = unifiedNativeAd
                        if (unifiedNative != null) {
                            unifiedNativeAdView = LayoutInflater.from(view.context).inflate(layoutId, null) as NativeAdView
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
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
                        override fun onAdClicked() {
                            super.onAdClicked()
                            SpUtil.eventsListener?.onAdClickedEvent()
                        }
                    })
                    .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            Log.d("FeedNativeAd", "requestFeedAd: crashed "+screen)
            e.printStackTrace()
        }
    }


    private fun checkAndStartTimer(){
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
                                        AdUtilsSDK().requestFeedAd(ad.view, ad.layoutId, ad.adUnit, ad.screen, true)
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

}