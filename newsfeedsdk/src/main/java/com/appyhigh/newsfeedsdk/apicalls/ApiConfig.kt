package com.appyhigh.newsfeedsdk.apicalls

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.AESCBCPKCS5Encryption
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.model.ConfigModel
import com.appyhigh.newsfeedsdk.model.CricketScheduleResponse
import com.appyhigh.newsfeedsdk.model.ItemAdsModel
import com.appyhigh.newsfeedsdk.model.PrivateAdResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.service.NotificationCricketService
import com.appyhigh.newsfeedsdk.utils.*
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ApiConfig {
    private var configModel:ConfigModel?=null
    private var showAds = false

    companion object {
        private var alreadyCalled = false
    }

    fun getConfigModel(context: Context):ConfigModel{
        if(configModel==null) {
            SpUtil.spUtilInstance?.init(context)
            val json = SpUtil.spUtilInstance!!.getString(Constants.CONFIG_MODEL, "")
            configModel = if (json!!.isEmpty()) ConfigModel()
            else {
                Gson().fromJson(json, ConfigModel::class.java)
            }
            showAds = (configModel!!.showParentAdmobAds || configModel!!.showPrivateAds)
        }
        return configModel!!
    }

    fun checkShowAds(context: Context):Boolean{
        if(configModel==null) getConfigModel(context)
        return showAds
    }

    fun requestAd(context: Context, adType: String, listener: ConfigAdRequestListener, isBanner: Boolean = false){
        if(checkShowAds(context)){
            val itemConfigModel = getItemAdModel(adType)
            if(itemConfigModel.showPrivate){
                ApiPrivateAds().getPrivateAd(context, isBanner, object : PrivateAdResponseListener{
                    override fun onSuccess(privateAdResponse: PrivateAdResponse) {
                        privateAdResponse.creative?.let {
                            val webView = WebView(context)
//                            val htmlString = "<body style=\"margin: 0 auto;\">$it</body>"
                            webView.loadDataWithBaseURL(null, it, "text/html", "utf-8", null)
                            listener.onPrivateAdSuccess(webView)
                            ApiPrivateAds().hitAdUrls(privateAdResponse.eUrl?:"", privateAdResponse.nUrl?:"")
                        }
                    }

                    override fun onFailure() {

                    }
                })
            } else if(itemConfigModel.showAdmob){
                listener.onAdmobAdSuccess(itemConfigModel.admobId)
            } else{
                listener.onAdHide()
            }
        }
    }

    private fun getItemAdModel(adType: String): ItemAdsModel {
        return when (adType){
            "feed_native" -> configModel!!.feedNative
            "video_native" -> configModel!!.videoNative
            "search_page_native" -> configModel!!.searchPageNative
            "search_footer_banner" -> configModel!!.searchFooterBanner
            "post_detail_article_top_native" -> configModel!!.postDetailArticleTopNative
            "post_detail_article_end_native" -> configModel!!.postDetailArticleEndNative
            "post_detail_footer_banner" -> configModel!!.postDetailFooterBanner
            else -> configModel!!.feedNative
        }
    }

    fun configEncrypted() {
        val allDetails =
            BaseAPICallObject().getBaseObjectWithAuth(Constants.GET, Endpoints.GET_CONFIG_ENCRYPTED, ArrayList(), ArrayList())

        LogDetail.LogDE("Test Data", allDetails.toString())
        val publicKey = SessionUser.Instance().publicKey
        val instanceEncryption = AESCBCPKCS5Encryption().getInstance(
            SessionUser.Instance().getPrivateKey(publicKey)
        )
        val sendingData: String = instanceEncryption.encrypt(
            allDetails.toString().toByteArray(
                StandardCharsets.UTF_8
            )
        ) + "." + publicKey
        LogDetail.LogD("Data to be Sent -> ", sendingData)

        AuthSocket.Instance().postData(sendingData, object : ResponseListener {
            override fun onSuccess(apiUrl: String, response: String) {
                LogDetail.LogDE("ApiConfig $apiUrl", response)
                val gson: Gson = GsonBuilder().create()
                val configResponseBase: ConfigModel =
                    gson.fromJson(
                        response,
                        object : TypeToken<ConfigModel>() {}.type
                    )
                val configResponse: Response<ConfigModel> = Response.success(configResponseBase)
                configModel = configResponse.body()
                setConfigValues()
                showAds = (configModel!!.showParentAdmobAds || configModel!!.showPrivateAds)
                SpUtil.spUtilInstance!!.putString(Constants.CONFIG_MODEL, Gson().toJson(configModel))
            }

            override fun onError(call: Call, e: IOException) {
                LogDetail.LogDE("ApiConfig ${Endpoints.GET_CONFIG_ENCRYPTED}", e.toString())
            }
        })
    }

    private fun setConfigValues(){
        try {
            SpUtil.spUtilInstance!!.putString(Constants.SOCKET_SERIES, configModel!!.customFirebaseConfig.socketSeries)
            if (FeedSdk.showCricketNotification && !alreadyCalled) {
                alreadyCalled = true
                handleCricketNotification()
                setSocketNotificationIntervals()
            }
            FeedSdk.nativeAdInterval = configModel!!.customFirebaseConfig.feedNativeAdInterval
            SpUtil.spUtilInstance!!.putLong("stickyTimerInterval", configModel!!.customFirebaseConfig.searchSticky.timer)
            SpUtil.spUtilInstance!!.putLong("stickyWorkInterval", configModel!!.customFirebaseConfig.searchSticky.workManager)
            SpUtil.spUtilInstance!!.putBoolean("showStickyOnTop",  configModel!!.customFirebaseConfig.searchSticky.showStickyOnTop)
        } catch (ex:Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    private fun setSocketNotificationIntervals() {
        val socketSeries = SpUtil.spUtilInstance!!.getString(Constants.SOCKET_SERIES, "")!!.split(",")
        ApiCricketSchedule().getCricketScheduleEncrypt(
            Endpoints.GET_CRICKET_SCHEDULE_ENCRYPTED,
            Constants.UPCOMING_MATCHES,
            object : ApiCricketSchedule.CricketScheduleResponseListener {
                override fun onSuccess(cricketScheduleResponse: CricketScheduleResponse) {
                    for (match in cricketScheduleResponse.cards) {
                        if (match.items[0].teama.lowercase() == "india"
                            || match.items[0].teamb.lowercase() == "india"
                            || socketSeries.contains(match.items[0].seriesname)
                        ) {
                            val matchDateTimeString =
                                match.items[0].matchdate_gmt + " " + match.items[0].matchtime_gmt
                            val formatter = SimpleDateFormat("MM/dd/yyyy hh:mm", Locale.US)
                            formatter.timeZone = TimeZone.getTimeZone("UTC");
                            val matchDateTime = formatter.parse(matchDateTimeString).time
                            val currentDateTime =
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time
                            val data = Data.Builder()
                            val workerName = match.items[0].seriesname + match.items[0].matchdate_gmt + match.items[0].matchtime_gmt
                            data.putString("worker", workerName)
                            val socketWorkRequest = OneTimeWorkRequestBuilder<CricketSocketWorker>()
                                .setInitialDelay(
                                    matchDateTime - currentDateTime,
                                    TimeUnit.MILLISECONDS
                                )
                                .setInputData(data.build())
                                .build()
                            WorkManager.getInstance(FeedSdk.mContext!!).enqueueUniqueWork(
                                workerName,
                                ExistingWorkPolicy.REPLACE,
                                socketWorkRequest
                            )
                        }
                        //                    call+=1
                        //                    LogDetail.LogD("check777", "onSuccess: "+call*10000)
                        //                    val socketWorkRequest = OneTimeWorkRequestBuilder<CricketSocketWorker>()
                        //                        .setInitialDelay((call*10000).toLong(), TimeUnit.MILLISECONDS)
                        //                        .build()
                        //                    WorkManager.getInstance(mContext!!).
                        //                    enqueueUniqueWork(match.items[0].seriesname+match.items[0].matchdate_gmt+match.items[0].matchtime_gmt,
                        //                        ExistingWorkPolicy.REPLACE,
                        //                        socketWorkRequest)
                    }
                }

                override fun onFailure(error: Throwable) {
                    LogDetail.LogEStack(error)
                }
            },
            0
        )
    }

    private fun handleCricketNotification() {
        val socketSeries =
            SpUtil.spUtilInstance!!.getString(Constants.SOCKET_SERIES, "")!!.split(",")
        ApiCricketSchedule().getCricketScheduleEncrypt(
            Endpoints.GET_CRICKET_SCHEDULE_ENCRYPTED,
            Constants.LIVE_MATCHES,
            object : ApiCricketSchedule.CricketScheduleResponseListener {
                override fun onSuccess(cricketScheduleResponse: CricketScheduleResponse) {
                    val cards = java.util.ArrayList<Card>()
                    for (card in cricketScheduleResponse.cards) {
                        if (card.items[0].matchstatus.lowercase() != "stumps"
                            && (card.items[0].teama.lowercase() == "india"
                                    || card.items[0].teamb.lowercase() == "india"
                                    || socketSeries.contains(card.items[0].seriesname))
                        ) {
                            cards.add(card)
                        }
                    }
                    if (cards.isEmpty() && FeedSdk.mContext!!.isMyServiceRunning(
                            NotificationCricketService::class.java
                        )
                    ) {
                        FeedSdk.mContext?.stopNotificationCricketService()
                    } else {
                        try {
                            //            var scoreObject = JSONObject("{\"data\":{\"filename\":\"innz11252021205688\",\"Status\":\"PlayInProgress\",\"Equation\":\"NewZealandtrailby256runs\",\"Innings\":{\"First\":{\"BattingteamId\":\"4\",\"BowlingteamId\":\"5\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BattingteamShort\":\"IND\",\"BowlingteamShort\":\"NZ\",\"Battingteam\":\"India\",\"Bowlingteam\":\"NewZealand\",\"Runs\":\"345\",\"Wickets\":\"10\",\"Overs\":\"111.1\",\"Runrate\":\"3.1\"},\"Second\":{\"BattingteamId\":\"5\",\"BowlingteamId\":\"4\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BattingteamShort\":\"NZ\",\"BowlingteamShort\":\"IND\",\"Battingteam\":\"NewZealand\",\"Bowlingteam\":\"India\",\"Runs\":\"89\",\"Wickets\":\"0\",\"Overs\":\"33.2\",\"Runrate\":\"2.67\"},\"Third\":{\"BattingteamId\":\"4\",\"BowlingteamId\":\"5\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BattingteamShort\":\"IND\",\"BowlingteamShort\":\"NZ\",\"Battingteam\":\"India\",\"Bowlingteam\":\"NewZealand\",\"Runs\":\"345\",\"Wickets\":\"10\",\"Overs\":\"111.1\",\"Runrate\":\"3.1\"},\"Fourth\":{\"BattingteamId\":\"5\",\"BowlingteamId\":\"4\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BattingteamShort\":\"NZ\",\"BowlingteamShort\":\"IND\",\"Battingteam\":\"NewZealand\",\"Bowlingteam\":\"India\",\"Runs\":\"89\",\"Wickets\":\"0\",\"Overs\":\"33.2\",\"Runrate\":\"2.67\"}},\"TourName\":\"NewZealandtourofIndia,2021\"}}")
                            //            LogDetail.LogD("TAG", "onBindViewHolder: "+scoreObject.toString());
                            //
                            //            mContext?.startNotificationCricketService(scoreObject)
                            //            var scoreObject = JSONObject("{\"data\":{\"filename\":\"sapk04102021200557\",\"status\":\"play in progress\",\"Equation\":\"pakistan need 34 runs in 17 balls at 12 rpo\",\"Innings\"" +
                            //                    ":{\"First\":{\"battingteamid\":\"7\",\"bowlingteamid\":\"6\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/7.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/6.png\",\"BattingteamShort\":\"sa\",\"BowlingteamShort\":\"pak\",\"battingteam\":\"south africa\",\"bowlingteam\":\"pakistan\",\"Runs\":\"188\",\"Wickets\":\"6\",\"Overs\":\"20.0\",\"Runrate\":\"9.40\",\"Allottedovers\":\"20\"}," +
                            //                    "\"Second\":{\"battingteamid\":\"6\",\"bowlingteamid\":\"7\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/6.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/7.png\",\"battingteamshort\":\"pak\",\"BowlingteamShort\":\"sa\",\"battingteam\":\"pakistan\",\"bowlingteam\":\"south africa\",\"Runs\":\"155\",\"Wickets\":\"5\",\"Overs\":\"17.1\",\"Runrate\":\"9.02\",\"Allottedovers\":\"20\"}},\"Tourname\":\"pakistan tour of south africa, 2021\"}}");
                            //            LogDetail.LogD("TAG", "onBindViewHolder: "+scoreObject.toString());
                            //            mContext?.startNotificationCricketService(scoreObject);
                            FeedSdk.spUtil?.putBoolean("dismissCricket", false)
                            if (!SocketConnection.isSocketListenersNotificationSet()) {
                                val socketClientCallback: SocketConnection.SocketClientCallback =
                                    object : SocketConnection.SocketClientCallback {
                                        override fun onLiveScoreUpdate(liveScoreData: String) {}
                                        override fun getLiveScore(liveScoreObject: JSONObject) {
                                            if (!FeedSdk.spUtil!!.getBoolean(
                                                    "dismissCricket",
                                                    false
                                                )
                                            )
                                                try {
                                                    if (liveScoreObject.getJSONObject("data")
                                                            .getString("Status")
                                                            .lowercase(Locale.getDefault()) == "match ended" && FeedSdk.mContext!!.isMyServiceRunning(
                                                            NotificationCricketService::class.java
                                                        )
                                                    ) {
                                                        val intent = Intent("dismissCricket")
                                                        FeedSdk.mContext?.sendBroadcast(intent)
                                                    } else {
                                                        FeedSdk.mContext?.startNotificationCricketService(
                                                            liveScoreObject
                                                        )
                                                    }
                                                } catch (ex: Exception) {
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
                }

                override fun onFailure(error: Throwable) {

                }
            }, 0
        )

    }
}




interface ConfigAdRequestListener{
    fun onPrivateAdSuccess(webView: WebView)
    fun onAdmobAdSuccess(adId: String)
    fun onAdHide()
}