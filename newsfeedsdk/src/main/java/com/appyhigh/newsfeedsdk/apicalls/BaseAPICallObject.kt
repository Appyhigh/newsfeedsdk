package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.google.gson.JsonObject

class BaseAPICallObject {
    fun getBaseObjectWithAuth(
        method:String,
        apiUrl: String,
        keys: ArrayList<String?>,
        values: ArrayList<String?>
    ): JsonObject {
        val allDetails = JsonObject()
        val main = JsonObject()
        main.addProperty(Constants.API_URl, apiUrl)
        main.addProperty(Constants.API_METHOD, method)
        main.addProperty(Constants.API_INTERNAL, SessionUser.Instance().apiInternal)
        val dataJO = JsonObject()
        for (index in 0 until keys.size) {
            if(values[index] !=null) {
                dataJO.addProperty(keys[index], values[index])
            }
        }
        main.add(Constants.API_DATA, dataJO)
        try {
            allDetails.add(Constants.API_CALLING, main)
            allDetails.add(Constants.USER_DETAIL, SessionUser.Instance().userDetails)
            allDetails.add(Constants.DEVICE_DETAIL, SessionUser.Instance().deviceDetails)
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
        if(BuildConfig.DEBUG) {
            LogDetail.LogDE("Test Data Base", allDetails.toString())
        }
        return allDetails
    }

}