package com.appyhigh.newsfeedsdk.callbacks

interface OnRelatedPostClickListener {
    fun onPostClick(postId:String, isNative:Boolean)
    fun onSharePost(postId:String, title:String, imageUrl:String, isWhatsapp: Boolean, url:String)
}