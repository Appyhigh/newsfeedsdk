package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.APIClient
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.*
import com.appyhigh.newsfeedsdk.utils.SpUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
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
        state: String?,
        listener: UpdatePersonalizationListener
    ){
        APIClient().getApiInterface()
            ?.updateUser(
                spUtil!!.getString(Constants.JWT_TOKEN),
                UpdateUserState(state)
            )
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(
                {
                    it?.let { listener.onSuccess() }
                },
                {
                    it?.let { listener.onFailure() }
                }
            )
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