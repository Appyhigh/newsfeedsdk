package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.getLanguages
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiCricketSchedule {
    private var spUtil = SpUtil.spUtilInstance
    private var upcomingMatches: CricketScheduleResponse? = null

    fun getCricketScheduleEncrypt(
        apiUrl: String,
        token: String,
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

        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, Constants.GET)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty(Constants.MATCH_TYPE, matchType)
        dataJO.addProperty(Constants.PAGE_NUMBER, page_number)
        dataJO.addProperty(Constants.LANGUAGE, getLanguages(listOf("hi", "ta", "te", "bn")))
        main.add(Constants.API_DATA, dataJO)
        val headerJO = JsonObject()
        headerJO.addProperty(Constants.AUTHORIZATION, token)
        main.add(Constants.API_HEADER, headerJO)
        try {
            allDetails.add(Constants.API_CALLING, main)
            allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
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
//                val gson: Gson = GsonBuilder().create()
//                val cricketSchedulee: CricketScheduleResponse =
//                    gson.fromJson(
//                        response.toString(),
//                        object : TypeToken<CricketScheduleResponse>() {}.type
//                    )
//                val cricketScheduleResponse: Response<CricketScheduleResponse> =
//                    Response.success(cricketSchedulee)
//                try {
//                    if (matchType == Constants.LIVE_MATCHES) {
//                        try {
//                            val tempLiveMatches =
//                                cricketScheduleResponse.body()?.cards as ArrayList<Card>
//                            if (tempLiveMatches.isNotEmpty()) {
//                                val iplMatches: ArrayList<Card> = ArrayList()
//                                val nonIplMatches: ArrayList<Card> = ArrayList()
//                                val filteredLiveMatches: ArrayList<Card> = ArrayList()
//                                for (liveMatch: Card in tempLiveMatches) {
//                                    if (liveMatch.items[0].league == "IPL") {
//                                        iplMatches.add(liveMatch)
//                                    } else {
//                                        nonIplMatches.add(liveMatch)
//                                    }
//                                }
//                                filteredLiveMatches.addAll(iplMatches)
//                                filteredLiveMatches.addAll(nonIplMatches)
//                                cricketScheduleResponse.body()?.cards = filteredLiveMatches
//                            }
//                        } catch (ex: Exception) {
//                            LogDetail.LogEStack(ex)
//                        }
//                        Constants.liveMatchResponse = cricketScheduleResponse.body()
//                        Constants.cricketLiveMatchURI =
//                            cricketScheduleResponse.raw().request.url.toString()
//                    } else if (matchType == Constants.UPCOMING_MATCHES) {
//                        Constants.cricketUpcomingMatchURI =
//                            cricketScheduleResponse.raw().request.url.toString()
//                        if (page_number == 0) {
//                            upcomingMatches = cricketScheduleResponse.body()
//                        }
//                    } else {
//                        Constants.cricketPastMatchURI =
//                            cricketScheduleResponse.raw().request.url.toString()
//                    }
//                    cricketHomeResponseListener.onSuccess(cricketScheduleResponse.body()!!)
//                } catch (e: Exception) {
//                    cricketHomeResponseListener.onSuccess(cricketScheduleResponse.body()!!)
//                }
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
            }

            override fun onError(call: Call, e: IOException) {
                cricketHomeResponseListener.onFailure(e)
            }
        })
    }

    fun getCricketTabsEncrypted(
        apiUrl: String,
        token: String,
        cricketHomeResponseListener: CricketScheduleResponseListener
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
            LogDetail.LogEStack(e)
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
                val gson: Gson = GsonBuilder().create()
                val cricketSchedulee: CricketScheduleResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<CricketScheduleResponse>() {}.type
                    )
                val cricketScheduleResponse: Response<CricketScheduleResponse> =
                    Response.success(cricketSchedulee)
                cricketHomeResponseListener.onSuccess(cricketScheduleResponse.body()!!)
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
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