package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiReportPost {
    fun reportPostEncrypted(
        apiUrl: String,
        postId: String,
        report: String,
        reportPostResponseListener: ReportPostResponseListener
    ) {
        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add("post_id")
        keys.add("report")

        values.add(postId)
        values.add(report)

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
                LogDetail.LogDE("ApiReportPost $apiUrl", response.toString())
                reportPostResponseListener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiReportPost $e", e.toString())
                reportPostResponseListener.onFailure()
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

    interface ReportPostResponseListener {
        fun onSuccess()
        fun onFailure()
    }
}