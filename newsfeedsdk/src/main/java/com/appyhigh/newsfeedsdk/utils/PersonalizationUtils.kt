package com.appyhigh.newsfeedsdk.utils

import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.UserResponse

class PersonalizationUtils() {

    var mUserDetails: UserResponse? = null
    var mInterestResponseModel: InterestResponseModel? = null
    private var interestQuery = ""
    private var interestMap = HashMap<String, Interest>()
    fun getPersonalization(getPersonalizationData: GetPersonalizationData) {
        ApiUserDetails().getUserResponseEncrypted(
            Endpoints.USER_DETAILS_ENCRYPTED,
            object : ApiUserDetails.UserResponseListener {
                override fun onSuccess(userDetails: UserResponse) {
                    mUserDetails = userDetails
                    getVideos(getPersonalizationData)
                }
            })
        ApiGetInterests().getInterestsEncrypted(
            Endpoints.GET_INTERESTS_ENCRYPTED,
            object : ApiGetInterests.InterestResponseListener {
                override fun onSuccess(interestResponseModel: InterestResponseModel) {
                    mInterestResponseModel = interestResponseModel
                    getVideos(getPersonalizationData)
                }
            })
    }

    private fun getVideos(getPersonalizationData:GetPersonalizationData) {
        var selectedInterestsList = ArrayList<Interest>()
        if (mUserDetails != null && mInterestResponseModel != null) {
            for (interest in mInterestResponseModel?.interestList!!) {
                interestMap[interest.keyId!!] = interest
            }
            if (mUserDetails?.user?.interests.isNullOrEmpty()) {
                selectedInterestsList =
                    (mInterestResponseModel?.interestList as ArrayList<Interest>?)!!
            } else {
                for (interest in interestMap.values) {
                    if (mUserDetails?.user?.interests!!.contains(interest.keyId)) {
                        selectedInterestsList.add(interest)
                    }
                }
            }
            for ((i, interest) in selectedInterestsList.withIndex()) {
                if (i < selectedInterestsList.size - 1) {
                    interestQuery =
                        interestQuery + interest.keyId.toString() + ","
                } else {
                    interestQuery += interest.keyId
                }
            }
            getPersonalizationData.onDataReceived("en", interestQuery)
        }
    }

    interface GetPersonalizationData {
        fun onDataReceived(languageQuery: String, interestsQuery: String)
    }
}