package com.appyhigh.newsfeedsdk.callbacks

import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.Language

interface PersonalizeCallback {
    fun onPersonalize(interestList: ArrayList<Interest>, languageList: ArrayList<Language>)
}