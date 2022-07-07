package com.appyhigh.newsfeedsdk.apicalls

import android.content.Context
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.AdsModel
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiConfig {
    private var spUtil = SpUtil.spUtilInstance
    private var adsModel:AdsModel?=null
    private var showAds = false

    fun getAdsModel():AdsModel{
        if(adsModel==null) {
            val json = SpUtil.spUtilInstance!!.getString(Constants.ADS_MODEL, "")
            if (json!!.isEmpty()) return AdsModel()
            else {
               adsModel = Gson().fromJson(json, AdsModel::class.java)
            }
        }
        return adsModel!!
    }

    fun checkShowAds():Boolean{
        return showAds
    }

    fun configEncrypted(token: String) {
        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, Endpoints.GET_CONFIG_ENCRYPTED, token, ArrayList(), ArrayList())

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
                LogDetail.LogDE("ApiConfig $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val configResponseBase: AdsModel =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<AdsModel>() {}.type
                    )
                val configResponse: Response<AdsModel> = Response.success(configResponseBase)
                adsModel = configResponse.body()
                showAds = (adsModel!!.showParentAdmobAds || adsModel!!.showPrivateAds)
                SpUtil.spUtilInstance!!.putString(Constants.ADS_MODEL, Gson().toJson(adsModel))
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiConfig $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiConfig $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiConfig ${Endpoints.GET_CONFIG_ENCRYPTED}", e.toString())
            }
        })
    }
}