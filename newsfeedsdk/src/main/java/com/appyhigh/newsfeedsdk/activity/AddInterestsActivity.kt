package com.appyhigh.newsfeedsdk.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.AddInterestAdapter
import com.appyhigh.newsfeedsdk.adapter.InterestChooserAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiUpdateUserPersonalization
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddInterestsActivity : AppCompatActivity() {
    private lateinit var flexLayoutManager: FlexboxLayoutManager
    private lateinit var rvSelectedInterest: RecyclerView
    private lateinit var rvAllInterests: RecyclerView
    private var mUserDetails: UserResponse? = null
    private lateinit var preferenceAdapter: AddInterestAdapter
    private lateinit var interestChooserAdapter: InterestChooserAdapter
    private var mInterestResponseModel: InterestResponseModel? = null
    private var interestMap = HashMap<String, Interest>()
    private var retrofit: Retrofit? = null
    private var ivClose: AppCompatImageView? = null
    var interestList = ArrayList<Interest>()
    var allInterestList = ArrayList<Interest>()
    private var etSearch: AppCompatEditText? = null
    private var isChanged = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_interests)
        flexLayoutManager = FlexboxLayoutManager(this)
        ivClose = findViewById(R.id.ivClose)
        etSearch = findViewById(R.id.etSearch)
        ivClose?.setOnClickListener {
            onBackPressed()
        }
        val manageCategory: AppCompatTextView = findViewById(R.id.manageCategories)
        Card.setFontFamily(manageCategory, true)
        Constants.setFontFamily(etSearch)
        etSearch?.typeface = FeedSdk.font
        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiGetInterests().getInterestsEncrypted(
                Endpoints.GET_INTERESTS_ENCRYPTED,
                it,
                object : ApiGetInterests.InterestResponseListener {
                    override fun onSuccess(interestResponseModel: InterestResponseModel) {
                        mInterestResponseModel = interestResponseModel
                        setUpInterests()
                    }
                })
        }

        FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let {
            ApiUserDetails().getUserResponseEncrypted(
                Endpoints.USER_DETAILS_ENCRYPTED,
                it,
                object : ApiUserDetails.UserResponseListener {
                    override fun onSuccess(userDetails: UserResponse) {
                        mUserDetails = userDetails
                        setUpInterests()
                    }
                })
        }



        rvSelectedInterest = findViewById(R.id.rvSelectedInterests)
        rvAllInterests = findViewById(R.id.rvAllInterests)


        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val filteredList = allInterestList.filter { interest ->
                        interest.label!!.lowercase(Locale.getDefault())
                            .contains(s.toString().lowercase(Locale.getDefault()))
                    }
                    interestChooserAdapter.updateData(filteredList as ArrayList<Interest>)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
    }

    private fun setUpInterests() {
        var selectedInterestsList = ArrayList<Interest>()
        var isSelectedInterestsEmpty = false
        if (mUserDetails != null && mInterestResponseModel != null) {
            for (interest in mInterestResponseModel?.interestList!!) {
                interestMap[interest.keyId!!] = interest
            }
            if (mUserDetails?.user?.interests.isNullOrEmpty()) {
                isSelectedInterestsEmpty = true
                selectedInterestsList =
                    (mInterestResponseModel?.interestList as ArrayList<Interest>?)!!
            } else {
                for (interest in interestMap.values) {
                    if (mUserDetails?.user?.interests!!.contains(interest.keyId)) {
                        selectedInterestsList.add(interest)
                    }
                }
            }
            interestList = selectedInterestsList
            allInterestList = mInterestResponseModel?.interestList as ArrayList<Interest>

            flexLayoutManager.justifyContent = JustifyContent.CENTER
            preferenceAdapter = AddInterestAdapter(interestList, object : OnInterestRemoved {
                override fun onRemoved(pos: Int) {
                    isChanged = true
                    interestList = preferenceAdapter.getItems()
                    interestList.remove(preferenceAdapter.getItems()[pos])
                    preferenceAdapter.updateData(interestList)
                    updateInterests()
                }
            })
            rvSelectedInterest.apply {
                adapter = preferenceAdapter
                layoutManager = flexLayoutManager
            }

            interestChooserAdapter =
                InterestChooserAdapter(allInterestList, object : OnInterestSelected {
                    override fun onSelected(interest: Interest) {
                        isChanged = true
                        if (!interestList.contains(interest)) {
                            interestList.add(interest)
                            preferenceAdapter.updateData(interestList)
                            updateInterests()
                        }
                    }
                })
            rvAllInterests.apply {
                adapter = interestChooserAdapter
                layoutManager = LinearLayoutManager(this@AddInterestsActivity)
            }
        }
    }

    private fun updateInterests() {

    }

    override fun onBackPressed() {
        if (interestList.size > 2 && isChanged) {
            FeedSdk.userId?.let {
                FeedSdk.spUtil?.getString(Constants.JWT_TOKEN)?.let { it1 ->
                    ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                        Endpoints.UPDATE_USER_ENCRYPTED,
                        it,
                        interestList,
                        ArrayList(),
                        object : ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                            override fun onFailure() {
                                finish()
                            }

                            override fun onSuccess() {
                                FeedSdk.isRefreshNeeded = true
                                finish()
                            }
                        }
                    )
                }
            }
        } else {
            finish()
        }
    }

    interface OnInterestRemoved {
        fun onRemoved(pos: Int)
    }

    interface OnInterestSelected {
        fun onSelected(interest: Interest)
    }
}