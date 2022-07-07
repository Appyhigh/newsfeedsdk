package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.API_CALLING
import com.appyhigh.newsfeedsdk.Constants.API_DATA
import com.appyhigh.newsfeedsdk.Constants.API_HEADER
import com.appyhigh.newsfeedsdk.Constants.API_INTERNAL
import com.appyhigh.newsfeedsdk.Constants.API_METHOD
import com.appyhigh.newsfeedsdk.Constants.API_URl
import com.appyhigh.newsfeedsdk.Constants.DEVICE_DETAIL
import com.appyhigh.newsfeedsdk.Constants.EN
import com.appyhigh.newsfeedsdk.Constants.LANG
import com.appyhigh.newsfeedsdk.Constants.USER_DETAIL
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.InterestStringResponseModel
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiGetInterests {
    private var interestsResponse: InterestResponseModel? = null
    private var selectedInterests = ""
    private var selectedOrderInterests = ArrayList<String>()
    fun getInterestsEncrypted(
        apiUrl: String,
        token: String,
        interestResponseListener: InterestResponseListener
    ) {
        if (interestsResponse == null) {
            val allDetails = JsonObject()
            val main = JsonObject()
            main.addProperty(API_URl, apiUrl)
            main.addProperty(API_METHOD, Constants.GET)
            main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)

            val dataJO = JsonObject()
            dataJO.addProperty(LANG, EN)
            main.add(API_DATA, dataJO)

            val headerJO = JsonObject()
            headerJO.addProperty(Constants.AUTHORIZATION, token)
            main.add(API_HEADER, headerJO)


            try {
                allDetails.add(API_CALLING, main)
                allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
                allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
            } catch (e: Exception) {
                e.printStackTrace()
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
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response.toString())
                    val gson: Gson = GsonBuilder().create()
                    val interestResponseModel: InterestResponseModel =
                        gson.fromJson(
                            response.toString(),
                            object : TypeToken<InterestResponseModel?>() {}.type
                        )
                    interestResponseListener.onSuccess(interestResponseModel)
                    handleInterestResponse(interestResponseModel, interestResponseListener)
                }

                override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response.toString())
                }

                override fun onSuccess(apiUrl: String?, response: String?) {
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response.toString())
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
        token: String,
        interests: String,
        interestOrderResponseListener: InterestOrderResponseListener
    ) {
        if (selectedInterests == interests) {
            interestOrderResponseListener.onSuccess(selectedOrderInterests)
        } else {
            val allDetails = JsonObject()
            val main = JsonObject()
            main.addProperty(API_URl, apiUrl)
            main.addProperty(API_METHOD, Constants.GET)
            main.addProperty(API_INTERNAL, SessionUser.Instance().apiInternal)
            val dataJO = JsonObject()
            dataJO.addProperty(Constants.INTERESTS, interests)
            main.add(API_DATA, dataJO)
            val headerJO = JsonObject()
            headerJO.addProperty(Constants.AUTHORIZATION, token)
            main.add(API_HEADER, headerJO)
            try {
                allDetails.add(API_CALLING, main)
                allDetails.add(USER_DETAIL, SessionUser.Instance().userDetails)
                allDetails.add(DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
            } catch (e: Exception) {
                e.printStackTrace()
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
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response.toString())
                    val gson: Gson = GsonBuilder().create()
                    val interestResponseModel: InterestStringResponseModel =
                        gson.fromJson(
                            response.toString(),
                            object : TypeToken<InterestStringResponseModel?>() {}.type
                        )
                    selectedOrderInterests = interestResponseModel.interestList as ArrayList<String>
                    interestOrderResponseListener.onSuccess(selectedOrderInterests)
                }

                override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response.toString())
                }

                override fun onSuccess(apiUrl: String?, response: String?) {
                    LogDetail.LogDE("ApiGetInterests $apiUrl", response.toString())
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
            Log.e(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
        }
    }

    interface InterestResponseListener {
        fun onSuccess(interestResponseModel: InterestResponseModel)
    }

    interface InterestOrderResponseListener {
        fun onSuccess(interestList: ArrayList<String>)
    }
}