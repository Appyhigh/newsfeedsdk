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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.NewsFeedPageActivity
import com.appyhigh.newsfeedsdk.activity.PostNativeDetailActivity
import com.appyhigh.newsfeedsdk.databinding.FragmentCricketPwaBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.RSAKeyGenerator
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.gson.Gson
import im.delight.android.webview.AdvancedWebView
import java.util.*

class PWAFragment: Fragment(), AdvancedWebView.Listener {

    private var pwaLink = ""
    var binding: FragmentCricketPwaBinding?=null
    var keyId = ""
    private var link = ""
    var alreadyExists = false
    var prevWeb: WebView?=null
    private var isloaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCricketPwaBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Card.setFontFamily(binding!!.noInternetTitle, true)
        Card.setFontFamily(binding!!.checkConnection)
//        if(BuildConfig.DEBUG)
//            pwaLink = pwaLink.replace("masterfeed.io", "staging.masterfeed.apyhi.com")
        setWebView(view)
    }

    private fun setWebView(view: View){
        if(Constants.pwaWebViews.containsKey(pwaLink)){
            try{
                binding!!.webview.visibility = View.GONE
                binding!!.progress.visibility = View.GONE
                val prevWebView = Constants.pwaWebViews[pwaLink]
                if (prevWebView!!.parent != null) {
                    (prevWebView.parent as ViewGroup).removeView(prevWebView)
                }
                prevWebView.id = View.generateViewId()
                binding!!.mainLayout.addView(prevWebView)
                prevWeb = view.findViewById(prevWebView.id)
                alreadyExists = true
            } catch (ex:Exception){
                LogDetail.LogEStack(ex)
            }
        } else{
            binding!!.webview.visibility = View.VISIBLE
            alreadyExists = false
        }
        binding!!.webview.setListener(requireActivity(), this)
        binding!!.webview.setMixedContentAllowed(false)
        binding!!.webview.settings.javaScriptEnabled = true
        binding!!.webview.settings.domStorageEnabled = true
        binding!!.webview.settings.useWideViewPort = true
        binding!!.webview.setCookiesEnabled(true)
        binding!!.webview.setThirdPartyCookiesEnabled(true)
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(binding!!.webview, true)
//            cookieManager.removeAllCookies {  }
        } else {
            cookieManager.setAcceptCookie(true)
//            cookieManager.removeAllCookie()
        }
        var languages = ""
        for ((i, language) in FeedSdk.languagesList.withIndex()) {
            if (i < FeedSdk.languagesList.size - 1) {
                languages = languages + language.id.lowercase(Locale.getDefault()) + ","
            } else {
                languages += language.id.lowercase(Locale.getDefault())
            }
        }
        if(languages.isEmpty()){
            languages = "en"
        }
        var pwaUri = Uri.parse(pwaLink)
        pwaUri = pwaUri.addUriParameter("platform","android")
        pwaUri = pwaUri.addUriParameter("language",languages)
        pwaUri = pwaUri.addUriParameter("theme", FeedSdk.sdkTheme)
        link = pwaUri.toString()
        val gson = Gson()
        cookieManager.setCookie(link, "token="+ RSAKeyGenerator.getJwtToken(FeedSdk.appId, FeedSdk.userId) ?: "")
        cookieManager.setCookie(link, "user_info="+ gson.toJson(Constants.userDetails))
        binding!!.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                isloaded = true
                try{
                    LogDetail.LogD("webtest", "onOverrideUrl: " + url)
                    if (url != link) {
                        if (!Constants.isNetworkAvailable(requireContext())) {
                            binding!!.webview.visibility = View.GONE
                            binding!!.progress.visibility = View.GONE
                            prevWeb?.visibility = View.GONE
                            binding!!.noInternet.visibility = View.VISIBLE
                        }
                    }

                    when {
                        url.contains("/post-options") -> {
                            onMoreOptionsClicked(url)
                            return true
                        }
                        url.contains("/share") -> {
                            onSharePostClicked(url)
                            return true
                        }
                        url.contains("/comment") -> {
                            openFeedDetailActivity(url, true)
                            return true
                        }
                        url.contains("/view-post") -> {
                            openFeedDetailActivity(url, false)
                            return true
                        }
                        else -> {
                            binding!!.webview.visibility = View.GONE
                            binding!!.progress.visibility = View.VISIBLE
                            prevWeb?.visibility = View.GONE
                            binding!!.noInternet.visibility = View.GONE
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
            binding!!.webview.visibility=View.GONE
            binding!!.progress.visibility=View.GONE
            if(!alreadyExists){
                prevWeb?.visibility=View.GONE
                binding!!.noInternet.visibility=View.VISIBLE
            }
//            Constants.pwaWebViews.remove(filename)
        }
    }

    fun onBackPressed(){
        try{
            if (binding!!.webview.canGoBack()) {
                binding!!.webview.goBack()
            } else{
                requireActivity().onBackPressed()
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
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
                            whatsAppIntent.putExtra(
                                Intent.EXTRA_TEXT,
                                postUrl + "\n\n" + Objects.requireNonNull(SpUtil.spUtilInstance)!!
                                    .getString(Constants.SHARE_MESSAGE) + requireContext().getString(
                                    R.string.share_link_prefix) + requireContext().packageName
                            )
                            try {
                                startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(context, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(
                                Intent.EXTRA_TEXT, postUrl + "\n\n" + Objects.requireNonNull(SpUtil.spUtilInstance)!!
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

    private fun openFeedDetailActivity(url_string: String, openComment: Boolean){
        try{
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
        }
    }

    companion object {
        fun newInstance(link: String, keyId: String): CricketPWAFragment {
            val cricketPWAFragment = CricketPWAFragment()
            cricketPWAFragment.pwaLink = link
            cricketPWAFragment.keyId = keyId
            return cricketPWAFragment
        }
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        if(url!="about:blank" && !alreadyExists) {
            binding!!.webview.visibility = View.VISIBLE
            binding!!.progress.visibility = View.VISIBLE
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
                binding!!.progress.visibility = View.GONE
            } else{
                Handler(Looper.getMainLooper()).postDelayed({
                    prevWeb?.visibility = View.GONE
                    binding!!.webview.visibility = View.VISIBLE
                }, 500)
            }
        }
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {
        binding!!.progress.visibility = View.GONE
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

    fun Uri.addUriParameter(key: String, newValue: String): Uri =
        with(buildUpon()) {
            clearQuery()
            queryParameterNames.forEach {
                if (it != key) appendQueryParameter(it, getQueryParameter(it))
            }
            appendQueryParameter(key, newValue)
            build()
        }
}