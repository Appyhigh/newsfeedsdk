package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.gson.JsonObject
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiUpdateUserPersonalization {
    var spUtil = SpUtil.spUtilInstance
    fun updateUserPersonalizationEncrypted(
        apiUrl: String,
        userId: String,
        interestsList: ArrayList<Interest>,
        languageList: ArrayList<Language>,
        updatePersonalizationListener: UpdatePersonalizationListener,
    ) {
        val token = spUtil!!.getString(Constants.JWT_TOKEN)
        var interestQuery: String? = ""
        for ((i, interest) in interestsList.withIndex()) {
            if (i < interestsList.size - 1) {
                interestQuery =
                    interestQuery + interest.keyId.toString() + ","
            } else {
                interestQuery += interest.keyId
            }
        }

        var languageQuery: String? = ""
        for ((i, language) in languageList.withIndex()) {
            if (i < languageList.size - 1) {
                languageQuery =
                    languageQuery + language.id + ","
            } else {
                languageQuery += language.id
            }
        }


        var pinnedInterestQuery:String? = ""
        for((i,interest) in interestsList.withIndex()){
            pinnedInterestQuery += if (i < interestsList.size - 1) {
                if(interest.isPinned) interest.keyId + "," else ""
            } else {
                if(interest.isPinned) interest.keyId else ""
            }
        }


        if (interestQuery == "") interestQuery = null
        if (languageQuery == "") languageQuery = null

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add("interests")
        keys.add("language")
        keys.add("ordered_interests")

        values.add(interestQuery)
        values.add(languageQuery)
        values.add(pinnedInterestQuery)

        val allDetails =
            token?.let {
                BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, apiUrl,
                    it, keys, values)
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
                LogDetail.LogDE("ApiUpdateUserPersonalization $apiUrl", response.toString())
                updatePersonalizationListener.onSuccess()
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiUpdateUserPersonalization $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiUpdateUserPersonalization $apiUrl", response.toString())
                updatePersonalizationListener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiUpdateUserPersonalization $e", e.toString())
                updatePersonalizationListener.onFailure()
            }
        })
    }

    fun updateUserState(
        token: String,
        state: String?,
        listener: UpdatePersonalizationListener
    ){
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, Endpoints.UPDATE_USER_ENCRYPTED)
        main.addProperty(Constants.API_METHOD, Constants.POST)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        dataJO.addProperty("state", state)
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
                listener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiCreateOrUpdateUser ${Endpoints.UPDATE_USER_ENCRYPTED}", e.toString())
                listener.onFailure()
            }
        })
    }

    interface UpdatePersonalizationListener {
        fun onSuccess()
        fun onFailure()
    }

    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            Log.e(ApiUpdateUserPersonalization::class.java.simpleName, "handleApiError: $it")
        }
    }
}