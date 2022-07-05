package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.APISearchStickyClient
import com.appyhigh.newsfeedsdk.model.SearchStickyActionModel
import com.appyhigh.newsfeedsdk.model.SearchStickyWidgetModel
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.gson.Gson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers


class ApiSearchSticky {
    private var spUtil = SpUtil.spUtilInstance

    fun getIconsNotification(stickyWidgetResponseListener: StickyWidgetResponseListener){
        APISearchStickyClient().getSearchStickyApiInterface()?.getIconsNotification(spUtil!!.getString(Constants.SEARCH_STICKY_JWT_TOKEN))
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                it?.let { stickyWidgetResponseListener.onSuccess(it.toString()) }
            }, {
                it?.let { error -> handleApiError(error) }
                stickyWidgetResponseListener.onSuccess(spUtil!!.getString(Constants.STICKY_NOTIFICATION)!!)
            })
    }

    fun getIconsSearch(stickyWidgetResponseListener: StickyWidgetResponseListener){
        APISearchStickyClient().getSearchStickyApiInterface()?.getIconsSearch(spUtil!!.getString(Constants.SEARCH_STICKY_JWT_TOKEN))
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                it?.let { stickyWidgetResponseListener.onSuccess(it.toString()) }
            }, {
                it?.let { error -> handleApiError(error) }
                stickyWidgetResponseListener.onSuccess(spUtil!!.getString(Constants.WEB_PLATFORMS)!!)
            })
    }

    fun userActionNotification(widget: String){
        val searchStickyActionModel = SearchStickyActionModel(widget, FeedSdk.sdkCountryCode ?: "in")
        APISearchStickyClient().getSearchStickyApiInterface()?.userActionNotification(spUtil!!.getString(Constants.SEARCH_STICKY_JWT_TOKEN), searchStickyActionModel)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                it?.let {  }
            }, {
                it?.let { error -> handleApiError(error) }
            })
    }

    fun userActionSearch(widget: String){
        val searchStickyActionModel = SearchStickyActionModel(widget, FeedSdk.sdkCountryCode ?: "in")
        APISearchStickyClient().getSearchStickyApiInterface()?.userActionSearch(spUtil!!.getString(Constants.SEARCH_STICKY_JWT_TOKEN), searchStickyActionModel)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                it?.let {  }
            }, {
                it?.let { error -> handleApiError(error) }
            })
    }



    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            Log.e(ApiSearchSticky::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface StickyWidgetResponseListener {
        fun onSuccess(stickyWidgetModelString: String)
    }
}