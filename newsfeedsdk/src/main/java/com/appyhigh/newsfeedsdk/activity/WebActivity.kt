package com.appyhigh.newsfeedsdk.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.postDelayed
import androidx.core.view.setPadding
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.WEB_HISTORY
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.WebPlatformListener
import com.appyhigh.newsfeedsdk.adapter.WebPlatformsGridAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiSearchSticky
import com.appyhigh.newsfeedsdk.databinding.ActivitySearchStickyWebBinding
import com.appyhigh.newsfeedsdk.model.SearchStickyWidgetModel
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.AdUtilsSDK
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.appyhigh.newsfeedsdk.utils.TrendingSearchItem
import com.appyhigh.newsfeedsdk.utils.TrendingSearchesApi
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import im.delight.android.webview.AdvancedWebView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.*
import kotlin.collections.ArrayList


class WebActivity : AppCompatActivity(), AdvancedWebView.Listener {
    var binding: ActivitySearchStickyWebBinding? = null
    var isSourceGoogle = true
    var backStack = 0
    private lateinit var trending_list: List<TrendingSearchItem>
    var historyList = ArrayList<WebHistoryModel>()
    val TAG = "WebActivity"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchStickyWebBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        AdUtilsSDK().requestFeedAd(binding!!.searchNativeAd, R.layout.native_ad_feed_small, FeedSdk.mAdsModel?.search_page_native?:"", "searchSticky")
        setFonts(binding!!.root)
        SpUtil.spUtilInstance?.init(this)
        binding?.webview?.setListener(this, this)
        binding?.webview?.setMixedContentAllowed(false)
        binding?.webview?.settings?.javaScriptEnabled = true;
        binding?.webview?.settings?.domStorageEnabled = true;
        binding?.webview?.setCookiesEnabled(true)
        binding?.webview?.setThirdPartyCookiesEnabled(true)
        binding?.webview?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if(url.startsWith("market://details?") || url.startsWith("https://play.google.com/store")){
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                } else if( binding?.appTitle?.text.toString().contains("Instagram") && url.contains("about:blank")){
                    Handler(Looper.getMainLooper()).postDelayed({binding?.webview?.loadUrl("https://www.instagram.com")}, 3000)
                    return true
                }
                return false
            }
        }

        val title = if (intent.hasExtra("title")) intent.getStringExtra("title") else "AsViral.com"
        binding?.appTitle?.text = title
        if (intent.hasExtra("showSearch") && intent.getBooleanExtra("showSearch", false)) {
            logFirebaseEvent("NotificationClick", "clickedOn", "Search")
            binding?.webview?.visibility = View.GONE
            binding?.appBar?.visibility = View.VISIBLE
            binding?.coLayout?.visibility = View.VISIBLE
            binding?.adContainer?.visibility = View.VISIBLE
            if(FeedSdk.showAds){
                val adView = AdView(this)
                adView.adSize = AdSize.BANNER
                adView.adUnitId = FeedSdk.mAdsModel?.search_footer_banner_intermediate?:""
                binding?.adContainer?.addView(adView)
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            }
            setSocialData()
            setSearchBar()
        } else {
            binding?.appBar?.visibility = View.GONE
            binding?.coLayout?.visibility = View.GONE
            binding?.adContainer?.visibility = View.GONE
            binding?.webview?.visibility = View.VISIBLE
            val URL =
                if (intent.hasExtra("link")) intent.getStringExtra("link") else "https://asviral.com/"
            binding?.webview?.loadUrl(URL.toString())
        }
        isSourceGoogle = SpUtil.spUtilInstance!!.getBoolean("isSourceGoogle", true)
        if (!isSourceGoogle) {
            binding?.sourceLogo?.setImageResource(R.drawable.ic_bing_logo)
            binding?.textLogo?.setImageResource(R.drawable.ic_bing)
            binding?.textLogo?.scaleType = ImageView.ScaleType.CENTER_CROP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding?.searchbox?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#f26522"))
            }
        } else {
            binding?.sourceLogo?.setImageResource(R.drawable.ic_google_logo)
            binding?.textLogo?.setImageResource(R.drawable.ic_google)
            binding?.textLogo?.scaleType = ImageView.ScaleType.FIT_CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding?.searchbox?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4285f4"))
            }
        }
        val spinner = binding!!.spinner
        val arrayAdapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, arrayOf("Google", "Bing"))
        spinner.adapter = arrayAdapter
        spinner.setSelection(if(isSourceGoogle) 0 else 1)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                try{
                    when (position){
                        1 -> {
                            binding?.sourceLogo?.setImageResource(R.drawable.ic_bing_logo)
                            binding?.textLogo?.setImageResource(R.drawable.ic_bing)
                            binding?.textLogo?.scaleType = ImageView.ScaleType.CENTER_CROP
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                binding?.searchbox?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#f26522"))
                            }
                            logFirebaseEvent("SearchpageClick", "ClickedOn", "Bing")
                            isSourceGoogle = false
                        }
                        0 -> {
                            binding?.sourceLogo?.setImageResource(R.drawable.ic_google_logo)
                            binding?.textLogo?.setImageResource(R.drawable.ic_google)
                            binding?.textLogo?.scaleType = ImageView.ScaleType.FIT_CENTER
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                binding?.searchbox?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4285f4"))
                            }
                            logFirebaseEvent("SearchpageClick", "ClickedOn", "Google")
                            isSourceGoogle = true
                        }
                    }
                    SpUtil.spUtilInstance!!.putBoolean("isSourceGoogle", isSourceGoogle)
                } catch (ex:Exception){
                    ex.printStackTrace()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding!!.sourceDropdown.setOnClickListener { spinner.performClick() }
        binding?.changeSource?.setOnClickListener {
            spinner.performClick()
        }
        binding?.homeButton?.setOnClickListener { onBackPressed() }
        binding?.changeSource?.visibility = View.VISIBLE
    }

    private fun CheckAndloadUrl(url: String, title: String){
        logFirebase(title, "socialApps")
        when (title) {
            "Facebook" -> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.facebook.katana")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "Youtube" -> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "Gmail" -> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "Twitter"-> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.twitter.android")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "Instagram"-> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.instagram.android")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "LinkedIn"-> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.linkedin.android")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "Snapchat"-> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.snapchat.android")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }
            "Reddit" -> {
                try{
                    val intent = packageManager.getLaunchIntentForPackage("com.reddit.frontpage")
                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (ex:Exception){
                    loadUrl(url, title)
                }
            }

        }
    }

    private fun loadUrl(url: String, title: String){
        binding?.coLayout?.visibility = View.GONE
        binding?.webview?.visibility = View.VISIBLE
        binding?.appTitle?.text = title
        binding?.webview!!.loadUrl(url)
        backStack += 1
    }

    override fun onBackPressed() {
        try{
            if (isTaskRoot) {
                val activity = Class.forName(FeedSdk.feedTargetActivity) as Class<out Activity?>?
                startActivity(Intent(this, activity).putExtra("fromSticky", "true"))
                finish()
            } else if(backStack>0) {
                showHistoryItems()
                binding?.search?.setText("")
                binding?.webview?.visibility = View.GONE
                binding?.appBar?.visibility = View.VISIBLE
                binding?.adContainer?.visibility = View.VISIBLE
                binding?.coLayout?.visibility = View.VISIBLE
                binding?.webview?.loadUrl("about:blank")
                val title = if (intent.hasExtra("title")) intent.getStringExtra("title") else "AsViral.com"
                binding?.appTitle?.text = title
                backStack-=1
            } else{
                super.onBackPressed()
            }
        } catch (ex:Exception){
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.searchFeed?.onDestroy()
        val historyString = Gson().toJson(if(historyList.size>5) historyList.subList(0,5) else historyList)
        SpUtil.spUtilInstance!!.putString(WEB_HISTORY, historyString)
    }
    override fun onPageStarted(url: String, favicon: Bitmap?) {
        if(url!="about:blank") {
            binding?.progress?.visibility = View.VISIBLE
        }
    }

    override fun onPageFinished(url: String) {
        binding?.progress?.visibility = View.GONE
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


    override fun onResume() {
        super.onResume()
        binding?.searchFeed?.onResume()
    }

    @SuppressLint("CheckResult")
    fun setSearchBar() {
        try {
            binding?.searchbox?.setOnClickListener {
                try {
                    val searchData = binding?.search!!.text.toString()
                    var searchEngine = "google"
                    if (!isSourceGoogle) {
                        binding?.webview!!.loadUrl("https://www.bing.com/search?q=$searchData")
                        searchEngine = "bing"
                    } else {
                        binding?.webview!!.loadUrl("https://www.google.com/search?q=$searchData")
                    }
                    backStack+=1
                    binding?.coLayout?.visibility = View.GONE
                    binding?.webview?.visibility = View.VISIBLE
                    historyList.add(0, WebHistoryModel(searchEngine, searchData))
                    hideKeyboard()
                    logFirebaseEvent("SearchpageClick", "ClickedOn", "Searchicon")
                    logFirebase(searchData, searchEngine)
                    ApiSearchSticky().userActionSearch(searchData)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            binding?.root?.viewTreeObserver?.addOnGlobalLayoutListener {
                try {
                    val r = Rect()
                    binding?.root?.getWindowVisibleDisplayFrame(r)
                    val screenHeight: Int = binding?.root?.rootView!!.getHeight()
                    val keypadHeight: Int = screenHeight - r.bottom
                    if (keypadHeight > screenHeight * 0.15) {
                        trendingVis()
                        binding?.socialLayout?.visibility = View.GONE
                    } else {
                        trendingHide()
                        binding?.socialLayout?.visibility = View.VISIBLE
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            binding?.search!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    binding?.searchbox?.performClick()
                    return@OnEditorActionListener true
                }
                false
            })
            showTrendingSearches()
            showHistoryItems()
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    private fun setSocialData(){
        val spUtil = SpUtil.spUtilInstance
        if (!spUtil!!.contains(Constants.WEB_PLATFORMS)){
            spUtil.putString(Constants.WEB_PLATFORMS,"{ \"icons\": [ \"Facebook\", \"Youtube\", \"Gmail\", \"Twitter\", \"Instagram\", \"LinkedIn\", \"Snapchat\", \"Reddit\" ] }")
        }
        ApiSearchSticky().getIconsSearch(object : ApiSearchSticky.StickyWidgetResponseListener{
            override fun onSuccess(stickyWidgetModelString: String) {
                try{
                    val gson = Gson()
                    val webPlatforms: SearchStickyWidgetModel = gson.fromJson(SpUtil.spUtilInstance!!.getString(Constants.WEB_PLATFORMS), SearchStickyWidgetModel::class.java)
                    val webPlatformsGridAdapter = WebPlatformsGridAdapter(this@WebActivity, webPlatforms, object: WebPlatformListener{
                        override fun onWebPlatformClicked(webPlatform: String) {
                            logFirebaseEvent("SearchpageClick", "ClickedOn", webPlatform)
                            val url = "https://www."+ webPlatform.replace(" ","").lowercase(Locale.getDefault())+".com"
                            CheckAndloadUrl(url, webPlatform)
                        }
                    })
                    binding?.gridOptions?.adapter = webPlatformsGridAdapter
                    binding?.gridOptions?.isExpanded = true
                } catch (ex:Exception){
                    ex.printStackTrace()
                }
            }

        })
    }

    private fun trendingVis() {
        binding?.trendingCl?.visibility = View.VISIBLE
    }

    private fun trendingHide() {
        binding?.trendingCl?.visibility = View.GONE
    }

    private fun showHistoryItems() {
        try {
            binding!!.historyLl.removeAllViews()
        } catch (e: Exception) {

        }
        try{
            val type = object : TypeToken<ArrayList<WebHistoryModel?>?>() {}.type
            historyList = Gson().fromJson(SpUtil.spUtilInstance!!.getString(WEB_HISTORY), type)
            if (historyList.size > 0) {
                binding!!.recentSearchCl.visibility = View.VISIBLE
                for(historyModel in historyList){
                    try {
                        val title = historyModel.query
                        if (title.replace(" ", "") != "") {
                            val tv = TextView(this@WebActivity)
                            tv.ellipsize = TextUtils.TruncateAt.END
                            tv.maxLines = 1
                            tv.text = title
                            tv.setPadding(5, 5, 15, 15)
                            tv.textSize = 14f
                            tv.typeface = FeedSdk.font
                            tv.setTextColor(Color.parseColor("#9DA7C8"))
                            tv.setOnClickListener {
                                binding?.adContainer?.visibility = View.GONE
                                binding?.coLayout?.visibility = View.GONE
                                binding?.webview?.visibility = View.VISIBLE
                                logFirebaseEvent("SearchpageClick", "ClickedOn", "RecentSearchQueries")
                                hideKeyboard()
                                loadUrl("https://www."+historyModel.type+".com/search?q="+historyModel.query, historyModel.query)
                                logFirebase(historyModel.query, "google")
                                ApiSearchSticky().userActionSearch(historyModel.query)
                            }
                            val suggestLL = LinearLayout(this@WebActivity)
                            suggestLL.orientation = LinearLayout.HORIZONTAL
                            val imageView = ImageView(this@WebActivity)
                            imageView.setPadding(15)
                            tv.setPadding(15)
                            imageView.setImageDrawable(
                                ContextCompat.getDrawable(
                                    this@WebActivity,
                                    R.drawable.ic_web_history_icon
                                )
                            )
                            suggestLL.gravity = Gravity.CENTER_VERTICAL
                            suggestLL.addView(imageView)
                            suggestLL.addView(tv)
                            binding!!.historyLl.addView(suggestLL)
                            binding!!.historyPbar.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        binding!!.recentSearchCl.visibility = View.GONE
                        Log.d(TAG, "openSearchEngine: " + e.localizedMessage)
                    }
                }
            } else {
                binding!!.recentSearchCl.visibility = View.GONE
            }
        } catch (ex:Exception){
            binding!!.recentSearchCl.visibility = View.GONE
            ex.printStackTrace()
        }
    }

    private fun showTrendingSearches() {
        try {
            binding!!.trendingLl.removeAllViews()
        } catch (e: Exception) {

        }
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://trends.google.com/trends/trendingsearches/daily/")
            .client(client)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(
                SimpleXmlConverterFactory.createNonStrict(
                    Persister(AnnotationStrategy())
                )
            )
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
        val trendingApi: TrendingSearchesApi = retrofit.create(TrendingSearchesApi::class.java)

        trendingApi.getTrendingSearches("IN")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ it ->
                trendingVis()
                trending_list = it.channel?.itemList!!
                var vert_ll = LinearLayout(this)
                vert_ll.orientation = LinearLayout.VERTICAL
                trending_list = if (trending_list.size > 10) {
                    trending_list.subList(0, 11)
                } else {
                    trending_list
                }
                for(searchItem in trending_list){
                    if (vert_ll.childCount >= 4) {
                        if (binding!!.trendingLl.childCount < 2) {
                            binding!!.trendingLl.addView(vert_ll)
                        }
                        vert_ll.orientation = LinearLayout.VERTICAL
                        //vert_ll.removeAllViews()
                        vert_ll = LinearLayout(this@WebActivity)
                    }
                    val tv = TextView(this@WebActivity)
                    tv.ellipsize = TextUtils.TruncateAt.END
                    tv.maxLines = 1
                    tv.text = searchItem.title
                    tv.setPadding(5, 5, 15, 15)
                    tv.textSize = 14f
                    tv.typeface = FeedSdk.font
                    tv.setTextColor(Color.parseColor("#9DA7C8"))
                    tv.setOnClickListener {
                        binding?.adContainer?.visibility = View.GONE
                        binding?.coLayout?.visibility = View.GONE
                        binding?.webview?.visibility = View.VISIBLE
                        logFirebaseEvent("SearchpageClick", "ClickedOn", "Trendingsearches")
                        ApiSearchSticky().userActionSearch(searchItem.title)
                        hideKeyboard()
                        if (!isSourceGoogle) {
                            loadUrl("https://www.bing.com/search?q="+searchItem.title, searchItem.title)
                            logFirebase(searchItem.title, "bing")
                            historyList.add(0, WebHistoryModel("bing", searchItem.title))
                            val historyString = Gson().toJson(if(historyList.size>5) historyList.subList(0,5) else historyList)
                            SpUtil.spUtilInstance!!.putString(WEB_HISTORY, historyString)
                        } else {
                            loadUrl("https://www.google.com/search?q="+searchItem.title, searchItem.title)
                            logFirebase(searchItem.title, "google")
                            historyList.add(0, WebHistoryModel("google", searchItem.title))
                            val historyString = Gson().toJson(if(historyList.size>5) historyList.subList(0,5) else historyList)
                            SpUtil.spUtilInstance!!.putString(WEB_HISTORY, historyString)
                        }
                    }
                    val param: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                    )
                    vert_ll.layoutParams = param
                    vert_ll.addView(tv)
                    binding!!.trendingPbar.visibility = View.GONE
                }
                Log.d(TAG, "showTrendingSearches: "+binding!!.trendingLl.childCount)
                if (binding!!.trendingLl.childCount < 2) {
                    binding!!.trendingLl.addView(vert_ll)
                }
            }, {
                trendingHide()
                it.printStackTrace()
            })

    }

    fun logFirebaseEvent(event:String, param:String, value:String){
        val bundle = Bundle()
        bundle.putString(param, value)
        FirebaseAnalytics.getInstance(this).logEvent(event, bundle)
    }

    fun logFirebase(query: String?, searchEngine: String?) {
        val bundle = Bundle()
        bundle.putString("query", query)
        bundle.putString("searchEngine", searchEngine)
        FirebaseAnalytics.getInstance(this).logEvent("searchQuery", bundle)
    }

    private fun hideKeyboard(){
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding?.search?.windowToken, 0)
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    private fun setFonts(view: View?){
        Card.setFontFamily(binding?.appTitle)
        Card.setFontFamily(binding?.search)
        Card.setFontFamily(binding?.textView13, true)
        Card.setFontFamily(binding?.textView14, true)
    }
}

data class WebHistoryModel(
    var type:String = "google",
    var query:String = ""
)