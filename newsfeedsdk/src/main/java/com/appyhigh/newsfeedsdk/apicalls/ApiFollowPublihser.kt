package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiFollowPublihser {

    fun followPublisherEncrypted(
        apiUrl: String,
        publisherId: String
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add("publisher_id")
        values.add(publisherId)

        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.POST, apiUrl, keys, values)
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
                LogDetail.LogDE("ApiFollowPublihser $apiUrl", response)
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiFollowPublihser $apiUrl", e.toString())
            }
        })
    }
}