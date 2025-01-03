package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.PublisherBlocklistModel
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetPublisherPosts {
    fun getPublisherPostsEncrypted(
        apiUrl: String,
        pageNumber: Int,
        publisherId: String,
        publisherPostsResponseListener: PublisherPostsResponseListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.PUBLISHER_ID)

        values.add(pageNumber.toString())
        values.add(publisherId)
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
            override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                LogDetail.LogDE("ApiGetPublisherPosts $apiUrl", response)
                val gson: Gson = GsonBuilder().create()
                val getFeedsResponseBase: GetFeedsResponse =
                    gson.fromJson(
                        response,
                        object : TypeToken<GetFeedsResponse>() {}.type
                    )
                val getFeedsResponse: Response<GetFeedsResponse> =
                    Response.success(getFeedsResponseBase)
                publisherPostsResponseListener.onSuccess(
                    getFeedsResponse.body()!!,
                    "https://feeds.apyhi.com$apiUrl",
                    timeStamp
                )
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetPublisherPosts $apiUrl", e.toString())
            }
        })
    }

    fun getPublisherBlocklist(
        publishersList: ArrayList<String>,
        listener: PublisherBlocklistListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.PUBLISHER_IDS)
        values.add(Constants.getStringFromList(publishersList)?:"")
        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, Endpoints.GET_BLOCK_PUBLISHER_DETAILS, keys, values)

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
            override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                LogDetail.LogDE("ApiGetPublisherPosts $apiUrl", response)
                val gson: Gson = GsonBuilder().create()
                val getPublisherBlocklistModel: PublisherBlocklistModel =
                    gson.fromJson(
                        response,
                        object : TypeToken<PublisherBlocklistModel>() {}.type
                    )
                val getPublisherBlocklistResponse: Response<PublisherBlocklistModel> =
                    Response.success(getPublisherBlocklistModel)
                try{
                    listener.onSuccess(getPublisherBlocklistResponse.body()!!)
                }catch (ex:Exception){
                    ex.printStackTrace()
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetPublisherPosts ${Endpoints.GET_BLOCK_PUBLISHER_DETAILS}", e.toString())
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

    interface PublisherPostsResponseListener {
        fun onSuccess(feedsResponse: GetFeedsResponse, url: String, timeStamp: Long)
    }

    interface PublisherBlocklistListener {
        fun onSuccess(response: PublisherBlocklistModel)
    }
}