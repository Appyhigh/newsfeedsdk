package com.appyhigh.newsfeedsdk.callbacks

interface BackPressListener {
    fun onFragmentBackPressed()
}

interface OnFragmentClickListener {
    fun onFragmentClicked()
    fun onCryptoConvertorClicked(coinId:String)
}