package com.appyhigh.newsfeedsdk.apicalls

import androidx.annotation.NonNull
import okhttp3.Call
import java.io.IOException

interface ResponseListener {
    fun onSuccess(apiUrl: String, response: String, timeStamp:Long)
    fun onError(@NonNull call: Call, @NonNull e: IOException)
}