package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.COUNTRY_CODE
import com.appyhigh.newsfeedsdk.Constants.DAILLING_CODE
import com.appyhigh.newsfeedsdk.Constants.EMAIL
import com.appyhigh.newsfeedsdk.Constants.FIRST_NAME
import com.appyhigh.newsfeedsdk.Constants.LAST_NAME
import com.appyhigh.newsfeedsdk.Constants.PHONE_NUMBER
import com.appyhigh.newsfeedsdk.Constants.PUSH_TOKEN_
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.FeedSdk.Companion.isExistingUser
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.User
import com.appyhigh.newsfeedsdk.utils.SpUtil
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCreateOrUpdateUser {
    private var spUtil = SpUtil.spUtilInstance
    fun createOrUpdateUserEncrypted(
        apiUrl: String,
        firebaseToken: String,
        sdkCountryCode: String?,
        user: User?
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add(PUSH_TOKEN_)
        keys.add(COUNTRY_CODE)
        keys.add(FIRST_NAME)
        keys.add(LAST_NAME)
        keys.add(EMAIL)
        keys.add(PHONE_NUMBER)
        keys.add(DAILLING_CODE)

        values.add(firebaseToken)
        values.add(sdkCountryCode)
        values.add(user?.firstName)
        values.add(user?.lastName)
        values.add(user?.email)
        values.add(user?.phoneNumber)
        values.add(user?.dailling_code)

        if (user?.username.isNullOrEmpty() || !isExistingUser) {
            keys.add(LAST_NAME)
            values.add(user?.username)
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
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response)
                handleCreateUserResponse()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", e.toString())
            }
        })
    }

    fun updateCricketNotificationEncrypt(
        apiUrl: String,
        isChecked: Boolean,
        onlyCricketHome: Boolean = false
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("cricket_notification")
        values.add(isChecked.toString())

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
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response)
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

    fun updateCryptoWatchlistEncrypted(apiUrl: String) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("crypto_watchlist")
        values.add(getWatchlistString())

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
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response)
                LogDetail.LogD("UpdateUser", "updated crypto watchlist")
                SpUtil.cryptoWatchListUpdateListener?.onCryptoWatchListUpdated(Constants.cryptoWatchList)
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", e.toString())
            }
        })
    }

    fun updateUserDislikeInterests(interest: String) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("user_disliked_interests")
        values.add(interest)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, Endpoints.UPDATE_USER_ENCRYPTED, keys, values)
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(allDetails.toString().toByteArray(StandardCharsets.UTF_8)) + "." + publicKey
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response)
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser ${Endpoints.UPDATE_USER_ENCRYPTED}", e.toString())
            }
        })
    }

    fun updateUserInterests(interests: String) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("interests")
        values.add(interests)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, Endpoints.UPDATE_USER_ENCRYPTED, keys, values)
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(allDetails.toString().toByteArray(StandardCharsets.UTF_8)) + "." + publicKey
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiCreateOrUpdateUser $apiUrl", response)
                for (listener in SpUtil.onRefreshListeners) {
                    listener.value.onRefreshNeeded()
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser ${Endpoints.UPDATE_USER_ENCRYPTED}", e.toString())
            }
        })
    }

    fun updateBlockPublisher(publisherId: String, action: String, doRefresh:Boolean = true){
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.PUBLISHER_ID)
        keys.add("action")

        values.add(publisherId)
        values.add(action)
        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, Endpoints.BLOCK_PUBLISHER, keys, values)

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
                LogDetail.LogDE("ApiGetPublisherPosts $apiUrl", response)
                if(action == "block") {
                    Constants.userDetails!!.blockedPublishers.add(publisherId)
                } else{
                    Constants.userDetails!!.blockedPublishers.remove(publisherId)
                }
                if(doRefresh){
                    for (listener in SpUtil.onRefreshListeners) {
                        listener.value.onRefreshNeeded()
                    }
                }
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetPublisherPosts ${Endpoints.BLOCK_PUBLISHER}", e.toString())
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
        LogDetail.LogD("FeedSdk", "handleCreateUserResponse")
        FeedSdk.onExploreInitialized?.onInitSuccess()
        for (userIntialiser in FeedSdk.onUserInitialized) {
            userIntialiser?.onInitSuccess()
        }
        FeedSdk.isSdkInitializationSuccessful = true
        FeedSdk.isExploreInitializationSuccessful = true
    }
}