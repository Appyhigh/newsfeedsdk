package com.appyhigh.newsfeedsdk.fragment

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.appyhigh.newsfeedsdk.BuildConfig
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.getLanguages
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.NewsFeedPageActivity
import com.appyhigh.newsfeedsdk.activity.PWACricketActivity
import com.appyhigh.newsfeedsdk.activity.PWACricketTabsActivity
import com.appyhigh.newsfeedsdk.activity.PostNativeDetailActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.apicalls.ApiGetLanguages
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.databinding.FragmentCricketHomePwaBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.ExtendedWebView
import com.appyhigh.newsfeedsdk.utils.RSAKeyGenerator
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.gson.Gson
import im.delight.android.webview.AdvancedWebView
import java.util.*


class CricketHomePWAFragment : Fragment(), AdvancedWebView.Listener, OnRefreshListener {

    private var pwaLink = ""
    private var keyId = ""
    lateinit var binding: FragmentCricketHomePwaBinding
    var alreadyExists = false
    var prevWeb: ExtendedWebView?=null
    private var isloaded = false
    private var alreadyLoaded = false
    private var link = ""
    private var mUserDetails: UserResponse? = null
    private var mLanguageResponseModel: ArrayList<Language>? = null
    private var languagesMap = HashMap<String, Language>()
    private var currentLanguage = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCricketHomePwaBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onRefreshNeeded() {
        if(isAdded && activity!=null){
            requireActivity().runOnUiThread {
                try{
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setCookie(link, "user_info="+ Gson().toJson(Constants.userDetails))
                    var languages = ""
                    if(keyId=="cricket") {
                        for ((i, language) in FeedSdk.languagesList.withIndex()) {
                            if (i < FeedSdk.languagesList.size - 1) {
                                languages = languages + language.id.lowercase(Locale.getDefault()) + ","
                            } else {
                                languages += language.id.lowercase(Locale.getDefault())
                            }
                        }
                    } else {
                        languages = getLanguages(listOf("hi", "ta", "te", "bn"))
                    }
                    if(languages.isEmpty()){
                        languages = "en"
                    }
                    currentLanguage = languages
                    val platform = "android"
                    link = if(pwaLink.contains("?"))
                        "$pwaLink&platform=$platform&language=$languages&${Constants.SHOW_FEED}=false"
                    else
                        "$pwaLink?platform=$platform&language=$languages&${Constants.SHOW_FEED}=false"
                    binding.noInternet.visibility=View.GONE
//            LogDetail.LogD("webtest", "onViewCreated: "+keyId+" "+link)
                    binding.webview.loadUrl(link)
                    LogDetail.LogD("webtest", "setWebView refresh: $link")
                    Constants.pwaWebViews[pwaLink] = binding.webview
                } catch (ex:java.lang.Exception){
                    LogDetail.LogEStack(ex)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SpUtil.onRefreshListeners["cricketHome"] = this
        Card.setFontFamily(binding.noInternetTitle, true)
        Card.setFontFamily(binding.checkConnection)
//        if(BuildConfig.DEBUG && !pwaLink.contains("staging.masterfeed.io") && pwaLink.contains("masterfeed.io")){
//            pwaLink = pwaLink.replace("masterfeed.io", "staging.masterfeed.io")
//        }
        if(FeedSdk.isCricketApp()){
            ApiGetLanguages().getLanguagesEncrypted(
                Endpoints.GET_LANGUAGES_ENCRYPTED,
                object : ApiGetLanguages.LanguageResponseListener {
                    override fun onSuccess(languageResponseModel: List<Language>) {
                        mLanguageResponseModel = languageResponseModel as ArrayList<Language>
                        setUpLanguages(view)
                    }
                }
            )
            ApiUserDetails().getUserResponseEncrypted(
                Endpoints.USER_DETAILS_ENCRYPTED,
                object : ApiUserDetails.UserResponseListener {
                    override fun onSuccess(userDetails: UserResponse) {
                        mUserDetails = userDetails
                        setUpLanguages(view)
                    }
                })
        } else {
            setWebView(view)
        }
    }

    private fun setWebView(view: View){
        if(Constants.pwaWebViews.containsKey(pwaLink)){
            try{
                binding.webview.visibility = View.GONE
                binding.progress.visibility = View.GONE
                val prevWebView = Constants.pwaWebViews[pwaLink]
                if (prevWebView!!.parent != null) {
                    (prevWebView.parent as ViewGroup).removeView(prevWebView)
                }
                prevWebView.id = View.generateViewId()
                binding.mainLayout.addView(prevWebView)
                prevWeb = view.findViewById(prevWebView.id)
                alreadyExists = true
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        } else{
            binding.webview.visibility = View.VISIBLE
            alreadyExists = false
        }
        binding.webview.setListener(requireActivity(), this)
        binding.webview.setMixedContentAllowed(false)
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        binding.webview.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        binding.webview.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        binding.webview.settings.saveFormData = true
        binding.webview.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        binding.webview.settings.setGeolocationEnabled(true)
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.useWideViewPort = true
        WebView.setWebContentsDebuggingEnabled(true);
        binding.webview.setCookiesEnabled(true)
        binding.webview.setThirdPartyCookiesEnabled(true)
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(binding!!.webview, true)
            cookieManager.removeAllCookies {  }
        } else {
            cookieManager.setAcceptCookie(true)
            cookieManager.removeAllCookie()
        }
        var languages = ""
        if(keyId=="cricket") {
            for ((i, language) in FeedSdk.languagesList.withIndex()) {
                if (i < FeedSdk.languagesList.size - 1) {
                    languages = languages + language.id.lowercase(Locale.getDefault()) + ","
                } else {
                    languages += language.id.lowercase(Locale.getDefault())
                }
            }
        } else {
            languages = getLanguages(listOf("hi", "ta", "te", "bn"))
        }
        if(languages.isEmpty()){
            languages = "en"
        }
        val platform = "android"
        currentLanguage = languages
        link = if(pwaLink.contains("?"))
                        "$pwaLink&platform=$platform&language=$languages&${Constants.SHOW_FEED}=false"
                    else
                        "$pwaLink?platform=$platform&language=$languages&${Constants.SHOW_FEED}=false"
        val gson = Gson()
        cookieManager.setCookie(link, "token="+ RSAKeyGenerator.getNewJwtToken(FeedSdk.appId, FeedSdk.userId) ?: "")
        cookieManager.setCookie(link, "user_info="+ gson.toJson(Constants.userDetails))
        LogDetail.LogD("webtest", "setWebView: $link")
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                isloaded = true
                try{
                    val newIntent = Intent(requireContext(), PWACricketTabsActivity::class.java)
                    LogDetail.LogD("webtest", "onOverrideUrl: " + url)
                    if (url != link) {
                        if (!Constants.isNetworkAvailable(requireContext())) {
                            binding.webview.visibility = View.GONE
                            binding.progress.visibility = View.GONE
                            prevWeb?.visibility = View.GONE
                            binding.noInternet.visibility = View.VISIBLE
                        }
                    }
                    when {
                        url.contains("crichouse/upcoming-matches") -> {
                            return initTabsActivity(newIntent,"upcoming_matches")
                        }
                        url.contains("crichouse/match-results") -> {
                            return initTabsActivity(newIntent, "results")
                        }
                        url.contains("view=scoreboard") -> {
                            return initActivity("Scoreboard", url)
                        }
                        url.contains("crichouse/match") -> {
                            val uri = Uri.parse(url)
                            val isLiveMatch = uri.getQueryParameter("isLiveMatch")
                            if(isLiveMatch=="true") {
                                newIntent.putExtra("link", url)
                                return initTabsActivity(newIntent, "match")
                            } else{
                                return initActivity("Commentary", url)
                            }
                        }
                        url.contains("cricket/match") -> {
                            val uri = Uri.parse(url)
                            val isLiveMatch = uri.getQueryParameter("isLiveMatch")
                            if(isLiveMatch=="true") {
                                newIntent.putExtra("link", url)
                                return initTabsActivity(newIntent, "match")
                            } else{
                                val newLink = url.replace("cricket/match", "crichouse/match")
                                return initActivity("Commentary", newLink)
                            }
                        }
                        url.contains("crichouse/tour") -> {
                            return initActivity("", url)
                        }
                        url.contains("crichouse/notification") -> {
                            return updateNotification(url)
                        }
                        url.contains("crichouse/series") -> {
                            return initActivity("Points Table", url)
                        }
                        url.contains("crichouse/team-ranking") -> {
                            return initTabsActivity(newIntent, "team_ranking")
                        }
                        url.contains("crichouse/player-ranking") -> {
                            return initTabsActivity(newIntent, "player_ranking")
                        }
                        url.contains("crichouse/post-options") -> {
                            onMoreOptionsClicked(url)
                            return true
                        }
                        url.contains("crichouse/share") -> {
                            onSharePostClicked(url)
                            return true
                        }
                        url.contains("crichouse/comment") -> {
                            openFeedDetailActivity(url, true)
                            return true
                        }
                        url.contains("crichouse/view-post") -> {
                            openFeedDetailActivity(url, false)
                            return true
                        }
                        else -> {
                            binding.webview.visibility = View.GONE
                            binding.progress.visibility = View.VISIBLE
                            prevWeb?.visibility = View.GONE
                            binding.noInternet.visibility = View.GONE
                            if(url.startsWith("market://details?") || url.startsWith("https://play.google.com/store")) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            }
                            return false
                        }
                    }
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                    return true
                }
            }
        }
        if(!Constants.isNetworkAvailable(requireContext())){
            binding.webview.visibility=View.GONE
            binding.progress.visibility=View.GONE
            if(!alreadyExists){
                prevWeb?.visibility=View.GONE
                binding.noInternet.visibility=View.VISIBLE
            }
//            Constants.pwaWebViews.remove(filename)
        } else{
            binding.noInternet.visibility=View.GONE
//            LogDetail.LogD("webtest", "onViewCreated: "+keyId+" "+link)
            binding.webview.loadUrl(link)
            Constants.pwaWebViews[pwaLink] = binding.webview
        }
    }

