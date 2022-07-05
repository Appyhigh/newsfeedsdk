package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.APIClient
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.UserIdResponse
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.firebase.messaging.FirebaseMessaging
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
import java.util.*

class ApiUserDetails {
    private var spUtil = SpUtil.spUtilInstance

    fun checkUserId(
        email: String? = null,
        phoneNumber: String? = null,
        userIdResponseListener: UserIdResponseListener
    ) {
        APIClient().getApiInterface()
            ?.checkUserId(spUtil!!.getString(Constants.JWT_TOKEN), email, phoneNumber)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                it?.let {
                    userIdResponseListener.onSuccess(it)
                }
            }, {
                it?.let { error -> handleApiError(error) }
            })
    }

    fun getUserResponseEncrypted(
        apiUrl: String,
        token: String,
        userResponseListener: UserResponseListener
    ) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, Constants.GET)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)

        val dataJO = JsonObject()
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
                LogDetail.LogDE("ApiUserDetails correct $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val userResponse: UserResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<UserResponse?>() {}.type
                    )
                Constants.impreesionModel = userResponse.user?.impressions
                Constants.isChecked =
                    if (userResponse.user?.cricket_notification != null) userResponse.user?.cricket_notification!! else FeedSdk.appName == "CricHouse"
                addTopic(Constants.isChecked)
                userResponseListener.onSuccess(userResponse)
                SpUtil.userResponseListener?.onSuccess(userResponse)
                Constants.userDetails = userResponse.user
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiUserDetails $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiUserDetails $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiUserDetails $apiUrl", e.toString())
            }
        })
    }

//    fun getUserResponse(userId: String?, userResponseListener: UserResponseListener) {
//        APIClient().getApiInterface()
//            ?.getUserDetails(spUtil!!.getString(Constants.JWT_TOKEN))
//            ?.subscribeOn(Schedulers.io())
//            ?.observeOn(AndroidSchedulers.mainThread())
//            ?.subscribe({
//                it?.let {
//                    Constants.impreesionModel = it.user?.impressions
//                    Constants.isChecked = if(it.user?.cricket_notification!=null) it.user?.cricket_notification!! else FeedSdk.appName=="CricHouse"
//                    addTopic(Constants.isChecked)
//                    userResponseListener.onSuccess(it)
//                    SpUtil.userResponseListener?.onSuccess(it)
//                    Constants.userDetails = it.user
//                }
//            }, {
//                it?.let { error -> handleApiError(error) }
//            })
//    }


    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            Log.e(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
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
                    Log.d("FeedSdk", msg)
                }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "unsubscribed to $topic"
                    if (!task.isSuccessful) {
                        msg = "not unsubscribed to $topic"
                    }
                    Log.d("FeedSdk", msg)
                }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("FeedSdk", "firebaseUnSubscribeToTopic: " + e)
    }
}