package com.appyhigh.newsfeedsdk.apicalls

import androidx.annotation.NonNull
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

interface ResponseListener {
    fun onSuccess(apiUrl: String?, response: JSONObject?)
    fun onSuccess(apiUrl: String?, response: JSONArray?)
    fun onSuccess(apiUrl: String?, response: String?)
    fun onError(@NonNull call: Call, @NonNull e: IOException)
}