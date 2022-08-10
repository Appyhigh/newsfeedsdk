package com.appyhigh.newsfeedsdk.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.google.gson.Gson

@SuppressLint("OverrideAbstract")
class NotificationListener: NotificationListenerService() {

    @SuppressLint("LogNotTimber")
    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        val statusBarString = Gson().toJson(sbn)
        val notificationString = Gson().toJson(sbn!!.notification)
        LogDetail.LogD("NotificationListener", "onNotificationPosted: $notificationString")
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