package com.appyhigh.newsfeedsdk.callbacks

interface NumberKeyboardListener {
    fun onResult(value: String, cursorAt: Int)
}