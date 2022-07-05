package com.appyhigh.newsfeedsdk.activity

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewsFeedSliderAdapter
import com.appyhigh.newsfeedsdk.adapter.TabsAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.apicalls.ApiCrypto
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.callbacks.TabSelectedListener
import com.appyhigh.newsfeedsdk.databinding.ActivityCryptoCoinDetailsBinding
import com.appyhigh.newsfeedsdk.fragment.ChartFragment
import com.appyhigh.newsfeedsdk.fragment.CryptoCoinFragment
import com.appyhigh.newsfeedsdk.model.crypto.CryptoFinderResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.DayAxisValueFormatter
import com.appyhigh.newsfeedsdk.utils.HtmlHelper
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.appbar.AppBarLayout
import java.lang.String.valueOf
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.text.DecimalFormat
import java.util.*

class CryptoCoinDetailsActivity : AppCompatActivity() {
    private lateinit var chartView: WebView
    private var binding: ActivityCryptoCoinDetailsBinding? = null
    private var coinId: String = ""
    private var coinSymbol: String = ""
    private var cards = ArrayList<Card>()
    private var graphTabs = ArrayList<Item>()
    private var cryptoTabs = ArrayList<String>()
    private var graphTabsAdapter: TabsAdapter? = null
    private var feedType:String?=null
    var useNative = true
    private var cryptoTabListener = object : TabSelectedListener {
        override fun onTabClicked(v: View, position: Int) {
            binding?.vpCryptoFeed?.currentItem = position
        }
    }
    private var graphTabListener = object : TabSelectedListener {
        override fun onTabClicked(v: View, position: Int) {
            graphTabsAdapter?.onTabCanged(position)
            changeGraph(position)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoCoinDetailsBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        setFonts()
        fetchGraphTabs()
        coinId = intent.getStringExtra(Constants.COIN_ID)!!
        coinSymbol = intent.getStringExtra("coin_symbol")?:""
        coinSymbol += "USDT"
        Constants.currentCryptoDetailCoinId = coinId
        binding!!.backBtn.setOnClickListener { finish() }
        binding!!.pbLoading.visibility = View.VISIBLE
        binding!!.mainLayout.visibility = View.GONE
        chartView = binding!!.chartView
        if(intent.hasExtra(Constants.FEED_TYPE)){
            feedType = intent.getStringExtra(Constants.FEED_TYPE)
        }
        binding!!.openFull.setOnClickListener {
            val orientationEventListener = object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    val isPortrait = orientation > 300 || orientation < 60 || orientation in 120..240

                    if ((requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && isPortrait) ||
                        (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && !isPortrait)){
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    }
                }
            }
            orientationEventListener.enable()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        chartView.settings.javaScriptEnabled = true
        chartView.settings.domStorageEnabled = true
        fetchData()
        binding!!.refresh.setOnClickListener {
            binding!!.refresh.isEnabled = false
            fetchData()
            graphTabsAdapter?.onTabCanged(0)
            Handler(Looper.getMainLooper()).postDelayed({
                binding!!.refresh.isEnabled = true
            }, 500)
        }
    }

    private fun setFonts(){
        Card.setFontFamily(binding?.coinId, true)
        Card.setFontFamily(binding?.currPrice, true)
        Card.setFontFamily(binding?.percentChange)
        Card.setFontFamily(binding?.tvPastMonth)
    }

