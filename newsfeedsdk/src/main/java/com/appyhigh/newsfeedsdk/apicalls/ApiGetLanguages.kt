package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.API_CALLING
import com.appyhigh.newsfeedsdk.Constants.API_DATA
import com.appyhigh.newsfeedsdk.Constants.API_HEADER
import com.appyhigh.newsfeedsdk.Constants.API_INTERNAL
import com.appyhigh.newsfeedsdk.Constants.API_METHOD
import com.appyhigh.newsfeedsdk.Constants.COUNTRY_CODE
import com.appyhigh.newsfeedsdk.Constants.DEVICE_DETAIL
import com.appyhigh.newsfeedsdk.Constants.USER_DETAIL
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.Language
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetLanguages {
    private var languageResponse: List<Language>? = null
    fun getLanguagesEncrypted(
        apiUrl: String,
        interestResponseListener: LanguageResponseListener
    ) {
        if (languageResponse == null) {
            val allDetails = JsonObject()
            val main = JsonObject()
            main.addProperty(Constants.API_URl, apiUrl)
            main.addProperty(API_METHOD, SessionUser.Instance().apiMethod)
            main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
            val dataJO = JsonObject()
            dataJO.addProperty(COUNTRY_CODE, "in")
            main.add(API_DATA, dataJO)
            val headerJO = JsonObject()
            main.add(API_HEADER, headerJO)
            try {
                allDetails.add(API_CALLING, main)
                allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
                allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
            }
            LogDetail.LogDE("Test Data", main.toString())
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
                    LogDetail.LogDE("ApiGetLanguages $apiUrl", response.toString())
                }

                override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                    LogDetail.LogDE("ApiGetLanguages $apiUrl", response.toString())
                    val gson: Gson = GsonBuilder().create()
                    val languageResponseModel: List<Language> =
                        gson.fromJson(
                            response.toString(),
                            object : TypeToken<List<Language>?>() {}.type
                        )
                    handleInterestResponse(languageResponseModel, interestResponseListener)
                }

                override fun onSuccess(apiUrl: String?, response: String?) {
                    LogDetail.LogDE("ApiGetLanguages $apiUrl", response.toString())
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiGetLanguages $apiUrl", e.toString())
                }
            })
        } else {
            interestResponseListener.onSuccess(languageResponse!!)
        }
    }

    /**
     * Handle create user response
     */
    private fun handleInterestResponse(
        languageResponseModel: List<Language>,
        languageResponseListener: LanguageResponseListener
    ) {
        languageResponse = languageResponseModel
        languageResponseListener.onSuccess(languageResponseModel)
    }

    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            LogDetail.LogDE(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface LanguageResponseListener {
        fun onSuccess(languageResponseModel: List<Language>)
    }
}