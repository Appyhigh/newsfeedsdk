package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.PostDetailsModel
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetPostDetails {
    private var spUtil = SpUtil.spUtilInstance
    fun getPostDetailsEncrypted(
        apiUrl: String,
        postId: String,
        postSource: String,
        feedType: String,
        postDetailsResponse: PostDetailsResponse
    ) {

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.POST_ID)
        keys.add(Constants.POST_SOURCE)
        keys.add(Constants.FEED_TYPE)
        keys.add(Constants.PAGE_NUMBER)

        values.add(postId)
        values.add(postSource)
        values.add(feedType)
        values.add(Constants.postDetailPageNo.toString())

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
                LogDetail.LogDE("ApiGetPostDetails $apiUrl", response.toString())

                val gson: Gson = GsonBuilder().create()
                val postDetailsModelBase: PostDetailsModel =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<PostDetailsModel>() {}.type
                    )
                val postDetailsModel: Response<PostDetailsModel> =
                    Response.success(postDetailsModelBase)
                Constants.postDetailPageNo += 1
                try {
                    postDetailsResponse.onSuccess(
                        postDetailsModel.body()!!,
                        postDetailsModel.raw().request.url.toString(),
                        postDetailsModel.raw().sentRequestAtMillis
                    )
                } catch (e: Exception) {
                    LogDetail.LogEStack(e)
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetPostDetails $apiUrl", e.toString())
                postDetailsResponse.onFailure()
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

    interface PostDetailsResponse {
        fun onSuccess(postDetailsModel: PostDetailsModel, url: String, timeStamp: Long)
        fun onFailure()
    }
}