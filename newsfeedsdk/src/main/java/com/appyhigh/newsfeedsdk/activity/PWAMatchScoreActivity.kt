package com.appyhigh.newsfeedsdk.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.getLanguages
import com.appyhigh.newsfeedsdk.Constants.isNetworkAvailable
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.databinding.ActivityPwaMatchScoreBinding
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.RSAKeyGenerator
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.gson.Gson
import im.delight.android.webview.AdvancedWebView


class PWAMatchScoreActivity : AppCompatActivity(), AdvancedWebView.Listener {
    var binding: ActivityPwaMatchScoreBinding? = null
    var link=""
    var alreadyExists = false
    var prevWeb: AdvancedWebView?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPwaMatchScoreBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        Card.setFontFamily(binding?.title, true)
        Card.setFontFamily(binding?.noInternetTitle, true)
        Card.setFontFamily(binding?.checkConnection)
        SpUtil.spUtilInstance?.init(this)
        val filename = intent.getStringExtra("filename")
        if(Constants.pwaWebViews.containsKey(filename)){
            try{
                binding?.webview?.visibility = View.GONE
                binding?.progress?.visibility = View.GONE
                val prevWebView = Constants.pwaWebViews[filename]
                if (prevWebView!!.parent != null) {
                    (prevWebView.parent as ViewGroup).removeView(prevWebView)
                }
                prevWebView?.id = View.generateViewId()
                binding?.mainLayout?.addView(prevWebView)
                prevWeb = findViewById(prevWebView!!.id)
                alreadyExists = true
            } catch (ex:Exception){
                ex.printStackTrace()
            }
        } else{
            binding?.webview?.visibility = View.VISIBLE
            alreadyExists = false
        }
        binding?.webview?.setListener(this, this)
        binding?.webview?.setMixedContentAllowed(false)
        binding?.webview?.settings?.javaScriptEnabled = true;
        binding?.webview?.settings?.domStorageEnabled = true;
        binding?.webview?.setCookiesEnabled(true)
        binding?.webview?.setThirdPartyCookiesEnabled(true)
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(binding!!.webview, true)
            cookieManager.removeAllCookies {  }
        } else {
            cookieManager.setAcceptCookie(true)
            cookieManager.removeAllCookie()
        }
        val language = getLanguages(listOf("hi", "ta", "te", "bn"))
        val platform = "android"
        val postSource = intent.getStringExtra("post_source")?:"cricket_fever"
        link = (intent.getStringExtra("link")?: "https://masterfeed.io/crichouse/match/$filename")+"?filename=$filename&language=$language&post_source=$postSource&platform=$platform&theme=${FeedSdk.sdkTheme}"
        cookieManager.setCookie(link, "token="+ RSAKeyGenerator.getJwtToken(FeedSdk.appId, FeedSdk.userId) ?: "")
        cookieManager.setCookie(link, "user_info="+ Gson().toJson(Constants.userDetails))
        Log.d("webtest", "link: $link")
        binding?.webview?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if(url!=link) {
                    if (!isNetworkAvailable(this@PWAMatchScoreActivity)) {
                        binding?.webview?.visibility = View.GONE
                        binding?.progress?.visibility = View.GONE
                        prevWeb?.visibility = View.GONE
                        binding?.noInternet?.visibility = View.VISIBLE
                    } else {
                        binding?.webview?.visibility = View.GONE
                        binding?.progress?.visibility = View.VISIBLE
                        prevWeb?.visibility = View.GONE
                        binding?.noInternet?.visibility = View.GONE
                    }
                }
                if(url.startsWith("market://details?") || url.startsWith("https://play.google.com/store")){
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
                return false
            }
        }
        if(!isNetworkAvailable(this)){
            binding?.webview?.visibility=View.GONE
            binding?.progress?.visibility=View.GONE
            if(!alreadyExists){
                prevWeb?.visibility=View.GONE
                binding?.noInternet?.visibility=View.VISIBLE
            }
            Constants.pwaWebViews.remove(filename)
        } else{
            binding?.noInternet?.visibility=View.GONE
            binding?.webview?.loadUrl(link)
            Constants.pwaWebViews[filename!!] = binding!!.webview
        }
        binding?.backBtn?.setOnClickListener {
            onBackPressed()
        }
        binding?.refresh?.setOnClickListener {
            if(!isNetworkAvailable(this)){
                binding?.webview?.visibility=View.GONE
                binding?.progress?.visibility=View.GONE
                prevWeb?.visibility=View.GONE
                binding?.noInternet?.visibility=View.VISIBLE
            } else{
                binding?.noInternet?.visibility=View.GONE
                binding?.webview?.loadUrl(link)
            }
        }
        binding!!.share.setOnClickListener {
            binding!!.share.isEnabled = false
            sharePost(this, intent.getStringExtra("filename"), intent.getStringExtra("matchType"), "Share Match Details ", "", false, "")
            Handler(Looper.getMainLooper()).postDelayed({
                binding!!.share.isEnabled = true
            }, 1000)
        }
        binding!!.whatsappShare.setOnClickListener {
            binding!!.whatsappShare.isEnabled = false
            sharePost(this, intent.getStringExtra("filename"), intent.getStringExtra("matchType"), "Share Match Details ", "", true, "")
            Handler(Looper.getMainLooper()).postDelayed({
                binding!!.whatsappShare.isEnabled = true
            }, 1000)
        }
    }

    fun sharePost(context: Context, filename: String?, matchType: String?, title: String?, imageUrl: String?, isWhatsApp: Boolean, postUrl: String) {
        try{
            val link = String.format(FeedSdk.mFirebaseDynamicLink + "?" + context.getString(R.string.dynamic_link_match_details), filename, matchType, intent.getStringExtra("link")!!)
            FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(link))
                .setDomainUriPrefix(FeedSdk.mFirebaseDynamicLink)
                .setAndroidParameters(DynamicLink.AndroidParameters.Builder(context.packageName).build())
                .setSocialMetaTagParameters(DynamicLink.SocialMetaTagParameters.Builder().setTitle(FeedSdk.appName!!).setDescription(title!!).setImageUrl(Uri.parse(imageUrl)).build())
                .buildShortDynamicLink()
                .addOnSuccessListener { shortDynamicLink: ShortDynamicLink ->
                    val shortLink: String = "${shortDynamicLink.shortLink}  ${context.getString(R.string.dynamic_link_url_suffix)} ${FeedSdk.appName!!} ${context.getString(
                        R.string.dynamic_link_url_suffix2)}$packageName"
                    if (isWhatsApp) {
                        val whatsAppIntent = Intent(Intent.ACTION_SEND)
                        whatsAppIntent.type = "text/plain"
                        whatsAppIntent.setPackage("com.whatsapp")
                        whatsAppIntent.putExtra(Intent.EXTRA_TEXT, shortLink)
                        try {
                            context.startActivity(whatsAppIntent)
                        } catch (ex: ActivityNotFoundException) {
                            Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, shortLink)
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    }
                }
                .addOnFailureListener { e: Exception ->
                    e.printStackTrace()
                    try {
                        if (isWhatsApp) {
                            val whatsAppIntent = Intent(Intent.ACTION_SEND)
                            whatsAppIntent.type = "text/plain"
                            whatsAppIntent.setPackage("com.whatsapp")
                            whatsAppIntent.putExtra(Intent.EXTRA_TEXT, """$link${
                                SpUtil.spUtilInstance?.getString(
                                    Constants.SHARE_MESSAGE)}${context.getString(R.string.share_link_prefix)}${context.packageName}""".trimIndent())
                            try {
                                context.startActivity(whatsAppIntent)
                            } catch (ex: ActivityNotFoundException) {
                                Toast.makeText(context, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, """$link${
                                SpUtil.spUtilInstance?.getString(
                                    Constants.SHARE_MESSAGE)}${context.getString(R.string.share_link_prefix)}${context.packageName}""".trimIndent())
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    } catch (e1: Exception) {
                        Toast.makeText(context, context.getString(R.string.error_share_post), Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }


    override fun onBackPressed() {
        if (isTaskRoot) {
            try{
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                startActivity(Intent(this, activity).putExtra("fromSticky", "true"))
                finish()
            } catch (ex:Exception){
                ex.printStackTrace()
            }
        } else{
            super.onBackPressed()
        }
    }

    override fun onPageStarted(url: String, favicon: Bitmap?) {
        if(url!="about:blank" && !alreadyExists) {
            binding?.webview?.visibility = View.VISIBLE
            binding?.progress?.visibility = View.VISIBLE
        }
    }

    override fun onPageFinished(url: String) {
        if(!alreadyExists) {
            binding?.progress?.visibility = View.GONE
        } else{
            Handler(Looper.getMainLooper()).postDelayed({
                prevWeb?.visibility = View.GONE
                binding?.webview?.visibility = View.VISIBLE
            }, 500)
        }
    }

    override fun onPageError(errorCode: Int, description: String, failingUrl: String) {
        binding?.progress?.visibility = View.GONE
    }

    override fun onDownloadRequested(
        url: String,
        suggestedFilename: String,
        mimeType: String,
        contentLength: Long,
        contentDisposition: String,
        userAgent: String
    ) {
        try {
            if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
                // download successfully handled
                Toast.makeText(applicationContext, "downloaded successfully ", Toast.LENGTH_LONG).show()
            } else {
                // download couldn't be handled because user has disabled download manager app on the device
                Toast.makeText(
                    applicationContext,
                    "download couldn't be handled because user has disabled download manager app on the device",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    override fun onExternalPageRequest(url: String) {}

}