    private fun initTabsActivity(intent:Intent, interest:String) : Boolean{
        if(alreadyLoaded) return true
        alreadyLoaded = true
        if(currentLanguage.isNotEmpty()){
            intent.putExtra(Constants.LANGUAGE, currentLanguage)
        }
        intent.putExtra(Constants.INTEREST, interest)
        startActivity(intent)
        return true
    }

    private fun initActivity(interest:String, url: String) : Boolean{
        if(alreadyLoaded) return true
        alreadyLoaded = true
        val intent = Intent(requireContext(), PWACricketActivity::class.java)
        val uri = Uri.parse(url)
        val title = uri.getQueryParameter("title")
        if(title.isNullOrEmpty()){
            intent.putExtra(Constants.INTEREST, interest)
        } else {
            intent.putExtra(Constants.INTEREST, title)
        }
        intent.putExtra("link", url)
        startActivity(intent)
        return true
    }

    private fun updateNotification(url_string: String) : Boolean{
        try {
            val uri = Uri.parse(url_string)
            val turnNotification = uri.getQueryParameter("turnNotification")=="true"
            Constants.isChecked = turnNotification
            ApiCreateOrUpdateUser().updateCricketNotificationEncrypt(
                Endpoints.UPDATE_USER_ENCRYPTED,
                turnNotification, true
            )
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
        }
        return true
    }

