package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.cryptoWatchList
import com.appyhigh.newsfeedsdk.Constants.cryptoWatchListMap
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.CryptoCoinDetailsActivity
import com.appyhigh.newsfeedsdk.activity.CryptoMainAlertActivity
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.OnFragmentClickListener
import com.appyhigh.newsfeedsdk.callbacks.OnRefreshListener
import com.appyhigh.newsfeedsdk.databinding.*
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.fragment.CryptoAlertPriceFragment
import com.appyhigh.newsfeedsdk.model.feeds.Item
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import it.sephiroth.android.library.xtooltip.Tooltip
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.text.DecimalFormat
import java.util.*

class CryptoDetailsAdapter(var cryptoItems: ArrayList<Item>, var isEditable: Boolean=false,
                           var cardType:String="", var isFromListActivity:Boolean=false, var interest:String="", var listener: OnFragmentClickListener?=null) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val CRYPTO_ITEM = 0
    private val LOAD_MORE = 1
    private val WATCHLIST_ITEM = 2
    private val MARKET_ITEM = 3
    private val ALERT_SELECT_ITEM = 4
    var tooltipShown:Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            LOAD_MORE -> {
                return LoadMoreViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_load_more,
                        parent,
                        false
                    )
                )
            }
            WATCHLIST_ITEM -> {
                return CryptoWatchlistItemViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_crypto_watchlist,
                        parent,
                        false
                    )
                )
            }
            MARKET_ITEM -> {
                return CoinMarketViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_coin_market,
                        parent,
                        false
                    )
                )
            }
            ALERT_SELECT_ITEM -> {
                return AlertSelectViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_alert_select,
                        parent,
                        false
                    )
                )
            }
            else -> {
                return CryptoGainersItemViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_crypto_gainers,
                        parent,
                        false
                    )
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateWatchList(watchList:ArrayList<Item>){
        cryptoItems = watchList
        notifyDataSetChanged()
    }

    fun updateList(newCryptoItems: ArrayList<Item>){
        cryptoItems.removeLast()
        val oldSize = cryptoItems.size
        cryptoItems.addAll(newCryptoItems)
        val newSize = cryptoItems.size
        notifyItemRangeInserted(oldSize, newSize-oldSize)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(mainHolder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            LOAD_MORE -> {
                mainHolder as LoadMoreViewHolder
            }
            MARKET_ITEM -> {
                onBindViewHolder(mainHolder as CoinMarketViewHolder, position)
            }
            WATCHLIST_ITEM -> {
                onBindViewHolder(mainHolder as CryptoWatchlistItemViewHolder, position)
            }
            ALERT_SELECT_ITEM -> {
                onBindViewHolder(mainHolder as AlertSelectViewHolder, position)
            }
            else -> onBindViewHolder(mainHolder as CryptoGainersItemViewHolder, position)
        }
    }

    private fun onBindViewHolder(holder: CoinMarketViewHolder, position: Int){
        val cryptoItem = cryptoItems[position]
    }


    private fun onBindViewHolder(holder: CryptoWatchlistItemViewHolder, position: Int){
        holder.view.item = cryptoItems[position]
        holder.view.position = position
        val cryptoItem = cryptoItems[position]
        val myFormatter = DecimalFormat("#,##,###.##")
        when {
            cryptoItem.inr!=null -> {
                val cryptoValue = BigDecimal(cryptoItem.inr.currPrice!!).setScale(2, RoundingMode.HALF_EVEN)
                holder.view.currPrice.text = Constants.getCryptoCoinSymbol()+ if(cryptoItem.inr.currPrice!!>=1.0) myFormatter.format(cryptoValue) else Constants.get0EValueFormat(cryptoItem.inr.currPrice!!)
                if(cryptoItem.inr.hChange>=0){
                    holder.view.priceChange.text = "+ "+ BigDecimal(cryptoItem.inr.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#21C17A"))
                } else {
                    holder.view.priceChange.text = ""+BigDecimal(cryptoItem.inr.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#FF585D"))
                }
            }
            cryptoItem.usd!=null -> {
                val cryptoValue = BigDecimal(cryptoItem.usd.currPrice!!).setScale(2, RoundingMode.HALF_EVEN)
                holder.view.currPrice.text = Constants.getCryptoCoinSymbol()+ if(cryptoItem.usd.currPrice!!>=1.0) myFormatter.format(cryptoValue) else Constants.get0EValueFormat(cryptoItem.usd.currPrice!!)
                if(cryptoItem.usd.hChange>=0){
                    holder.view.priceChange.text = "+ "+BigDecimal(cryptoItem.usd.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#21C17A"))
                } else {
                    holder.view.priceChange.text = ""+BigDecimal(cryptoItem.usd.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#FF585D"))
                }
            }
            else -> {
                holder.view.currPrice.visibility = View.GONE
            }
        }
        holder.itemView.setOnClickListener {
            try{
                val intent = Intent(holder.itemView.context, CryptoCoinDetailsActivity::class.java)
                intent.putExtra(Constants.COIN_ID, cryptoItem.coinId)
                intent.putExtra("coin_symbol", cryptoItem.coinSymbol)
                holder.itemView.context.startActivity(intent)
                SpUtil.cryptoEventsListener?.openCoinFromCryptoHome("Watchlist", cryptoItem.coinId!!)
            } catch (ex:java.lang.Exception){}
        }
    }

    private fun onBindViewHolder(holder: CryptoGainersItemViewHolder, position: Int){
        holder.view.item = cryptoItems[position]
        holder.view.position = position
        val cryptoItem = cryptoItems[position]
        val myFormatter = DecimalFormat("#,##,###.##")
        var isCryptoGainer = false
        if(isEditable){
            holder.view.cryptoSelected.visibility = View.VISIBLE
            try{
                if(position == 0 && !tooltipShown && SpUtil.spUtilInstance!!.getLong("showWatchlistTooltip", 0)<4){
                    val tooltip = Tooltip.Builder(holder.itemView.context)
                        .anchor(holder.view.cryptoSelected, 0, 0, true)
                        .styleId(R.style.ToolTipAltStyle)
                        .text("Add to Watchlist")
                        .arrow(true)
                        .floatingAnimation(Tooltip.Animation.DEFAULT)
                        .showDuration(1000)
                        .overlay(false)
                        .create()

                    holder.view.cryptoSelected.post {
                        try{
                            tooltip
                                .doOnHidden { }
                                .doOnFailure { }
                                .doOnShown { }
                                .show(holder.view.cryptoSelected, Tooltip.Gravity.BOTTOM, true)
                            tooltipShown = true
                            SpUtil.spUtilInstance!!.putLong("showWatchlistTooltip", SpUtil.spUtilInstance!!.getLong("showWatchlistTooltip", 0)+1)
                        } catch (ex:java.lang.Exception){
                            LogDetail.LogEStack(ex)
                        }
                    }
                }
            } catch (ex:Exception) {}
        } else {
            holder.view.cryptoSelected.visibility = View.GONE
        }
        if(cryptoWatchListMap.containsKey(cryptoItem.coinId)){
            holder.view.cryptoSelected.setImageResource(R.drawable.ic_crypto_selected)
        } else {
            holder.view.cryptoSelected.setImageResource(R.drawable.ic_crypto_not_selected)
        }
        holder.view.cryptoSelected.setOnClickListener {
            var isSelected = false
            if(cryptoWatchListMap.containsKey(cryptoItem.coinId)){
                holder.view.cryptoSelected.setImageResource(R.drawable.ic_crypto_not_selected)
                cryptoWatchList.remove(cryptoWatchList.find { it.coinId==cryptoItem.coinId })
                cryptoWatchListMap.remove(cryptoItem.coinId)
            } else {
                holder.view.cryptoSelected.setImageResource(R.drawable.ic_crypto_selected)
                isSelected = true
                cryptoWatchList.add(cryptoItem)
                cryptoWatchListMap[cryptoItem.coinId!!] = cryptoItem.coinId
            }
            ApiCreateOrUpdateUser().updateCryptoWatchlistEncrypted(
                Endpoints.UPDATE_USER_ENCRYPTED
            )
            when(interest){
                "crypto_gainers" -> SpUtil.cryptoEventsListener?.onAddWatchlist(cryptoItem.coinId!!, "Top Gainers", isSelected)
                "crypto_losers" -> SpUtil.cryptoEventsListener?.onAddWatchlist(cryptoItem.coinId!!, "Top Losers", isSelected)
                "crypto_watchlist_edit" -> SpUtil.cryptoEventsListener?.onAddWatchlist(cryptoItem.coinId!!, "Coin Detail Edit Page", isSelected)
                else -> SpUtil.cryptoEventsListener?.onAddWatchlist(cryptoItem.coinId!!, "All Coins Page", isSelected)
            }
        }
        when {
            cryptoItem.inr!=null -> {
                val cryptoValue = BigDecimal(cryptoItem.inr.currPrice!!).setScale(2, RoundingMode.HALF_EVEN)
                setData(holder.view.chart, cryptoItem.timestamps as ArrayList<String>, cryptoItem.prices as ArrayList<Double>, cryptoItem.inr.hChange>0)
                holder.view.currPrice.text = Constants.getCryptoCoinSymbol()+ if(cryptoItem.inr.currPrice!!>=1.0) myFormatter.format(cryptoValue) else Constants.get0EValueFormat(cryptoItem.inr.currPrice!!)
                if(cryptoItem.inr.hChange>=0){
                    holder.view.priceChange.text = "+ "+BigDecimal(cryptoItem.inr.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#21C17A"))
                    isCryptoGainer = true
                } else {
                    holder.view.priceChange.text = ""+BigDecimal(cryptoItem.inr.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#FF585D"))
                    isCryptoGainer = false
                }
                val marketCapString = cryptoItem.inr.marketCap.toString()+""
                if(cryptoItem.inr.marketCap>=1.0){
                    if(marketCapString.contains("E",false)){
                        val marketCapList = marketCapString.split("E")
                        holder.view.marketCap.text = "MCap "+Constants.getCryptoCoinSymbol() +  Constants.getUnitFromValue(marketCapList[0].toDouble(), marketCapList[1].toInt())
                    } else {
                        holder.view.marketCap.text = "MCap "+Constants.getCryptoCoinSymbol() + Constants.getEValueFormat(marketCapString.toDouble(), 0)
                    }
                } else{
                    holder.view.marketCap.text = "MCap "+Constants.getCryptoCoinSymbol() + Constants.get0EValueFormat(cryptoItem.inr.marketCap)
                }
            }
            cryptoItem.usd!=null -> {
                val cryptoValue = BigDecimal(cryptoItem.usd.currPrice!!).setScale(2, RoundingMode.HALF_EVEN)
                setData(holder.view.chart, cryptoItem.timestamps as ArrayList<String>, cryptoItem.prices as ArrayList<Double>, cryptoItem.usd.hChange>0)
                holder.view.currPrice.text = Constants.getCryptoCoinSymbol()+ if(cryptoItem.usd.currPrice!!>=1.0) myFormatter.format(cryptoValue) else Constants.get0EValueFormat(cryptoItem.usd.currPrice!!)
                if(cryptoItem.usd.hChange>=0){
                    holder.view.priceChange.text = "+ "+ BigDecimal(cryptoItem.usd.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#21C17A"))
                    isCryptoGainer = true
                } else {
                    holder.view.priceChange.text = ""+BigDecimal(cryptoItem.usd.hChange).setScale(2, RoundingMode.HALF_EVEN)+" %"
                    holder.view.priceChange.setTextColor(Color.parseColor("#FF585D"))
                    isCryptoGainer = false
                }
                val marketCapString = cryptoItem.usd.marketCap.toString()+""
                if(cryptoItem.usd.marketCap>=1.0){
                    if(marketCapString.contains("E",false)){
                        val marketCapList = marketCapString.split("E")
                        holder.view.marketCap.text = "MCap "+Constants.getCryptoCoinSymbol() +  Constants.getUnitFromValue(marketCapList[0].toDouble(), marketCapList[1].toInt())
                    } else {
                        holder.view.marketCap.text = "MCap "+Constants.getCryptoCoinSymbol() + Constants.getEValueFormat(marketCapString.toDouble(), 0)
                    }
                } else{
                    holder.view.marketCap.text = "MCap "+Constants.getCryptoCoinSymbol() + Constants.get0EValueFormat(cryptoItem.usd.marketCap)
                }
            }
            else -> {
                holder.view.currPrice.visibility = View.GONE
            }
        }
        holder.itemView.setOnClickListener {
            try{
                val intent = Intent(holder.itemView.context, CryptoCoinDetailsActivity::class.java)
                intent.putExtra(Constants.COIN_ID, cryptoItem.coinId)
                intent.putExtra("coin_symbol", cryptoItem.coinSymbol)
                holder.itemView.context.startActivity(intent)
                if(isFromListActivity){
                    when (interest) {
                        "crypto_gainers" -> {
                            SpUtil.cryptoEventsListener?.openCoinFromListPage("Top Gainers", cryptoItem.coinId!!)
                        }
                        "crypto_losers" -> {
                            SpUtil.cryptoEventsListener?.openCoinFromListPage("Top Losers", cryptoItem.coinId!!)
                        }
                        else -> {
                            SpUtil.cryptoEventsListener?.openCoinFromListPage("Watchlist", cryptoItem.coinId!!)
                        }
                    }
                } else{
                    if(isCryptoGainer){
                        SpUtil.cryptoEventsListener?.openCoinFromCryptoHome("Top Gainers", cryptoItem.coinId!!)
                    } else{
                        SpUtil.cryptoEventsListener?.openCoinFromCryptoHome("Top Losers", cryptoItem.coinId!!)
                    }
                }
                CryptoCoinDetailsActivity.addListener(object : OnRefreshListener{
                    override fun onRefreshNeeded() {
                        notifyItemChanged(holder.bindingAdapterPosition)
                    }
                })
            } catch (ex:java.lang.Exception){ }
        }
    }

    private fun onBindViewHolder(holder: AlertSelectViewHolder, position: Int){
        val cryptoItem = cryptoItems[position]
        holder.view.item = cryptoItem
        holder.view.position = position
        var price: Double?=null
        try{
            if(FeedSdk.sdkCountryCode == null || FeedSdk.sdkCountryCode!!.lowercase() == "in"){
                price = cryptoItem.inr?.currPrice
            } else{
                price = cryptoItem.usd?.currPrice
            }
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
        holder.itemView.setOnClickListener {
            if(cardType==Constants.CRYPTO_CONVERTER){
                listener?.onCryptoConvertorClicked(cryptoItem.coinId?:"")
            } else{
                listener?.onFragmentClicked()
                (holder.itemView.context as CryptoMainAlertActivity)
                    .supportFragmentManager.beginTransaction()
                    .add(R.id.baseFragment, CryptoAlertPriceFragment.newInstance(cryptoItem.coinId?:"", cryptoItem.coinName?:"", cryptoItem.imageLink?:"", 0.0, price))
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(cardType){
            Constants.CardType.CRYPTO_WATCHLIST.toString()
                .lowercase(Locale.getDefault()) -> {
                WATCHLIST_ITEM
            }
            Constants.CardType.COIN_MARKETS.toString()
                .lowercase(Locale.getDefault()) -> {
                MARKET_ITEM
            }
            Constants.CRYPTO_ALERT_SELECT -> ALERT_SELECT_ITEM
            Constants.CRYPTO_CONVERTER -> ALERT_SELECT_ITEM
            else -> {
                if(cryptoItems[position].key_id == Constants.LOADER)
                    LOAD_MORE
                else  CRYPTO_ITEM
            }
        }
    }

    private fun setData(chart: LineChart, xList: ArrayList<String>, yList: ArrayList<Double>, isProfitable: Boolean){
        try{
            chart.xAxis.isEnabled = false
            chart.axisLeft.isEnabled = false
            chart.axisRight.isEnabled = false
            val values = ArrayList<Entry>()
            for(i in xList.indices){
                try{
                    values.add(Entry(Timestamp.valueOf(xList[i]).time.toFloat(), yList[i].toFloat()))
                } catch (ex:Exception){
                    LogDetail.LogEStack(ex)
                }
            }
            // create a dataset and give it a type
            val set = LineDataSet(values, "DataSet 1")
            set.setDrawIcons(false)
            set.setDrawValues(false)
            set.setDrawCircles(false)
            set.disableDashedLine()
            set.fillColor = Color.parseColor(if(isProfitable) "#21C17A" else "#FF585D")
            set.fillAlpha = if(isProfitable) 10 else 40
            set.setDrawFilled(true)
            set.color = Color.parseColor(if(isProfitable) "#21C17A" else "#FF585D")
            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(set)
            chart.data = LineData(dataSets)
            chart.description.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.legend.isEnabled = false
            chart.setTouchEnabled(false)
            chart.isDragEnabled = false
            chart.setScaleEnabled(false)
            chart.setPinchZoom(false)
        } catch (ex:Exception){
            LogDetail.LogEStack(ex)
        }
    }

    override fun getItemCount(): Int {
        return cryptoItems.size
    }

    class CryptoGainersItemViewHolder(val view: ItemCryptoGainersBinding) : RecyclerView.ViewHolder(view.root)
    class LoadMoreViewHolder(val view: ItemLoadMoreBinding) : RecyclerView.ViewHolder(view.root)
    class CryptoWatchlistItemViewHolder(val view: ItemCryptoWatchlistBinding) :
        RecyclerView.ViewHolder(view.root)
    class CoinMarketViewHolder(val view: ItemCoinMarketBinding) :
        RecyclerView.ViewHolder(view.root)
    class AlertSelectViewHolder(val view: ItemAlertSelectBinding) : RecyclerView.ViewHolder(view.root)
}

interface CryptoWatchListUpdateListener{
    fun onCryptoWatchListUpdated(newWatchlist: ArrayList<Item>)
}