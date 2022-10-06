package com.appyhigh.newsfeedsdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.appyhigh.newsfeedsdk.Constants.APP_NAME
import com.appyhigh.newsfeedsdk.Constants.FEED_APP_ICON
import com.appyhigh.newsfeedsdk.Constants.FEED_TARGET_ACTIVITY
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_NOTIFICATION_ON
import com.appyhigh.newsfeedsdk.Constants.IS_STICKY_SERVICE_ON
import com.appyhigh.newsfeedsdk.Constants.STICKY_NOTIFICATION
import com.appyhigh.newsfeedsdk.Constants.getLifecycleOwner
import com.appyhigh.newsfeedsdk.activity.*
import com.appyhigh.newsfeedsdk.apicalls.*
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_INTERESTS_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.GET_LANGUAGES_ENCRYPTED
import com.appyhigh.newsfeedsdk.apiclient.Endpoints.UPDATE_USER_ENCRYPTED
import com.appyhigh.newsfeedsdk.callbacks.OnAPISuccess
import com.appyhigh.newsfeedsdk.callbacks.PersonalizationListener
import com.appyhigh.newsfeedsdk.callbacks.ShowFeedScreenListener
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.AddInterestBottomSheet
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.model.User
import com.appyhigh.newsfeedsdk.service.StickyNotificationService
import com.appyhigh.newsfeedsdk.utils.*
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.messaging.FirebaseMessaging
import com.jaredrummler.android.device.DeviceName
import java.util.*

