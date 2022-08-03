package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

class ApiGetPostsByTag {

    fun getPostsByTagEncrypted(
        apiUrl: String,
        tag: String,
        postSource: String,
        feedType: String,
        postsByTagResponseListener: PostsByTagResponseListener
    ) {
        var languages: String? = ""
        for ((i, language) in FeedSdk.languagesList.withIndex()) {
            if (i < FeedSdk.languagesList.size - 1) {
                languages =
                    languages + language.id.lowercase(Locale.getDefault()) + ","
            } else {
                languages += language.id.lowercase(Locale.getDefault())
            }
        }
        if (languages == "") {
            languages = null
        }

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.TAG)
        keys.add(Constants.POST_SOURCE)
        keys.add(Constants.FEED_TYPE)
        keys.add(Constants.LANG)

        values.add(tag)
        values.add(postSource)
        values.add(feedType)
        values.add(languages)

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, keys, values)

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
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiGetPostsByTag $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val getFeedsResponseBase: GetFeedsResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<GetFeedsResponse>() {}.type
                    )
                val getFeedsResponse: Response<GetFeedsResponse> =
                    Response.success(getFeedsResponseBase)
                try {
                    postsByTagResponseListener.onSuccess(
                        getFeedsResponse.body()!!,
                        getFeedsResponse.raw().request.url.toString(),
                        getFeedsResponse.raw().sentRequestAtMillis
                    )
                }catch (e:Exception){
                    LogDetail.LogEStack(e)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetPostsByTag $apiUrl", e.toString())
            }
        })

    }


    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            LogDetail.LogDE(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface PostsByTagResponseListener {
        fun onSuccess(getFeedsResponse: GetFeedsResponse, url: String, timeStamp: Long)
    }
}