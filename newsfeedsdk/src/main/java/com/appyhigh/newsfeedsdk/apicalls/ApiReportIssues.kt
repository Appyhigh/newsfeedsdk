package com.appyhigh.newsfeedsdk.apicalls

import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import okhttp3.Call
import java.io.IOException
import java.nio.charset.StandardCharsets

class ApiReportIssues {
    fun reportIssuesEncrypted(
        apiUrl: String,
        reportIssueModel: ReportIssueModel,
        listener: ApiReportIssueListener
    ) {

        val keys = ArrayList<String?>()
        val values = ArrayList<String?>()

        keys.add("post_id")
        keys.add("interests")
        keys.add("feed_type")
        keys.add("page_number")
        keys.add("language")
        keys.add("post_source")
        keys.add("issue_selected")
        keys.add("additional_comments")

        values.add(reportIssueModel.post_id)
        values.add(reportIssueModel.interests)
        values.add(reportIssueModel.feed_type)
        values.add(reportIssueModel.page_number.toString())
        values.add(reportIssueModel.language)
        values.add(reportIssueModel.post_source)
        values.add(reportIssueModel.issue_selected)
        values.add(reportIssueModel.additional_comments)

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
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiReportIssues $apiUrl", response.toString())
                listener.onSuccess()
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiReportIssues $e", e.toString())
            }
        })
    }

    private fun handleApiError(throwable: Throwable) {
        throwable.message?.let {
            LogDetail.LogDE(ApiReportIssues::class.java.simpleName, "handleApiError: $it")
        }
    }
}

interface ApiReportIssueListener {
    fun onSuccess()
}


data class ReportIssueModel(
    val post_id: String,
    val interests: String? = null,
    val feed_type: String? = null,
    val page_number: Int? = null,
    val language: String? = null,
    val post_source: String? = null,
    val issue_selected: String? = null,
    val additional_comments: String? = null,
)

data class IssueResponseModel(
    val msg: String
)