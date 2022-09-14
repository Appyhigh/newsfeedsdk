package com.appyhigh.newsfeedsdk.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
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

    companion object {
        private var isThreadRunning = false
    }

//    var mRunnable: Runnable? = null
    private var mHandler = Handler(Looper.getMainLooper())
    private fun scheduleSyncIn(aSeconds: Int) {
//        mRunnable = Runnable { mHandler.postDelayed(mRunnable!!, aSeconds.toLong()) }
        mHandler.postDelayed({
            LogDetail.LogDE("ImpressionUtils", "scheduleSyncIn")
            mContext?.let { it1 ->
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    it1
                )
            }
            scheduleSyncIn(aSeconds)
        }, aSeconds.toLong())
        isThreadRunning = true
    }

    fun initialize(activity: Activity) {
        try {
            if (mContext == null) {
                this.mContext = activity.baseContext
            }
            LogDetail.LogDE("ImpressionUtils", "initialize")
            try {
                if (Constants.impreesionModel?.impression_time_interval_in_sec != 0) {
                    startPostingData(
                        (Constants.impreesionModel?.impression_time_interval_in_sec!! * 1000).toLong()
                    )
                }
            } catch (e: Exception) {
                startPostingData(60000)
            }
        } catch (ex: Exception) {
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

    private fun startPostingData(postingInterval: Long) {
        if (mContext != null && !isThreadRunning) {
            scheduleSyncIn(postingInterval.toInt())
        }
        WorkManager.getInstance(FeedSdk.mContext!!).cancelAllWorkByTag("ImpressionsWork")
        try {
            if (Constants.impreesionModel!=null && Constants.impreesionModel!!.background_time_interval_in_min > 15) {
                val impressionsWorkRequest = PeriodicWorkRequest.Builder(
                    ImpressionWorker::class.java,
                    Constants.impreesionModel?.background_time_interval_in_min!!.toLong(),
                    TimeUnit.MINUTES,
                    1, // flex interval - worker will run somewhen within this period of time, but at the end of repeating interval
                    TimeUnit.MINUTES
                ).setInitialDelay(5000, TimeUnit.MILLISECONDS)
                    .addTag("ImpressionsWork")
                    .build()
                WorkManager.getInstance(FeedSdk.mContext!!).enqueueUniquePeriodicWork(
                    "ImpressionsWork",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    impressionsWorkRequest)
            } else{
                defaultPeriodicWorkRequest()
            }
        } catch (e: Exception) {
            defaultPeriodicWorkRequest()
        }
    }

    private fun defaultPeriodicWorkRequest(){
        val impressionsWorkRequest = PeriodicWorkRequest.Builder(
            ImpressionWorker::class.java,
            15,
            TimeUnit.MINUTES,
            1, // flex interval - worker will run somewhen within this period of time, but at the end of repeating interval
            TimeUnit.MINUTES
        ).setInitialDelay(5000, TimeUnit.MILLISECONDS)
            .addTag("ImpressionsWork")
            .build()
        WorkManager.getInstance(FeedSdk.mContext!!).enqueueUniquePeriodicWork(
            "ImpressionsWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            impressionsWorkRequest)
    }

}