package com.appyhigh.newsfeedsdk.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.apicalls.ApiSearchSticky
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.google.firebase.analytics.FirebaseAnalytics

class SdkEventsActivity : AppCompatActivity() {

    private var packagesMap = hashMapOf("bharat.browser" to true, "u.see.browser.for.uc.browser" to true, "u.browser.for.lite.uc.browser" to true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sdk_events)
        LogDetail.LogDE("SdkEventsActivity", "OnCreate called ")
        if(intent.hasExtra("isCollapsed") && intent.hasExtra("widget")){
            val widget = intent.getStringExtra("widget")!!
            LogDetail.LogDE("SdkEventsActivity", "widget called $widget")
            LogDetail.LogDE("SdkEventsActivity", "widget called ${intent.action}")
            logEvent(intent.getBooleanExtra("isCollapsed", true), widget)
            val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (widget) {
                "Settings" -> {
                    val settingsIntent = Intent(this, SettingsActivity::class.java)
                    settingsIntent.putExtra("stickyTitle", "Settings")
                    settingsIntent.flags = intentFlags
                    startActivity(settingsIntent)
                }
                "Camera" -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    intent.flags = intentFlags
                    startActivity(intent)
                }
                "Weather" -> {
                    val intent = checkAndGetIntent(packagesMap)
                    intent.putExtra("link", "https://www.google.com/search?q=weather")
                    intent.putExtra("title", "Weather")
                    intent.putExtra("fromSticky", "true")
                    intent.flags = intentFlags
                    startActivity(intent)
                }
                "Email" -> {
                    val intent = this.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                    intent!!.flags = intentFlags
                    startActivity(intent)
                }
                "Whatsapp" -> {
                    val intent = this.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    intent!!.flags = intentFlags
                    startActivity(intent)
                }
                "Call" -> {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.flags = intentFlags
                    startActivity(intent)
                }
                "Alarm" -> {
                    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    intent.flags = intentFlags
                    startActivity(intent)
                }
                "Calendar" -> {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
                    intent.flags = intentFlags
                    startActivity(intent)
                }
                "News" -> {
                    val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                    val intent = Intent(this, activity)
                    intent.putExtra("fromSticky", "news")
                    intent.putExtra("push_source", "feedsdk")
                    intent.putExtra(Constants.FEED_TYPE, "search_bar_for_you" )
                    intent.action = System.currentTimeMillis().toString()
                    intent.flags = intentFlags
                    startActivity(intent)
                }
                "Video" -> {
                    val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                    val intent = Intent(this, activity)
                    intent.putExtra("fromSticky", "reels")
                    intent.putExtra("push_source", "feedsdk")
                    intent.putExtra(Constants.FEED_TYPE, "search_bar_quick_bites" )
                    intent.action = System.currentTimeMillis().toString()
                    intent.flags = intentFlags
                    startActivity(intent)
                }
            }
        }
        finish()
    }

    private fun checkAndGetIntent(packagesMap: HashMap<String, Boolean>):Intent{
        return try{
            if(packagesMap.containsKey(applicationContext.packageName)){
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                Intent(this, activity)
            } else {
                Intent(this, WebActivity::class.java)
            }
        } catch (ex:Exception){
            Intent(this, WebActivity::class.java)
        }
    }

    private fun logEvent(isCollapsed: Boolean, widget: String){
        try {
            val bundle = Bundle()
            bundle.putString("clickedOn", widget)
            bundle.putString("brand", Build.BRAND)
            if (isCollapsed) {
                bundle.putString("trayState", "Collapsed")
            } else {
                bundle.putString("trayState", "Expanded")
            }
            FirebaseAnalytics.getInstance(this).logEvent("NotificationClick", bundle)
            ApiSearchSticky().userActionNotification(widget)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }
}