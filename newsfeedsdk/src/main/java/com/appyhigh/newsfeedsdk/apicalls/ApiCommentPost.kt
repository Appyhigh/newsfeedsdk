package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.APIClient
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCommentPost {
    fun postCommentEncrypted(
        apiUrl: String,
        token: String,
        postId: String,
        commentType: String,
        commentValue: String,
        postCommentResponse: PostCommentResponse
    ) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, Constants.POST)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)

        val dataJO = JsonObject()
        dataJO.addProperty("comment_type", commentType)
        dataJO.addProperty("post_id", postId)
        dataJO.addProperty("comment_value", commentValue)
        main.add(Constants.API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(Constants.AUTHORIZATION, token)
        main.add(Constants.API_HEADER, headerJO)
        try {
            allDetails.add(Constants.API_CALLING, main)
            allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())

                val gson: Gson = GsonBuilder().create()
                val feedCommentResponseWrapper: FeedCommentResponseWrapper =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<FeedCommentResponseWrapper?>() {}.type
                    )
                postCommentResponse.onSuccess(feedCommentResponseWrapper)
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
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
            Log.e(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface PostCommentResponse {
        fun onSuccess(feedCommentResponseWrapper: FeedCommentResponseWrapper)
    }
}