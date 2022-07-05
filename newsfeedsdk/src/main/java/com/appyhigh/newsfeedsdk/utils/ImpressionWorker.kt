package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apicalls.ApiCricketSchedule
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.service.NotificationCricketService
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class ImpressionWorker(var appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        Log.d("check777", "doWork: "+Date().toString())
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