class FeedSdk {
    companion object {

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
                LogDetail.LogEStack(ex)
            }
        }

        fun getSDKVersion(): Long {
            return sdkVersion.toLong()
        }

        fun isCricketApp(): Boolean{
            return isCricketApp
        }

        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null
        var mLifecycle: Lifecycle? = null
        var mUser: User? = null
        var spUtil: SpUtil? = null
        var sdkCountryCode: String? = "in"
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
        var interestsList = ArrayList<Interest>()
        var languagesList = ArrayList<Language>()
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
        private var sdkVersion = 1008
        private var isCricketApp = false
    }

    private var parentAppIntent = Intent()

    fun initializeSdk(
        activity: Activity,
        intent: Intent,
        user: User? = null,
        showCricketNotification: Boolean? = true,
        isDark: Boolean? = false
    ) {
        LogDetail.LogD("FeedSdk", "initializeSdk")
        if(activity.baseContext.packageName.contains("cricket.scores")){
            isCricketApp = true
        }
        if (font == null && !isFontDownloading)
            applyFont(activity.baseContext, "Roboto", false)
        onUserInitialized = ArrayList()
        mContext = activity.baseContext
        mLifecycle = activity.getLifecycleOwner().lifecycle
        mUser = user ?: run {
            val defaultUser = User()
            defaultUser.firstName =
                "guest-" + (Random().nextInt(99999999 - 88888888 + 1) + 88888888)
            spUtil?.putString(Constants.GUEST_USER_SDK, defaultUser.firstName ?: "")
            defaultUser
        }
        SpUtil.spUtilInstance?.init(activity.baseContext)
        spUtil = SpUtil.spUtilInstance
        val info: PackageInfo = mContext!!.packageManager.getPackageInfo(mContext!!.packageName, 0)
        appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toString()
        } else {
            info.versionCode.toString()
        }
        appVersionName = info.versionName.toString()
        try{
            Constants.userAgent = WebSettings.getDefaultUserAgent(mContext)
        } catch (ex:Exception){}
        setIntentBeforeInitialise(intent)
        setTheme(isDark)
        setShareBody(null)
        getCountryCode()
        getAppNameFromManifest()
        getFeedTargetActivityFromManifest()
        getFeedAppIconFromManifest()
        setDeviceDetailsToLocal()
        initializeContentModified()
        spUtil!!.putString(Constants.NETWORK, checkAndSetNetworkType())
        FeedSdk.showCricketNotification = showCricketNotification == null || showCricketNotification
        ApiConfig().configEncrypted()
        getFirebasePushToken(object : FirebaseTokenListener {
            override fun onSuccess(token: String) {
                LogDetail.LogDE("getFirebasePushToken", "getInterestsApiCall")
                ApiCreateOrUpdateUser().createOrUpdateUserEncrypted(
                    UPDATE_USER_ENCRYPTED,
                    firebaseToken,
                    sdkCountryCode,
                    mUser
                )
                apiGetInterests()
                sendPostImpressions()
            }

            override fun onFailure() {
                LogDetail.LogDE(logTag, "Failed to generate Firebase Push Token")
                apiGetInterests()
                sendPostImpressions()
            }
        })
        ImpressionUtils().initialize(activity)
    }

    private fun apiGetInterests() {
        ApiGetInterests().getInterestsEncrypted(
            GET_INTERESTS_ENCRYPTED,
            object : ApiGetInterests.InterestResponseListener {
                override fun onSuccess(interestResponseModel: InterestResponseModel) {
                    for (interest in interestResponseModel.interestList) {
                        Constants.allInterestsMap[interest.keyId!!] = interest
                    }
                }
            }
        )
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
            LogDetail.LogEStack(ex)
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
            LogDetail.LogEStack(ex)
            null
        }
    }

    fun removeNudge(item: BottomNavigationItemView) {
        try {
            if (parentNudgeView != null && parentNudgeView!!.visibility == View.VISIBLE) {
                parentNudgeView!!.visibility = View.GONE
            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
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
                    ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                        Endpoints.UPDATE_USER_ENCRYPTED,
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
                    FeedSdk.languagesList = languageList
                }
            }
        )
    }

    fun setInterestsForFeedSDK(interests: String) {
        ApiGetInterests().getInterestsEncrypted(
            Endpoints.GET_INTERESTS_ENCRYPTED,
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
                        interestList,
                        FeedSdk.languagesList,
                        object : ApiUpdateUserPersonalization.UpdatePersonalizationListener {
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

    fun setShowFeedAdFirst(show: Boolean) {
        showFeedAdAtFirst = show
    }

    fun setPersonalizationListener(personalizationListenerCallback: PersonalizationListener) {
        personalizationListener = personalizationListenerCallback
    }

    fun setListener(userInitialize: OnUserInitialized, tag: String? = "") {
        LogDetail.LogD("FeedSdk $tag", "add userInitialize")
        onUserInitialized.add(userInitialize)
        LogDetail.LogD("FeedSdk $tag", "add userInitialize size ${onUserInitialized.size}")
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
                        LogDetail.LogEStack(ex)
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
            LogDetail.LogEStack(ex)
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
        try {
            DeviceName.with(mContext).request { info, _ ->
                spUtil!!.putString(
                    Constants.DEVICE_MODEL,
                    info.manufacturer.toString() + " " + info.marketName + " " + info.model
                )
            }
        } catch (ex: Exception) {
            spUtil?.putString(Constants.DEVICE_MODEL, Build.MODEL)
        }
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
     * Get Sdk Country Code
     */
    private fun getCountryCode() {
        val telephonyManager =
            mContext?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if(BuildConfig.DEBUG){
            sdkCountryCode = "in"
        } else {
            sdkCountryCode = if (telephonyManager.networkCountryIso.isNotEmpty()) {
                telephonyManager.networkCountryIso
            } else {
                "in"
            }
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
        mContext?.let { context ->
            ApiPostImpression().addPostImpressionsEncrypted(
                Endpoints.POST_IMPRESSIONS_ENCRYPTED,
                context
            )
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
//            if (!intent.hasExtra("fromSticky")) {
//                if (spUtil.getBoolean(IS_STICKY_SERVICE_ON) && (!spUtil.getBoolean(IS_STICKY_NOTIFICATION_ON)
//                            || !mContext!!.isMyServiceRunning(SearchStickyWorker::class.java))
//                ) {
//                    mContext!!.startStickyNotificationService()
//                }
//            }
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
            LogDetail.LogEStack(ex)
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
//            if (mContext!!.isMyServiceRunning(SearchStickyWorker::class.java)) {
//                mContext!!.stopStickyNotificationService()
//            }
        } catch (ex: Exception) {
            LogDetail.LogEStack(ex)
        }
    }

    fun getDynamicUrlData(activity: Activity, intent: Intent, listener: OnAPISuccess) {
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener(activity) { pendingDynamicLinkData ->
                // Get deep link from result (may be null if no link is found)
                var deepLink: Uri? = null
                try {
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.link
                        try {
                            if (deepLink!!.getQueryParameter(Constants.FEED_ID) != null) {
                                if (deepLink!!.getQueryParameter(Constants.IS_NATIVE) != null) {
                                    parentAppIntent.putExtra(Constants.IS_NATIVE, "true")
                                }
                                parentAppIntent.putExtra(
                                    Constants.POST_ID,
                                    pendingDynamicLinkData.link?.getQueryParameter(Constants.FEED_ID)!!
                                )
                            }
                            if (deepLink!!.getQueryParameter(Constants.COVID_CARD) != null) {
                                parentAppIntent.putExtra(
                                    Constants.COVID_CARD,
                                    pendingDynamicLinkData.link?.getQueryParameter(Constants.COVID_CARD)!!
                                )
                            }
                            if (deepLink!!.getQueryParameter(Constants.PODCAST_ID) != null) {
                                parentAppIntent.putExtra(
                                    Constants.PODCAST_ID,
                                    deepLink.getQueryParameter(Constants.PODCAST_ID)!!
                                )

                            }
                            if (deepLink!!.getQueryParameter(Constants.FILENAME) != null
                                && deepLink.getQueryParameter(Constants.MATCHTYPE) != null
                                && deepLink.getQueryParameter(Constants.PWA) != null
                            ) {
                                parentAppIntent.putExtra(
                                    Constants.FILENAME,
                                    deepLink.getQueryParameter(Constants.FILENAME)!!
                                )
                                parentAppIntent.putExtra(
                                    Constants.MATCHTYPE,
                                    deepLink.getQueryParameter(Constants.MATCHTYPE)!!
                                )
                                parentAppIntent.putExtra(
                                    Constants.PWA,
                                    deepLink.getQueryParameter(Constants.PWA)!!
                                )
                            }
                            if (deepLink.getQueryParameter(Constants.MATCHES_MODE) != null) {
                                parentAppIntent.putExtra(
                                    Constants.MATCHES_MODE,
                                    deepLink.getQueryParameter(Constants.MATCHES_MODE)!!
                                )
                            }
                        } catch (ex: Exception) {
                        }
                    }
                    listener.onSuccess()
                } catch (e: Exception) {
                    LogDetail.LogEStack(e)
                    listener.onSuccess()
                }
            }
            .addOnFailureListener(activity) { e ->
                LogDetail.LogEStack(e)
                listener.onSuccess()
            }

    }

    private fun isScreenNotification(intent: Intent): Boolean {
        if (intent.hasExtra(Constants.PUSH_SOURCE) && (intent.getStringExtra(Constants.PUSH_SOURCE) == Constants.FEEDSDK)) {
            if (intent.hasExtra(Constants.INTERESTS) && intent.hasExtra(Constants.POST_ID) && intent.hasExtra(
                    Constants.SHORT_VIDEO
                )
            ) {
                return true
            } else if (intent.hasExtra(Constants.PAGE) && intent.getStringExtra(Constants.PAGE)!!
                    .contains("SDK://") && !intent.getStringExtra(Constants.PAGE)!!
                    .contains("Detail")
            ) {
                return true
            } else if (intent.hasExtra(Constants.FROM_STICKY)) {
                return true
            }
        }
        return false
    }

    private fun setIntentBeforeInitialise(intent: Intent) {
        if (isScreenNotification(intent)) {
            SpUtil.pushIntent = intent
        } else {
            SpUtil.pushIntent = null
        }
        if (intent.hasExtra(Constants.FROM_STICKY) && intent.getStringExtra(Constants.FROM_STICKY) == Constants.REELS) {
            Constants.isVideoFromSticky = true
        } else {
            Constants.videoUnitAdFromSticky = ""
        }
    }

    fun checkFeedSDKNotifications(
        activity: Activity,
        intent: Intent,
        showFeedScreenListener: ShowFeedScreenListener
    ) {
        getDynamicUrlData(activity, intent) {
            parentAppIntent.extras?.let {
                intent.putExtras(it)
            }
            intent.extras?.let {
                for (key in it.keySet()) {
                    LogDetail.LogD("Feedsdk", "handleIntent: $key ${it.get(key)}")
                }
            }
            if (intent.hasExtra(Constants.FROM_STICKY)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (intent.getStringExtra(Constants.FROM_STICKY) == Constants.REELS) {
                        showFeedScreenListener.showReels()
                    } else {
                        showFeedScreenListener.showFeeds()
                    }
                }, 1000)
            } else if (isScreenNotification(intent) || fromLiveMatch(intent)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (checkFeedSdkTab(Constants.EXPLORE, intent)) {
                        showFeedScreenListener.showExplore()
                    } else if (checkFeedSdkTab(Constants.REELS, intent)) {
                        showFeedScreenListener.showReels()
                    } else {
                        showFeedScreenListener.showFeeds()
                    }
                }, 1000)
            } else if (!handleIntent(activity.baseContext, intent)) {
                showFeedScreenListener.checkParentAppNotifications()
            }
        }
    }

    private fun fromLiveMatch(intent: Intent): Boolean {
        return intent.hasExtra(Constants.FROM_LIVE_MATCH) && intent.hasExtra(Constants.INTERESTS)
    }

    private fun checkFeedSdkTab(tab: String, intent: Intent): Boolean {
        return when (tab) {
            Constants.EXPLORE -> (intent.hasExtra(Constants.INTERESTS) && intent.getStringExtra(
                Constants.INTERESTS
            ) == Constants.EXPLORE)
                    || (intent.hasExtra(Constants.PAGE) && intent.getStringExtra(Constants.PAGE)!!
                .contains(Constants.SDK_EXPLORE))
            Constants.REELS -> (intent.hasExtra(Constants.SHORT_VIDEO) && intent.getStringExtra(
                Constants.SHORT_VIDEO
            ) == Constants.TRUE)
                    || (intent.hasExtra(Constants.PAGE) && intent.getStringExtra(Constants.PAGE)!!
                .contains(Constants.SDK_REELS))
            else -> true
        }
    }

    private fun handleIntent(context: Context, intentData: Intent): Boolean {
        if(intentData.extras == null) return false
        if (intentData.hasExtra(Constants.PAGE) && intentData.hasExtra(Constants.PUSH_SOURCE)
            && intentData.getStringExtra(Constants.PUSH_SOURCE) == Constants.FEEDSDK
        ) {
            when (intentData.getStringExtra(Constants.PAGE)) {
                Constants.SDK_PODCAST_DETAIL -> {
                    val intent = Intent(context, PodcastPlayerActivity::class.java)
                    intent.putExtra(Constants.POSITION, 0)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intentData.extras?.let { intent.putExtras(it) }
                    if (intentData.hasExtra(Constants.POST_ID)) {
                        context.startActivity(intent)
                    }
                }
                Constants.SDK_POST_DETAIL -> {
                    val intent =
                        if (intentData.hasExtra(Constants.IS_NATIVE) && intentData.getStringExtra(
                                Constants.IS_NATIVE
                            ) == Constants.TRUE
                        ) {
                            val bundle = Bundle()
                            bundle.putString("NativePageOpen", "Notfication")
                            FirebaseAnalytics.getInstance(context).logEvent("NativePage", bundle)
                            Intent(context, PostNativeDetailActivity::class.java)
                        } else {
                            Intent(context, NewsFeedPageActivity::class.java)
                        }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra(Constants.POSITION, 0)
                    intent.putExtra(Constants.FROM_APP, true)
                    intent.putExtra(
                        Constants.POST_ID,
                        intentData.getStringExtra(Constants.POST_ID).toString()
                    )
                    intentData.extras?.let { intent.putExtras(it) }
                    context.startActivity(intent)
                }
                Constants.SDK_CRYPTO_DETAIL -> {
                    val intent = Intent(context, CryptoCoinDetailsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intentData.extras?.let { intent.putExtras(it) }
                    context.startActivity(intent)
                }
                Constants.SDK_CRICKET_DETAIL -> {
                    val pushIntent = Intent(context, PWAMatchScoreActivity::class.java)
                    // Need post_source value from intent to store analytics to backend
                    if (intentData.hasExtra(Constants.IPL_PUSH)) {
                        pushIntent.putExtra(
                            Constants.POST_SOURCE,
                            intentData.getStringExtra(Constants.IPL_PUSH)
                        )
                    }
                    pushIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intentData.extras?.let { pushIntent.putExtras(it) }
                    context.startActivity(pushIntent)
                }
            }

        } else if (intentData.hasExtra(Constants.PODCAST_ID)) {
            val intent = Intent(context, PodcastPlayerActivity::class.java)
            intent.putExtra(Constants.POSITION, 0)
            intentData.extras?.let { intent.putExtras(it) }
            intent.putExtra(Constants.POST_ID, intentData.getStringExtra(Constants.PODCAST_ID))
            intentData.extras?.let { intent.putExtras(it) }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else if (intentData.hasExtra(Constants.FILENAME) && intentData.hasExtra(Constants.MATCHTYPE)) {
            val pushIntent = Intent(context, PWAMatchScoreActivity::class.java)
            pushIntent.putExtra(Constants.FROM_APP, true)
            // Need post_source value from intent to store analytics to backend
            intentData.extras?.let { pushIntent.putExtras(it) }
            pushIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intentData.hasExtra(Constants.POST_SOURCE)) {
                pushIntent.putExtra(
                    Constants.POST_SOURCE,
                    intentData.getStringExtra(Constants.POST_SOURCE)
                )
            } else if (intentData.hasExtra(Constants.IPL_PUSH)) {
                pushIntent.putExtra(
                    Constants.POST_SOURCE,
                    intentData.getStringExtra(Constants.IPL_PUSH)
                )
            }

            context.startActivity(pushIntent)
        } else if (intentData.hasExtra(Constants.FILENAME) && intentData.hasExtra(Constants.LAUNCHTYPE)
            && intentData.getStringExtra(Constants.LAUNCHTYPE) == Constants.CRICKET
        ) {
            val pushIntent = Intent(context, PWAMatchScoreActivity::class.java)
            pushIntent.putExtra(Constants.FROM_APP, true)
            pushIntent.putExtra(Constants.FILENAME, intentData.getStringExtra(Constants.FILENAME))
            pushIntent.putExtra(Constants.MATCHTYPE, Constants.LIVE_MATCHES)
            pushIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intentData.hasExtra(Constants.POST_SOURCE)) {
                pushIntent.putExtra(
                    Constants.POST_SOURCE,
                    intentData.getStringExtra(Constants.POST_SOURCE)
                )
            } else if (intentData.hasExtra(Constants.IPL_PUSH)) {
                pushIntent.putExtra(
                    Constants.POST_SOURCE,
                    intentData.getStringExtra(Constants.IPL_PUSH)
                )
            }
            intentData.extras?.let { pushIntent.putExtras(it) }
            context.startActivity(pushIntent)
        } else if (intentData.hasExtra(Constants.POST_ID) && intentData.getStringExtra(Constants.POST_ID)!!
                .isNotEmpty()
        ) {
            val intent =
                if (intentData.hasExtra(Constants.IS_NATIVE) && intentData.getStringExtra(Constants.IS_NATIVE) == Constants.TRUE) {
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
            intent.putExtra(
                Constants.POST_ID,
                intentData.getStringExtra(Constants.POST_ID).toString()
            )
            intentData.extras?.let { intent.putExtras(it) }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else if (intentData.hasExtra(Constants.PUSH_SOURCE) && (intentData.getStringExtra(
                Constants.PUSH_SOURCE
            ) == Constants.FEEDSDK)
            && intentData.hasExtra(Constants.WHICH)
        ) {
            LogDetail.LogD("Result", "Got the data " + intentData.getStringExtra(Constants.WHICH))
            val which: String = intentData.getStringExtra(Constants.WHICH).toString()
            if (which.equals("L", ignoreCase = true)) {
                try {
                    if (intentData.hasExtra(Constants.POST_ID)) {
                        val bundle = Bundle()
                        bundle.putString("NativePageOpen", "Notfication")
                        FirebaseAnalytics.getInstance(context).logEvent("NativePage", bundle)
                        val intent =
                            if (intentData.hasExtra(Constants.IS_NATIVE) && intentData.getStringExtra(
                                    Constants.IS_NATIVE
                                ) == Constants.TRUE
                            ) {
                                Intent(context, PostNativeDetailActivity::class.java)
                            } else {
                                Intent(context, NewsFeedPageActivity::class.java)
                            }
                        intent.putExtra(Constants.FROM_APP, true)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intentData.extras?.let { intent.putExtras(it) }
                        context.startActivity(intent)
                    }
                } catch (e: java.lang.Exception) {
                    LogDetail.LogEStack(e)
                }
            }
        } else {
            return false
        }
        return true
    }


    interface FirebaseTokenListener {
        fun onSuccess(token: String)
        fun onFailure()
    }

    interface OnUserInitialized {
        fun onInitSuccess()
    }
}