package com.appyhigh.newsfeedsdk.utils

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.webkit.URLUtil
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.PWAMatchScoreActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiCricketSchedule
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.bumptech.glide.signature.ObjectKey
import org.json.JSONObject
import java.util.*

class CricketSocketWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    private val channelID = "NotificationCricketService"
    val notificationIds:HashMap<String, Int> = HashMap()
    var serviceRunning = false
    var receiver: BroadcastReceiver?=null
    var workerName = ""
    private val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else{
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    override fun doWork(): Result {
        try{
            SpUtil.spUtilInstance?.init(applicationContext)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        try{
            inputData.getString("worker")?.let {
                workerName = it
            }
            // Do the work here--in this case, upload the images.
            if(skipCricketScheduleApiCall){
                skipCricketScheduleApiCall = false
                startNotification()
            } else {
                handleCricketNotification(applicationContext)
            }
            LogDetail.LogDE("CricketSocketWorker", "doWork: "+Date().toString())
            // Indicate whether the work finished successfully with the Result
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        return Result.success()
    }

    private fun handleCricketNotification(context: Context) {
        ApiCricketSchedule().getCricketScheduleEncrypt(
            Endpoints.GET_CRICKET_SCHEDULE_ENCRYPTED,
            Constants.LIVE_MATCHES,
            object : ApiCricketSchedule.CricketScheduleResponseListener {
                override fun onSuccess(cricketScheduleResponse: CricketScheduleResponse) {
                    try{
                        val cards = ArrayList<Card>()
                        for(card in cricketScheduleResponse.cards){
                            if(card.items[0].matchstatus.lowercase()!="stumps"){
                                cards.add(card)
                            }
                        }
                        if(cards.isEmpty() && context.isMyServiceRunning(CricketSocketWorker::class.java)){
                            stopWorker()
                        } else{
                            startNotification()
                        }
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                }

                override fun onFailure(error: Throwable) {

                }
            }, 0)

    }

    private fun startNotification(){
        try {
            createNotificationChannel()
            try {
                SpUtil.spUtilInstance!!.putBoolean("dismissCricket", false)
                val socketClientCallback: SocketConnection.SocketClientCallback = object :
                    SocketConnection.SocketClientCallback {
                    override fun onLiveScoreUpdate(liveScoreData: String) {}
                    override fun getLiveScore(liveScoreObject: JSONObject) {
                        if (!SpUtil.spUtilInstance!!.getBoolean("dismissCricket", false)) {
//                                startNotificationCricketService(liveScoreObject)
                            var data = liveScoreObject
                            if (data.has("data")) {
                                data = data.getJSONObject("data")
                                try {
                                    if (data.getString("Status").lowercase(Locale.getDefault()) == "match ended" && FeedSdk.mContext!!.isMyServiceRunning(CricketSocketWorker::class.java)) {
                                        stopNotification(data.getString("filename"))
                                    } else {
                                        if (!notificationIds.containsKey(data.getString("filename"))) {
                                            val random = Random()
                                            val a = random.nextInt(101) + 1
                                            notificationIds[data.getString("filename")] = a
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
                SocketConnection.setSocketListenersNotification(socketClientCallback)

                if (!SocketConnection.isSocketConnected())
                    SocketConnection.initSocketConnection()
            } catch (e: java.lang.Exception) {
                LogDetail.LogEStack(e)
            }
            // add actions here !
            val intentFilter = IntentFilter()
            intentFilter.addAction("dismissCricket")
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                    if (!intent.hasExtra(FeedSdk.appName) && intent.action == "dismissCricket") {
                        stopWorker()
                    }
                }
            }
            try{
                applicationContext.unregisterReceiver(receiver)
            } catch (ex:Exception){}
            applicationContext.registerReceiver(receiver, intentFilter)
        }
        catch (e: java.lang.Exception){
            LogDetail.LogEStack(e)
        }
    }

    private fun stopWorker(){
        try{
            SpUtil.spUtilInstance?.putBoolean("dismissCricket", true)
            val notificationManager =
                applicationContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            SocketConnection.closeSocketConnection()
            for(notificationId in notificationIds.values){
                notificationManager.cancel(notificationId)
            }
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(workerName);
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun stopNotification(fileName: String){
        try{
            val notificationManager =
                applicationContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            notificationIds[fileName]?.let {
                notificationManager.cancel(it)
                notificationIds.remove(fileName)
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelID,
                "NotificationCricketService Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
            LogDetail.LogD("TAG", "createNotificationChannel: ")
        }
    }

    private fun setNotification(liveScore: JSONObject) {
        LogDetail.LogD("SocketConnection", "getLiveScore: $liveScore")
        LogDetail.LogD("SocketConnection", "getLiveScore: " + notificationIds.get(liveScore.getString("filename")))
        var firstTeamLogo = "";
        var secondTeamLogo = "";
        var contentTitle = "FeedSDK Cricket"
        val collapsedRemoteView = RemoteViews(applicationContext.packageName, R.layout.notification_live_match_collapsed_view)
        val expandedRemoteView = RemoteViews(applicationContext.packageName, R.layout.notification_live_match_expanded_view)
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
        val dismissIntent = PendingIntent.getBroadcast(applicationContext, 1, intent, flags)

        expandedRemoteView.setOnClickPendingIntent(R.id.dismiss, dismissIntent)
        val startIntent =
            Intent(applicationContext, PWAMatchScoreActivity::class.java)
        startIntent.putExtra("filename", liveScore.getString("filename"))
        startIntent.putExtra("matchType", Constants.LIVE_MATCHES)
        startIntent.putExtra("post_source", "cricket_fever")
        startIntent.action = System.currentTimeMillis().toString();
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1,
            startIntent,
            flags
        )
        val notification = NotificationCompat.Builder(applicationContext, channelID)
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
            applicationContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if(serviceRunning) {
            notificationIds[liveScore.getString("filename")]?.let { notificationManager.notify(it, notification) }
        } else {
            notificationIds[liveScore.getString("filename")]?.let {
                notificationManager.notify(it, notification)
                serviceRunning = true
            }
        }
    }

    private fun setTeamLogs(remoteView: RemoteViews, notification: Notification, firstTeamLogo: String, secondTeamLogo: String, key: String) {
        try {
            if(firstTeamLogo == "" || !URLUtil.isValidUrl(firstTeamLogo)){
                remoteView.setViewVisibility(R.id.firstTeamLogo, View.GONE)
            }
            if(secondTeamLogo == "" || !URLUtil.isValidUrl(secondTeamLogo)){
                remoteView.setViewVisibility(R.id.secondTeamLogo, View.GONE)
            }
            val firstTeamNotificationTarget = NotificationTarget(applicationContext, R.id.firstTeamLogo, remoteView, notification, notificationIds[key]!!)
            Glide.with(applicationContext)
                .asBitmap()
                .load(firstTeamLogo)
                .circleCrop()
                .error(R.drawable.team_sample)
                .placeholder(R.drawable.team_sample)
                .signature(ObjectKey(key))
                .into(firstTeamNotificationTarget)
            val secondTeamNotificationTarget = NotificationTarget(applicationContext, R.id.secondTeamLogo, remoteView, notification, notificationIds[key]!!)
            Glide.with(applicationContext)
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


    private fun setTextColors(remoteView: RemoteViews){
        if(isDarkTheme()){
            remoteView.setTextColor(R.id.appName, ContextCompat.getColor(applicationContext, R.color.white))
            remoteView.setTextColor(R.id.dot, ContextCompat.getColor(applicationContext, R.color.white))
            remoteView.setTextColor(R.id.tourName, ContextCompat.getColor(applicationContext, R.color.white))
            remoteView.setTextColor(R.id.firstTeamName, ContextCompat.getColor(applicationContext, R.color.white))
            remoteView.setTextColor(R.id.firstTeamScore, ContextCompat.getColor(applicationContext, R.color.white))
            remoteView.setTextColor(R.id.secondTeamName, ContextCompat.getColor(applicationContext, R.color.white))
            remoteView.setTextColor(R.id.secondTeamScore, ContextCompat.getColor(applicationContext, R.color.white))

        } else{
            remoteView.setTextColor(R.id.appName, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.dot, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.tourName, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.firstTeamName, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.firstTeamScore, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.secondTeamName, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
            remoteView.setTextColor(R.id.secondTeamScore, ContextCompat.getColor(applicationContext, R.color.cricket_feed_title))
        }
    }

    private fun isDarkTheme(): Boolean {
        return applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onStopped() {
        super.onStopped()
        if(receiver!=null){
            applicationContext.unregisterReceiver(receiver)
        }
    }

    private fun setMatchScore(matchScore1: String, matchScore2: String):String{
        return if(matchScore2.isNotEmpty()){
            "$matchScore1 & $matchScore2"
        } else{
            matchScore1
        }
    }

    companion object {
        var skipCricketScheduleApiCall = false
    }
}