package com.appyhigh.newsfeedsdk.apicalls

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.ImpressionsListModel
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiPostImpression {

    @SuppressLint("CommitPrefEdits")
    fun addPostImpressionsEncrypted(
        apiUrl: String,
        mContext: Context
    ) {
        val sharedPrefs = mContext.getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
        val postPrefs = mContext.getSharedPreferences("postIdsDb", Context.MODE_PRIVATE)
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
                    try{
                        if(postPrefs.contains(post.key) && postPrefs.getBoolean(post.key, false)){
                            continue
                        }
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
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }
                if(postArray.size()<1){
                    continue
                }
                jsonObject.add("post_views", postArray)
                impressionList.add(jsonObject)
            }
            if(impressionList.size()<1){
                Constants.isImpressionApiHit = false
                return
            }
            val dataJO = JsonObject()
            dataJO.add("impressions_list", impressionList)
            main.add(Constants.API_DATA, dataJO)
            LogDetail.LogDE("Test Data", main.toString())

            try {
                allDetails.add(Constants.API_CALLING, main)
                allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
                allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
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
                override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                    LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
                    try {
                        addPostIds(postPrefs, impressionsList)
                    } catch (ex: Exception) {
                        LogDetail.LogEStack(ex)
                    }
                    Constants.isImpressionApiHit = false
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiPostImpression $apiUrl", e.toString())
                }
            })
        } else {
            LogDetail.LogD("addPostImpressions", "Nothing to post yet!")
        }
    }

    fun clearImpressions(context: Context){
        try {
            val sharedPrefs = context.getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
            val postPrefs = context.getSharedPreferences("postIdsDb", Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
            postPrefs.edit().clear().apply()
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun addCricketPostImpression(
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
            val allDetails = JsonObject()
            val main = JsonObject()
            main.addProperty(Constants.API_URl, Endpoints.POST_IMPRESSIONS_ENCRYPTED)
            main.addProperty(Constants.API_METHOD, Constants.POST)
            main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)

            val impressionList = JsonArray()
            for (impression in impressionsList.impressions_list) {
                val jsonObject = JsonObject()
                jsonObject.addProperty("api_uri", impression.api_uri)
                jsonObject.addProperty("timestamp", impression.timestamp)
                val postArray = JsonArray()
                jsonObject.add("post_views", postArray)
                impressionList.add(jsonObject)
            }
            val dataJO = JsonObject()
            dataJO.add("impressions_list", impressionList)
            main.add(Constants.API_DATA, dataJO)
            LogDetail.LogDE("Test Data", main.toString())

            try {
                allDetails.add(Constants.API_CALLING, main)
                allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
                allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
            } catch (e: Exception) {
                LogDetail.LogEStack(e)
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
                override fun onSuccess(apiUrl: String, response: String, timeStamp:Long) {
                    LogDetail.LogDE("ApiPostImpression $apiUrl", response.toString())
                }

                override fun onError(call: Call, e: IOException) {
                    LogDetail.LogDE("ApiPostImpression ${Endpoints.POST_IMPRESSIONS_ENCRYPTED}", e.toString())
                }
            })
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    @SuppressLint("LogNotTimber")
    fun storeImpression(sharedPrefs: SharedPreferences, postPrefs: SharedPreferences,
                        url: String, timeStamp: Long, postView: PostView) {
        try {
            if(postPrefs.contains(postView.key) && postPrefs.getBoolean(postView.key, false)){
                return
            }
            val dataString = sharedPrefs.getString(timeStamp.toString(), "")
            val postImpressions = ArrayList<PostView>()
            if(dataString!!.isNotEmpty()){
                val existingPostImpression = gson.fromJson(dataString, PostImpressionsModel::class.java)
                if(existingPostImpression.api_uri == url){
                    postImpressions.addAll(existingPostImpression.post_views)
                }
            }
            if(postsMap.containsKey(postView.key)){
                postImpressions[postsMap[postView.key]!!] = postView
            } else {
                postImpressions.add(postView)
                postsMap[postView.key] = postImpressions.size - 1
            }
            val postImpressionsModel = PostImpressionsModel(url, postImpressions, timeStamp)
            val postImpressionString = gson.toJson(postImpressionsModel)
            sharedPrefs.edit().putString(timeStamp.toString(), postImpressionString).apply()
//            Log.e("PostImpression", "addPost "+postView.key)
        } catch (ex: java.lang.Exception) {
            Log.e("PostImpression", "not added "+postView.key)
            LogDetail.LogEStack(ex)
        }
    }

    @SuppressLint("LogNotTimber")
    private fun addPostIds(postPrefs: SharedPreferences, impressionsListModel: ImpressionsListModel){
        for (impression in impressionsListModel.impressions_list) {
            for (post in impression.post_views) {
                try{
//                    Log.e("PostImpression", "key "+post.key+" value "+postPrefs.getBoolean(post.key, false))
                    postPrefs.edit().putBoolean(post.key, true).apply()
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        }
    }

    companion object {
        private var postsMap = HashMap<String, Int>()
        private var gson = Gson()
    }
}