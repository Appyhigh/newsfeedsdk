package com.appyhigh.newsfeedsdk.fragment

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.NewInterestAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiGetInterests
import com.appyhigh.newsfeedsdk.apicalls.ApiUpdateUserPersonalization
import com.appyhigh.newsfeedsdk.apicalls.ApiUserDetails
import com.appyhigh.newsfeedsdk.apiclient.Endpoints
import com.appyhigh.newsfeedsdk.callbacks.NewInterestClickListener
import com.appyhigh.newsfeedsdk.databinding.LayoutIntrestSelectBinding
import com.appyhigh.newsfeedsdk.encryption.LogDetail
import com.appyhigh.newsfeedsdk.model.Interest
import com.appyhigh.newsfeedsdk.model.InterestResponseModel
import com.appyhigh.newsfeedsdk.model.User
import com.appyhigh.newsfeedsdk.model.UserResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*
import kotlin.collections.ArrayList

const val isFirstUse="ISFIRSTUSE"

class AddInterestBottomSheet :
    BottomSheetDialogFragment() {
    private lateinit var rvAllInterests: RecyclerView
    private var mUserDetails: User? = null
    private lateinit var interestChooserAdapter: NewInterestAdapter
    private var mInterestResponseModel: InterestResponseModel? = null
    private var interestMap = HashMap<String, Interest>()
    private var ivClose: AppCompatImageView? = null
    var interestList = ArrayList<Interest>()
    var allInterestList = ArrayList<Interest>()
    private var etSearch: AppCompatEditText? = null
    private var isChanged = false
    private var invalidSearch: AppCompatTextView? = null

    companion object {
        fun newInstance(): AddInterestBottomSheet {
            return AddInterestBottomSheet()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft: FragmentTransaction = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            LogDetail.LogEStack(e)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = LayoutIntrestSelectBinding.inflate(inflater)
        ivClose = binding.backBtn
        invalidSearch = binding.invalidTv
        ivClose?.setOnClickListener {
            dismiss()
        }
        etSearch = binding.etSearch
        binding.saveBtn.setOnClickListener {
            if(interestList.size < 3){
                Constants.Toaster.show(requireContext(),"Please select at least three interest")
            }else{
                updateInterests()
                dismiss()
            }
        }
        if(SpUtil.spUtilInstance?.getBoolean(isFirstUse) == null || SpUtil.spUtilInstance!!.getBoolean(
                isFirstUse)
        ){
            binding.flFirstUse.visibility = View.VISIBLE
        }
        binding.flFirstUse.setOnClickListener {
            binding.flFirstUse.visibility=View.GONE
            SpUtil.spUtilInstance?.putBoolean(isFirstUse,false)
        }
        rvAllInterests = binding.rvInterests
        Card.setFontFamily(binding.title)
        Constants.setFontFamily(etSearch)
        Constants.setFontFamily(binding.saveBtn, true)
        etSearch?.addTextChangedListener { s ->
            try {
                val filteredList = allInterestList.filter { interest ->
                    interest.label!!.lowercase(
                        Locale.getDefault()
                    )
                        .contains(s.toString().lowercase(Locale.getDefault()))
                }
                if(filteredList.isEmpty()){
                    invalidSearch?.visibility = View.VISIBLE
                    rvAllInterests.visibility = View.GONE
                    binding.saveBtn.visibility = View.GONE
                }else{
                    invalidSearch?.visibility = View.GONE
                    rvAllInterests.visibility = View.VISIBLE
                    binding.saveBtn.visibility = View.VISIBLE
                    interestChooserAdapter.updateData(filteredList as ArrayList<Interest>)
                }

            } catch (ex: Exception) {
                LogDetail.LogEStack(ex)
            }
        }
        ApiGetInterests().getInterestsEncrypted(
            Endpoints.GET_INTERESTS_ENCRYPTED,
            object : ApiGetInterests.InterestResponseListener {
                override fun onSuccess(interestResponseModel: InterestResponseModel) {
                    mInterestResponseModel = interestResponseModel
                    setUpInterests(binding)
                }
            })

        ApiUserDetails().getUserResponseEncrypted(
            Endpoints.USER_DETAILS_ENCRYPTED,
            object : ApiUserDetails.UserResponseListener{
                override fun onSuccess(userDetails: UserResponse) {
                    mUserDetails = userDetails.user
                    setUpInterests(binding)
                }
            }
        )
        return binding.root
    }

    private fun setUpInterests(binding: LayoutIntrestSelectBinding) {
        var selectedInterestsList = ArrayList<Interest>()
        if (mUserDetails != null && mInterestResponseModel != null) {
            binding.pbLoading.visibility = View.GONE
            for (interest in mInterestResponseModel?.interestList!!) {
                interestMap[interest.keyId!!] = interest
            }
            if (mUserDetails?.interests.isNullOrEmpty()) {
                selectedInterestsList = ArrayList()
            } else {
                for (interest in interestMap.values) {
                    if (mUserDetails?.interests!!.contains(interest.keyId)) {
                        selectedInterestsList.add(interest)
                    }
                }
            }
            interestList = selectedInterestsList
            allInterestList = mInterestResponseModel?.interestList as ArrayList<Interest>

            allInterestList.forEach { interest ->
                if (interestList.contains(interest)) {
                    interest.userSelected = true
                }
            }
            if (!mUserDetails?.pinnedInterests.isNullOrEmpty()) {
                allInterestList.forEach { interests ->
                    if (mUserDetails?.pinnedInterests!!.contains(interests.keyId)) {
                        interests.isPinned = true
                    }
                }
            }

            interestChooserAdapter = NewInterestAdapter(
                allInterestList,
                object : NewInterestClickListener {
                    override fun onInterestPinned(v: View, position: Int, isPinned: Boolean) {
                        isChanged = true
                        if(isPinned){
                            if(!interestList.contains(allInterestList[position])) {
                                interestList.add(allInterestList[position])
                            }
                            interestList[interestList.indexOf(allInterestList[position])].isPinned = true
                            interestList[interestList.indexOf(allInterestList[position])].userSelected = true
                        } else if(interestList.indexOf(allInterestList[position]) == -1) {
                            // interest wants to be unpinned but not present in interest list
                        }else{
                            interestList[interestList.indexOf(allInterestList[position])].isPinned = false
                        }
                    }

                    override fun onInterestFollowed(v: View, position: Int, isSelected: Boolean) {
                        isChanged = true
                        allInterestList[position].userSelected = isSelected
                        if (isSelected) {
                            interestList.add(allInterestList[position])
                        } else {
                            allInterestList[position].isPinned = false
                            interestList.remove(allInterestList[position])
                        }
                    }
                }
            )

            if(isAdded){
                rvAllInterests.apply {
                    adapter = interestChooserAdapter
                    layoutManager = LinearLayoutManager(requireActivity())
                    addItemDecoration(
                        DividerItemDecoration(
                            requireContext(),
                            DividerItemDecoration.VERTICAL
                        ).also {
                            it.setDrawable(ColorDrawable(ContextCompat.getColor(requireContext(),R.color.dividerColor)))
                        }
                    )
                }
            }
        }
    }

    private fun updateInterests() {
        if (interestList.size > 2) {
            ApiUpdateUserPersonalization().updateUserPersonalizationEncrypted(
                Endpoints.UPDATE_USER_ENCRYPTED,
                interestList,
                ArrayList(),
                object : ApiUpdateUserPersonalization.UpdatePersonalizationListener {
                    override fun onFailure() {
                        try{
                            Constants.Toaster.show(requireContext(),"Please try again")
                        } catch (ex:Exception){
                            LogDetail.LogEStack(ex)
                        }
                    }

                    override fun onSuccess() {
                        FeedSdk.interestsList = interestList
                        for (listener in SpUtil.onRefreshListeners) {
                            listener.value.onRefreshNeeded()
                        }
                    }
                },
            )
        }
    }
}
