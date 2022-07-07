package com.appyhigh.newsfeedsdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import com.appyhigh.newsfeedsdk.Constants.APP_NAME
import com.appyhigh.newsfeedsdk.Constants.FEED_APP_ICON
import com.appyhigh.newsfeedsdk.Constants.FEED_TARGET_ACTIVITY
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_NOTIFICATION_ON
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_SERVICE_ON
import com.appyhigh.newsfeedsdk.Constants.NEWS_FEED_APP_ID
import com.appyhigh.newsfeedsdk.Constants.STICKY_NOTIFICATION
import com.appyhigh.newsfeedsdk.activity.*
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.callbacks.PersonalizationListener
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.service.NotificationCricketService
import com.appyhigh.newsfeedsdk.service.StickyNotificationService
import com.appyhigh.newsfeedsdk.utils.*
import com.appyhigh.newsfeedsdk.utils.SocketConnection.SocketClientCallback
import com.appyhigh.newsfeedsdk.utils.SocketConnection.initSocketConnection
import com.appyhigh.newsfeedsdk.utils.SocketConnection.isSocketConnected
import com.appyhigh.newsfeedsdk.utils.SocketConnection.isSocketListenersNotificationSet
import com.appyhigh.newsfeedsdk.utils.SocketConnection.setSocketListenersNotification
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jaredrummler.android.device.DeviceName
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import android.graphics.Typeface
import android.os.*
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.appyhigh.newsfeedsdk.Constants.USER_ID
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_FEEDS_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_INTERESTS_APPWISE_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_INTERESTS_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_LANGUAGES_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.UPDATE_USER_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.USER_DETAILS_ENCRYPTED
import com.appyhigh.newsfeedsdk.encryption.AuthSocket
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.encryption.SessionUser
import com.appyhigh.newsfeedsdk.customview.NewsFeedList
import com.appyhigh.newsfeedsdk.fragment.AddInterestBottomSheet
import com.appyhigh.newsfeedsdk.model.*
import com.google.common.reflect.TypeToken
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONArray
import kotlin.collections.HashMap

