package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.utils.SpUtil
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiUpdateUserPersonalization {
    var spUtil = SpUtil.spUtilInstance
    fun updateUserPersonalizationEncrypted(
        apiUrl: String,
        interestsList: ArrayList<Interest>,
        languageList: ArrayList<Language>,
        updatePersonalizationListener: UpdatePersonalizationListener,
    ) {
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
        val pinnedInterestsList = interestsList.filter { it.isPinned }
        for(i in pinnedInterestsList.indices){
            pinnedInterestQuery += if(i==pinnedInterestsList.size-1){
                pinnedInterestsList[i].keyId
            } else{
                pinnedInterestsList[i].keyId+","
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
        LogDetail.LogD("Data to be Sent -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
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
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()
        keys.add("state")
        values.add(state)

        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, Endpoints.UPDATE_USER_ENCRYPTED, keys, values)
        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(allDetails.toString().toByteArray(StandardCharsets.UTF_8)) + "." + publicKey
        LogDetail.LogD("Test Data Encrypted -> ", sendingData)
        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
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
            LogDetail.LogDE(ApiUpdateUserPersonalization::class.java.simpleName, "handleApiError: $it")
        }
    }
}