    private fun onMoreOptionsClicked(url_string: String){
        try{
            val uri = Uri.parse(url_string)
            val reportBottomSheet = FeedMenuBottomSheetFragment.newInstance(
                uri.getQueryParameter("publisher_contact_us") ?: "",
                uri.getQueryParameter("publisher_id") ?: "",
                uri.getQueryParameter("post_id")?:""
            )
            if (context is FragmentActivity) {
                reportBottomSheet.show(
                    (requireContext() as FragmentActivity).supportFragmentManager,
                    "reportBottomSheet"
                )
            } else if (((context as ContextWrapper).baseContext is FragmentActivity)) {
                reportBottomSheet.show(
                    ((context as ContextWrapper).baseContext as FragmentActivity).supportFragmentManager,
                    "reportBottomSheet"
                )
            }
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
        }
    }

    private fun onSharePostClicked(url_string: String){
        try {
            val uri = Uri.parse(url_string)
            val title = uri.getQueryParameter("title")
            val postId = uri.getQueryParameter("post_id")
            val imageUrl = uri.getQueryParameter("imageURL")
            val isWhatsApp = uri.getQueryParameter("isWhatsapp")?:"false"
            val postUrl = uri.getQueryParameter("postURL")
            val link: String = FeedSdk.mFirebaseDynamicLink + "?feed_id=" + postId!!
            var prefix = ""
            if(!title.isNullOrEmpty()){
                prefix = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()+"\n"
            }
            FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(link))
                .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                .setAndroidParameters(
                    DynamicLink.AndroidParameters.Builder(requireContext().packageName).build()
                )
                .setSocialMetaTagParameters(
                    DynamicLink.SocialMetaTagParameters.Builder()
                        .setTitle(FeedSdk.appName ?: "Feed")
                        .setDescription(title ?: "")
                        .setImageUrl(Uri.parse(imageUrl ?: ""))
                        .build()
                )
                .buildShortDynamicLink()
                .addOnSuccessListener { shortDynamicLink ->
                    val shortLink: String =
                        prefix+shortDynamicLink.shortLink.toString() + requireContext().getString(R.string.dynamic_link_url_suffix) +
                                " " + FeedSdk.appName + " " + requireContext().getString(R.string.dynamic_link_url_suffix2) + requireContext().packageName
                    if (isWhatsApp=="true") {
                        val whatsAppIntent = Intent(Intent.ACTION_SEND)
                        whatsAppIntent.type = "text/plain"
                        whatsAppIntent.setPackage("com.whatsapp")
                        whatsAppIntent.putExtra(Intent.EXTRA_TEXT, shortLink)
                        try {
                            startActivity(whatsAppIntent)
                        } catch (ex: ActivityNotFoundException) {
                            Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, shortLink)
                        startActivity(Intent.createChooser(intent, "Share via"))
                    }
                }
                .addOnFailureListener { e ->
                    LogDetail.LogEStack(e)
                    try {
                        if (isWhatsApp=="true") {
                            val whatsAppIntent = Intent(Intent.ACTION_SEND)
                            whatsAppIntent.type = "text/plain"
                            whatsAppIntent.setPackage("com.whatsapp")
                            whatsAppIntent.putExtra(Intent.EXTRA_TEXT,
                                postUrl + "\n\n" + Objects.requireNonNull(SpUtil.spUtilInstance)!!
                                    .getString(Constants.SHARE_MESSAGE) + requireContext().getString(R.string.share_link_prefix) + requireContext().packageName
                            )
                            try {
                                startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(context, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, postUrl + "\n\n" + Objects.requireNonNull(SpUtil.spUtilInstance)!!
                                    .getString(Constants.SHARE_MESSAGE) + requireContext().getString(R.string.share_link_prefix) + requireContext().packageName
                            )
                            startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    } catch (e1: Exception) {
                        Toast.makeText(context, requireContext().getString(R.string.error_share_post), Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            LogDetail.LogEStack(e)
        }
    }

    override fun onResume() {
        super.onResume()
        alreadyLoaded = false
    }
    private fun openFeedDetailActivity(url_string: String, openComment: Boolean){
        try{
            if(alreadyLoaded) return
            alreadyLoaded = true
            val uri = Uri.parse(url_string)
            val postId = uri.getQueryParameter("post_id")?:""
            val postSource = uri.getQueryParameter("post_source")?:""
            val feedType = uri.getQueryParameter("feed_type")?:""
            val isNative = uri.getQueryParameter("is_native")?:"false"

            val intent = if(isNative=="true"){
                val bundle = Bundle()
                bundle.putString("NativePageOpen","Feed")
                FirebaseAnalytics.getInstance(requireContext()).logEvent("NativePage",bundle)
                Intent(context, PostNativeDetailActivity::class.java)
            } else{
                Intent(context, NewsFeedPageActivity::class.java)
            }
            intent.putExtra(Constants.INTEREST, "cricket") // send interest
            intent.putExtra(Constants.FROM_APP, true)
            intent.putExtra(Constants.POST_SOURCE, postSource)
            intent.putExtra(Constants.FEED_TYPE, feedType)
            intent.putExtra(Constants.POST_ID, postId)
            startActivity(intent)
        } catch (ex:java.lang.Exception){
            LogDetail.LogEStack(ex)
            alreadyLoaded = false
        }
    }

    companion object {
        fun newInstance(link: String, keyId: String): CricketHomePWAFragment {
            val cricketPWAFragment = CricketHomePWAFragment()
            cricketPWAFragment.pwaLink = link
            cricketPWAFragment.keyId = keyId
            return cricketPWAFragment
        }
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        if(url!="about:blank" && !alreadyExists) {
            binding.webview.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
        }
        isloaded = true
    }

    override fun onPageFinished(url: String?) {
        if(isloaded){
            isloaded = false
            val cookies = CookieManager.getInstance().getCookie(url)
            LogDetail.LogD("webtest", keyId)
            LogDetail.LogD("webtest", "All the cookies in a string:$cookies")
            if(!alreadyExists) {
                binding.progress.visibility = View.GONE
            } else{
                Handler(Looper.getMainLooper()).postDelayed({
                    prevWeb?.visibility = View.GONE
                    binding.webview.visibility = View.VISIBLE
                }, 500)
            }
        }
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {
        binding.progress.visibility = View.GONE
    }

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {

    }

    override fun onExternalPageRequest(url: String?) {
        LogDetail.LogD("tag", "onExternalPageRequest: "+url)
    }

    private fun setUpLanguages(view: View) {
        if (mUserDetails != null && mLanguageResponseModel != null) {
            var selectedLanguagesList = ArrayList<Language>()
            for (language in mLanguageResponseModel!!) {
                languagesMap[language.id] = language
                Constants.allLanguagesMap[language.id.lowercase(Locale.getDefault())] = language
            }
            Constants.selectedLanguagesMap = HashMap()
            if (mUserDetails?.user?.languages.isNullOrEmpty()) {
//                selectedLanguagesList = mLanguageResponseModel!!
            } else {
                for (language in languagesMap.values) {
                    if (mUserDetails?.user?.languages!!.contains(language.id.lowercase(Locale.getDefault()))) {
                        selectedLanguagesList.add(language)
                        Constants.selectedLanguagesMap[language.id.lowercase(Locale.getDefault())] = language
                    }
                }
            }
            FeedSdk.languagesList = selectedLanguagesList
            setWebView(view)
        }
    }
}