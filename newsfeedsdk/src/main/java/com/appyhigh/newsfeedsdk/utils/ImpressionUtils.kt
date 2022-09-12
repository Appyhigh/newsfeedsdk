package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import java.util.*
import java.util.concurrent.TimeUnit

class ImpressionUtils {

    private var mContext: Context? = null
    var timer: Timer? = null
    fun initialize(context: Context) {
        try{
            if (timer != null) {
                timer?.cancel()
            }
            timer = Timer()
            val task: TimerTask = MyTask(mContext)
            if (mContext == null) {
                this.mContext = context
            }
            try {
                if (Constants.impreesionModel?.impression_time_interval_in_sec != 0) {
                    startPostingData(
                        task,
                        (Constants.impreesionModel?.impression_time_interval_in_sec!! * 1000).toLong()
                    )
                }
            } catch (e: Exception) {
                startPostingData(task, 60000)
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    internal class MyTask(var context: Context?) : TimerTask() {
        override fun run() {
            context?.let { it1 ->
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it1
                )
            }
        }
    }

    private fun startPostingData(task: TimerTask, postingInterval: Long) {
        if (mContext != null) {
            timer?.scheduleAtFixedRate(task, 0, postingInterval)
        }
        WorkManager.getInstance(FeedSdk.mContext!!).cancelAllWorkByTag("ImpressionsWork")
        try {
            if (Constants.impreesionModel?.background_time_interval_in_min != 0) {
                val impressionsWorkRequest = OneTimeWorkRequestBuilder<ImpressionWorker>()
                    .setInitialDelay(
                        Constants.impreesionModel?.background_time_interval_in_min!!.toLong(),
                        TimeUnit.MINUTES
                    )
                    .addTag("ImpressionsWork")
                    .build()
                WorkManager.getInstance(FeedSdk.mContext!!).enqueueUniqueWork(
                    "ImpressionsWork",
                    ExistingWorkPolicy.REPLACE,
                    impressionsWorkRequest
                )
            }
        } catch (e: Exception) {
            val impressionsWorkRequest = OneTimeWorkRequestBuilder<ImpressionWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .addTag("ImpressionsWork")
                .build()
            WorkManager.getInstance(FeedSdk.mContext!!).enqueueUniqueWork(
                "ImpressionsWork",
                ExistingWorkPolicy.REPLACE,
                impressionsWorkRequest
            )
        }
    }

}