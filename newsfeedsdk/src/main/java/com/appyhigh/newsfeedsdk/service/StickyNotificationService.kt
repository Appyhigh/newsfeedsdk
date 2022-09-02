package com.appyhigh.newsfeedsdk.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.os.*
import android.provider.AlarmClock
import android.view.View
import android.widget.LinearLayout
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_NOTIFICATION_ON
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_SERVICE_ON
import com.appyhigh.newsfeedsdk.Constants.STICKY_NOTIFICATION
import com.appyhigh.newsfeedsdk.Constants.getStickyBackground
import com.appyhigh.newsfeedsdk.Constants.getWidgetImage
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.SettingsActivity
import com.appyhigh.newsfeedsdk.activity.WebActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiConfig
import com.appyhigh.newsfeedsdk.apicalls.ApiSearchSticky
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.SearchStickyModel
import com.appyhigh.newsfeedsdk.utils.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import java.util.*
import java.util.concurrent.TimeUnit

class StickyNotificationService : Service(){

    private val channelID = "StickyNotification"
    var serviceRunning = false
    var receiver: BroadcastReceiver?=null
    val TAG = "StickyNotifService"
    var isFlashOn = false
    var NOTIFICATION_ID = 1283
    var intentFilter:IntentFilter? = null
    var notificationManager: NotificationManagerCompat? = null
    var notification: NotificationCompat.Builder? = null
    private var myTimer = Timer("SearchStickyTimer", true)

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
                SpUtil.spUtilInstance!!.putBoolean(IS_STICKY_NOTIFICATION_ON, true)
            } catch (ex: Exception) {
                myTimer.cancel()
                if(!serviceRunning){
                    applicationContext.startStickyNotificationService()
                    LogDetail.LogEStack(ex)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try{
            createNotificationChannel()
            setNotification()
            SpUtil.spUtilInstance?.putBoolean(IS_STICKY_NOTIFICATION_ON, true)
            val appName = FeedSdk.appName
            // add actions here !
            intentFilter = IntentFilter()
            intentFilter!!.addAction(appName+"Settings")
            intentFilter!!.addAction(appName+"Flashlight")
            intentFilter!!.addAction(appName+"Search")
            intentFilter!!.addAction(appName+"Camera")
            intentFilter!!.addAction(appName+"Video")
            intentFilter!!.addAction(appName+"Weather")
            intentFilter!!.addAction(appName+"Email")
            intentFilter!!.addAction(appName+"Whatsapp")
            intentFilter!!.addAction(appName+"Call")
            intentFilter!!.addAction(appName+"Messages")
            intentFilter!!.addAction(appName+"Alarm")
            intentFilter!!.addAction(appName+"Calendar")
            intentFilter!!.addAction(appName+"News")
            intentFilter!!.addAction("Dismiss")
            receiver = object : BroadcastReceiver() {
                @SuppressLint("MissingPermission")
                override fun onReceive(context: Context?, receiveIntent: Intent) {
                    try{
                        val packagesMap = HashMap<String, Boolean>()
                        packagesMap["bharat.browser"] = true
                        packagesMap["u.see.browser.for.uc.browser"] = true
                        packagesMap["u.browser.for.lite.uc.browser"] = true
                        if(receiveIntent.hasExtra("isCollapsed") && receiveIntent.hasExtra("widget")){
                            receiveIntent.action?.let { logEvent(it, receiveIntent.getBooleanExtra("isCollapsed", true), receiveIntent.getStringExtra("widget")!!) }
                        }
                        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                        sendBroadcast(closeIntent)
                        when (receiveIntent.action) {
                            appName+"Settings" -> {
                                val settingsIntent = Intent(this@StickyNotificationService, SettingsActivity::class.java)
                                settingsIntent.putExtra("stickyTitle", "Settings")
                                settingsIntent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(settingsIntent)
                            }
                            appName+"Camera" -> {
                                try {
                                    val intent =
                                        Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Video" -> {
                                try{
                                    val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                                    val intent = Intent(this@StickyNotificationService, activity)
                                    intent.putExtra("fromSticky", "reels")
                                    intent.putExtra("push_source", "feedsdk")
                                    intent.putExtra(Constants.FEED_TYPE, "search_bar_quick_bites" )
                                    intent.action = System.currentTimeMillis().toString()
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Weather" -> {
                                try {
                                    val intent = checkAndGetIntent(packagesMap)
                                    intent.putExtra("link","https://www.google.com/search?q=weather")
                                    intent.putExtra("title", "Weather")
                                    intent.putExtra("fromSticky", "true")
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:java.lang.Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Email" -> {
                                try{
                                    val intent = this@StickyNotificationService.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                                    intent!!.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:java.lang.Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Whatsapp" -> {
                                try {
                                    val intent = this@StickyNotificationService.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                    intent!!.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:java.lang.Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Call" -> {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL)
                                    intent!!.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Messages" -> {
                                try {
                                    val intent = Intent(ACTION_MAIN)
                                    intent.addCategory(CATEGORY_APP_MESSAGING)
                                    intent!!.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Alarm" -> {
                                try {
                                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Calendar" -> {
                                try{
                                    val intent = Intent(ACTION_MAIN)
                                    intent.addCategory(CATEGORY_APP_CALENDAR)
                                    intent!!.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:java.lang.Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"News" -> {
                                try{
                                    val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                                    val intent = Intent(this@StickyNotificationService, activity)
                                    intent.putExtra("fromSticky", "news")
                                    intent.putExtra("push_source", "feedsdk")
                                    intent.putExtra(Constants.FEED_TYPE, "search_bar_for_you" )
                                    intent.action = System.currentTimeMillis().toString()
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }
                            }
                            appName+"Flashlight" -> {
                                val isFlashAvailable = applicationContext.packageManager
                                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    val mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                                    if(isFlashAvailable){
                                        try {
                                            val mCameraId = mCameraManager.getCameraIdList()[0];
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                isFlashOn = !isFlashOn
                                                mCameraManager.setTorchMode(mCameraId, isFlashOn)
                                                setNotification()
                                            }
                                        } catch (e: java.lang.Exception) {
                                            LogDetail.LogEStack(e)
                                        }
                                    }
                                }
                            }
                            appName+"Search" -> {
                                val searchIntent = checkAndGetIntent(packagesMap)
                                searchIntent.putExtra("link", receiveIntent.getStringExtra("link"))
                                searchIntent.putExtra("title", appName)
                                searchIntent.putExtra("showSearch", true)
                                searchIntent.putExtra("fromSticky", "true")
                                searchIntent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(searchIntent)
                            }
                            "Dismiss" -> {
                                SpUtil.spUtilInstance?.putBoolean(IS_STICKY_NOTIFICATION_ON, false)
                                serviceRunning = false
                                stopForeground(true)
                                stopSelf()
                            }
                        }
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }
            }
            try{
                this@StickyNotificationService.unregisterReceiver(receiver)
            } catch (ex:java.lang.Exception){}
            this@StickyNotificationService.registerReceiver(receiver, intentFilter)
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

    private fun checkAndGetIntent(packagesMap: HashMap<String, Boolean>):Intent{
        return try{
            if(packagesMap.containsKey(applicationContext.packageName)){
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                Intent(this@StickyNotificationService, activity)
            } else {
                Intent(this@StickyNotificationService, WebActivity::class.java)
            }
        } catch (ex:Exception){
            Intent(this@StickyNotificationService, WebActivity::class.java)
        }
    }

    @SuppressLint("LogNotTimber")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try{
            LogDetail.LogD(TAG, "onStartCommand: " + intent.action)
            if(intent.action!! == Constants.ACTION.STOPFOREGROUND.toString()) {
                SpUtil.spUtilInstance?.putBoolean(IS_STICKY_NOTIFICATION_ON, false)
                serviceRunning = false
                stopForeground(true)
                this@StickyNotificationService.unregisterReceiver(receiver)
                stopSelf()
                return START_NOT_STICKY
            } else{
                setNotification()
                return START_STICKY
            }
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
            return START_NOT_STICKY
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
            val notificationManager = this@StickyNotificationService!!.getSystemService<NotificationManager>(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun setNotification(){
        val gson = Gson()
        val stickyNotification: SearchStickyModel = gson.fromJson(SpUtil.spUtilInstance!!.getString(STICKY_NOTIFICATION), SearchStickyModel::class.java)
        val collapsedRemoteView = RemoteViews(packageName, R.layout.sticky_notification_collapsed)
        val expandedRemoteView = RemoteViews(packageName, R.layout.sticky_notification_expanded)
        setCollapsedData(collapsedRemoteView, stickyNotification)
        setExpandedData(expandedRemoteView, stickyNotification)
        notification = NotificationCompat.Builder(this@StickyNotificationService, channelID)
            .setCustomContentView(collapsedRemoteView)
            .setCustomBigContentView(expandedRemoteView)
            .setSmallIcon(applicationInfo.icon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setChannelId(channelID)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(longArrayOf(0L))
            .setGroup("Search Notification")
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)

        notificationManager = NotificationManagerCompat.from(this@StickyNotificationService)
        if(serviceRunning) {
            notificationManager?.notify(NOTIFICATION_ID, notification!!.build())
            SpUtil.spUtilInstance!!.putBoolean(IS_STICKY_NOTIFICATION_ON, true)
        } else {
            startForeground(NOTIFICATION_ID, notification!!.build())
            serviceRunning = true
            SpUtil.spUtilInstance!!.putBoolean(IS_STICKY_NOTIFICATION_ON, true)
            SpUtil.spUtilInstance!!.putBoolean(IS_STICKY_SERVICE_ON, true)
            val packagesMap = HashMap<String, Boolean>()
            packagesMap["bharat.browser"] = true
            packagesMap["u.see.browser.for.uc.browser"] = true
            packagesMap["u.browser.for.lite.uc.browser"] = true
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
                StickyWorker::class.java,
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            if(android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1) {
                applicationContext.stopStickyNotificationService()
                val dismissIntent = Intent("Dismiss")
                sendBroadcast(dismissIntent)
                Handler(Looper.getMainLooper()).postDelayed({
                    val startIntent = Intent(this, StickyNotificationService::class.java)
                    startIntent.action = Constants.ACTION.STARTFOREGROUND.toString()
                    ContextCompat.startForegroundService(this, startIntent)
                }, 1000)
                LogDetail.LogD("StickyWorker", "doWork: enteredTaskRemovedif")
            }
            else {
                val restartServiceIntent = Intent(applicationContext, StickyNotificationService::class.java)
                restartServiceIntent.action = Constants.ACTION.STARTFOREGROUND.toString()
                restartServiceIntent.setPackage(packageName)

                val restartServicePendingIntent = PendingIntent.getService(
                    applicationContext,
                    1,
                    restartServiceIntent,
                    0
                )
                val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
                alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000] =
                    restartServicePendingIntent
                LogDetail.LogD("StickyWorker", "doWork: enteredTaskRemovedelse")
            }
        }
        catch (e:Exception){
            LogDetail.LogD("StickyWorker", "doWork: enteredTaskRemovedcatch")
        }
        super.onTaskRemoved(rootIntent)
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    fun openStickyTile(widget: String, isCollapsed: Boolean):PendingIntent{
        val intent = Intent(FeedSdk.appName+widget)
        intent.putExtra("isCollapsed", isCollapsed)
        intent.putExtra("widget", widget)
        return PendingIntent.getBroadcast(this@StickyNotificationService, 1, intent, 0)
    }

    private fun setCollapsedData(remoteView: RemoteViews, stickyNotification: SearchStickyModel){
        try{
            val dismissIntent = PendingIntent.getBroadcast(this@StickyNotificationService, 1, Intent(FeedSdk.appName+"Settings"), 0)
            remoteView.setOnClickPendingIntent(R.id.settings, dismissIntent)
            val searchPendingIntent =  PendingIntent.getBroadcast(this@StickyNotificationService, 1,
                Intent(FeedSdk.appName+"Search").putExtra("link", "google"), 0)
            remoteView.setOnClickPendingIntent(R.id.tile0, searchPendingIntent)
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
            val searchPendingIntent =  PendingIntent.getBroadcast(this@StickyNotificationService, 1,
                Intent(FeedSdk.appName+"Search").putExtra("link", "google"), 0)
            remoteView.setOnClickPendingIntent(R.id.searchLayout, searchPendingIntent)
            remoteView.setTextViewText(R.id.searchHint, "Search Web")
            val settingsIntent = PendingIntent.getBroadcast(this@StickyNotificationService, 1, Intent(FeedSdk.appName+"Settings"), 0)
            remoteView.setOnClickPendingIntent(R.id.settings, settingsIntent)
            remoteView.setImageViewResource(R.id.close, R.drawable.ic_close)
            if(stickyNotification.type == "color"){
                remoteView.setInt(R.id.close, "setColorFilter", Color.parseColor("#98B1D9"))
            } else{
                remoteView.setInt(R.id.close, "setColorFilter", Color.parseColor("#B3FFFFFF"))
            }
            val closeIntent = Intent("Dismiss")
            closeIntent.putExtra("isCollapsed", false)
            closeIntent.putExtra("widget", "CloseIcon")
            val dismissIntent = PendingIntent.getBroadcast(this@StickyNotificationService, 1, closeIntent, 0)
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
        val scale: Float = resources.displayMetrics.density
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
        val background = getStickyBackground(stickyNotification.backgroundType!!, stickyNotification.background!!)
        if(!isColored){
//            remoteView.setInt(R.id.searchIconUrl, "setColorFilter", if(isCollapsed) Color.parseColor(stickyNotification.tint) else background)
        }
        for (i in 0..3) {
            if (i < stickyNotification.icons!!.size) {
                remoteView.setViewVisibility(tilesLayouts[i], View.VISIBLE)
                remoteView.setTextViewText(tilesNames[i], stickyNotification.icons?.get(i))
                remoteView.setTextColor(tilesNames[i], Color.parseColor(stickyNotification.tint))
                remoteView.setImageViewResource(tileIconUrls[i], getWidgetImage(isColored ,
                    if(isFlashOn && stickyNotification.icons[i]=="Flashlight") "FlashlightOn" else stickyNotification.icons[i]))
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

    override fun onDestroy() {
        super.onDestroy()
        try{
            myTimer.cancel()
            this@StickyNotificationService.unregisterReceiver(receiver)
        } catch (ex:Exception){ }
    }


    fun logEvent(action: String, isCollapsed: Boolean, widget: String){
        try {
            val bundle = Bundle()
            bundle.putString("clickedOn", widget)
            bundle.putString("brand", Build.BRAND)
            if (isCollapsed) {
                bundle.putString("trayState", "Collapsed")
            } else {
                bundle.putString("trayState", "Expanded")
            }
            FirebaseAnalytics.getInstance(this@StickyNotificationService).logEvent("NotificationClick", bundle)
            ApiSearchSticky().userActionNotification(widget)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }
}