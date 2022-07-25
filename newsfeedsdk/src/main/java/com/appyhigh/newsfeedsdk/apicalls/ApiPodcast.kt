package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiPodcast {
    private var spUtil = SpUtil.spUtilInstance

    fun getPodcastHomeEncrypted(
        apiUrl: String,
        token: String,
        language: String?,
        page_number: Int,
        podcastResponseListener: PodcastResponseListener
    ) {
        var selectedLang = language
        if (language.isNullOrBlank()) {
            selectedLang = null
        }
        var feedType: String? = null
        var postSource: String? = null
        if (SpUtil.pushIntent != null && SpUtil.pushIntent!!.hasExtra("page")
            && SpUtil.pushIntent!!.getStringExtra("page") == "SDK://podcastHome"
        ) {
            if (SpUtil.pushIntent!!.hasExtra("feed_type")) {
                feedType = SpUtil.pushIntent!!.getStringExtra("feed_type")!!
            }
            if (SpUtil.pushIntent!!.hasExtra("post_source")) {
                postSource = SpUtil.pushIntent!!.getStringExtra("post_source")!!
            }
        }

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.LANGUAGE)
        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.POST_SOURCE)
        keys.add(Constants.FEED_TYPE)

        values.add(selectedLang)
        values.add(page_number.toString())
        values.add(postSource)
        values.add(feedType)

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, token, keys, values)

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
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val getFeedsResponseBase: PodcastResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<PodcastResponse>() {}.type
                    )
                val getFeedsResponse: Response<PodcastResponse> =
                    Response.success(getFeedsResponseBase)
                try {
                    podcastResponseListener.onSuccess(
                        getFeedsResponse.body()!!,
                        getFeedsResponse.raw().request.url.toString(),
                        getFeedsResponse.raw().sentRequestAtMillis
                    )
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }

            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiPodcast $apiUrl", e.toString())
            }
        })
    }

    fun getPodcastCategoryEncrypted(
        apiUrl: String,
        token: String,
        interests: String?,
        language: String?,
        page_number: Int,
        podcastResponseListener: PodcastResponseListener
    ) {
        var selectedLang = language
        if (language.isNullOrBlank()) {
            selectedLang = null
        }

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.INTERESTS)
        keys.add(Constants.LANGUAGE)
        keys.add(Constants.PAGE_NUMBER)

        values.add(interests)
        values.add(selectedLang)
        values.add(page_number.toString())

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, token, keys, values)

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
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val getFeedsResponseBase: PodcastResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<PodcastResponse>() {}.type
                    )
                val getFeedsResponse: Response<PodcastResponse> =
                    Response.success(getFeedsResponseBase)
                try {
                    podcastResponseListener.onSuccess(
                        getFeedsResponse.body()!!,
                        getFeedsResponse.raw().request.url.toString(),
                        getFeedsResponse.raw().sentRequestAtMillis
                    )
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }

            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiPodcast $e", e.toString())
            }
        })
    }

    fun getPodcastPublisherEncrypted(
        apiUrl: String,
        token: String,
        publisherId: String?,
        language: String?,
        page_number: Int,
        podcastResponseListener: PodcastResponseListener
    ) {
        var selectedLang = language
        if (language.isNullOrBlank()) {
            selectedLang = null
        }
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.PUBLISHER_ID)
        keys.add(Constants.LANGUAGE)
        keys.add(Constants.PAGE_NUMBER)

        values.add(publisherId)
        values.add(selectedLang)
        values.add(page_number.toString())

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, token, keys, values)

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
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val getFeedsResponseBase: PodcastResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<PodcastResponse>() {}.type
                    )
                val getFeedsResponse: Response<PodcastResponse> =
                    Response.success(getFeedsResponseBase)
                try {
                    podcastResponseListener.onSuccess(
                        getFeedsResponse.body()!!,
                        getFeedsResponse.raw().request.url.toString(),
                        getFeedsResponse.raw().sentRequestAtMillis
                    )
                } catch (ex: Exception) {
                    LogDetail.LogEStack(ex)
                }

            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiPodcast $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiPodcast $e", e.toString())
            }
        })
    }
}

/**
 * Handle Error messages
 */
private fun handleApiError(throwable: Throwable) {
    throwable.message?.let {
        LogDetail.LogDE(ApiPodcast::class.java.simpleName, "handleApiError: $it")
    }
}

interface PodcastResponseListener {
    fun onSuccess(podcastResponse: PodcastResponse, url: String, timeStamp: Long)
}

data class PodcastResponse(
    @SerializedName("cards")
    @Expose
    var cards: List<Card> = ArrayList(),
    @SerializedName("ad_placement")
    @Expose
    var adPlacement: List<Int> = arrayListOf(6)
)