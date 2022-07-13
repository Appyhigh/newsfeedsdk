package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import java.util.*

class ImpressionWorker(var appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        LogDetail.LogD("check777", "doWork: "+Date().toString())
        // Indicate whether the work finished successfully with the Result
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                it,
                appContext
            )
        }
        return Result.success()
    }
}