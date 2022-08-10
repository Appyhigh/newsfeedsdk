package com.appyhigh.newsfeedsdk.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.PodcastPlayerActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiPostImpression
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.PostImpressionsModel
import com.appyhigh.newsfeedsdk.model.PostView
import com.bumptech.glide.Glide
import com.google.gson.Gson

class PodcastMediaPlayer {

    companion object {
        var mediaPlayer:MediaPlayer?=null
        private var podcastListeners =  HashMap<String, PodcastMediaPlayerListener>()
        @SuppressLint("StaticFieldLeak")
        var mContext:Context?=null
        private var podcastMediaCard = PodcastMediaCard()
        private val channelID = "PodcastNotification"
        var check = false
        var receiver: BroadcastReceiver =object : BroadcastReceiver() {
            override fun onReceive(context: Context, receiveIntent: Intent) {
                // val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                // sendBroadcast(closeIntent)
                val appName = FeedSdk.appName
                when (receiveIntent.action) {
                    appName + "PodcastBackward" -> movePlayer(false)
                    appName + "PodcastForward" -> movePlayer(true)
                    appName + "PodcastPlay" -> {
                        if(!check) {
                            check = true
                            playOrPause(context)
                        }
                        Handler(Looper.getMainLooper()).postDelayed({ check = false }, 1000)
                    }
                    appName + "PodcastDismiss" -> releasePlayer(context)
                    "PodcastPause" -> pausePlayer(context)
                }
            }
        }
        val TAG = "PodcastNotification"
        var serviceRunning = false
        var NOTIFICATION_ID = 1253

        fun init(context: Context, podcastCard:PodcastMediaCard, podcastMediaPlayerListener: PodcastMediaPlayerListener, screen: String){
            try{
                if(mediaPlayer==null || podcastCard.postId!= podcastMediaCard.postId){
                    releasePlayer(context)
                    LogDetail.LogD(TAG, "init: $context")
                    podcastListeners[screen] = podcastMediaPlayerListener
                    podcastMediaCard = podcastCard
                    mediaPlayer = MediaPlayer()
                    mediaPlayer!!.setDataSource(podcastMediaCard.url)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaPlayer!!.setAudioAttributes(
                            AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    }
                    mediaPlayer!!.prepareAsync()
                    mediaPlayer!!.setOnPreparedListener {
                        AudioTracker.init(context, "podcasts", AudioTracker.PODCASTS, null, object : AudioTrackerListener {
                            override fun onSuccess() {
                                mediaPlayer!!.start()
                                for (listener in podcastListeners.values) {
                                    listener.onStarted()
                                }
                                startPodcastNotification(context)
                                Handler(Looper.getMainLooper()).postDelayed(updateSeekBar, 1000)
                            }

                            override fun onFailure() {
                                pausePlayer(context)
                            }
                        })
                    }
                    mediaPlayer!!.setOnCompletionListener {
                        for(listener in podcastListeners.values) {
                            listener.onCompleted()
                        }
                        startPodcastNotification(context)
                    }
                } else{
                    podcastListeners[screen] = podcastMediaPlayerListener
                }
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }

        fun addPodcastListener(key: String, listener: PodcastMediaPlayerListener){
            podcastListeners[key] = listener
        }

        fun isPlaying():Boolean{
            return try {
                mediaPlayer != null && mediaPlayer!!.isPlaying
            } catch (ex:Exception){
                false
            }
        }

        fun playOrPause(context: Context) {
            try {
                if(isPlaying()){
                    pausePlayer(context)
                } else{
                    resumePlayer(context)
                }
            } catch (ex:Exception){

            }
        }

        fun pausePlayer(context: Context){
            mediaPlayer?.pause()
            for(listener in podcastListeners.values){
                listener.onPause()
            }
            startPodcastNotification(context)
        }

        fun resumePlayer(context: Context){
            AudioTracker.init(context, "podcasts", AudioTracker.PODCASTS, null, object : AudioTrackerListener {
                override fun onSuccess() {
                    mediaPlayer?.start()
                    for (listener in podcastListeners.values) {
                        listener.onPlay()
                    }
                    startPodcastNotification(context)
                    Handler(Looper.getMainLooper()).postDelayed(updateSeekBar, 1000)
                }

                override fun onFailure() {
                    pausePlayer(context)
                }
            })
        }

        fun releasePlayer(context: Context){
            LogDetail.LogD(TAG, "releasePlayer: called")
            try {
                if(mediaPlayer!=null){
                    storeData(context)
                }
                removeNotification(context)
                mediaPlayer?.release()
                for (listener in podcastListeners.values) {
                    listener.onReleased()
                }
                podcastMediaCard = PodcastMediaCard()
                mediaPlayer = null
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }

        fun movePlayer(isForward:Boolean){
            try {
                if (isForward) {
                    mediaPlayer?.seekTo(mediaPlayer!!.currentPosition + 10 * 1000)
                } else{
                    mediaPlayer?.seekTo(mediaPlayer!!.currentPosition - 10 * 1000)
                }
                val totalDuration = mediaPlayer!!.duration.toLong()
                val currentDuration = mediaPlayer!!.currentPosition.toLong()
                val progress = ((currentDuration*1.0)/totalDuration)*100
                for(listener in podcastListeners.values) {
                    listener.onPlayerProgressed(convertTime(currentDuration), progress.toInt())
                }
            } catch (ex:Exception){

            }
        }

        fun movePlayer(progress: Int){
            try {
                val totalDuration = mediaPlayer!!.duration.toLong()
                val currentDuration = (progress*totalDuration)/100
                mediaPlayer?.seekTo(currentDuration.toInt())
                for(listener in podcastListeners.values) {
                    listener.onPlayerProgressed(convertTime(currentDuration), progress.toInt())
                }
            } catch (ex:Exception){

            }
        }

        fun totalDuration():String{
            return if(mediaPlayer!=null){
                convertTime(mediaPlayer!!.duration.toLong())!!
            } else{
                "00:00"
            }
        }

        fun getPodcastMediaCard():PodcastMediaCard{
            return podcastMediaCard
        }

        private val updateSeekBar: Runnable = object : Runnable {
            override fun run() {
                try{
                    val totalDuration = mediaPlayer!!.duration.toLong()
                    val currentDuration = mediaPlayer!!.currentPosition.toLong()
                    val progress = ((currentDuration*1.0)/totalDuration)*100
                    for(listener in podcastListeners.values) {
                        listener.onPlayerProgressed(convertTime(currentDuration), progress.toInt())
                    }
                    // Call this thread again after 15 milliseconds => ~ 1000/60fps
                    if(isPlaying())
                        Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                }catch (ex:Exception){}
            }
        }


        fun convertTime(milliSeconds: Long): String {
            val seconds = milliSeconds/1000
            val s = seconds % 60
            val m = seconds / 60 % 60
            val h = seconds / (60 * 60) % 24
            return if(h>0){
                String.format("%02d:%02d:%02d", h, m, s)
            } else{
                String.format("%02d:%02d", m, s)
            }
        }

        fun startPodcastNotification(context: Context){
            if(serviceRunning){
                setNotification(context)
            } else {
                createNotificationChannel(context)
                setNotification(context)
                val appName = FeedSdk.appName
                // add actions here !
                val intentFilter = IntentFilter()
                intentFilter.addAction(appName + "PodcastBackward")
                intentFilter.addAction(appName + "PodcastForward")
                intentFilter.addAction(appName + "PodcastPlay")
                intentFilter.addAction(appName + "PodcastDismiss")
                intentFilter.addAction("PodcastPause")
                try{
                    context.unregisterReceiver(receiver)
                } catch (ex:Exception){}
                context.registerReceiver(receiver, intentFilter)
            }
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serviceChannel = NotificationChannel(
                    channelID,
                    "Podcast Notification",
                    NotificationManager.IMPORTANCE_HIGH
                )
                val manager = context.getSystemService(NotificationManager::class.java)
                serviceChannel.setSound(null, null)
                manager?.createNotificationChannel(serviceChannel)
            }
        }

        private fun setNotification(context: Context){
            try {
                if(mediaPlayer==null) return
                val podcastCard = getPodcastMediaCard()
                val backgroundTheme = if (isDarkTheme(context)) Color.parseColor("#36424E") else Color.WHITE
                val colorTheme = if (isDarkTheme(context)) Color.WHITE else Color.parseColor("#36424E")
                val remoteView = RemoteViews(context.packageName, R.layout.layout_podcast_collapsed)
                remoteView.setInt(R.id.podcastMainLayout, "setBackgroundColor", backgroundTheme)
                remoteView.setImageViewResource(R.id.appIcon, FeedSdk.feedAppIcon)
                remoteView.setTextViewText(R.id.appName, FeedSdk.appName)
                remoteView.setTextColor(R.id.appName, colorTheme)
                remoteView.setTextViewText(R.id.title, podcastCard.title)
                remoteView.setTextColor(R.id.title, colorTheme)
                if (podcastCard.publisherName.isNotEmpty()) {
                    remoteView.setTextViewText(R.id.publisherName, "By " + podcastCard.publisherName)
                }
                remoteView.setInt(R.id.backward, "setColorFilter", colorTheme)
                remoteView.setInt(R.id.play, "setColorFilter", colorTheme)
                remoteView.setInt(R.id.forward, "setColorFilter", colorTheme)
                if (isPlaying()) {
                    remoteView.setImageViewResource(R.id.play, R.drawable.ic_podcast_pause_white)
                } else {
                    remoteView.setImageViewResource(R.id.play, R.drawable.ic_podcast_play_white)
                }
                var intent = Intent(FeedSdk.appName + "PodcastBackward")
                remoteView.setOnClickPendingIntent(
                    R.id.backward,
                    PendingIntent.getBroadcast(context, 1, intent, 0)
                )
                intent = Intent(FeedSdk.appName + "PodcastForward")
                remoteView.setOnClickPendingIntent(
                    R.id.forward,
                    PendingIntent.getBroadcast(context, 1, intent, 0)
                )
                intent = Intent(FeedSdk.appName + "PodcastPlay")
                remoteView.setOnClickPendingIntent(
                    R.id.play,
                    PendingIntent.getBroadcast(context, 1, intent, 0)
                )
                if(podcastCard.imageBitmap!=null){
                    remoteView.setImageViewBitmap(R.id.image, podcastCard.imageBitmap)
                }
                if (podcastCard.imageUrl == "" || !URLUtil.isValidUrl(podcastCard.imageUrl)) {
                    remoteView.setViewVisibility(R.id.image, View.GONE)
                }
                val startIntent = Intent(context, PodcastPlayerActivity::class.java)
                startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startIntent.putExtra(Constants.POSITION, podcastCard.position)
                startIntent.putExtra(Constants.INTEREST, podcastCard.interest)
                startIntent.putExtra(Constants.POST_ID, podcastCard.postId)
                startIntent.putExtra(Constants.ALREADY_EXISTS, true)
                startIntent.action = System.currentTimeMillis().toString()
                val pendingIntent = PendingIntent.getActivity(context, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                val notification = NotificationCompat.Builder(context!!, channelID)
                    .setSmallIcon(FeedSdk.feedAppIcon)
                    .setCustomContentView(remoteView)
                    .setContent(remoteView)
                    .setCustomBigContentView(remoteView)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(PendingIntent.getBroadcast(context, 1, Intent(FeedSdk.appName + "PodcastDismiss"), 0))
                    .setSilent(true)
                    .setGroup("Podcast Notification")
                    .setGroupSummary(true)
                    .build()
                LogDetail.LogD(TAG, "setNotification: image "+podcastCard.imageUrl)
                val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
                serviceRunning = true
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
                LogDetail.LogD(TAG, "setNotification: error $context")
                removeNotification(context)
            }
        }

        private fun removeNotification(context: Context){
            try {
                val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
                serviceRunning = false
                LogDetail.LogD(TAG, "removeNotification: ")
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }

        private fun isDarkTheme(context: Context): Boolean {
            return context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }


        fun setPodcastListener(view: View, key: String){
            try {
                val podcastMainLayout: LinearLayout = view.findViewById(R.id.podcastMainLayout)
                val playIcon: ImageView = view.findViewById(R.id.podcastBottomPlay)
                val forward: ImageView = view.findViewById(R.id.podcastBottomForward)
                val backward: ImageView = view.findViewById(R.id.podcastBottomBackward)
                val image: ImageView = view.findViewById(R.id.podcastBottomImage)
                val title: TextView = view.findViewById(R.id.podcastBottomTitle)
                val publisherName: TextView = view.findViewById(R.id.podcastBottomPublisherName)
                if (mediaPlayer != null) {
                    podcastMainLayout.visibility = View.VISIBLE
                    title.text = getPodcastMediaCard().title
                    publisherName.text = "By "+getPodcastMediaCard().publisherName
                    try{
                        Glide.with(view.context)
                            .load(getPodcastMediaCard().imageUrl)
                            .into(image)
                    } catch (ex:Exception){
                        LogDetail.LogEStack(ex)
                    }
                    if(isPlaying()){
                        playIcon.setImageResource(R.drawable.ic_podcast_pause_white)
                    } else{
                        playIcon.setImageResource(R.drawable.ic_podcast_play_white)
                    }
                } else {
                    podcastMainLayout.visibility = View.GONE
                }
                forward.setOnClickListener { movePlayer(true) }
                backward.setOnClickListener { movePlayer(false) }
                playIcon.setOnClickListener {
                    playOrPause(view.context)
                }
                podcastMainLayout.setOnClickListener {
                    val startIntent = Intent(view.context, PodcastPlayerActivity::class.java)
                    startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startIntent.putExtra(Constants.POSITION, getPodcastMediaCard().position)
                    startIntent.putExtra(Constants.INTEREST, getPodcastMediaCard().interest)
                    startIntent.putExtra(Constants.POST_ID, getPodcastMediaCard().postId)
                    startIntent.putExtra(Constants.ALREADY_EXISTS, true)
                    startIntent.action = System.currentTimeMillis().toString()
                    view.context.startActivity(startIntent)
                }
                addPodcastListener(key,
                    object : PodcastMediaPlayerListener {
                        override fun onStarted() {
                            podcastMainLayout.visibility = View.VISIBLE
                            title.text = getPodcastMediaCard().title
                            publisherName.text = getPodcastMediaCard().publisherName
                            try {
                                image.setImageBitmap(getPodcastMediaCard().imageBitmap)
                            } catch (ex:Exception){
                                LogDetail.LogEStack(ex)
                            }
                            playIcon.setImageResource(R.drawable.ic_podcast_pause_white)
                        }

                        override fun onCompleted() {
                            playIcon.setImageResource(R.drawable.ic_podcast_play_white)
                        }

                        override fun onReleased() {
                            podcastMainLayout.visibility = View.GONE
                        }

                        override fun onPlayerProgressed(duration: String, progress: Int) {}

                        override fun onPause() {
                            playIcon.setImageResource(R.drawable.ic_podcast_play_white)
                        }

                        override fun onPlay() {
                            playIcon.setImageResource(R.drawable.ic_podcast_pause_white)
                        }

                    })
            } catch (ex:java.lang.Exception){
                LogDetail.LogEStack(ex)
            }
        }

        @SuppressLint("CommitPrefEdits")
        private fun storeData(context: Context){
            if(podcastMediaCard.presentUrl=="" || podcastMediaCard.presentTimeStamp==(0).toLong()){
                return
            }
            try {
                var totalDuration = 0
                var duration = 0
                if(mediaPlayer!=null){
                    totalDuration = mediaPlayer?.duration!!/1000
                    duration = mediaPlayer?.currentPosition!!/1000
                }
                val postView = PostView(
                    FeedSdk.sdkCountryCode ?: "in",
                    podcastMediaCard.feedType,
                    false,
                    podcastMediaCard.languageString,
                    podcastMediaCard.interestString,
                    podcastMediaCard.postId,
                    podcastMediaCard.postSource,
                    podcastMediaCard.publisherId,
                    false,
                    podcastMediaCard.publisherName,
                    totalDuration,
                    duration
                )
                val postImpressions = ArrayList<PostView>()
                postImpressions.add(postView)
                val postImpressionsModel = PostImpressionsModel(podcastMediaCard.presentUrl, postImpressions, podcastMediaCard.presentTimeStamp)
                val gson = Gson()
                val sharedPrefs = context.getSharedPreferences("postImpressions", Context.MODE_PRIVATE)
                val postImpressionString = gson.toJson(postImpressionsModel)
                sharedPrefs.edit().putString(podcastMediaCard.postId, postImpressionString).apply()
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    context
                )
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        }
    }
}

interface PodcastMediaPlayerListener{
    fun onStarted()
    fun onCompleted()
    fun onReleased()
    fun onPlayerProgressed(duration: String, progress:Int)
    fun onPause()
    fun onPlay()
}

data class PodcastMediaCard(
    var url:String = "",
    var postId:String = "",
    var title:String = "",
    var publisherName:String = "",
    var publisherId:String = "",
    var imageUrl:String = "",
    var imageBitmap: Bitmap?=null,
    var position: Int = 0,
    var interest: String = "unknown",
    var languageString: String? = null,
    var interestString: String? = null,
    var feedType:String = "unknown",
    var postSource:String = "unknown",
    var presentTimeStamp:Long = 0,
    var presentUrl:String = ""
)