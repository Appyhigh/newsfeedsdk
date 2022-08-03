package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.EN
import com.appyhigh.newsfeedsdk.Constants.LANG
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.InterestStringResponseModel
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetInterests {
    private var interestsResponse: InterestResponseModel? = null
    private var selectedInterests = ""
    private var selectedOrderInterests = ArrayList<String>()
    fun getInterestsEncrypted(
        apiUrl: String,
        interestResponseListener: InterestResponseListener
    ) {
        if (interestsResponse == null) {
            val keys = ArrayList<String?>()
            val values = ArrayList<String?>()
            keys.add(LANG)
            values.add(EN)

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
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response)
                    val gson: Gson = GsonBuilder().create()
                    val interestResponseModel: InterestResponseModel =
                        gson.fromJson(
                            response,
                            object : TypeToken<InterestResponseModel?>() {}.type
                        )
                    handleInterestResponse(interestResponseModel, interestResponseListener)
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiGetInterests $apiUrl", e.toString())
                }
            })
        } else {
            interestResponseListener.onSuccess(interestsResponse!!)
        }
    }

    fun getInterestsAppWiseEncrypted(
        apiUrl: String,
        interests: String,
        interestOrderResponseListener: InterestOrderResponseListener
    ) {
        if (selectedOrderInterests.isNotEmpty() && makeString() == interests) {
            interestOrderResponseListener.onSuccess(selectedOrderInterests)
        } else {
            val keys = ArrayList<String?>()
            val values = ArrayList<String?>()
            keys.add(Constants.INTERESTS)
            values.add(interests)

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
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response)
                    val gson: Gson = GsonBuilder().create()
                    val interestResponseModel: InterestStringResponseModel =
                        gson.fromJson(
                            response,
                            object : TypeToken<InterestStringResponseModel?>() {}.type
                        )
                    selectedOrderInterests = interestResponseModel.interestList as ArrayList<String>
                    interestOrderResponseListener.onSuccess(selectedOrderInterests)
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiGetInterests $apiUrl", e.toString())
                    val selectedOrderInterests = ArrayList<String>()
                    val tempInterests = interests.split(",")
                    for (i in tempInterests) {
                        if (i.isNotEmpty())
                            selectedOrderInterests.add(i)
                    }
                    interestOrderResponseListener.onSuccess(selectedOrderInterests)
                }
            })
        }
    }

    private fun makeString(): String{
        var result = ""
        for(i in 0 until selectedOrderInterests.size){
            if(i==selectedOrderInterests.size-1){
                result += selectedInterests[i]
            } else{
                result+= selectedInterests[i]+","
            }
        }
        return result
    }

    /**
     * Handle create user response
     */
    private fun handleInterestResponse(
        interestResponseModel: InterestResponseModel,
        interestResponseListener: InterestResponseListener
    ) {
        interestsResponse = interestResponseModel
        interestResponseListener.onSuccess(interestsResponse!!)
    }

    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            LogDetail.LogDE(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface InterestResponseListener {
        fun onSuccess(interestResponseModel: InterestResponseModel)
    }

    interface InterestOrderResponseListener {
        fun onSuccess(interestList: ArrayList<String>)
    }
}