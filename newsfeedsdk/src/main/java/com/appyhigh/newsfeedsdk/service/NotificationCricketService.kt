package com.appyhigh.newsfeedsdk.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.view.View
import android.webkit.URLUtil.isValidUrl
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.PWAMatchScoreActivity
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.utils.SocketConnection
import com.appyhigh.newsfeedsdk.utils.SocketConnection.SocketClientCallback
import com.appyhigh.newsfeedsdk.utils.SocketConnection.initSocketConnection
import com.appyhigh.newsfeedsdk.utils.SocketConnection.isSocketConnected
import com.appyhigh.newsfeedsdk.utils.SocketConnection.setSocketListenersNotification
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.SpUtil.Companion.spUtilInstance
import com.appyhigh.newsfeedsdk.utils.isMyServiceRunning
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.bumptech.glide.signature.ObjectKey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class NotificationCricketService  : Service(){

    private val channelID = "NotificationCricketService"
    val notificationIds:HashMap<String, Int> = HashMap()
    var serviceRunning = false
    var receiver: BroadcastReceiver?=null
    private val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else{
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()

            try {
                spUtilInstance!!.putBoolean("dismissCricket", false)
                val socketClientCallback: SocketClientCallback = object : SocketClientCallback {
                    override fun onLiveScoreUpdate(liveScoreData: String) {}
                    override fun getLiveScore(liveScoreObject: JSONObject) {
                        if (!spUtilInstance!!.getBoolean("dismissCricket", false)) {
//                                startNotificationCricketService(liveScoreObject)
                            var data = liveScoreObject
                            if (data.has("data")) {
                                data = data.getJSONObject("data")
                                try {
                                    if (data.getString("Status").lowercase(Locale.getDefault()) == "match ended" && FeedSdk.mContext!!.isMyServiceRunning(NotificationCricketService::class.java)) {
                                        val intent = Intent("dismissCricket")
                                        sendBroadcast(intent)
                                    } else {
                                        if (!notificationIds.containsKey(data.getString("filename"))) {
                                            val random = Random()
                                            val a = random.nextInt(101) + 1
                                            notificationIds.put(data.getString("filename"), a)
                                        }
                                        setNotification(data)
                                    }
                                } catch (ex:Exception){
                                    LogDetail.LogEStack(ex)
                                }

                            }
                        }
                    }
                }
                setSocketListenersNotification(socketClientCallback)

                if (!isSocketConnected())
                    initSocketConnection()
            } catch (e: java.lang.Exception) {
                LogDetail.LogEStack(e)
            }
            // add actions here !
            val intentFilter = IntentFilter()
            intentFilter.addAction("dismissCricket")
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                    val notificationManager =
                            context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    if (!intent.hasExtra(packageName) && intent.action == "dismissCricket") {
                        SpUtil.spUtilInstance?.putBoolean("dismissCricket", true)
                        notificationManager.cancelAll()
                        stopForeground(true)
                        SocketConnection.closeSocketConnection()
                        stopSelf()
                    }
                }
            }
            try{
                unregisterReceiver(receiver)
            } catch (ex:Exception){}
            this@NotificationCricketService.registerReceiver(receiver, intentFilter)
        }
        catch (e: java.lang.Exception){
            LogDetail.LogEStack(e)
            stopSelf()
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    channelID,
                    "NotificationCricketService Channel",
                    NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(
                    NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
            LogDetail.LogD("TAG", "createNotificationChannel: ")
        }
        val notification: Notification = NotificationCompat.Builder(this, channelID)
                .setContentTitle("")
                .setContentText("").build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if(intent?.hasExtra("bundleData")  == true){
                val extras = intent.getBundleExtra("bundleData")
                if (extras?.containsKey("broadcasting") == true) {
                    if (extras.getString("broadcasting")!!.equals("off")) {
                        intent.action = Constants.ACTION.STOPFOREGROUND.toString()
                    } else {
                        intent.action = Constants.ACTION.STARTFOREGROUND.toString()
                    }
                }
            }
            LogDetail.LogD("TAG", "onStartCommand: " + intent?.action)
            when {
                intent?.action!! == Constants.ACTION.STOPFOREGROUND.toString() -> {
                    SocketConnection.closeSocketConnection()
                    stopSelf()
                }
                intent.action!! == Constants.ACTION.STARTFOREGROUND.toString() -> {
                    if (intent.hasExtra("liveScores")) {
                        var data = JSONObject(intent.getStringExtra("liveScores")!!)
                        if (data.has("data")) {
                            data = data.getJSONObject("data")
                            if (!notificationIds.containsKey(data.getString("filename"))) {
                                val random = Random()
                                val a = random.nextInt(101) + 1
                                notificationIds.put(data.getString("filename"), a)
                            }
                            setNotification(data)

                        }
                    }
                }
            }
        } catch (ex: Exception){
            LogDetail.LogEStack(ex)
        }
        return START_NOT_STICKY
    }

    fun setNotification(
            liveScore: JSONObject
    ) {
        LogDetail.LogD("SocketConnection", "getLiveScore: " + liveScore.toString())
        LogDetail.LogD("SocketConnection", "getLiveScore: " + notificationIds.get(liveScore.getString("filename")))
        var firstTeamLogo = "";
        var secondTeamLogo = "";
        var contentTitle = NotificationCricketService::class.java.simpleName
        val collapsedRemoteView = RemoteViews(packageName, R.layout.notification_live_match_collapsed_view)
        val expandedRemoteView = RemoteViews(packageName, R.layout.notification_live_match_expanded_view)
        setTextColors(collapsedRemoteView)
        setTextColors(expandedRemoteView)
        collapsedRemoteView.setTextViewText(R.id.appName, FeedSdk.appName)
        expandedRemoteView.setTextViewText(R.id.appName, FeedSdk.appName)
        collapsedRemoteView.setImageViewResource(R.id.appLogo, FeedSdk.feedAppIcon)
        expandedRemoteView.setImageViewResource(R.id.appLogo, FeedSdk.feedAppIcon)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
            collapsedRemoteView.setViewVisibility(R.id.appLogo, View.GONE)
            collapsedRemoteView.setViewVisibility(R.id.appName, View.GONE)
            expandedRemoteView.setViewVisibility(R.id.appLogo, View.GONE)
            expandedRemoteView.setViewVisibility(R.id.appName, View.GONE)
        } else{
            collapsedRemoteView.setViewVisibility(R.id.appLogo, View.VISIBLE)
            collapsedRemoteView.setViewVisibility(R.id.appName, View.VISIBLE)
            expandedRemoteView.setViewVisibility(R.id.appLogo, View.VISIBLE)
            expandedRemoteView.setViewVisibility(R.id.appName, View.VISIBLE)
        }
        if(liveScore.has("TourName")){
            contentTitle = liveScore.getString("TourName")
            collapsedRemoteView.setTextViewText(R.id.tourName, liveScore.getString("TourName"))
            expandedRemoteView.setTextViewText(R.id.tourName, liveScore.getString("TourName"))
        } else{
            collapsedRemoteView.setViewVisibility(R.id.tourName, View.GONE)
            expandedRemoteView.setViewVisibility(R.id.tourName, View.GONE)
        }
        if(liveScore.has("Equation")){
            collapsedRemoteView.setTextViewText(R.id.equation, liveScore.getString("Equation"))
            expandedRemoteView.setTextViewText(R.id.equation, liveScore.getString("Equation"))
        } else{
            collapsedRemoteView.setViewVisibility(R.id.equation, View.GONE)
            expandedRemoteView.setViewVisibility(R.id.equation, View.GONE)
        }
        if(liveScore.has("Innings")) {
            val innings = liveScore.getJSONObject("Innings")
            if (innings.has("First")) {
                val firstInnings = innings.getJSONObject("First")
                firstTeamLogo = firstInnings.getString("BattingteamImage")
                secondTeamLogo = firstInnings.getString("BowlingteamImage")
                collapsedRemoteView.setTextViewText(R.id.firstTeamName, firstInnings.getString("BattingteamShort"))
                expandedRemoteView.setTextViewText(R.id.firstTeamName, firstInnings.getString("BattingteamShort"))
                collapsedRemoteView.setTextViewText(R.id.secondTeamName, firstInnings.getString("BowlingteamShort"))
                expandedRemoteView.setTextViewText(R.id.secondTeamName, firstInnings.getString("BowlingteamShort"))
                val matchScore1 = firstInnings.getString("Runs") + "/" + firstInnings.getString("Wickets")
                var matchScore2 = ""
                try {
                    if (innings.has("Third")) {
                        val third = JSONObject(innings["Third"].toString())
                        matchScore2 = third.getString("Runs") + "/" + third.getString("Wickets")
                    }
                } catch (ex:Exception){}
                collapsedRemoteView.setTextViewText(R.id.firstTeamScore, setMatchScore(matchScore1, matchScore2))
                expandedRemoteView.setTextViewText(R.id.firstTeamScore, setMatchScore(matchScore1, matchScore2))
                if (innings.has("Third")) {
                    collapsedRemoteView.setTextViewText(R.id.firstTeamOvers, "")
                    expandedRemoteView.setTextViewText(R.id.firstTeamOvers, "")
                } else{
                    collapsedRemoteView.setTextViewText(R.id.firstTeamOvers, "(" + firstInnings.getString("Overs") + ")")
                    expandedRemoteView.setTextViewText(R.id.firstTeamOvers, "(" + firstInnings.getString("Overs") + ")")
                }
            }
            if (innings.has("Second")) {
                collapsedRemoteView.setViewVisibility(R.id.secondTeamLayout, View.VISIBLE)
                expandedRemoteView.setViewVisibility(R.id.secondTeamLayout, View.VISIBLE)
                val secondInnings = innings.getJSONObject("Second")
                val matchScore1 = secondInnings.getString("Runs") + "/" + secondInnings.getString("Wickets")
                var matchScore2 = ""
                try {
                    if (innings.has("Fourth")) {
                        val fourth = JSONObject(innings["Fourth"].toString())
                        matchScore2 = fourth.getString("Runs") + "/" + fourth.getString("Wickets")
                    }
                } catch (ex:Exception){}
                collapsedRemoteView.setTextViewText(R.id.secondTeamScore, setMatchScore(matchScore1, matchScore2))
                expandedRemoteView.setTextViewText(R.id.secondTeamScore, setMatchScore(matchScore1, matchScore2))
                if (innings.has("Fourth")) {
                    collapsedRemoteView.setTextViewText(R.id.secondTeamOvers, "")
                    expandedRemoteView.setTextViewText(R.id.secondTeamOvers, "")
                } else{
                    collapsedRemoteView.setTextViewText(R.id.secondTeamOvers, "(" + secondInnings.getString("Overs") + ")")
                    expandedRemoteView.setTextViewText(R.id.secondTeamOvers, "(" + secondInnings.getString("Overs") + ")")
                }
            } else {
                collapsedRemoteView.setViewVisibility(R.id.secondTeamLayout, View.GONE)
                expandedRemoteView.setViewVisibility(R.id.secondTeamLayout, View.GONE)
            }
        }

        val intent = Intent("dismissCricket")
        intent.putExtra("filename", liveScore.getString("filename"))
        val dismissIntent = PendingIntent.getBroadcast(this@NotificationCricketService, 1, intent, flags)

        expandedRemoteView.setOnClickPendingIntent(R.id.dismiss, dismissIntent)
        val startIntent =
                Intent(this@NotificationCricketService, PWAMatchScoreActivity::class.java)
        startIntent.putExtra("filename", liveScore.getString("filename"))
        startIntent.putExtra("matchType", Constants.LIVE_MATCHES)
        startIntent.putExtra("post_source", "cricket_fever")
        startIntent.setAction(System.currentTimeMillis().toString());
        val pendingIntent = PendingIntent.getActivity(
                this@NotificationCricketService,
                1,
                startIntent,
                flags
        )
        val notification = NotificationCompat.Builder(this, channelID)
                .setContentTitle(contentTitle)
                .setCustomContentView(collapsedRemoteView)
                .setCustomBigContentView(expandedRemoteView)
                .setSmallIcon(R.drawable.news_logo_sample)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setGroup("Cricket Notification")
                .setGroupSummary(true)
                .build()
        notification.flags = Notification.FLAG_ONGOING_EVENT
        setTeamLogs(collapsedRemoteView, notification, firstTeamLogo, secondTeamLogo, liveScore.getString("filename"))
        setTeamLogs(expandedRemoteView, notification, firstTeamLogo, secondTeamLogo, liveScore.getString("filename"))
        val notificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if(serviceRunning) {
            notificationIds.get(liveScore.getString("filename"))?.let { notificationManager.notify(it, notification) }
        } else {
            notificationIds.get(liveScore.getString("filename"))?.let {
                startForeground(it, notification)
                serviceRunning = true
            }
        }
    }

    fun setTeamLogs(remoteView: RemoteViews, notification: Notification, firstTeamLogo: String, secondTeamLogo: String, key: String) {
        try {
            if(firstTeamLogo.equals("") || !isValidUrl(firstTeamLogo)){
                remoteView.setViewVisibility(R.id.firstTeamLogo, View.GONE)
            }
            if(secondTeamLogo.equals("") || !isValidUrl(secondTeamLogo)){
                remoteView.setViewVisibility(R.id.secondTeamLogo, View.GONE)
            }
            val firstTeamNotificationTarget = NotificationTarget(this, R.id.firstTeamLogo, remoteView, notification, notificationIds.get(key)!!)
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(firstTeamLogo)
                    .circleCrop()
                    .error(R.drawable.team_sample)
                    .placeholder(R.drawable.team_sample)
                    .signature(ObjectKey(key))
                    .into(firstTeamNotificationTarget)
            val secondTeamNotificationTarget = NotificationTarget(this, R.id.secondTeamLogo, remoteView, notification, notificationIds.get(key)!!)
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(secondTeamLogo)
                    .circleCrop()
                    .error(R.drawable.team_sample)
                    .placeholder(R.drawable.team_sample)
                    .signature(ObjectKey(key))
                    .into(secondTeamNotificationTarget)
        } catch (ex: Exception){
            LogDetail.LogEStack(ex)
            remoteView.setViewVisibility(R.id.firstTeamLogo, View.GONE)
            remoteView.setViewVisibility(R.id.secondTeamLogo, View.GONE)
        }
    }


    fun setTextColors(remoteView: RemoteViews){
        if(isDarkTheme()){
            remoteView.setTextColor(R.id.appName, ContextCompat.getColor(this, R.color.white))
            remoteView.setTextColor(R.id.dot, ContextCompat.getColor(this, R.color.white))
            remoteView.setTextColor(R.id.tourName, ContextCompat.getColor(this, R.color.white))
            remoteView.setTextColor(R.id.firstTeamName, ContextCompat.getColor(this, R.color.white))
            remoteView.setTextColor(R.id.firstTeamScore, ContextCompat.getColor(this, R.color.white))
            remoteView.setTextColor(R.id.secondTeamName, ContextCompat.getColor(this, R.color.white))
            remoteView.setTextColor(R.id.secondTeamScore, ContextCompat.getColor(this, R.color.white))

        } else{
            remoteView.setTextColor(R.id.appName, ContextCompat.getColor(this, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.dot, ContextCompat.getColor(this, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.tourName, ContextCompat.getColor(this, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.firstTeamName, ContextCompat.getColor(this, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.firstTeamScore, ContextCompat.getColor(this, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.secondTeamName, ContextCompat.getColor(this, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.secondTeamScore, ContextCompat.getColor(this, R.color.cricket_feed_title))
        }
    }

    fun isDarkTheme(): Boolean {
        return this.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onDestroy() {
        super.onDestroy()
        if(receiver!=null){
            this@NotificationCricketService.unregisterReceiver(receiver)
        }
    }

    fun getBitmapfromUrl(imageUrl: String?, context: Context): Bitmap? {
        var bitmap: Bitmap? = null
        if (imageUrl==null || imageUrl.equals("") || !isValidUrl(imageUrl)) {
            return bitmap
        }
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            bitmap = BitmapFactory.decodeStream(input)
            return bitmap
        } catch (e: Exception) {
            LogDetail.LogD("SocketConnection", "getBitmapfromUrl: $e")
            return bitmap
        }
    }

    fun setMatchScore(matchScore1: String, matchScore2: String):String{
        return if(matchScore2.isNotEmpty()){
            "$matchScore1 & $matchScore2"
        } else{
            matchScore1
        }
    }


}