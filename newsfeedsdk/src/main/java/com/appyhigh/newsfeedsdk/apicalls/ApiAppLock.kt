package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
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

class ApiAppLock {

    fun getAppLockPosts(
        showMultiplePosts: Boolean,
        showOnlyReels: Boolean,
        interests: String,
        language: String,
        feedsResponseListener: ApiGetFeeds.GetFeedsResponseListener
    ){
        val apiUrl = if(showMultiplePosts) Endpoints.GET_MULTIPLE_POSTS else Endpoints.GET_SINGLE_POST
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.COUNTRY_CODE)
        if(showOnlyReels) {
            keys.add(Constants.IS_VIDEO)
            keys.add(Constants.SHORT_VIDEO)
        }

        values.add(FeedSdk.sdkCountryCode ?: "in")
        if(showOnlyReels) {
            values.add("true")
            values.add("true")
        }
        Constants.userDetails?.let {
            keys.add(Constants.BLOCKED_PUBLISHERS)
            values.add(Constants.getStringFromList(it.blockedPublishers))
        }
        if(interests.isNotEmpty()){
            keys.add(Constants.INTERESTS)
            values.add(interests)
        }
        if(language.isNotEmpty()){
            keys.add(Constants.LANGUAGE)
            values.add(language)
        }

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(
                Constants.GET,
                apiUrl,
                keys,
                values
            )
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
                LogDetail.LogDE("ApiAppLock $apiUrl", response)

                val gson: Gson = GsonBuilder().create()
                val getFeedsResponseBase: GetFeedsResponse =
                    gson.fromJson(
                        response,
                        object : TypeToken<GetFeedsResponse>() {}.type
                    )
                val getFeedsResponse: Response<GetFeedsResponse> =
                    Response.success(getFeedsResponseBase)

                feedsResponseListener.onSuccess(
                    getFeedsResponse.body()!!,
                    getFeedsResponse.raw().request.url.toString(),
                    getFeedsResponse.raw().sentRequestAtMillis
                )
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiAppLock $apiUrl", e.toString())
            }
        })
    }
}