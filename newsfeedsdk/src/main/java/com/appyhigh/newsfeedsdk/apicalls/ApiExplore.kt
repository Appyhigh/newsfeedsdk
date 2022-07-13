package com.appyhigh.newsfeedsdk.apicalls

import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.StateListResponse
import com.appyhigh.newsfeedsdk.model.explore.ExploreResponseModel
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiExplore {

    fun exploreEncrypted(
        apiUrl: String,
        token: String,
        userId: String?,
        lang: String?,
        country: String,
        another_interest: String,
        exploreResponseListener: ExploreResponseListener
    ) {
        val languageString: String? = if (lang == "") null else lang
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        if (lang !=null)
        keys.add(Constants.LANG)
        keys.add(Constants.COUNTRY)
        keys.add(Constants.ANOTHER_INTEREST)

        if (lang !=null)
        values.add(languageString)
        values.add(country)
        values.add(another_interest)

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, token, keys, values)

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
                LogDetail.LogDE("ApiExplore $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val exploreResponseBase: ExploreResponseModel =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<ExploreResponseModel>() {}.type
                    )
                val exploreResponse: Response<ExploreResponseModel> =
                    Response.success(exploreResponseBase)
                exploreResponseListener.onSuccess(
                    exploreResponse.body()!!,
                    exploreResponse.raw().request.url.toString(),
                    exploreResponse.raw().sentRequestAtMillis
                )
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiExplore $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiExplore $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiExplore $apiUrl", e.toString())
            }
        })

    }

    fun getStateListEncrypted(
        apiUrl: String,
        token: String,
        listener: StateResponseListener
    ){
        val allDetails = BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, apiUrl, token, ArrayList(), ArrayList())
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
                LogDetail.LogDE("ApiExplore $apiUrl", response.toString())
                val gson: Gson = GsonBuilder().create()
                val stateResponseBase: StateListResponse =
                    gson.fromJson(
                        response.toString(),
                        object : TypeToken<StateListResponse>() {}.type
                    )
                val stateResponse: Response<StateListResponse> = Response.success(stateResponseBase)
                listener.onSuccess(
                    stateResponse.body()!!,
                    stateResponse.raw().request.url.toString(),
                    stateResponse.raw().sentRequestAtMillis
                )
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiExplore $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiExplore $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiExplore $apiUrl", e.toString())
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

    interface ExploreResponseListener {
        fun onSuccess(exploreResponseModel: ExploreResponseModel, url: String, timeStamp: Long)
    }

    interface StateResponseListener{
        fun onSuccess(response: StateListResponse, url: String, timeStamp: Long)
    }
}