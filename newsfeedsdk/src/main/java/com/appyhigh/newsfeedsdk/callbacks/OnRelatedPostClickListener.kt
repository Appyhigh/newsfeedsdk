package com.appyhigh.newsfeedsdk.callbacks

import com.appyhigh.newsfeedsdk.model.PostDetailsModel

interface OnRelatedPostClickListener {
    fun onPostClick(nextPost: PostDetailsModel.NextPost)
    fun onSharePost(postId:String, title:String, imageUrl:String, isWhatsapp: Boolean, url:String)
}