package com.appyhigh.newsfeedsdk.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class AudioTracker {
    companion object {
        private val audioTrackerListeners : HashMap<String, AudioTrackerModel> = HashMap()
        var presentRequestType:Int = -1
        const val PODCASTS = 2
        const val VIDEOS = 0
        const val REELS = 1
        private var am:AudioManager?=null
        private var listener:AudioManager.OnAudioFocusChangeListener?=null
        const val TAG = "AudioFocus"
        fun init(context: Context, key: String, type: Int, postId: String?="", audioTrackerListener: AudioTrackerListener){
            am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioTrackerListeners[key] = AudioTrackerModel(type, postId, audioTrackerListener)
            presentRequestType = type
            listener = AudioManager.OnAudioFocusChangeListener {
                when (it) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "AUDIOFOCUS_LOSS $context")
                        for(model in audioTrackerListeners){
                            if(model.value.type != presentRequestType){
                                model.value.listener.onFailure()
                            }
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(TAG, "AUDIOFOCUS_GAIN $context")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                }
            }
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                    setOnAudioFocusChangeListener(listener!!)
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        build()
                    })
                    build()
                }
                am?.requestAudioFocus(audioFocusRequest)
            } else{
                am?.requestAudioFocus(listener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioTrackerListener.onSuccess()
                Handler(Looper.getMainLooper()).postDelayed({ presentRequestType=-1 }, 500)
            }
        }
    }
}

data class AudioTrackerModel(
    val type:Int,
    val postId:String?="",
    val listener: AudioTrackerListener
)

interface AudioTrackerListener{
    fun onSuccess()
    fun onFailure()
}