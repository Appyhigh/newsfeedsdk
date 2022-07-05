package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.activity.CryptoCoinDetailsActivity
import com.appyhigh.newsfeedsdk.databinding.ItemCoinMarketBinding
import com.appyhigh.newsfeedsdk.databinding.ItemCryptoSearchBinding
import com.appyhigh.newsfeedsdk.model.feeds.Item
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class CryptoMarketAdapter (var cryptoItems: ArrayList<Item.Market>): RecyclerView.Adapter<CryptoMarketAdapter.CoinMarketViewHolder>() {

    class CoinMarketViewHolder(val view: ItemCoinMarketBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinMarketViewHolder {
        return CoinMarketViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_coin_market,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CoinMarketViewHolder, position: Int) {
        val cryptoItem = cryptoItems[position]
        val myFormatter = DecimalFormat("#,##,###")
        holder.view.baseTarget.text = cryptoItem.base+"/"+cryptoItem.target
        holder.view.liquidityScore.text = "Liquidity: "
        holder.view.last.text =Constants.getCryptoCoinSymbol()+ if(cryptoItem.last!!>1) myFormatter.format(cryptoItem.last) else Constants.get0EValueFormat(cryptoItem.last)
        holder.view.marketName.text = cryptoItem.marketName
        if(cryptoItem.trustScore=="green") {
            holder.view.trustScore.text = "High"
            holder.view.trustScore.setBackgroundResource(R.drawable.bg_coin_market_green)
        } else{
            holder.view.trustScore.text = "Low"
            holder.view.trustScore.setBackgroundResource(R.drawable.bg_coin_market_green)
        }
        val volString = cryptoItem.volume.toString()+""
        if(cryptoItem.volume>=1.0) {
            if (volString.contains("E", false)) {
                val marketCapList = volString.split("E")
                holder.view.volume.text = "Vol " + Constants.getCryptoCoinSymbol() + Constants.getUnitFromValue(
                    marketCapList[0].toDouble(),
                    marketCapList[1].toInt()
                )
            } else {
                holder.view.volume.text = "Vol " + Constants.getCryptoCoinSymbol() + Constants.getEValueFormat(volString.toDouble(), 0)
            }
        } else{
            holder.view.volume.text = "Vol " + Constants.getCryptoCoinSymbol() + Constants.get0EValueFormat(cryptoItem.volume)
        }
    }

    override fun getItemCount(): Int {
        return cryptoItems.size
    }
}