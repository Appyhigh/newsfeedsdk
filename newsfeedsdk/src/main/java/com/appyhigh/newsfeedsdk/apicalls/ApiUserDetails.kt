package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.UserIdResponse
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

class ApiUserDetails {

    fun checkUserId(
        email: String? = null,
        phoneNumber: String? = null,
        userIdResponseListener: UserIdResponseListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.EMAIL)
        keys.add(Constants.PHONE_NUMBER)

        values.add(email)
        values.add(phoneNumber)

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, Endpoints.CHECK_EMAIL_NUMBER_AVAILABILITY_ENCRYPTED, keys, values)
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
                LogDetail.LogDE("ApiUserDetails $apiUrl", response)

                val gson: Gson = GsonBuilder().create()
                val userIdResponseBase: UserIdResponse =
                    gson.fromJson(
                        response,
                        object : TypeToken<UserIdResponse>() {}.type
                    )
                val userIdResponse: Response<UserIdResponse> = Response.success(userIdResponseBase)
                userIdResponse.body()?.let { it1 -> userIdResponseListener.onSuccess(it1) }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiUserDetails "+Endpoints.CHECK_EMAIL_NUMBER_AVAILABILITY_ENCRYPTED, e.toString())
            }
        })
    }

    fun getUserResponseEncrypted(
        apiUrl: String,
        userResponseListener: UserResponseListener
    ) {
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, ArrayList(), ArrayList())
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
                LogDetail.LogDE("ApiUserDetails correct $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val userResponse: UserResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<UserResponse?>() {}.type
                    )
                Constants.impreesionModel = userResponse.user?.impressions
                Constants.isChecked =
                    if (userResponse.user?.cricket_notification != null) userResponse.user?.cricket_notification!! else FeedSdk.isCricketApp()
                addTopic(Constants.isChecked)
                userResponseListener.onSuccess(userResponse)
                SpUtil.userResponseListener?.onSuccess(userResponse)
                Constants.userDetails = userResponse.user
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiUserDetails $apiUrl", e.toString())
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

    interface UserResponseListener {
        fun onSuccess(userDetails: UserResponse)
    }

    interface UserIdResponseListener {
        fun onSuccess(userIdResponse: UserIdResponse)
    }
}

fun addTopic(subscribe: Boolean) {
    try {
        val topic = FeedSdk.appName?.replace(" ", "")?.lowercase(Locale.getDefault()) + "Cricket"
        if (subscribe) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "subscribed to $topic"
                    if (!task.isSuccessful) {
                        msg = "not subscribed to $topic"
                    }
                    LogDetail.LogD("FeedSdk", msg)
                }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "unsubscribed to $topic"
                    if (!task.isSuccessful) {
                        msg = "not unsubscribed to $topic"
                    }
                    LogDetail.LogD("FeedSdk", msg)
                }
        }
    } catch (e: Exception) {
        LogDetail.LogEStack(e)
        LogDetail.LogDE("FeedSdk", "firebaseUnSubscribeToTopic: " + e)
    }
}