    private fun fetchData(listener: OnRefreshListener? = null) {
        ApiCrypto().findCrypto(
            coinSymbol,
            object : ApiCrypto.FindCryptoResponse{
                override fun onSuccess(cryptoResponse: CryptoFinderResponse) {
                    for(item in cryptoResponse){
                        if(item.symbol.equals(coinSymbol,true)){
                            useNative = false
                        }
                    }
                    if(useNative){
                        binding!!.nativeChart.visibility = View.VISIBLE
                        binding!!.chartView.visibility = View.GONE
                        binding!!.openFull.visibility = View.GONE
                    }else{
                        binding!!.nativeChart.visibility = View.GONE
                        binding!!.chartView.visibility = View.VISIBLE
                        binding!!.openFull.visibility = View.VISIBLE
                    }
                }

            }
        )
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoCoinDetailsEncrypted(
                Endpoints.GET_CRYPTO_COIN_DETAILS_ENCRYPTED,
                it,
                coinId,
                null,
                null,
                null, feedType, object : ApiCrypto.CryptoResponseListener {
                    override fun onSuccess(
                        cryptoResponse: ApiCrypto.CryptoResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        cards = cryptoResponse.cards as ArrayList<Card>
                        for (i in cards.indices) {
                            setData(cards[i], i)
                        }
                        binding!!.pbLoading.visibility = View.GONE
                        listener?.onRefreshNeeded()
                        binding!!.mainLayout.visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    fun setData(card: Card, position: Int) {
        try {
            val myFormatter = DecimalFormat("#,##,###.##")
            Log.d("CARD_ITEMS", card.toString())
            when (card.cardType) {
                "coin_title" -> {
                    binding!!.coinId.text = card.items[0].coinName
//                    coinSymbol = card.items[0].coinSymbol
                    if (!card.items[0].images.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(card.items[0].images?.get(0))
                            .into(binding!!.coinImage)
                    }
                    if (Constants.cryptoWatchListMap.containsKey(coinId)) {
                        binding!!.cryptoSelected.setImageResource(R.drawable.ic_crypto_selected)
                    } else {
                        binding!!.cryptoSelected.setImageResource(R.drawable.ic_crypto_not_selected)
                    }
                    binding!!.cryptoSelected.setOnClickListener {
                        if (Constants.cryptoWatchListMap.containsKey(coinId)) {
                            binding!!.cryptoSelected.setImageResource(R.drawable.ic_crypto_not_selected)
                            Constants.cryptoWatchList.remove(Constants.cryptoWatchList.find { it.coinId == coinId })
                            Constants.cryptoWatchListMap.remove(coinId)
                        } else {
                            binding!!.cryptoSelected.setImageResource(R.drawable.ic_crypto_selected)
                            Constants.cryptoWatchList.add(
                                Item(
                                    coinId = coinId, coinName = card.items[0].coinName,
                                    coinSymbol = card.items[0].coinName ?: ""
                                )
                            )
                            Constants.cryptoWatchListMap[coinId] = coinId
                        }
                        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                            ApiCreateOrUpdateUser().updateCryptoWatchlistEncrypted(
                                Endpoints.UPDATE_USER_ENCRYPTED,
                                it1
                            )
                        }
                        listener?.onRefreshNeeded()
                    }
                    val cryptoValue =
                        BigDecimal(card.items[0].current_price).setScale(2, RoundingMode.HALF_EVEN)
                    binding!!.currPrice.text =
                        Constants.getCryptoCoinSymbol() + " " + if (card.items[0].current_price >= 1) myFormatter.format(
                            cryptoValue
                        ) else Constants.get0EValueFormat(card.items[0].current_price)
                    if (card.items[0].percentage_change >= 0) {
                        binding!!.percentChange.text =
                            getString(R.string.change_percentage,BigDecimal(card.items[0].percentage_change).setScale(
                                2,
                                RoundingMode.HALF_EVEN
                            ) )
                        binding!!.percentChange.setTextColor(Color.parseColor("#21C17A"))
                    } else {
                        binding!!.percentChange.text =
                            getString(R.string.change_percentage,BigDecimal(card.items[0].percentage_change).setScale(
                                2,
                                RoundingMode.HALF_EVEN
                            ) )
                        binding!!.percentChange.setTextColor(Color.parseColor("#FF585D"))
                    }
                }
                "coin_graph" -> {
                    graphTabsAdapter = TabsAdapter(graphTabs, graphTabListener, "crypto")
                    binding!!.rvGraphTabs.apply {
                        layoutManager =
                            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        adapter = graphTabsAdapter
                    }
                    setNativeChartData(binding!!.nativeChart,
                        card.items[0].timestamps as ArrayList<String>,
                        card.items[0].prices as ArrayList<Double>, 0
                    )
                    setNewChartData("60")
//                    binding!!.chart.setOnClickListener {
//                        val intent = Intent(this, TradingViewActivity::class.java)
//                        intent.putExtra("coin",getIntent().getStringExtra("coin_symbol"))
//                        startActivity(intent)
//                    }
                }
                "tabs" -> {
                    val fragmentList = ArrayList<Fragment>()
                    var tabAdapter: TabsAdapter? = null
                    if (card.items.isNotEmpty()) {
                        tabAdapter = TabsAdapter(card.items, cryptoTabListener, "crypto")
                        binding!!.rvCryptoTabs.apply {
                            layoutManager =
                                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                            adapter = tabAdapter
                            disableToolBarScrolling()
                            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                override fun onScrollStateChanged(
                                    recyclerView: RecyclerView,
                                    newState: Int
                                ) {
                                    super.onScrollStateChanged(recyclerView, newState)
                                    when (newState) {
                                        RecyclerView.SCROLL_STATE_IDLE -> {
                                            disableToolBarScrolling()
                                        }
                                        else -> {
                                            enableToolBarScrolling()
                                        }
                                    }
                                }
                            })
                        }
                        val tabCards = ArrayList<Card>()
                        tabCards.addAll(cards.subList(position + 1, cards.size))
                        Constants.cardsMap[card.items[0].key_id!!] = tabCards
                        cryptoTabs = ArrayList()
                        for (tab in card.items) {
                            fragmentList.add(CryptoCoinFragment.newInstance(coinId, tab.key_id!!))
                            cryptoTabs.add(tab.key_id!!)
                        }
                    }
                    binding!!.vpCryptoFeed.isUserInputEnabled = true
                    binding!!.vpCryptoFeed.adapter =
                        NewsFeedSliderAdapter(
                            if (baseContext is FragmentActivity) baseContext as FragmentActivity else this as FragmentActivity,
                            fragmentList
                        )
                    binding!!.vpCryptoFeed.registerOnPageChangeCallback(object :
                        ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            tabAdapter?.onTabCanged(position)
                            SpUtil.cryptoEventsListener?.onCoinDetailTabChanged(
                                coinId,
                                cryptoTabs[position]
                            )
                        }
                    })
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun changeGraph(position: Int) {
        try{
            if(useNative){
                changeNativeGraph(position)
            } else {
                val interval = when (position) {
                    0 -> "60"
                    1 -> "D"
                    2 -> "W"
                    3 -> "M"
                    4 -> "90D"
                    else -> "3"
                }
                setNewChartData(interval)
            }
        } catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    private fun changeNativeGraph(position: Int){
        val cal = Calendar.getInstance()
        var start:Long = 0
        val end:Long = cal.timeInMillis/1000
        val hr:Long = (60*60).toLong()
        val day:Long = 24*hr
        start = when(position){
            0 -> end-hr
            1 -> end-day
            2 -> end-(7*day)
            3 -> end-(30*day)
            4 -> end-(90*day)
            5 -> end-(365*day)
            else -> 1329004800 //12 Feb 2012
        }
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiCrypto().getCryptoCoinDetailsEncrypted(
                Endpoints.GET_CRYPTO_COIN_DETAILS_ENCRYPTED,
                it,
                coinId,
                null,
                start,
                end, feedType,  object : ApiCrypto.CryptoResponseListener {
                    override fun onSuccess(
                        cryptoResponse: ApiCrypto.CryptoResponse,
                        url: String,
                        timeStamp: Long
                    ) {
                        try{
                            val cards = cryptoResponse.cards
                            if(cards.size>1){
                                setNativeChartData(binding!!.nativeChart, cards[1].items[0].timestamps as ArrayList<String>, cards[1].items[0].prices as ArrayList<Double>, position)
                            }
                            SpUtil.cryptoEventsListener?.onCoinDetailDateChanged(coinId, graphTabs[position].value!!)
                        } catch (ex:Exception){
                            ex.printStackTrace()
                        }
                    }
                }
            )
        }
    }

    private fun setNativeChartData(chart: LineChart, xList: ArrayList<String>, yList: ArrayList<Double>, type: Int) {
        chart.xAxis.isEnabled = true
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.textColor = ContextCompat.getColor(this, R.color.feedPrimaryTextColor)
        chart.xAxis.valueFormatter = DayAxisValueFormatter(type, HashMap())
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#EFF2F5")
        leftAxis.axisLineColor = Color.parseColor("#EFF2F5")
        leftAxis.textColor = ContextCompat.getColor(this, R.color.feedPrimaryTextColor)
//        leftAxis.textColor = ContextCompat.getColor(this, R.color.feedBackground)
//        leftAxis.textSize = 6f

        chart.axisRight.isEnabled = false
        val values = ArrayList<Entry>()
        for (i in xList.indices) {
            try {
                values.add(Entry(Timestamp.valueOf(xList[i]).time.toFloat(), yList[i].toFloat()))
            } catch (ex:Exception){ }
        }
        values.sortBy { it.x }
        val set = LineDataSet(values, "DataSet 1")
        // create a dataset and give it a type
        set.setDrawIcons(false)
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.disableDashedLine()
        set.fillColor = Color.parseColor("#FFC90E")
        set.fillAlpha = 10
        set.setDrawFilled(true)
        set.color = Color.parseColor("#FFC90E")
        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(set)
        chart.data = LineData(dataSets)
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(false)
        chart.fitScreen()
//        chart.invalidate()
        chart.moveViewTo(values[0].x, values[0].y, YAxis.AxisDependency.LEFT)
        // animate calls invalidate()...
        chart.animateX(1000)
        binding!!.chartLayout.visibility = View.VISIBLE
    }

    private fun setNewChartData(interval: String) {
        val html = HtmlHelper.getSmallScreenHtml(coinSymbol, interval, FeedSdk.sdkTheme)
        Log.d("COIN_ID", coinSymbol)
        Log.d("HTML_SCRIPT", html)
        chartView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        binding!!.chartLayout.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding!!.llDetails.visibility = View.GONE
            binding!!.chartContainer.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction().apply {
                add(R.id.chart_container, ChartFragment.newInstance(coinSymbol))
                addToBackStack(null)
                commit()
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding!!.llDetails.visibility = View.VISIBLE
            binding!!.chartContainer.visibility = View.INVISIBLE
        }
    }

    private fun fetchGraphTabs() {
        graphTabs = ArrayList()
        graphTabs.add(Item(value = "1 Hr"))
        graphTabs.add(Item(value = "1 Day"))
        graphTabs.add(Item(value = "7 Day"))
        graphTabs.add(Item(value = "30 Day"))
    }

    override fun onDestroy() {
        super.onDestroy()
        for (tab in cryptoTabs) {
            Constants.cardsMap.remove(tab)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun disableToolBarScrolling() {
        val params = binding!!.appBar.layoutParams as CoordinatorLayout.LayoutParams
        if (params.behavior == null)
            params.behavior = AppBarLayout.Behavior()
        val behaviour = params.behavior as AppBarLayout.Behavior
        behaviour.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
    }

    private fun enableToolBarScrolling() {
        val params = binding!!.appBar.layoutParams as CoordinatorLayout.LayoutParams
        if (params.behavior == null)
            params.behavior = AppBarLayout.Behavior()
        val behaviour = params.behavior as AppBarLayout.Behavior
        behaviour.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return true
            }
        })
    }

    companion object {
        private var listener: OnRefreshListener? = null
        fun addListener(refreshListener: OnRefreshListener) {
            listener = refreshListener
        }
    }
}