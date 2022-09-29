package com.appyhigh.newsfeedsdk.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.service.NotificationCricketService
import com.appyhigh.newsfeedsdk.service.StickyNotificationService
import org.json.JSONObject
import java.util.concurrent.TimeUnit


//fun Context.startStickyNotificationService(){
//    try{
//        val dismissIntent = Intent("Dismiss")
//        sendBroadcast(dismissIntent)
//        Handler(Looper.getMainLooper()).postDelayed({
//            startStickyWorkManager(this)
//        }, 1000)
//    } catch (ex:Exception){
//        LogDetail.LogEStack(ex)
//    }
//}
//
//fun Context.stopStickyNotificationService(){
//    try{
//        val dismissIntent = Intent(FeedSdk.appName+"Dismiss")
//        sendBroadcast(dismissIntent)
//    } catch (ex:Exception){
//        LogDetail.LogEStack(ex)
//    }
//}

fun Context.startStickyNotificationService(){
    try{
        if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
            val dismissIntent = Intent("Dismiss")
            dismissIntent.putExtra("package",packageName)
            sendBroadcast(dismissIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val startIntent = Intent(this, StickyNotificationService::class.java)
                    startIntent.action = Constants.ACTION.STARTFOREGROUND.toString()
                    ContextCompat.startForegroundService(this, startIntent)
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }, 1000)
        }
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}

fun Context.stopStickyNotificationService(){
    try{
        if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
            val startIntent = Intent(this, StickyNotificationService::class.java)
            startIntent.action = Constants.ACTION.STOPFOREGROUND.toString()
            ContextCompat.startForegroundService(
                this,
                startIntent
            )
        }
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}


private fun startStickyWorkManager(context: Context){
    try{
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SearchStickyWorker>()
            .setInitialDelay(
                10,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SEARCH_STICKY_BAR",
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}

fun Context.startNotificationCricketService() {
    try{
        val dismissIntent = Intent("dismissCricket")
        dismissIntent.putExtra(packageName, true)
        sendBroadcast(dismissIntent)
        Handler(Looper.getMainLooper()).postDelayed({
            startCricketWorkManager(this)
        }, 1000)
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}

private fun startCricketWorkManager(context: Context){
    try{
        val data = Data.Builder()
        val workerName = "SDK_CRICKET_NOTIFICATION"
        data.putString("worker", workerName)
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<CricketSocketWorker>()
            .setInitialDelay(
                10,
                TimeUnit.MILLISECONDS
            )
            .setInputData(data.build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workerName,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}

fun Context.startNotificationCricketService(liveScores : JSONObject) {
    try{
        if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
            val dismissIntent = Intent("dismissCricket")
            dismissIntent.putExtra(packageName, true)
            sendBroadcast(dismissIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                try{
                    val startIntent = Intent(this, NotificationCricketService::class.java)
                    startIntent.putExtra("liveScores", liveScores.toString())
                    startIntent.action = Constants.ACTION.STARTFOREGROUND.toString()
                    ContextCompat.startForegroundService(
                        this,
                        startIntent
                    )
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }, 1000)
        }
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}

fun Context.stopNotificationCricketService() {
    try{
        if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
            val startIntent = Intent(this, NotificationCricketService::class.java)
            startIntent.action = Constants.ACTION.STOPFOREGROUND.toString()
            ContextCompat.startForegroundService(
                this,
                startIntent
            )
        }
    } catch (ex:Exception){
        LogDetail.LogEStack(ex)
    }
}

fun Context.isMyServiceRunning(serviceClass: Class<*>): Boolean {
    try {
        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    } catch (ex:Exception){
        return false
    }
}