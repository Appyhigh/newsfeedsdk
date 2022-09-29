package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.feeds.GetFeedsResponse
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetFeeds {

    fun getFeedsEncrypted(
        apiUrl: String,
        countryCode: String,
        interests: String?,
        languages: String?,
        pageSkip: Int,
        feedType: String,
        hasFirstPostId: Boolean,
        feedsResponseListener: GetFeedsResponseListener
    ) {
        var languageString: String? = if (languages == "") null else languages
        if (FeedSdk.languageForAPICalls.isNotEmpty()) {
            languageString = FeedSdk.languageForAPICalls
        }
        var interestsString: String? = if (interests == "") null else interests
        if (FeedSdk.interestsForAPICalls.isNotEmpty()) {
            interestsString = FeedSdk.interestsForAPICalls
        }
        var newFeedType = feedType
        var postSource: String? = null
        if (SpUtil.pushIntent != null) {
            if (SpUtil.pushIntent!!.hasExtra("page")
                && SpUtil.pushIntent!!.getStringExtra("page") == "SDK://feed"
                && SpUtil.pushIntent!!.getStringExtra("category") == interests
            ) {
                if (SpUtil.pushIntent!!.hasExtra("feed_type")) {
                    newFeedType = SpUtil.pushIntent!!.getStringExtra("feed_type")!!
                }
                if (SpUtil.pushIntent!!.hasExtra("post_source")) {
                    postSource = SpUtil.pushIntent!!.getStringExtra("post_source")!!
                }
            } else if (SpUtil.pushIntent!!.hasExtra("fromSticky")) {
                if (SpUtil.pushIntent!!.hasExtra("feed_type") && SpUtil.pushIntent!!.getStringExtra(
                        "feed_type"
                    ) != "search_bar_quick_bites"
                ) {
                    newFeedType = SpUtil.pushIntent!!.getStringExtra("feed_type")!!
                }
            }
        }
        if (hasFirstPostId) {
            getFeedsForFirstPostIdEncrypted(
                apiUrl,
                countryCode,
                interestsString,
                languageString,
                pageSkip,
                newFeedType,
                feedsResponseListener
            )
        } else {

            val keys = ArrayList<String?>()
            val values = ArrayList<String?>()

            keys.add(Constants.COUNTRY_CODE)
            keys.add(Constants.INTERESTS)
            keys.add(Constants.PAGE_NUMBER)
            keys.add(Constants.LANGUAGE)
            keys.add(Constants.FEED_TYPE)
            keys.add(Constants.POST_SOURCE)
            keys.add(Constants.BLOCKED_PUBLISHERS)

            values.add(countryCode)
            values.add(interestsString)
            values.add(pageSkip.toString())
            values.add(languageString)
            values.add(newFeedType)
            values.add(postSource)
            values.add(Constants.userDetails?.let { Constants.getStringFromList(it.blockedPublishers) })

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
                override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                    LogDetail.LogDE("ApiGetFeeds $apiUrl", response)

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
                        "https://feeds.apyhi.com+$apiUrl",
                        timeStamp
                    )
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiGetFeeds $apiUrl", e.toString())
                }
            })
        }
    }

    fun getRegionalFeedsEncrypted(
        apiUrl: String,
        latitude: Double?,
        longitude: Double?,
        stateCode: String,
        pageSkip: Int,
        feedsResponseListener: GetFeedsResponseListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.LATITUDE)
        keys.add(Constants.LONGITUDE)
        keys.add(Constants.STATE_CODE)
        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.BLOCKED_PUBLISHERS)

        values.add(latitude?.toString() ?: "")
        values.add(longitude?.toString() ?: "")
        values.add(stateCode)
        values.add(pageSkip.toString())
        values.add(Constants.userDetails?.let { Constants.getStringFromList(it.blockedPublishers) })

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
            override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                LogDetail.LogDE("ApiGetFeeds $apiUrl", response)

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
                    "https://feeds.apyhi.com+$apiUrl",
                    timeStamp
                )
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetFeeds $apiUrl", e.toString())
            }
        })
    }


    fun getFeedsForFirstPostIdEncrypted(
        apiUrl: String,
        countryCode: String,
        interests: String?,
        languages: String?,
        pageSkip: Int,
        feedType: String,
        feedsResponseListener: GetFeedsResponseListener
    ) {

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.COUNTRY_CODE)
        keys.add(Constants.INTERESTS)
        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.LANGUAGE)
        keys.add(Constants.FEED_TYPE)
        keys.add(Constants.FIRST_POST_ID)
        keys.add(Constants.POST_SOURCE)
        keys.add(Constants.BLOCKED_PUBLISHERS)

        values.add(
            if (SpUtil.pushIntent!!.hasExtra("country_code")) SpUtil.pushIntent!!.getStringExtra(
                "country_code"
            ) else countryCode
        )
        values.add(
            if (SpUtil.pushIntent!!.hasExtra("interests")) SpUtil.pushIntent!!.getStringExtra(
                "interests"
            ) else interests
        )
        values.add(pageSkip.toString())
        values.add(
            if (SpUtil.pushIntent!!.hasExtra("language")) SpUtil.pushIntent!!.getStringExtra(
                "language"
            ) else languages
        )
        values.add(
            if (SpUtil.pushIntent!!.hasExtra("feed_type")) SpUtil.pushIntent!!.getStringExtra(
                "feed_type"
            ) else "push"
        )
        values.add(SpUtil.pushIntent!!.getStringExtra("post_id"))
        values.add(SpUtil.pushIntent!!.getStringExtra("post_source"))
        values.add(Constants.userDetails?.let { Constants.getStringFromList(it.blockedPublishers) })

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
                LogDetail.LogDE("ApiGetFeeds $apiUrl", response)

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
                    "https://feeds.apyhi.com+$apiUrl",
                    timeStamp
                )
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetFeeds $apiUrl", e.toString())
            }
        })
    }


    fun getVideoFeedsEncrypted(
        apiUrl: String,
        countryCode: String,
        interests: String?,
        languages: String?,
        pageSkip: Int,
        feedType: String,
        isVideo: Boolean,
        shortVideo: Boolean,
        feedsResponseListener: GetFeedsResponseListener
    ) {
        var languageString: String? = if (languages == "") null else languages
        if (FeedSdk.languageForAPICalls.isNotEmpty()) {
            languageString = FeedSdk.languageForAPICalls
        }
        var newFeedType = feedType
        var postSource: String? = null
        var hasFirstPostId = false
        var interestsString: String? = if (interests == "") null else interests
        if (FeedSdk.interestsForAPICalls.isNotEmpty()) {
            interestsString = FeedSdk.interestsForAPICalls
        }
        if (SpUtil.pushIntent != null) {
            if (SpUtil.pushIntent!!.hasExtra("page")
                && SpUtil.pushIntent!!.getStringExtra("page") == "SDK://reels"
            ) {
                hasFirstPostId = true
                if (SpUtil.pushIntent!!.hasExtra("feed_type")) {
                    newFeedType = SpUtil.pushIntent!!.getStringExtra("feed_type")!!
                }
                if (SpUtil.pushIntent!!.hasExtra("post_source")) {
                    postSource = SpUtil.pushIntent!!.getStringExtra("post_source")!!
                }

            } else if (SpUtil.pushIntent!!.getStringExtra("post_id") != "" && SpUtil.pushIntent!!.getStringExtra(
                    "post_source"
                ) != null
                && SpUtil.pushIntent!!.getStringExtra("short_video") != "false"
            ) {
                hasFirstPostId = true
            } else if (SpUtil.pushIntent!!.hasExtra("fromSticky")) {
                if (SpUtil.pushIntent!!.hasExtra("feed_type") && SpUtil.pushIntent!!.getStringExtra(
                        "feed_type"
                    ) != "search_bar_for_you"
                ) {
                    newFeedType = SpUtil.pushIntent!!.getStringExtra("feed_type")!!
                }
            }
        }
        if (hasFirstPostId) {
            getVideoFeedsForFirstPostIdEncrypted(
                apiUrl,
                countryCode,
                interestsString,
                languageString,
                pageSkip,
                newFeedType,
                isVideo,
                shortVideo,
                feedsResponseListener
            )
        } else {

            val keys = ArrayList<String?>()
            val values = ArrayList<String?>()

            keys.add(Constants.COUNTRY_CODE)
            keys.add(Constants.PAGE_NUMBER)
            keys.add(Constants.IS_VIDEO)
            keys.add(Constants.SHORT_VIDEO)
            keys.add(Constants.INTERESTS)
            keys.add(Constants.LANGUAGE)
            keys.add(Constants.FEED_TYPE)
            keys.add(Constants.POST_SOURCE)
            keys.add(Constants.BLOCKED_PUBLISHERS)

            values.add(countryCode)
            values.add(pageSkip.toString())
            values.add(isVideo.toString())
            values.add(shortVideo.toString())
            values.add(interests)
            values.add(languages)
            values.add(feedType)
            values.add(postSource)
            values.add(Constants.userDetails?.let { Constants.getStringFromList(it.blockedPublishers) })

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
                override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                    LogDetail.LogDE("ApiGetFeeds $apiUrl", response)

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
                        "https://feeds.apyhi.com+$apiUrl",
                        timeStamp
                    )
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiGetFeeds $apiUrl", e.toString())
                }
            })
        }
    }


    fun getVideoFeedsForFirstPostIdEncrypted(
        apiUrl: String,
        countryCode: String,
        interests: String?,
        languages: String?,
        pageSkip: Int,
        feedType: String,
        isVideo: Boolean,
        shortVideo: Boolean,
        feedsResponseListener: GetFeedsResponseListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add(Constants.COUNTRY_CODE)
        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.IS_VIDEO)
        keys.add(Constants.SHORT_VIDEO)
        keys.add(Constants.INTERESTS)
        keys.add(Constants.LANGUAGE)
        keys.add(Constants.FEED_TYPE)
        keys.add(Constants.FIRST_POST_ID)
        keys.add(Constants.POST_SOURCE)
        keys.add(Constants.BLOCKED_PUBLISHERS)

        values.add(
            if (SpUtil.pushIntent!!.hasExtra("country_code")) SpUtil.pushIntent!!.getStringExtra(
                "country_code"
            ) else countryCode
        )
        values.add(pageSkip.toString())
        val video =
            if (SpUtil.pushIntent!!.hasExtra("is_video")) SpUtil.pushIntent!!.getStringExtra("is_video") == "true" else isVideo
        values.add(video.toString())
        val sVideo =
            if (SpUtil.pushIntent!!.hasExtra("short_video")) SpUtil.pushIntent!!.getStringExtra(
                "short_video"
            ) == "true" else shortVideo
        values.add(sVideo.toString())
        values.add(
            if (SpUtil.pushIntent!!.hasExtra("interests")) SpUtil.pushIntent!!.getStringExtra(
                "interests"
            ) else interests
        )
        values.add(
            if (SpUtil.pushIntent!!.hasExtra("language")) SpUtil.pushIntent!!.getStringExtra(
                "language"
            ) else languages
        )
        values.add(
            if (SpUtil.pushIntent!!.hasExtra("feed_type")) SpUtil.pushIntent!!.getStringExtra(
                "feed_type"
            ) else "push"
        )
        values.add(SpUtil.pushIntent!!.getStringExtra("post_id"))
        values.add(SpUtil.pushIntent!!.getStringExtra("post_source"))
        values.add(Constants.userDetails?.let { Constants.getStringFromList(it.blockedPublishers) })

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
                LogDetail.LogDE("ApiGetFeeds $apiUrl", response)

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
                    "https://feeds.apyhi.com+$apiUrl",
                    timeStamp
                )
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiGetFeeds $apiUrl", e.toString())
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

    interface GetFeedsResponseListener {
        fun onSuccess(getFeedsResponse: GetFeedsResponse, url: String, timeStamp: Long)
    }
}
