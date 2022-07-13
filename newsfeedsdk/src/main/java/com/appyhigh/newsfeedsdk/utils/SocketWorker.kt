package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apicalls.ApiCricketSchedule
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.service.NotificationCricketService
import org.json.JSONObject
import java.util.*

class SocketWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        try{
            SpUtil.spUtilInstance?.init(applicationContext)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        // Do the work here--in this case, upload the images.
        handleCricketNotification(applicationContext)
        LogDetail.LogD("check777", "doWork: "+Date().toString())
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    private fun handleCricketNotification(context: Context) {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCricketSchedule().getCricketScheduleEncrypt(
                Endpoints.GET_CRICKET_SCHEDULE_ENCRYPTED,
                it,
                Constants.LIVE_MATCHES,
                object : ApiCricketSchedule.CricketScheduleResponseListener {
                    override fun onSuccess(cricketScheduleResponse: CricketScheduleResponse) {
                        val cards = ArrayList<Card>()
                        for(card in cricketScheduleResponse.cards){
                            if(card.items[0].matchstatus.lowercase()!="stumps"){
                                cards.add(card)
                            }
                        }
                        if(cards.isEmpty() && context.isMyServiceRunning(
                                NotificationCricketService::class.java)){
                            context.stopNotificationCricketService()
                        }
                        try {
                            SpUtil.spUtilInstance!!.putBoolean("dismissCricket", false)
                            if (!SocketConnection.isSocketListenersNotificationSet()) {
                                val socketClientCallback: SocketConnection.SocketClientCallback = object :
                                    SocketConnection.SocketClientCallback {
                                    override fun onLiveScoreUpdate(liveScoreData: String) {}
                                    override fun getLiveScore(liveScoreObject: JSONObject) {
                                        try {
                                            if (!SpUtil.spUtilInstance!!.getBoolean("dismissCricket", false)){
                                                if (liveScoreObject.getJSONObject("data").getString("Status")
                                                        .lowercase(Locale.getDefault()) == "match ended" && context.isMyServiceRunning(
                                                        NotificationCricketService::class.java)) {
                                                    val intent = Intent("dismissCricket")
                                                    context.sendBroadcast(intent)
                                                } else {
                                                    context.startNotificationCricketService(liveScoreObject)
                                                }
                                            }
                                        } catch (ex:Exception){
                                            LogDetail.LogEStack(ex)
                                        }
                                    }
                                }
                                SocketConnection.setSocketListenersNotification(socketClientCallback)
                            }
                            if (!SocketConnection.isSocketConnected()) {
                                SocketConnection.initSocketConnection()
                            }
                        } catch (e: java.lang.Exception) {
                            LogDetail.LogEStack(e)
                        }
                    }

                    override fun onFailure(error: Throwable) {

                    }
                }, 0)
        }

    }
}