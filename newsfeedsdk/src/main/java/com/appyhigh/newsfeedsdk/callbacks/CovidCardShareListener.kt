package com.appyhigh.newsfeedsdk.callbacks

import android.view.View

interface CovidCardShareListener {
    fun onShareCovidData(v: View, position: Int, isWhatsApp: Boolean)
}