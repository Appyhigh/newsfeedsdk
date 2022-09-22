package com.appyhigh.newsfeedsdk.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.SdkEventsActivity
import com.appyhigh.newsfeedsdk.activity.WebActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiSearchSticky
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.SearchStickyModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import java.util.*
import java.util.concurrent.TimeUnit

class SearchStickyWorker (appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    private val channelID = "StickyNotification"
    var serviceRunning = false
    var receiver: BroadcastReceiver?=null
    val TAG = "StickyNotifService"
    var isFlashOn = false
    var NOTIFICATION_ID = 1283
    var intentFilter: IntentFilter? = null
    var notificationManager: NotificationManagerCompat? = null
    var notification: NotificationCompat.Builder? = null
    private var packagesMap = hashMapOf("bharat.browser" to true, "u.see.browser.for.uc.browser" to true, "u.browser.for.lite.uc.browser" to true)
    private var myTimer = Timer("SearchStickyTimer", true)
    private val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else{
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    private var myTask: TimerTask = object : TimerTask() {
        override fun run() {
            try {
                if (!SpUtil.spUtilInstance!!.getBoolean("showStickyOnTop", true)){
                    myTimer.cancel()
                }
            } catch (ex:Exception){ LogDetail.LogEStack(ex) }
            try {
                notificationManager?.notify(NOTIFICATION_ID, notification!!.build())
                SpUtil.spUtilInstance!!.putBoolean(Constants.IS_STICKY_NOTIFICATION_ON, true)
            } catch (ex: Exception) {
                myTimer.cancel()
                if(!serviceRunning){
                    applicationContext.startStickyNotificationService()
                    LogDetail.LogEStack(ex)
                }
            }
        }
    }

    override fun doWork(): Result {
        try {
            LogDetail.LogDE("SearchStickyWorker", "doWork: called")
            SpUtil.spUtilInstance?.init(applicationContext)
            if (!SpUtil.spUtilInstance!!.getBoolean(Constants.IS_STICKY_SERVICE_ON) || !serviceRunning) {
               startNotification()
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        return Result.success()
    }

    private fun startNotification(){
        try{
            createNotificationChannel()
            setNotification()
            SpUtil.spUtilInstance?.putBoolean(Constants.IS_STICKY_NOTIFICATION_ON, true)
            // add actions here !
            intentFilter = IntentFilter()
            intentFilter!!.addAction("Dismiss")
            intentFilter!!.addAction(FeedSdk.appName+"Dismiss")
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, receiveIntent: Intent) {
                    try{
                        if(receiveIntent.hasExtra("isCollapsed") && receiveIntent.hasExtra("widget")){
                            receiveIntent.action?.let { logEvent(it, receiveIntent.getBooleanExtra("isCollapsed", true), receiveIntent.getStringExtra("widget")!!) }
                        }
                        when (receiveIntent.action) {
                            "Dismiss" -> {
                                SpUtil.spUtilInstance?.putBoolean(Constants.IS_STICKY_NOTIFICATION_ON, false)
                                serviceRunning = false
                                notificationManager?.cancel(NOTIFICATION_ID)
                                WorkManager.getInstance(applicationContext).cancelAllWorkByTag("SEARCH_STICKY_BAR");
                            }
                            FeedSdk.appName+"Dismiss" -> {
                                SpUtil.spUtilInstance?.putBoolean(Constants.IS_STICKY_NOTIFICATION_ON, false)
                                serviceRunning = false
                                notificationManager?.cancel(NOTIFICATION_ID)
                                WorkManager.getInstance(applicationContext).cancelAllWorkByTag("SEARCH_STICKY_BAR");
                            }
                        }
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }
            }
            try{
                applicationContext.unregisterReceiver(receiver)
            } catch (ex:java.lang.Exception){}
            applicationContext.registerReceiver(receiver, intentFilter)
            val stickyTimerInterval = SpUtil.spUtilInstance!!.getLong("stickyTimerInterval", 300)
            myTimer.scheduleAtFixedRate(myTask, 0L, (stickyTimerInterval * 1000).toLong())
            try{
                val adsModel =  ApiConfig().getConfigModel(applicationContext)
                if(adsModel.showParentAdmobAds && adsModel.searchPageNative.showAdmob) {
                    AdUtilsSDK().requestFeedAd(LinearLayout(applicationContext), R.layout.native_ad_feed_small, adsModel.searchPageNative.admobId, "searchSticky")
                }
            } catch (ex:Exception){}
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun checkAndGetIntent(packagesMap: HashMap<String, Boolean>): Intent {
        return try{
            if(packagesMap.containsKey(applicationContext.packageName)){
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                Intent(applicationContext, activity)
            } else {
                Intent(applicationContext, WebActivity::class.java)
            }
        } catch (ex:Exception){
            Intent(applicationContext, WebActivity::class.java)
        }
    }

    val CHANNEL_DESCRIPTION = "Get informed with new updates"

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelID,
                "Search Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = CHANNEL_DESCRIPTION
            notificationChannel.setShowBadge(true)
//            notificationChannel.enableVibration(false)
//            notificationChannel.setSound(null,null)
            val notificationManager = applicationContext!!.getSystemService<NotificationManager>(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun setNotification(){
        val gson = Gson()
        val stickyNotification: SearchStickyModel = gson.fromJson(SpUtil.spUtilInstance!!.getString(
            Constants.STICKY_NOTIFICATION
        ), SearchStickyModel::class.java)
        val collapsedRemoteView = RemoteViews(applicationContext.packageName, R.layout.sticky_notification_collapsed)
        val expandedRemoteView = RemoteViews(applicationContext.packageName, R.layout.sticky_notification_expanded)
        setCollapsedData(collapsedRemoteView, stickyNotification)
        setExpandedData(expandedRemoteView, stickyNotification)
        notification = NotificationCompat.Builder(applicationContext, channelID)
            .setCustomContentView(collapsedRemoteView)
            .setCustomBigContentView(expandedRemoteView)
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setChannelId(channelID)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(longArrayOf(0L))
            .setGroup("Search Notification")
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)

        notificationManager = NotificationManagerCompat.from(applicationContext)
        if(serviceRunning) {
            notificationManager?.notify(NOTIFICATION_ID, notification!!.build())
            SpUtil.spUtilInstance!!.putBoolean(Constants.IS_STICKY_NOTIFICATION_ON, true)
        } else {
            notificationManager?.notify(NOTIFICATION_ID, notification!!.build())
            serviceRunning = true
            SpUtil.spUtilInstance!!.putBoolean(Constants.IS_STICKY_NOTIFICATION_ON, true)
            SpUtil.spUtilInstance!!.putBoolean(Constants.IS_STICKY_SERVICE_ON, true)
            if(packagesMap.containsKey(applicationContext.packageName)){
                val stickyWorkInterval = SpUtil.spUtilInstance!!.getLong("stickyWorkInterval", 6)
                startWorkManager(true, stickyWorkInterval)
            } else{
                val stickyWorkInterval = SpUtil.spUtilInstance!!.getLong("stickyWorkInterval", 1)
                startWorkManager(false, stickyWorkInterval)
            }

        }
    }

    private fun startWorkManager(useHours:Boolean, stickyWorkInterval: Long){
        try{
            val stickyWorkRequest = PeriodicWorkRequest.Builder(
                SearchStickyWorker::class.java,
                if(useHours) stickyWorkInterval else (stickyWorkInterval*15),
                if(useHours) TimeUnit.HOURS else TimeUnit.MINUTES,
                5, // flex interval - worker will run somewhen within this period of time, but at the end of repeating interval
                TimeUnit.MINUTES
            ).setInitialDelay(5000, TimeUnit.MILLISECONDS)
                .addTag("SEARCH_STICKY_BAR")
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "SEARCH_STICKY_BAR",
                ExistingPeriodicWorkPolicy.REPLACE,
                stickyWorkRequest)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun openStickyTile(widget: String, isCollapsed: Boolean):PendingIntent {
        val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        try{
            return when (widget) {
                "Flashlight" -> {
                    getWidgetBroadcast(widget, isCollapsed)
                }
                "Search" -> {
                    val searchIntent = checkAndGetIntent(packagesMap)
                    searchIntent.putExtra("link", "google")
                    searchIntent.putExtra("title", FeedSdk.appName)
                    searchIntent.putExtra("showSearch", true)
                    searchIntent.putExtra("fromSticky", "true")
                    searchIntent.flags = intentFlags
                    PendingIntent.getActivity(applicationContext, 1, searchIntent, flags)
                }
                else -> {
                    val sdkEventsIntent = Intent(applicationContext, SdkEventsActivity::class.java)
                    sdkEventsIntent.flags = intentFlags
                    sdkEventsIntent.action = widget
                    sdkEventsIntent.putExtra("isCollapsed", isCollapsed)
                    sdkEventsIntent.putExtra("widget", widget)
                    PendingIntent.getActivity(applicationContext, 0, sdkEventsIntent, flags)
                }
            }
        } catch (ex: java.lang.Exception) {
            return getWidgetBroadcast(widget, isCollapsed)
        }
    }

    private fun getWidgetBroadcast(widget: String, isCollapsed: Boolean):PendingIntent {
        val intent = Intent(FeedSdk.appName + widget)
        intent.putExtra("isCollapsed", isCollapsed)
        intent.putExtra("widget", widget)
        return PendingIntent.getBroadcast(applicationContext, 1, intent, flags)
    }

    private fun setCollapsedData(remoteView: RemoteViews, stickyNotification: SearchStickyModel){
        try{
            remoteView.setOnClickPendingIntent(R.id.settings, openStickyTile("Settings", true))
            remoteView.setOnClickPendingIntent(R.id.tile0, openStickyTile("Search", true))
            remoteView.setTextColor(R.id.tileName0, Color.parseColor(stickyNotification.tint))
            val isColored = stickyNotification.type == "color"
            remoteView.setImageViewResource(R.id.searchIconUrl, if(isColored) R.drawable.ic_sticky_color_search else R.drawable.ic_sticky_solid_search)
            setCommonData(remoteView, stickyNotification, true)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun setExpandedData(remoteView: RemoteViews, stickyNotification: SearchStickyModel){
        try {
            remoteView.setOnClickPendingIntent(R.id.searchLayout, openStickyTile("Search", true))
            remoteView.setTextViewText(R.id.searchHint, "Search Web")
            remoteView.setOnClickPendingIntent(R.id.settings, openStickyTile("Settings", true))
            remoteView.setImageViewResource(R.id.close, R.drawable.ic_close)
            if(stickyNotification.type == "color"){
                remoteView.setInt(R.id.close, "setColorFilter", Color.parseColor("#98B1D9"))
            } else{
                remoteView.setInt(R.id.close, "setColorFilter", Color.parseColor("#B3FFFFFF"))
            }
            val closeIntent = Intent("Dismiss")
            closeIntent.putExtra("isCollapsed", false)
            closeIntent.putExtra("widget", "CloseIcon")
            val dismissIntent = PendingIntent.getBroadcast(applicationContext, 1, closeIntent, flags)
            remoteView.setOnClickPendingIntent(R.id.close, dismissIntent)
            setCommonData(remoteView, stickyNotification, false)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun setCommonData(remoteView: RemoteViews, stickyNotification: SearchStickyModel, isCollapsed: Boolean){
        val tilesLayouts = arrayOf(R.id.tile1, R.id.tile2, R.id.tile3, R.id.tile4)
        val tileIconUrls = arrayOf(R.id.tileIconUrl1, R.id.tileIconUrl2, R.id.tileIconUrl3, R.id.tileIconUrl4)
        val tileIconFrames = arrayOf(R.id.tileIconFrame1, R.id.tileIconFrame2, R.id.tileIconFrame3, R.id.tileIconFrame4)
        val tilesNames = arrayOf(R.id.tileName1, R.id.tileName2, R.id.tileName3, R.id.tileName4)
        val scale: Float = applicationContext.resources.displayMetrics.density
        val padding = (5 * scale + 0.5f).toInt()
        val isColored = stickyNotification.type == "color"
        if(isCollapsed){
            val searchPadding = if(!isColored) 5 else 0
            remoteView.setViewPadding(R.id.searchIconFrame, searchPadding, searchPadding, searchPadding, searchPadding)
        }
        remoteView.setImageViewResource(R.id.settings, if(isColored) R.drawable.ic_sticky_color_settings else R.drawable.ic_sticky_solid_settings)
        try{
            FeedSdk.searchStickyBackground = SpUtil.spUtilInstance!!.getString(Constants.STICKY_BG, "#337CFF")!!
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        val background = Constants.getStickyBackground(
            stickyNotification.backgroundType!!,
            stickyNotification.background!!
        )
        if(!isColored){
//            remoteView.setInt(R.id.searchIconUrl, "setColorFilter", if(isCollapsed) Color.parseColor(stickyNotification.tint) else background)
        }
        for (i in 0..3) {
            if (i < stickyNotification.icons!!.size) {
                remoteView.setViewVisibility(tilesLayouts[i], View.VISIBLE)
                remoteView.setTextViewText(tilesNames[i], stickyNotification.icons?.get(i))
                remoteView.setTextColor(tilesNames[i], Color.parseColor(stickyNotification.tint))
                remoteView.setImageViewResource(tileIconUrls[i], Constants.getWidgetImage(
                    isColored,
                    if (isFlashOn && stickyNotification.icons[i] == "Flashlight") "FlashlightOn" else stickyNotification.icons[i]
                )
                )
                if(!isColored){
                    if(stickyNotification.icons[i]!="Whatsapp") {
                        remoteView.setInt(tileIconUrls[i], "setColorFilter", Color.parseColor(stickyNotification.tint))
                    } else{
                        remoteView.setViewPadding(tileIconUrls[i], 7, 7, 7, 7)
                    }
                } else{
                    remoteView.setInt(tileIconFrames[i], "setBackgroundResource", R.drawable.bg_sticky_icon_not_selected)
                    remoteView.setViewPadding(tileIconUrls[i], padding, padding, padding, padding)
                }
                remoteView.setOnClickPendingIntent(tilesLayouts[i], openStickyTile(stickyNotification.icons[i], isCollapsed))
            } else {
                remoteView.setViewVisibility(tilesLayouts[i], View.INVISIBLE)
            }
        }
        if(stickyNotification.backgroundType=="color"){
            remoteView.setInt(R.id.notificationLayout, "setBackgroundResource", background)
        } else{
            remoteView.setInt(R.id.notificationLayout, "setBackgroundColor", background)
        }
    }

    override fun onStopped() {
        super.onStopped()
        try{
            myTimer.cancel()
            applicationContext.unregisterReceiver(receiver)
        } catch (ex:Exception){ }
    }

    private fun logEvent(action: String, isCollapsed: Boolean, widget: String){
        try {
            val bundle = Bundle()
            bundle.putString("clickedOn", widget)
            bundle.putString("brand", Build.BRAND)
            if (isCollapsed) {
                bundle.putString("trayState", "Collapsed")
            } else {
                bundle.putString("trayState", "Expanded")
            }
            FirebaseAnalytics.getInstance(applicationContext).logEvent("NotificationClick", bundle)
            ApiSearchSticky().userActionNotification(widget)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

}