package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.FeedCommentResponseWrapper
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCommentPost {
    fun postCommentEncrypted(
        apiUrl: String,
        postId: String,
        postSource: String?,
        feedType: String?,
        commentType: String,
        commentValue: String,
        postCommentResponse: PostCommentResponse
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("comment_type")
        keys.add("post_id")
        keys.add("comment_value")
        values.add(commentType)
        values.add(postId)
        values.add(commentValue)

        if(!postSource.isNullOrEmpty()) {
            keys.add(Constants.POST_SOURCE)
            values.add(postSource)
        }
        if(!feedType.isNullOrEmpty()) {
            keys.add(Constants.FEED_TYPE)
            values.add(feedType)
        }
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, apiUrl, keys, values)
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
                LogDetail.LogDE("ApiPostImpression $apiUrl", response)

                val gson: Gson = GsonBuilder().create()
                val feedCommentResponseWrapper: FeedCommentResponseWrapper =
                    gson.fromJson(
                        response,
                        object : TypeToken<FeedCommentResponseWrapper?>() {}.type
                    )
                postCommentResponse.onSuccess(feedCommentResponseWrapper)
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiPostImpression $apiUrl", e.toString())
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

    interface PostCommentResponse {
        fun onSuccess(feedCommentResponseWrapper: FeedCommentResponseWrapper)
    }
}