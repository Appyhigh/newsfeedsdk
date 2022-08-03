package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.getLanguages
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCricketSchedule {
    private var spUtil = SpUtil.spUtilInstance
    private var upcomingMatches: CricketScheduleResponse? = null

    fun getCricketScheduleEncrypt(
        apiUrl: String,
        matchType: String,
        cricketHomeResponseListener: CricketScheduleResponseListener,
        page_number: Int
    ) {
        if (matchType == Constants.UPCOMING_MATCHES && page_number == 0 && upcomingMatches != null) {
            cricketHomeResponseListener.onSuccess(upcomingMatches!!)
            return
        }
        if (matchType == Constants.LIVE_MATCHES && Constants.liveMatchResponse != null) {
            cricketHomeResponseListener.onSuccess(Constants.liveMatchResponse!!)
            return
        }

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add(Constants.MATCH_TYPE)
        keys.add(Constants.PAGE_NUMBER)
        keys.add(Constants.LANGUAGE)
        values.add(matchType)
        values.add(page_number.toString())
        values.add(getLanguages(listOf("hi", "ta", "te", "bn")))

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, keys, values)
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
                LogDetail.LogDE("ApiCricketSchedule $apiUrl", response)
                val gson: Gson = GsonBuilder().create()
                val cricketSchedulee: CricketScheduleResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CricketScheduleResponse>() {}.type
                    )
                val cricketScheduleResponse: Response<CricketScheduleResponse> =
                    Response.success(cricketSchedulee)
                try {
                    if (matchType == Constants.LIVE_MATCHES) {
                        try {
                            val tempLiveMatches =
                                cricketScheduleResponse.body()?.cards as ArrayList<Card>
                            if (tempLiveMatches.isNotEmpty()) {
                                val iplMatches: ArrayList<Card> = ArrayList()
                                val nonIplMatches: ArrayList<Card> = ArrayList()
                                val filteredLiveMatches: ArrayList<Card> = ArrayList()
                                for (liveMatch: Card in tempLiveMatches) {
                                    if (liveMatch.items[0].league == "IPL") {
                                        iplMatches.add(liveMatch)
                                    } else {
                                        nonIplMatches.add(liveMatch)
                                    }
                                }
                                filteredLiveMatches.addAll(iplMatches)
                                filteredLiveMatches.addAll(nonIplMatches)
                                cricketScheduleResponse.body()?.cards = filteredLiveMatches
                            }
                        } catch (ex: Exception) {
                            LogDetail.LogEStack(ex)
                        }
                        Constants.liveMatchResponse = cricketScheduleResponse.body()
                        Constants.cricketLiveMatchURI =
                            cricketScheduleResponse.raw().request.url.toString()
                    } else if (matchType == Constants.UPCOMING_MATCHES) {
                        Constants.cricketUpcomingMatchURI =
                            cricketScheduleResponse.raw().request.url.toString()
                        if (page_number == 0) {
                            upcomingMatches = cricketScheduleResponse.body()
                        }
                    } else {
                        Constants.cricketPastMatchURI =
                            cricketScheduleResponse.raw().request.url.toString()
                    }
                    cricketHomeResponseListener.onSuccess(cricketScheduleResponse.body()!!)
                } catch (e: Exception) {
                    cricketHomeResponseListener.onSuccess(cricketScheduleResponse.body()!!)
                }
            }

            override fun onError(call: Call, e: IOException) {
                cricketHomeResponseListener.onFailure(e)
            }
        })
    }

    fun getCricketTabsEncrypted(
        apiUrl: String,
        cricketHomeResponseListener: CricketScheduleResponseListener
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
                LogDetail.LogDE("ApiCricketSchedule $apiUrl", response)
                val gson: Gson = GsonBuilder().create()
                val cricketSchedulee: CricketScheduleResponse =
                    gson.fromJson(
                        response,
                        object : TypeToken<CricketScheduleResponse>() {}.type
                    )
                val cricketScheduleResponse: Response<CricketScheduleResponse> =
                    Response.success(cricketSchedulee)
                cricketHomeResponseListener.onSuccess(cricketScheduleResponse.body()!!)
            }

            override fun onError(call: Call, e: IOException) {
                cricketHomeResponseListener.onFailure(e)
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

    interface CricketScheduleResponseListener {
        fun onSuccess(cricketScheduleResponse: CricketScheduleResponse)
        fun onFailure(error: Throwable)
    }
}