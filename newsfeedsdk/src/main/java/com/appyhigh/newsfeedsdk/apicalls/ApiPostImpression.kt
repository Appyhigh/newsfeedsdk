package com.appyhigh.newsfeedsdk.apicalls

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.APIClient
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.ImpressionsListModel
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiPostImpression {

    @SuppressLint("CommitPrefEdits")
    fun addPostImpressionsEncrypted(
        apiUrl: String,
        token: String,
        mContext: Context
    ) {
        val sharedPrefs = mContext.getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
        val gson = Gson()
        val postImpressions = ArrayList<PostImpressionsModel>()
        val keys: Map<String?, *> = sharedPrefs.all
        for ((_, value) in keys) {
            val dataString = value.toString()
            val postImpression: PostImpressionsModel =
                gson.fromJson(dataString, PostImpressionsModel::class.java)
            postImpressions.add(postImpression)
        }
        if (postImpressions.size > 0 && !Constants.isImpressionApiHit) {
            Constants.isImpressionApiHit = true
            val impressionsList = ImpressionsListModel(postImpressions)
            val allDetails = JsonObject()
            val main = JsonObject()
            main.addProperty(Constants.API_URl, apiUrl)
            main.addProperty(Constants.API_METHOD, Constants.POST)
            main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)

            val impressionList = JsonArray()

            for (impression in impressionsList.impressions_list) {
                val jsonObject = JsonObject()
                jsonObject.addProperty("api_uri", impression.api_uri)
                jsonObject.addProperty("timestamp", impression.timestamp)
                val postArray = JsonArray()
                for (post in impression.post_views) {
                    val postObject = JsonObject()
                    postObject.addProperty("country", post.country)
                    postObject.addProperty("feed_type", post.feed_type)
                    postObject.addProperty("interest", post.interest)
                    postObject.addProperty("is_video", post.is_video)
                    postObject.addProperty("language", post.language)
                    postObject.addProperty("post_id", post.post_id)
                    postObject.addProperty("post_source", post.post_source)
                    postObject.addProperty("publisher_id", post.publisher_id)
                    postObject.addProperty("short_video", post.short_video)
                    postObject.addProperty("source", post.source)
                    postObject.addProperty("total_video_duration", post.total_video_duration)
                    postObject.addProperty("watched_duration", post.watched_duration)
                    postArray.add(postObject)
                }
                jsonObject.add("post_views", postArray)
                impressionList.add(jsonObject)
            }


            val dataJO = JsonObject()
            dataJO.add("impressions_list", impressionList)
            main.add(Constants.API_DATA, dataJO)

            val headerJO = JsonObject()
            headerJO.addProperty(Constants.AUTHORIZATION, token)
            main.add(Constants.API_HEADER, headerJO)
            LogDetail.LogDE("Test Data", main.toString())

            try {
                allDetails.add(Constants.API_CALLING, main)
                allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
                allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val publicKey = SessionUser.Instance().publicKey
            LogDetail.LogDE("Test Data", allDetails.toString())
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
                    LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
                    try {
                        sharedPrefs.edit().clear().apply()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    Constants.isImpressionApiHit = false
                }

                override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                    LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
                }

                override fun onSuccess(apiUrl: String?, response: String?) {
                    LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiPostImpression $apiUrl", e.toString())
                }
            })
        } else {
            Log.d("addPostImpressions", "Nothing to post yet!")
        }
    }

    fun addCricketPostImpression(
        mContext: Context,
        url: String
    ) {
        try {
            if (url.isEmpty()) {
                return
            }
            val postImpression = PostImpressionsModel(api_uri = url)
            val postImpressionList = ArrayList<PostImpressionsModel>()
            postImpressionList.add(postImpression)
            val impressionsList = ImpressionsListModel(postImpressionList)
            val apiInterface =
                if (BuildConfig.DEBUG) APIClient().getQAApiInterface() else APIClient().getApiInterface()
            apiInterface?.addPostImpressions(
                SpUtil.spUtilInstance!!.getString(Constants.JWT_TOKEN),
                impressionsList
            )?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    it?.let {
                        Log.d("TAG", "addPostImpressions: " + it.isSuccessful)
                    }
                }, {
                    it?.let { error -> handleApiError(error) }
                })
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    /**
     * Handle Error messages
     */
    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            Constants.isImpressionApiHit = false
            Log.e(ApiCreateOrUpdateUser::class.java.simpleName, "handleApiError: $it")
        }
    }
}