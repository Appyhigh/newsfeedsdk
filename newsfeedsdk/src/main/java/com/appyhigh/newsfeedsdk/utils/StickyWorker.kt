package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.service.StickyNotificationService
import timber.log.Timber

class StickyWorker (appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        try {
            SpUtil.spUtilInstance?.init(applicationContext)
            Timber.tag("StickyWorker").d("doWork: called")
            if (SpUtil.spUtilInstance!!.getBoolean(Constants.IS_STICKY_SERVICE_ON)
                && !applicationContext.isMyServiceRunning(StickyNotificationService::class.java)
            ) {
                applicationContext.startStickyNotificationService()
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        return Result.success()
    }
}