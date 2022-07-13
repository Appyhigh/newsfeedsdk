package com.appyhigh.newsfeedsdk.apicalls

import android.content.Context
import android.webkit.WebView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.AdsModel
import com.appyhigh.newsfeedsdk.model.ItemAdsModel
import com.appyhigh.newsfeedsdk.model.PrivateAdResponse
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiConfig {
    private var adsModel:AdsModel?=null
    private var showAds = false

    fun getAdsModel(context: Context):AdsModel{
        if(adsModel==null) {
            SpUtil.spUtilInstance?.init(context)
            val json = SpUtil.spUtilInstance!!.getString(Constants.ADS_MODEL, "")
            adsModel = if (json!!.isEmpty()) AdsModel()
            else {
                Gson().fromJson(json, AdsModel::class.java)
            }
            showAds = (adsModel!!.showParentAdmobAds || adsModel!!.showPrivateAds)
        }
        return adsModel!!
    }

    fun checkShowAds(context: Context):Boolean{
        if(adsModel==null) getAdsModel(context)
        return showAds
    }

    fun requestAd(context: Context, adType: String, listener: ConfigAdRequestListener, isBanner: Boolean = false){
        if(checkShowAds(context)){
            val itemAdsModel = getItemAdModel(adType)
            if(itemAdsModel.showPrivate){
                ApiPrivateAds().getPrivateAd(context, isBanner, object : PrivateAdResponseListener{
                    override fun onSuccess(privateAdResponse: PrivateAdResponse) {
                        privateAdResponse.creative?.let {
                            val webView = WebView(context)
                            webView.loadDataWithBaseURL(null, it, "text/html", "utf-8", null)
                            listener.onPrivateAdSuccess(webView)
                            ApiPrivateAds().hitAdUrls(privateAdResponse.eUrl?:"", privateAdResponse.nUrl?:"")
                        }
                    }
                })
            } else if(itemAdsModel.showAdmob){
                listener.onAdmobAdSuccess(itemAdsModel.admobId)
            } else{
                listener.onAdHide()
            }
        }
    }

    private fun getItemAdModel(adType: String): ItemAdsModel {
        return when (adType){
            "feed_native" -> adsModel!!.feedNative
            "video_native" -> adsModel!!.videoNative
            "search_page_native" -> adsModel!!.searchPageNative
            "search_footer_banner" -> adsModel!!.searchFooterBanner
            "post_detail_article_top_native" -> adsModel!!.postDetailArticleTopNative
            "post_detail_article_end_native" -> adsModel!!.postDetailArticleEndNative
            "post_detail_footer_banner" -> adsModel!!.postDetailFooterBanner
            else -> adsModel!!.feedNative
        }
    }

    fun configEncrypted(token: String) {
        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, Endpoints.GET_CONFIG_ENCRYPTED, token, ArrayList(), ArrayList())

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiConfig $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val configResponseBase: AdsModel =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<AdsModel>() {}.type
                    )
                val configResponse: Response<AdsModel> = Response.success(configResponseBase)
                adsModel = configResponse.body()
                showAds = (adsModel!!.showParentAdmobAds || adsModel!!.showPrivateAds)
                SpUtil.spUtilInstance!!.putString(Constants.ADS_MODEL, Gson().toJson(adsModel))
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiConfig $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiConfig $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiConfig ${Endpoints.GET_CONFIG_ENCRYPTED}", e.toString())
            }
        })
    }
}

interface ConfigAdRequestListener{
    fun onPrivateAdSuccess(webView: WebView)
    fun onAdmobAdSuccess(adId: String)
    fun onAdHide()
}