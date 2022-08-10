package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.COUNTRY_CODE
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.Language
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetLanguages {
    private var languageResponse: List<Language>? = null
    fun getLanguagesEncrypted(
        apiUrl: String,
        interestResponseListener: LanguageResponseListener
    ) {
        if (languageResponse == null) {
            val keys = ArrayList<String?>()
            val values = ArrayList<String?>()
            keys.add(COUNTRY_CODE)
            values.add("in")

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
            LogDetail.LogD("Test Data Encrypted -> ", sendingData)
            AuthSocket.Instance().postData(sendingData, object : ResponseListener {
                override fun onSuccess(apiUrl: String, response: String) {
                    LogDetail.LogDE("ApiGetLanguages $apiUrl", response)
                    val gson: Gson = GsonBuilder().create()
                    val languageResponseModel: List<Language> =
                        gson.fromJson(
                            response,
                            object : TypeToken<List<Language>?>() {}.type
                        )
                    handleInterestResponse(languageResponseModel, interestResponseListener)
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