package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.utils.SpUtil
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiFollowPublihser {

    fun followPublisherEncrypted(
        apiUrl: String,
        token: String,
        userId: String?,
        publisherId: String
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add("publisher_id")
        values.add(publisherId)

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, apiUrl, token, keys, values)
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
                LogDetail.LogDE("ApiFollowPublihser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: JSONArray?) {
                LogDetail.LogDE("ApiFollowPublihser $apiUrl", response.toString())
            }

            override fun onSuccess(apiUrl: String?, response: String?) {
                LogDetail.LogDE("ApiFollowPublihser $apiUrl", response.toString())
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiFollowPublihser $apiUrl", e.toString())
            }
        })
    }
}