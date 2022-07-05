package com.appyhigh.newsfeedsdk.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.gson.Gson
import timber.log.Timber

@SuppressLint("OverrideAbstract")
class NotificationListener: NotificationListenerService() {

    @SuppressLint("LogNotTimber")
    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        val statusBarString = Gson().toJson(sbn)
        val notificationString = Gson().toJson(sbn!!.notification)
        Log.d("NotificationListener", "onNotificationPosted: $notificationString")
        super.onNotificationPosted(sbn, rankingMap)
    }



    override fun onNotificationChannelModified(
        pkg: String?,
        user: UserHandle?,
        channel: NotificationChannel?,
        modificationType: Int
    ) {
        super.onNotificationChannelModified(pkg, user, channel, modificationType)
    }


    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        super.onNotificationRankingUpdate(rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationRemoved(sbn, rankingMap)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}