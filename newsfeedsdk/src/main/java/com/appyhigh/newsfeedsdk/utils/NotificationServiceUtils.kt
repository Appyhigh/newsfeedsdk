package com.appyhigh.newsfeedsdk.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.service.NotificationCricketService
import com.appyhigh.newsfeedsdk.service.StickyNotificationService
import org.json.JSONObject


fun Context.startStickyNotificationService(){
    try{
        if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
            val dismissIntent = Intent("Dismiss")
            sendBroadcast(dismissIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                val startIntent = Intent(this, StickyNotificationService::class.java)
                startIntent.action = Constants.ACTION.STARTFOREGROUND.toString()
                ContextCompat.startForegroundService(this, startIntent)
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

fun Context.startNotificationCricketService(liveScores : JSONObject) {
    try{
        if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
            val dismissIntent = Intent("dismissCricket")
            dismissIntent.putExtra(FeedSdk.appName, true)
            sendBroadcast(dismissIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                val startIntent = Intent(this, NotificationCricketService::class.java)
                startIntent.putExtra("liveScores", liveScores.toString())
                startIntent.action = Constants.ACTION.STARTFOREGROUND.toString()
                ContextCompat.startForegroundService(
                    this,
                    startIntent
                )
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