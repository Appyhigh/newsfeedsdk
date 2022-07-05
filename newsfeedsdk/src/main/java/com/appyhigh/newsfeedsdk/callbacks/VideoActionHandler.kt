package com.appyhigh.newsfeedsdk.callbacks

interface VideoActionHandler {
    fun onStart()
    fun onStop()
    fun onPause()
    fun onResume()
}