class FeedSdk {
    companion object {
        fun handleIntent(context: Context, intentData: Intent): Boolean {
            Log.d("cricHouse", "handleIntent: " + intentData.extras.toString())
            if (intentData.hasExtra("page") && intentData.hasExtra("push_source")
                && intentData.getStringExtra("push_source") == "feedsdk"
            ) {
                when (intentData.getStringExtra("page")) {
                    "SDK://podcastDetail" -> {
                        val intent = Intent(context, PodcastPlayerActivity::class.java)
                        intent.putExtra(Constants.POSITION, 0)
                        intentData.extras?.let { intent.putExtras(it) }
                        if (intentData.hasExtra(Constants.POST_ID)) {
                            context.startActivity(intent)
                        }
                    }
                    "SDK://feedDetail" -> {
                        val intent =
                            if (intentData.hasExtra("is_native") && intentData.getStringExtra("is_native") == "true") {
                                val bundle = Bundle()
                                bundle.putString("NativePageOpen", "Notfication")
                                FirebaseAnalytics.getInstance(context)
                                    .logEvent("NativePage", bundle)
                                Intent(context, PostNativeDetailActivity::class.java)
                            } else {
                                Intent(context, NewsFeedPageActivity::class.java)
                            }
                        intent.putExtra(Constants.POSITION, 0)
                        intent.putExtra(Constants.FROM_APP, true)
                        intent.putExtra(
                            Constants.POST_ID,
                            intentData.getStringExtra("post_id").toString()
                        )
                        intentData.extras?.let { intent.putExtras(it) }
                        context.startActivity(intent)
                    }
                    "SDK://cryptoCoinDetail" -> {
                        val intent = Intent(context, CryptoCoinDetailsActivity::class.java)
                        intentData.extras?.let { intent.putExtras(it) }
                        context.startActivity(intent)
                    }
                    "SDK://cricketMatchDetail" -> {
                        val pushIntent = Intent(context, PWAMatchScoreActivity::class.java)
                        // Need post_source value from intent to store analytics to backend
                        if (intentData.hasExtra("ipl_push")) {
                            pushIntent.putExtra(
                                "post_source",
                                intentData.getStringExtra("ipl_push")
                            )
                        }
                        intentData.extras?.let { pushIntent.putExtras(it) }
                        context.startActivity(pushIntent)
                    }
                }

            } else if (intentData.hasExtra("podcast_id")) {
                val intent = Intent(context, PodcastPlayerActivity::class.java)
                intent.putExtra(Constants.POSITION, 0)
                intentData.extras?.let { intent.putExtras(it) }
                intent.putExtra(Constants.POST_ID, intentData.getStringExtra("podcast_id"))
                intentData.extras?.let { intent.putExtras(it) }
                context.startActivity(intent)
            } else if (intentData.hasExtra("filename") && intentData.hasExtra("matchType")) {
                val pushIntent = Intent(context, PWAMatchScoreActivity::class.java)
                pushIntent.putExtra("from_app", true)
                // Need post_source value from intent to store analytics to backend
                intentData.extras?.let { pushIntent.putExtras(it) }
                if (intentData.hasExtra("post_source")) {
                    pushIntent.putExtra("post_source", intentData.getStringExtra("post_source"))
                } else if (intentData.hasExtra("ipl_push")) {
                    pushIntent.putExtra("post_source", intentData.getStringExtra("ipl_push"))
                }

                context.startActivity(pushIntent)
            } else if (intentData.hasExtra("filename") && intentData.hasExtra("launchType") && intentData.getStringExtra(
                    "launchType"
                ) == "cricket"
            ) {
                val pushIntent = Intent(context, PWAMatchScoreActivity::class.java)
                pushIntent.putExtra("from_app", true)
                pushIntent.putExtra("filename", intentData.getStringExtra("filename"))
                pushIntent.putExtra("matchType", Constants.LIVE_MATCHES)
                // Need post_source value from intent to store analytics to backend
                if (intentData.hasExtra("post_source")) {
                    pushIntent.putExtra("post_source", intentData.getStringExtra("post_source"))
                } else if (intentData.hasExtra("ipl_push")) {
                    pushIntent.putExtra("post_source", intentData.getStringExtra("ipl_push"))
                }
                intentData.extras?.let { pushIntent.putExtras(it) }
                context.startActivity(pushIntent)
            } else if (intentData.hasExtra("post_id") && intentData.getStringExtra("post_id")!!
                    .isNotEmpty()
            ) {
                val intent =
                    if (intentData.hasExtra("is_native") && intentData.getStringExtra("is_native") == "true") {
                        val bundle = Bundle()
                        bundle.putString("NativePageOpen", "Notfication")
                        FirebaseAnalytics.getInstance(context).logEvent("NativePage", bundle)
                        Intent(context, PostNativeDetailActivity::class.java)
                    } else {
                        Intent(context, NewsFeedPageActivity::class.java)
                    }
                intent.putExtra(Constants.INTEREST, "dynamicUrl") // send interest
                intent.putExtra(Constants.POSITION, 0)
                intent.putExtra(Constants.FROM_APP, true)
                intentData.extras?.let { intent.putExtras(it) }
                intent.putExtra(Constants.POST_ID, intentData.getStringExtra("post_id").toString())
                intentData.extras?.let { intent.putExtras(it) }
                context.startActivity(intent)
            } else if (intentData.hasExtra("push_source") && (intentData.getStringExtra("push_source") == "feedsdk") && intentData.hasExtra(
                    "which"
                )
            ) {
                Log.i("Result", "Got the data " + intentData.getStringExtra("which"))
                val which: String = intentData.getStringExtra("which").toString()
                if (which.equals("L", ignoreCase = true)) {
                    try {
                        if (intentData.hasExtra("post_id")) {
                            val bundle = Bundle()
                            bundle.putString("NativePageOpen", "Notfication")
                            FirebaseAnalytics.getInstance(context).logEvent("NativePage", bundle)
                            val intent =
                                if (intentData.hasExtra("is_native") && intentData.getStringExtra("is_native") == "true") {
                                    Intent(context, PostNativeDetailActivity::class.java)
                                } else {
                                    Intent(context, NewsFeedPageActivity::class.java)
                                }
                            intent.putExtra("from_app", true)
                            intentData.extras?.let { intent.putExtras(it) }
                            context.startActivity(intent)
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                return false
            }
            return true
        }

        fun isScreenNotification(intent: Intent): Boolean {
            if (intent.hasExtra("push_source") && (intent.getStringExtra("push_source") == "feedsdk")) {
                if (intent.hasExtra("interests") && intent.hasExtra("post_id") && intent.hasExtra("short_video")) {
                    return true
                } else if (intent.hasExtra("page") && intent.getStringExtra("page")!!
                        .contains("SDK://") && !intent.getStringExtra("page")!!.contains("Detail")
                ) {
                    return true
                } else if (intent.hasExtra("fromSticky")) {
                    return true
                }
            }
            return false
        }

        fun checkFeedSdkTab(tab: String, intent: Intent): Boolean {
            return when (tab) {
                "explore" -> (intent.hasExtra("interests") && intent.getStringExtra("interests") == "explore")
                        || (intent.hasExtra("page") && intent.getStringExtra("page")!!
                    .contains("SDK://explore"))
                "reels" -> (intent.hasExtra("short_video") && intent.getStringExtra("short_video") == "true")
                        || (intent.hasExtra("page") && intent.getStringExtra("page")!!
                    .contains("SDK://reels"))
                else -> true
            }
        }

        fun fromLiveMatch(intent: Intent): Boolean {
            return intent.hasExtra("fromLiveMatch") && intent.hasExtra("interests")
        }

        fun onDestroyCalled(context: Context) {
            PodcastMediaPlayer.releasePlayer(context)
        }

        fun applyFont(typeface: Typeface) {
            font = typeface
        }

        fun applyFont(context: Context, fontName: String, refreshNeeded: Boolean = true) {
            try {
                isFontDownloading = true
                val request =
                    FontRequest(
                        "com.google.android.gms.fonts",
                        "com.google.android.gms",
                        fontName,
                        R.array.com_google_android_gms_fonts_certs
                    )
                val handlerThread = HandlerThread("fonts")
                handlerThread.start()
                val mHandler = Handler(handlerThread.looper)
                val callback = object : FontsContractCompat.FontRequestCallback() {

                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        // Your code to use the font goes here
                        font = typeface
//                        Constants.Toaster.show(context, "Font Changed")
                        if (refreshNeeded) {
                            for (listener in SpUtil.onRefreshListeners) {
                                listener.value.onRefreshNeeded()
                            }
                        }
                        isFontDownloading = false
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        // Your code to deal with the failure goes here
//                        Constants.Toaster.show(context, "Font Change Failed")
                        isFontDownloading = false
                    }
                }
                FontsContractCompat.requestFont(context, request, callback, mHandler)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        var mContext: Context? = null
        var mLifecycle: Lifecycle? = null
        var mUser: User? = null
        var spUtil: SpUtil? = null
        var sdkCountryCode: String? = ""
        var userId: String? = ""
        var appId: String? = ""
        var appName: String? = ""
        var appVersionCode: String = ""
        var appVersionName: String = ""
        var hideFilters: Boolean = false
        private val logTag = "FeedSdk"
        private var firebaseToken = ""
        var isSdkInitializationSuccessful = false
        var isExploreInitializationSuccessful = false
        private var mSharePrefixText = ""
        var mFirebaseDynamicLink = ""
        var onUserInitialized: ArrayList<OnUserInitialized?> = ArrayList()
        var onExploreInitialized: OnUserInitialized? = null
        var personalizationListener: PersonalizationListener? = null
        var isRefreshNeeded = false
        var areContentsModified: HashMap<String, Boolean> = HashMap()
        var interestsList = ArrayList<com.appyhigh.newsfeedsdk.model.Interest>()
        var languagesList = ArrayList<com.appyhigh.newsfeedsdk.model.Language>()
        var shareBody = ""
        var feedTargetActivity = ""
        var feedAppIcon = 0
        var showFeedAdAtFirst = true
        var hasPluto = false
        var searchStickyBackground = "#337CFF"
        var showCricketNotification = true
        var languageForAPICalls = ""
        var interestsForAPICalls = ""
        var nativeAdInterval: Long = 60
        var font: Typeface? = null
        private var isFontDownloading = false
        var sdkTheme: String = "light"
        var isExistingUser: Boolean = false
        var isCryptoApp = false
        var parentNudgeView: FrameLayout? = null
    }

    fun refreshToken(): String {
        return try {
            RSAKeyGenerator.getNewJwtToken(appId, userId)!!
        } catch (e: Exception) {
            ""
        }
    }

    fun initializeSdk(
        context: Context, lifecycle: Lifecycle, versionCode: String, versionName: String,
        user: User? = null, showCricketNotification: Boolean? = true, isDark: Boolean? = false
    ) {
        Log.d("FeedSdk", "initializeSdk")
        if (font == null && !isFontDownloading)
            applyFont(context, "Roboto", false)
        onUserInitialized = ArrayList()
        mContext = context
        mLifecycle = lifecycle
        mUser = user ?: run {
            val defaultUser = User()
            defaultUser.firstName =
                "guest-" + (Random().nextInt(99999999 - 88888888 + 1) + 88888888)
            spUtil?.putString(Constants.GUEST_USER_SDK, defaultUser.firstName ?: "")
            defaultUser
        }

        SpUtil.spUtilInstance?.init(context)
        spUtil = SpUtil.spUtilInstance
        appVersionCode = versionCode
        appVersionName = versionName
        setTheme(isDark)
        setShareBody(null)
        getCountryCode()
        getAppIdFromManifest()
        getAppNameFromManifest()
        getFeedTargetActivityFromManifest()
        getFeedAppIconFromManifest()
        setUserId()
        setDeviceDetailsToLocal()
        initializeContentModified()
        spUtil!!.putString(Constants.NETWORK, checkAndSetNetworkType())
        FeedSdk.showCricketNotification = showCricketNotification == null || showCricketNotification
        val token = RSAKeyGenerator.getJwtToken(appId, userId)
        token?.let { ApiConfig().configEncrypted(it) }
        getFirebasePushToken(object : FirebaseTokenListener {
            override fun onSuccess(token: String) {
                LogDetail.LogDE("getFirebasePushToken", "getInterestsApiCall")
                val mDataIntent = Intent()
                mDataIntent.putExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_EMAIL, user?.email)
                mDataIntent.putExtra(
                    AuthSocket.INTENT_CONSTANTS.AUTH_USER_NUMBER,
                    user?.phoneNumber
                )
                mDataIntent.putExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_NAME, user?.username)
                mDataIntent.putExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_ID, userId)
                mDataIntent.putExtra(AuthSocket.INTENT_CONSTANTS.AUTH_USER_FCM, firebaseToken)
                SessionUser.Instance().setData(mDataIntent)

                spUtil!!.getString(Constants.JWT_TOKEN)?.let {
                    ApiCreateOrUpdateUser().createOrUpdateUserEncrypted(
                        UPDATE_USER_ENCRYPTED,
                        it,
                        firebaseToken,
                        sdkCountryCode,
                        mUser
                    )
                }
                apiGetInterests()
                sendPostImpressions()
                setDataFromFirebase()
            }

            override fun onFailure() {
                Log.e(logTag, "Failed to generate Firebase Push Token")
                apiGetInterests()
                sendPostImpressions()
                setDataFromFirebase()
            }
        })
        ImpressionUtils().initialize(context)
    }

    private fun apiGetInterests() {
        spUtil!!.getString(Constants.JWT_TOKEN)?.let {
            ApiGetInterests().getInterestsEncrypted(
                GET_INTERESTS_ENCRYPTED,
                it,
                object : ApiGetInterests.InterestResponseListener {
                    override fun onSuccess(interestResponseModel: InterestResponseModel) {
                        for (interest in interestResponseModel.interestList) {
                            Constants.allInterestsMap[interest.keyId!!] = interest
                        }
                    }
                }
            )
        }
    }

    fun setTheme(isDark: Boolean?) {
        try {
            if (isDark != null) {
                if (isDark) {
                    sdkTheme = "dark"
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            } else {
                sdkTheme = if (isDarkTheme(mContext!!)) "dark" else "light"
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun openInterestMenu(fragmentManager: FragmentManager) {
        val bottomSheet = AddInterestBottomSheet.newInstance()
        bottomSheet.show(fragmentManager, "BOTTOMSHEETINTEREST")
    }

    private fun isDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun setNudge(
        bottomNav: BottomNavigationView,
        positionOfIcon: Int,
        position: String? = "right",
        isDark: Boolean? = false,
        themeColor: String? = null,
        notificationCount: String? = null,
    ): BottomNavigationItemView? {
        return try {
            val itemView =
                (bottomNav.getChildAt(0) as BottomNavigationMenuView).getChildAt(positionOfIcon) as BottomNavigationItemView
            val item = LayoutInflater.from(bottomNav.context).inflate(
                when (position) {
                    "top" -> R.layout.nudge_design_top
                    "left" -> R.layout.nudge_design_left
                    else -> R.layout.nudge_design_right
                }, itemView, true
            )
            itemView.setOnClickListener {
                if (parentNudgeView?.visibility == View.VISIBLE) {
                    FirebaseAnalytics.getInstance(itemView.context).logEvent("FeedNudgeClick", null)
                }
                bottomNav.selectedItemId = itemView.id
            }
            val randomPosts = notificationCount ?: (Random().nextInt(15 - (5 + 1)) + 5)
            val color = if (themeColor.isNullOrEmpty()) ContextCompat.getColor(
                bottomNav.context,
                R.color.purple_500
            ) else Color.parseColor(themeColor)
            val nudgeText = item.findViewById<TextView>(R.id.notificationsBadge)
            val nudgeFrame = item.findViewById<FrameLayout>(R.id.nudgeFrame)
            val nudgeInnerImage: AppCompatImageView? = item.findViewById(R.id.nudgeInnerImage)
            parentNudgeView = item.findViewById(R.id.main_frame)
            parentNudgeView?.setOnClickListener {
                FirebaseAnalytics.getInstance(itemView.context).logEvent("FeedNudgeClick", null)
                bottomNav.selectedItemId = itemView.id
            }
            nudgeText.text =
                if (notificationCount.isNullOrEmpty()) "$randomPosts+" else "$notificationCount+"
            if (isDark == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    nudgeFrame.backgroundTintList = ColorStateList.valueOf(color)
                }
                nudgeInnerImage?.setImageResource(R.drawable.ic_nudge_inner_white)
                nudgeText.setTextColor(Color.WHITE)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    nudgeFrame.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                }
                nudgeInnerImage?.setImageResource(R.drawable.ic_nudge_inner_dark)
                nudgeInnerImage?.setColorFilter(color)
                nudgeText.setTextColor(color)
            }
            itemView
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun removeNudge(item: BottomNavigationItemView) {
        try {
            if (parentNudgeView != null && parentNudgeView!!.visibility == View.VISIBLE) {
                parentNudgeView!!.visibility = View.GONE
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    fun setLanguageForAPICalls(languages: String) {
        languageForAPICalls = languages
    }

    fun setInterestsForAPICalls(interests: String) {
        interestsForAPICalls = interests
    }

    fun setLanguagesForFeedSDK(languages: String) {
        ApiGetLanguages().getLanguagesEncrypted(
            GET_LANGUAGES_ENCRYPTED,
            object : ApiGetLanguages.LanguageResponseListener {
                override fun onSuccess(languageResponseModel: List<Language>) {
                    val paramLanguageList = languages.split(",")
                    val mLanguageResponseModel = languageResponseModel as ArrayList<Language>
                    val languageList = ArrayList<Language>()
                    for (language in mLanguageResponseModel) {
                        if (paramLanguageList.contains(language.id)) {
                            languageList.add(language)
                        }
                    }
                    FeedSdk.userId?.let {
                        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                            ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                                Endpoints.UPDATE_USER_ENCRYPTED,
                                it,
                                FeedSdk.interestsList,
                                languageList,
                                object :
                                    ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                                    override fun onFailure() {}

                                    override fun onSuccess() {
                                        FeedSdk.isRefreshNeeded = true
                                    }
                                }
                            )
                        }
                    }
                    FeedSdk.languagesList = languageList
                }
            }
        )
    }

    fun setInterestsForFeedSDK(interests: String) {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetInterests().getInterestsEncrypted(
                Endpoints.GET_INTERESTS_ENCRYPTED,
                it,
                object : ApiGetInterests.InterestResponseListener {
                    override fun onSuccess(interestResponseModel: InterestResponseModel) {
                        val paramInterestList = interests.split(",")
                        val mInterestResponseModel =
                            interestResponseModel.interestList as ArrayList<Interest>
                        val interestList = ArrayList<Interest>()
                        for (interest in mInterestResponseModel) {
                            if (paramInterestList.contains(interest.keyId)) {
                                interestList.add(interest)
                            }
                        }
                        ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                            Endpoints.UPDATE_USER_ENCRYPTED,
                            it,
                            interestList,
                            FeedSdk.languagesList,
                            object :
                                ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                                override fun onFailure() {}

                                override fun onSuccess() {
                                    FeedSdk.isRefreshNeeded = true
                                }
                            }
                        )
                        FeedSdk.interestsList = interestList
                    }
                }
            )
        }
    }

    fun setShowFeedAdFirst(show: Boolean) {
        showFeedAdAtFirst = show
    }

    fun setPersonalizationListener(personalizationListenerCallback: PersonalizationListener) {
        personalizationListener = personalizationListenerCallback
    }

    fun setListener(userInitialize: OnUserInitialized) {
        onUserInitialized.add(userInitialize)
    }

    fun setExploreListener(userInitialize: OnUserInitialized) {
        onExploreInitialized = userInitialize
    }

    /**
     * Get firebase push token
     */
    private fun getFirebasePushToken(param: FirebaseTokenListener) {
        try {
            if (isNetworkAvailable()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener {
                    try {
                        if (it.isComplete) {
                            Log.i(logTag, "token ${it.result.toString()}")
                            firebaseToken = it.result.toString()
                            spUtil!!.putString(Constants.PUSH_TOKEN, firebaseToken)
                            param.onSuccess(firebaseToken)
                        } else {
                            param.onFailure()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        param.onSuccess("")
                    }
                }.addOnFailureListener {
                    param.onFailure()
                }
            } else {
                param.onSuccess("")
            }
        } catch (e: Exception) {
            param.onSuccess("")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager =
                mContext?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    /**
     * Check connected network type
     */
    private fun checkAndSetNetworkType(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isConnectedNewApi()
        } else {
            isConnectedOld()
        }
    }

    @Suppress("DEPRECATION")
    fun isConnectedOld(): String {
        val connManager =
            mContext?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.activeNetworkInfo
        return try {
            val isWiFi = networkInfo!!.type == ConnectivityManager.TYPE_WIFI
            if (isWiFi) {
                "Wifi"
            } else {
                "Mobile"
            }
        } catch (e: Exception) {
            "Mobile"
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun isConnectedNewApi(): String {
        val cm = mContext?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return try {
            when {
                capabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                else -> "Mobile"
            }
        } catch (e: java.lang.Exception) {
            "Mobile"
        }
    }

    /**
     * Fetch device details and set to local store
     */
    private fun setDeviceDetailsToLocal() {
        DeviceName.with(mContext).request { info, _ ->
            spUtil!!.putString(
                Constants.DEVICE_MODEL,
                info.manufacturer.toString() + " " + info.marketName + " " + info.model
            )
        }
    }


    /**
     * Verify if NEWS_FEED_APP_ID is provided or not
     * if not throw an error
     */
    private fun getAppIdFromManifest() {
        val ai: ApplicationInfo? = mContext?.packageManager?.getApplicationInfo(
            mContext?.packageName!!,
            PackageManager.GET_META_DATA
        )
        val bundle = ai?.metaData
        appId = bundle?.getString(NEWS_FEED_APP_ID)!!.toString()
//        val tempId = bundle?.get(NEWS_FEED_APP_ID)!!.toString()
//        if(tempId.contains("E")){
//            val tempIds = tempId.split("E")
//            val result = BigDecimal(tempIds[0].toDouble() * Math.exp(tempIds[1].toDouble()))
//            Log.d(TAG, "getAppIdFromManifest: "+result)
//        }
    }

    private fun getAppNameFromManifest() {
        val ai: ApplicationInfo? = mContext?.packageManager?.getApplicationInfo(
            mContext?.packageName!!,
            PackageManager.GET_META_DATA
        )
        val bundle = ai?.metaData
        appName = bundle?.getString(APP_NAME)!!
    }

    private fun getFeedTargetActivityFromManifest() {
        val ai: ApplicationInfo? = mContext?.packageManager?.getApplicationInfo(
            mContext?.packageName!!,
            PackageManager.GET_META_DATA
        )
        val bundle = ai?.metaData
        feedTargetActivity = bundle?.getString(FEED_TARGET_ACTIVITY)!!
    }

    private fun getFeedAppIconFromManifest() {
        val ai: ApplicationInfo? = mContext?.packageManager?.getApplicationInfo(
            mContext?.packageName!!,
            PackageManager.GET_META_DATA
        )
        val bundle = ai?.metaData
        feedAppIcon = bundle?.getInt(FEED_APP_ICON)!!
    }

    /**
     * Set User Id
     */
    @SuppressLint("HardwareIds")
    private fun setUserId() {
        val existingId = SpUtil.spUtilInstance?.getString(USER_ID, "")
        userId = if (!existingId.isNullOrEmpty()) {
            existingId
        } else {
            appId + "_" + Settings.Secure.getString(
                mContext?.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }
        try {
            RSAKeyGenerator.getNewJwtToken(appId, userId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * Get Sdk Country Code
     */
    private fun getCountryCode() {
        val telephonyManager =
            mContext?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        sdkCountryCode = if (telephonyManager.networkCountryIso.isNotEmpty()) {
            telephonyManager.networkCountryIso
        } else {
            "IN"
        }
    }

    fun setHideFilters(hide: Boolean) {
        hideFilters = hide
    }

    /**
     * Set prefix Share text for posts
     */
    fun setSharePrefixText(sharePrefixText: String) {
        mSharePrefixText = sharePrefixText
    }

    fun getSharePrefixText(): String {
        return mSharePrefixText
    }

    /**
     * Set prefix Share text for posts
     */
    fun setFirebaseDynamicLink(firebaseDynamicLink: String) {
        mFirebaseDynamicLink = firebaseDynamicLink
    }

    fun getFirebaseDynamicLink(): String {
        return mFirebaseDynamicLink
    }

    private fun initializeContentModified() {
        areContentsModified[Constants.FEED] = false
        areContentsModified[Constants.EXPLORE] = false
        areContentsModified[Constants.HASHTAG] = false
    }

    private fun sendPostImpressions() {
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { token ->
            mContext?.let { context ->
                ApiPostImpression().addPostImpressionsEncrypted(
                    Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                    token,
                    context
                )
            }
        }
    }

    fun setShareBody(shareText: String?) {
        if (shareText == null) {
            shareBody = """
            फ़ास्ट और फ्री फाइल ट्रांसफर के लिए ये ऍप बेस्ट है।और सबसे अच्छी बात ये इंडियन है🇮🇳 ट्राई करो
            Love this File Sharing App😍. Ultra-fast, no internet needed & Indian🇮🇳! 
            Download: https://play.google.com/store/apps/details?id=${mContext!!.applicationContext.packageName}""".trimIndent()
        } else {
            shareBody = shareText
        }
    }

    private fun setDataFromFirebase() {
        val spUtil = SpUtil.spUtilInstance!!
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val map = mutableMapOf<String, Any>()
        map[Constants.FEED_SDK_CONFIG] =
            "{\"socket_series\":\"\",\"feed_native_ad_interval\":60,\"searchSticky\":{\"timer\":300,\"workManager\":6}}"
        remoteConfig.setDefaultsAsync(map)
        remoteConfig.fetch(if (BuildConfig.DEBUG) 0 else TimeUnit.HOURS.toSeconds(12))
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    remoteConfig.activate()
                }
                try {
                    val feedSdkConfig =
                        JSONObject(remoteConfig.getString(Constants.FEED_SDK_CONFIG))
                    try {
                        val socketSeries = feedSdkConfig.getString(Constants.SOCKET_SERIES)
                        spUtil.putString(Constants.SOCKET_SERIES, socketSeries)
                        if (FeedSdk.showCricketNotification) {
                            handleCricketNotification()
                            setSocketNotificationIntervals()
                        }
                    } catch (ex: Exception) {
                    }
                    try {
                        nativeAdInterval = feedSdkConfig.getLong(Constants.FEED_NATIVE_AD_INTEVRAL)
                    } catch (e: java.lang.Exception) {
                    }
                    try {
                        val searchStickyConfig = feedSdkConfig.getJSONObject("searchSticky")
                        SpUtil.spUtilInstance!!.putLong(
                            "stickyTimerInterval",
                            searchStickyConfig.getLong("timer")
                        )
                        SpUtil.spUtilInstance!!.putLong(
                            "stickyWorkInterval",
                            searchStickyConfig.getLong("workManager")
                        )
                    } catch (ex: Exception) {
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
    }

    fun setSearchStickyNotification(defaultBackground: String, intent: Intent) {
        try {
            val spUtil = SpUtil.spUtilInstance
            if (defaultBackground != "") {
                searchStickyBackground = defaultBackground
            }
            if (!spUtil!!.contains(STICKY_NOTIFICATION)) {
                spUtil.putString(
                    STICKY_NOTIFICATION,
                    "{ \"icons\": [\"Camera\",\"News\", \"Video\", \"Whatsapp\"], \"tint\" : \"#FFFFFF\", \"background\" : \"default\", \"type\":\"solid\", \"backgroundType\":\"solid\" }"
                )
            }
            if (!spUtil!!.contains(IS_STICKY_SERVICE_ON)) {
                spUtil.putBoolean(IS_STICKY_SERVICE_ON, true)
            }
            if (!spUtil.contains(IS_STICKY_NOTIFICATION_ON)) {
                spUtil.putBoolean(IS_STICKY_NOTIFICATION_ON, false)
            }
            if (!intent.hasExtra("fromSticky")) {
                if (spUtil.getBoolean(IS_STICKY_SERVICE_ON) && (!spUtil.getBoolean(
                        IS_STICKY_NOTIFICATION_ON
                    )
                            || !mContext!!.isMyServiceRunning(StickyNotificationService::class.java))
                ) {
                    mContext!!.startStickyNotificationService()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun stopStickySearchNotification(switchOffService: Boolean = false) {
        try {
            val spUtil = SpUtil.spUtilInstance
            if (switchOffService) {
                spUtil?.putBoolean(IS_STICKY_SERVICE_ON, false)
            }
            if (mContext!!.isMyServiceRunning(StickyNotificationService::class.java)) {
                mContext!!.stopStickyNotificationService()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setSocketNotificationIntervals() {
        val socketSeries =
            SpUtil.spUtilInstance!!.getString(Constants.SOCKET_SERIES, "")!!.split(",")
        var call = 0
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCricketSchedule().getCricketScheduleEncrypt(
                Endpoints.GET_CRICKET_SCHEDULE_ENCRYPTED,
                it,
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
                                val socketWorkRequest = OneTimeWorkRequestBuilder<SocketWorker>()
                                    .setInitialDelay(
                                        matchDateTime - currentDateTime,
                                        TimeUnit.MILLISECONDS
                                    )
                                    .build()
                                WorkManager.getInstance(mContext!!).enqueueUniqueWork(
                                    match.items[0].seriesname + match.items[0].matchdate_gmt + match.items[0].matchtime_gmt,
                                    ExistingWorkPolicy.REPLACE,
                                    socketWorkRequest
                                )
                            }
                            //                    call+=1
                            //                    Log.d("check777", "onSuccess: "+call*10000)
                            //                    val socketWorkRequest = OneTimeWorkRequestBuilder<SocketWorker>()
                            //                        .setInitialDelay((call*10000).toLong(), TimeUnit.MILLISECONDS)
                            //                        .build()
                            //                    WorkManager.getInstance(mContext!!).
                            //                    enqueueUniqueWork(match.items[0].seriesname+match.items[0].matchdate_gmt+match.items[0].matchtime_gmt,
                            //                        ExistingWorkPolicy.REPLACE,
                            //                        socketWorkRequest)
                        }
                    }

                    override fun onFailure(error: Throwable) {
                        error.printStackTrace()
                    }
                },
                0
            )
        }
    }

    private fun handleCricketNotification() {
        val socketSeries =
            SpUtil.spUtilInstance!!.getString(Constants.SOCKET_SERIES, "")!!.split(",")
        SpUtil.spUtilInstance?.getString(Constants.JWT_TOKEN)?.let {
            ApiCricketSchedule().getCricketScheduleEncrypt(
                Endpoints.GET_CRICKET_SCHEDULE_ENCRYPTED,
                it,
                Constants.LIVE_MATCHES,
                object : ApiCricketSchedule.CricketScheduleResponseListener {
                    override fun onSuccess(cricketScheduleResponse: CricketScheduleResponse) {
                        val cards = ArrayList<Card>()
                        for (card in cricketScheduleResponse.cards) {
                            if (card.items[0].matchstatus.lowercase() != "stumps"
                                && (card.items[0].teama.lowercase() == "india"
                                        || card.items[0].teamb.lowercase() == "india"
                                        || socketSeries.contains(card.items[0].seriesname))
                            ) {
                                cards.add(card)
                            }
                        }
                        if (cards.isEmpty() && mContext!!.isMyServiceRunning(
                                NotificationCricketService::class.java
                            )
                        ) {
                            mContext?.stopNotificationCricketService()
                        } else {
                            try {
                                //            var scoreObject = JSONObject("{\"data\":{\"filename\":\"innz11252021205688\",\"Status\":\"PlayInProgress\",\"Equation\":\"NewZealandtrailby256runs\",\"Innings\":{\"First\":{\"BattingteamId\":\"4\",\"BowlingteamId\":\"5\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BattingteamShort\":\"IND\",\"BowlingteamShort\":\"NZ\",\"Battingteam\":\"India\",\"Bowlingteam\":\"NewZealand\",\"Runs\":\"345\",\"Wickets\":\"10\",\"Overs\":\"111.1\",\"Runrate\":\"3.1\"},\"Second\":{\"BattingteamId\":\"5\",\"BowlingteamId\":\"4\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BattingteamShort\":\"NZ\",\"BowlingteamShort\":\"IND\",\"Battingteam\":\"NewZealand\",\"Bowlingteam\":\"India\",\"Runs\":\"89\",\"Wickets\":\"0\",\"Overs\":\"33.2\",\"Runrate\":\"2.67\"},\"Third\":{\"BattingteamId\":\"4\",\"BowlingteamId\":\"5\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BattingteamShort\":\"IND\",\"BowlingteamShort\":\"NZ\",\"Battingteam\":\"India\",\"Bowlingteam\":\"NewZealand\",\"Runs\":\"345\",\"Wickets\":\"10\",\"Overs\":\"111.1\",\"Runrate\":\"3.1\"},\"Fourth\":{\"BattingteamId\":\"5\",\"BowlingteamId\":\"4\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/5.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/4.png\",\"BattingteamShort\":\"NZ\",\"BowlingteamShort\":\"IND\",\"Battingteam\":\"NewZealand\",\"Bowlingteam\":\"India\",\"Runs\":\"89\",\"Wickets\":\"0\",\"Overs\":\"33.2\",\"Runrate\":\"2.67\"}},\"TourName\":\"NewZealandtourofIndia,2021\"}}")
                                //            Log.d("TAG", "onBindViewHolder: "+scoreObject.toString());
                                //
                                //            mContext?.startNotificationCricketService(scoreObject)
                                //            var scoreObject = JSONObject("{\"data\":{\"filename\":\"sapk04102021200557\",\"status\":\"play in progress\",\"Equation\":\"pakistan need 34 runs in 17 balls at 12 rpo\",\"Innings\"" +
                                //                    ":{\"First\":{\"battingteamid\":\"7\",\"bowlingteamid\":\"6\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/7.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/6.png\",\"BattingteamShort\":\"sa\",\"BowlingteamShort\":\"pak\",\"battingteam\":\"south africa\",\"bowlingteam\":\"pakistan\",\"Runs\":\"188\",\"Wickets\":\"6\",\"Overs\":\"20.0\",\"Runrate\":\"9.40\",\"Allottedovers\":\"20\"}," +
                                //                    "\"Second\":{\"battingteamid\":\"6\",\"bowlingteamid\":\"7\",\"BattingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/6.png\",\"BowlingteamImage\":\"https:\\/\\/cricketimage.blob.core.windows.net\\/teams\\/7.png\",\"battingteamshort\":\"pak\",\"BowlingteamShort\":\"sa\",\"battingteam\":\"pakistan\",\"bowlingteam\":\"south africa\",\"Runs\":\"155\",\"Wickets\":\"5\",\"Overs\":\"17.1\",\"Runrate\":\"9.02\",\"Allottedovers\":\"20\"}},\"Tourname\":\"pakistan tour of south africa, 2021\"}}");
                                //            Log.d("TAG", "onBindViewHolder: "+scoreObject.toString());
                                //            mContext?.startNotificationCricketService(scoreObject);
                                spUtil?.putBoolean("dismissCricket", false)
                                if (!isSocketListenersNotificationSet()) {
                                    val socketClientCallback: SocketClientCallback =
                                        object : SocketClientCallback {
                                            override fun onLiveScoreUpdate(liveScoreData: String) {}
                                            override fun getLiveScore(liveScoreObject: JSONObject) {
                                                if (!spUtil!!.getBoolean("dismissCricket", false))
                                                    try {
                                                        if (liveScoreObject.getJSONObject("data")
                                                                .getString("Status")
                                                                .lowercase(Locale.getDefault()) == "match ended" && mContext!!.isMyServiceRunning(
                                                                NotificationCricketService::class.java
                                                            )
                                                        ) {
                                                            val intent = Intent("dismissCricket")
                                                            mContext?.sendBroadcast(intent)
                                                        } else {
                                                            mContext?.startNotificationCricketService(
                                                                liveScoreObject
                                                            )
                                                        }
                                                    } catch (ex: Exception) {
                                                        ex.printStackTrace()
                                                    }
                                            }
                                        }
                                    setSocketListenersNotification(socketClientCallback)
                                }
                                if (!isSocketConnected()) {
                                    initSocketConnection()
                                }
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onFailure(error: Throwable) {

                    }
                }, 0
            )
        }

    }

    interface FirebaseTokenListener {
        fun onSuccess(token: String)
        fun onFailure()
    }

    interface OnUserInitialized {
        fun onInitSuccess()
    }
}