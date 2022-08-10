package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.LanguageAdapter
import com.appyhigh.newsfeedsdk.adapter.NewInterestAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiGetLanguages
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.PersonalizeCallback
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.Language
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import java.util.*

class PersonalizeFragment : Fragment() {
    private var pos = 0
    private lateinit var flexLayoutManager: FlexboxLayoutManager
    private lateinit var preferenceAdapter: NewInterestAdapter
    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var btnSave: AppCompatButton
    private lateinit var etSearch: AppCompatEditText
    private lateinit var rvlanguges: RecyclerView
    private lateinit var rvInterests: RecyclerView
    private lateinit var pbLoading: ProgressBar
    lateinit var callback: PersonalizeCallback
    private var mUserDetails: UserResponse? = null
    private var mResponseModel: InterestResponseModel? = null
    var saveCallback: PersonaliseBottomSheetFragment.SavePersonalization? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_interests, container, false)
        val tvIntro = view.findViewById<TextView>(R.id.tvIntro)
        btnSave = view.findViewById(R.id.btnSave)
        Card.setFontFamily(tvIntro, true)
        Card.setFontFamily(btnSave, true)
        rvInterests = view.findViewById(R.id.rvInterests)
        rvlanguges = view.findViewById(R.id.rvLanguages)
        pbLoading = view.findViewById(R.id.pbLoading)
        etSearch = view.findViewById(R.id.et_search)
        flexLayoutManager = FlexboxLayoutManager(requireContext())
        flexLayoutManager.justifyContent = JustifyContent.CENTER
        preferenceAdapter = NewInterestAdapter(ArrayList(), null)
        languageAdapter = LanguageAdapter(ArrayList())

        rvInterests.apply {
            adapter = preferenceAdapter
            layoutManager = flexLayoutManager
            itemAnimator = null
        }
        rvlanguges.apply {
            adapter = languageAdapter
            layoutManager = LinearLayoutManager(requireActivity())
            itemAnimator = null
        }

        if (pos == 0) {
            rvInterests.visibility = View.VISIBLE
            rvlanguges.visibility = View.GONE
            tvIntro.visibility = View.GONE
            etSearch.visibility = View.VISIBLE
            ApiGetInterests().getInterestsEncrypted(
                Endpoints.GET_INTERESTS_ENCRYPTED,
                object : ApiGetInterests.InterestResponseListener {
                    override fun onSuccess(interestResponseModel: InterestResponseModel) {
                        mResponseModel = interestResponseModel
                        setUpInterests()
                    }
                })
            ApiUserDetails().getUserResponseEncrypted(
                Endpoints.USER_DETAILS_ENCRYPTED,
                object : ApiUserDetails.UserResponseListener {
                    override fun onSuccess(userDetails: UserResponse) {
                        mUserDetails = userDetails
                        setUpInterests()
                    }
                })
        } else {
            rvInterests.visibility = View.GONE
            tvIntro.visibility = View.GONE
            rvlanguges.visibility = View.VISIBLE
            etSearch.visibility = View.GONE
            ApiGetLanguages().getLanguagesEncrypted(
                Endpoints.GET_LANGUAGES_ENCRYPTED,
                object : ApiGetLanguages.LanguageResponseListener {
                    override fun onSuccess(languageResponseModel: List<Language>) {
                        pbLoading.visibility = View.GONE
                        for (language in FeedSdk.languagesList) {
                            for (languageIndex in 0 until languageResponseModel.size) {
                                if (language.id.toLowerCase(Locale.ROOT).equals(
                                        languageResponseModel[languageIndex].id.toLowerCase(
                                            Locale.ROOT
                                        ), true
                                    )
                                ) {
                                    languageResponseModel[languageIndex].isSelected = true
                                    break
                                }
                            }
                        }
                        languageAdapter.updateList(languageResponseModel)
                    }
                })
        }

        btnSave.setOnClickListener {
            saveCallback?.onSave(pos)
        }
        return view
    }

    private fun setUpInterests() {
        if (mUserDetails == null || mResponseModel == null) {
            return
        }
        pbLoading.visibility = View.GONE
        val list = mResponseModel!!.interestList
        list.forEach { interest ->
            if (FeedSdk.interestsList.contains(interest)) {
                interest.userSelected = true
            }
        }
        for (interest in FeedSdk.interestsList) {
            for (interestIndex in list.indices) {
                if (interest.keyId.equals(list[interestIndex].keyId, true)) {
                    list[interestIndex].isSelected = true
                    break
                }
            }

        }
        if (!mUserDetails?.user?.pinnedInterests.isNullOrEmpty()) {
            list.forEach { interests ->
                if (mUserDetails?.user!!.pinnedInterests.contains(interests.keyId)) {
                    interests.isPinned = true
                }
            }
        }

        etSearch.addTextChangedListener { s ->
            try {
                val filteredList = list.filter { interest ->
                    interest.label!!.lowercase(
                        Locale.getDefault()
                    )
                        .contains(s.toString().lowercase(Locale.getDefault()))
                }
                preferenceAdapter.updateData(filteredList as ArrayList<Interest>)
            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }
        preferenceAdapter.updateData(list as ArrayList<Interest>)
    }

    fun savePinnedInterests(): ArrayList<Interest>? {
        return try {
            preferenceAdapter.getItems().filter { interest ->
                interest.isPinned
            } as java.util.ArrayList<Interest>?

        } catch (e: Exception) {
            ArrayList()
        }
    }

    fun saveInterests(): ArrayList<Interest>? {
        return try {
            val interestList = preferenceAdapter.getItems().filter { interest ->
                interest.userSelected
            } as java.util.ArrayList<Interest>?
            if (interestList?.size!! < 3) {
                Toast.makeText(
                    requireContext(),
                    "Please select atleast 3 categories!",
                    Toast.LENGTH_SHORT
                ).show()
                ArrayList()
            } else {
                interestList
            }
        } catch (e: java.lang.Exception) {
            ArrayList()
        }
    }

    fun saveLanguages(): ArrayList<Language> {
        return try {
            val languageList = languageAdapter.getItems().filter { language ->
                language.isSelected
            } as ArrayList<Language>?
            if (languageList?.isEmpty()!!) {
                Toast.makeText(
                    requireContext(),
                    "Please choose atleast one language!",
                    Toast.LENGTH_SHORT
                ).show()
                ArrayList()
            } else {
                languageList
            }
        } catch (e: java.lang.Exception) {
            ArrayList()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            position: Int,
            personalizeCallback: PersonalizeCallback,
            savePersonalization: PersonaliseBottomSheetFragment.SavePersonalization?
        ) =
            PersonalizeFragment().apply {
                arguments = Bundle().apply {
                    saveCallback = savePersonalization
                    callback = personalizeCallback
                    pos = position
                }
            }
    }
}
