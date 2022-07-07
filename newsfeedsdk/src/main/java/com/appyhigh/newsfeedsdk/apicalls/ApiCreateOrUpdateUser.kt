package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.API_CALLING
import com.appyhigh.newsfeedsdk.Constants.API_DATA
import com.appyhigh.newsfeedsdk.Constants.API_HEADER
import com.appyhigh.newsfeedsdk.Constants.API_INTERNAL
import com.appyhigh.newsfeedsdk.Constants.API_METHOD
import com.appyhigh.newsfeedsdk.Constants.API_URl
import com.appyhigh.newsfeedsdk.Constants.AUTHORIZATION
import com.appyhigh.newsfeedsdk.Constants.COUNTRY_CODE
import com.appyhigh.newsfeedsdk.Constants.DAILLING_CODE
import com.appyhigh.newsfeedsdk.Constants.DEVICE_DETAIL
import com.appyhigh.newsfeedsdk.Constants.EMAIL
import com.appyhigh.newsfeedsdk.Constants.FIRST_NAME
import com.appyhigh.newsfeedsdk.Constants.LAST_NAME
import com.appyhigh.newsfeedsdk.Constants.PHONE_NUMBER
import com.appyhigh.newsfeedsdk.Constants.PUSH_TOKEN_
import com.appyhigh.newsfeedsdk.Constants.USER_DETAIL
import com.appyhigh.newsfeedsdk.Constants.USER_NAME
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.FeedSdk.Companion.isExistingUser
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.User
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.gson.JsonObject
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCreateOrUpdateUser {
    private var spUtil = SpUtil.spUtilInstance
    fun createOrUpdateUserEncrypted(
        apiUrl: String,
        token: String,
        firebaseToken: String,
        sdkCountryCode: String?,
        user: User?
    ) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(API_URl, apiUrl)
        main.addProperty(API_METHOD, Constants.POST)
        main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty(PUSH_TOKEN_, firebaseToken)
        dataJO.addProperty(COUNTRY_CODE, sdkCountryCode)
        dataJO.addProperty(FIRST_NAME, user?.firstName)
        dataJO.addProperty(LAST_NAME, user?.lastName)
        dataJO.addProperty(EMAIL, user?.email)
        dataJO.addProperty(PHONE_NUMBER, user?.phoneNumber)
        dataJO.addProperty(DAILLING_CODE, user?.dailling_code)
        if (user?.username.isNullOrEmpty() || !isExistingUser) {
            dataJO.addProperty(USER_NAME, user?.username)
        }
        main.add(API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(AUTHORIZATION, token)
        main.add(API_HEADER, headerJO)
        try {
            allDetails.add(API_CALLING, main)
            allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
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
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
                handleCreateUserResponse()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", e.toString())
            }
        })
    }

    fun updateCricketNotificationEncrypt(
        apiUrl: String,
        token: String,
        isChecked: Boolean,
        onlyCricketHome: Boolean = false
    ) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(API_URl, apiUrl)
        main.addProperty(API_METHOD, Constants.POST)
        main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("cricket_notification", isChecked)
        main.add(API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(AUTHORIZATION, token)
        main.add(API_HEADER, headerJO)
        try {
            allDetails.add(API_CALLING, main)
            allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
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
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
                addTopic(isChecked)
                Constants.userDetails?.cricket_notification = isChecked
                if (onlyCricketHome && SpUtil.onRefreshListeners.containsKey("cricketHome")) {
                    SpUtil.onRefreshListeners["cricketHome"]?.onRefreshNeeded()
                } else {
                    handleCreateUserResponse()
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", e.toString())
            }
        })
    }

    fun updateCryptoWatchlistEncrypted(apiUrl: String, token: String) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(API_URl, apiUrl)
        main.addProperty(API_METHOD, Constants.POST)
        main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("crypto_watchlist", getWatchlistString())
        main.add(API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(AUTHORIZATION, token)
        main.add(API_HEADER, headerJO)
        try {
            allDetails.add(API_CALLING, main)
            allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
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
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
                Log.d("UpdateUser", "updated crypto watchlist")
                SpUtil.cryptoWatchListUpdateListener?.onCryptoWatchListUpdated(Constants.cryptoWatchList)
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", e.toString())
            }
        })
    }

    fun updateUserDislikeInterests(token: String, interest: String) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(API_URl, Endpoints.UPDATE_USER_ENCRYPTED)
        main.addProperty(API_METHOD, Constants.POST)
        main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("user_disliked_interests", interest)
        main.add(API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(AUTHORIZATION, token)
        main.add(API_HEADER, headerJO)
        try {
            allDetails.add(API_CALLING, main)
            allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(allDetails.toString().toByteArray(StandardCharsets.UTF_8)) + "." + publicKey
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser ${Endpoints.UPDATE_USER_ENCRYPTED}", e.toString())
            }
        })
    }

    fun updateUserInterests(token: String, interests: String) {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(API_URl, Endpoints.UPDATE_USER_ENCRYPTED)
        main.addProperty(API_METHOD, Constants.POST)
        main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("interests", interests)
        main.add(API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(AUTHORIZATION, token)
        main.add(API_HEADER, headerJO)
        try {
            allDetails.add(API_CALLING, main)
            allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(allDetails.toString().toByteArray(StandardCharsets.UTF_8)) + "." + publicKey
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String?, response: JSONObject?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response.toString())
                for (listener in SpUtil.onRefreshListeners) {
                    listener.value.onRefreshNeeded()
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser ${Endpoints.UPDATE_USER_ENCRYPTED}", e.toString())
            }
        })
    }


    fun getWatchlistString():String{
        var watchlistString = ""
        var i = 0
        for (crypto in Constants.cryptoWatchListMap.keys) {
            watchlistString += if (i != Constants.cryptoWatchListMap.size - 1) {
                "$crypto,"
            } else {
                crypto
            }
            i += 1
        }
        return watchlistString
    }


    /**
     * Handle create user response
     */
    private fun handleCreateUserResponse() {
        Log.d("FeedSdk", "handleCreateUserResponse")
        FeedSdk.onExploreInitialized?.onInitSuccess()
        for (userIntialiser in FeedSdk.onUserInitialized) {
            userIntialiser?.onInitSuccess()
        }
        FeedSdk.isSdkInitializationSuccessful = true
        FeedSdk.isExploreInitializationSuccessful = true
    }
}