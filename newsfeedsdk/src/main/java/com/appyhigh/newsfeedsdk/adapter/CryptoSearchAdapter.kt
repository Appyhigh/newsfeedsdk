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
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser
import com.appyhigh.newsfeedsdk.databinding.ItemCryptoSearchBinding

class CryptoSearchAdapter(var cryptoItems: ArrayList<CryptoSearchItem>): RecyclerView.Adapter<CryptoSearchAdapter.CryptoSearchViewHolder>() {

    class CryptoSearchViewHolder(val view: ItemCryptoSearchBinding) : RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CryptoSearchViewHolder {
        return CryptoSearchViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_crypto_search,
                parent,
                false
            )
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newCryptoItems: ArrayList<CryptoSearchItem>){
        cryptoItems = newCryptoItems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CryptoSearchViewHolder, position: Int) {
        holder.view.item = cryptoItems[position]
        holder.view.position = position
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, CryptoCoinDetailsActivity::class.java)
            intent.putExtra(Constants.COIN_ID, cryptoItems[position].coinId)
            intent.putExtra("coin_symbol", cryptoItems[position].coinSymbol)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return cryptoItems.size
    }
}

data class CryptoSearchItem(
    val coinId:String,
    val coinName:String,
    val coinSymbol:String
)

