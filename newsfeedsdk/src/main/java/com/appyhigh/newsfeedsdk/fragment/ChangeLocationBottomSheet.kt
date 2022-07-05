package com.appyhigh.newsfeedsdk.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.FeedSdk
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.adapter.ChangeLocationAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiExplore
import com.appyhigh.newsfeedsdk.apicalls.ApiUpdateUserPersonalization
import com.appyhigh.newsfeedsdk.callbacks.LocationClickListener
import com.appyhigh.newsfeedsdk.databinding.LayoutSelectLocationBinding
import com.appyhigh.newsfeedsdk.model.StateListResponse
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.collections.ArrayList

const val LOCATION_DEF = "location_def"

class ChangeLocationBottomSheet : BottomSheetDialogFragment() {

    private var ivClose: AppCompatImageView? = null
    private var etSearch: EditText? = null
    private var rvLocation: RecyclerView? = null
    private var isChanged = false
    private var tvCurrLocation: TextView? = null
    private var rvAdapter: ChangeLocationAdapter? = null
    private var invalidTV: AppCompatTextView? = null
    private var currLocation = ""
    private var binding: LayoutSelectLocationBinding? = null

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
        binding = LayoutSelectLocationBinding.inflate(inflater)
        tvCurrLocation = binding?.currLocation
        tvCurrLocation?.visibility = View.GONE
        SpUtil.spUtilInstance?.getString(LOCATION_DEF)?.let {
            currLocation = it
        }
        FeedSdk.mUser?.state?.let {
            currLocation = it
        }
        ivClose = binding?.backBtn
        ivClose?.setOnClickListener { dismiss() }
        etSearch = binding?.etSearch
        invalidTV = binding?.invalidTv
        rvLocation = binding?.rvInterests
        Card.setFontFamily(binding?.title)
        Constants.setFontFamily(etSearch)
        Card.setFontFamily(binding?.currLocation)
        if(Constants.stateMap.isEmpty()){
            ApiExplore().getStateList(
                object : ApiExplore.StateResponseListener{
                    override fun onSuccess(response: StateListResponse, url: String, timeStamp: Long) {
                        response.cards[0].items.forEach {
                            Constants.stateMap[it.state] = it.stateCode
                        }
                        setData()
                    }
                }
            )
        }else {
            setData()
        }
        return binding!!.root
    }

    private fun setData(){
        binding?.progressLayout?.visibility = View.GONE
        tvCurrLocation?.visibility = View.VISIBLE
        val statesList = Constants.stateMap.keys.toList() as ArrayList<String>
        var list = ArrayList<String>()
        list.addAll(statesList)
        if(currLocation.isNotEmpty()){
            try{
                tvCurrLocation?.text = currLocation
                list.remove(currLocation)
            } catch (ex:Exception){
                ex.printStackTrace()
            }
        } else {
            tvCurrLocation?.text = getString(R.string.location_msg)
        }
        rvAdapter = ChangeLocationAdapter(
            list,
            object : LocationClickListener {
                override fun onLocationClicked(v: View, position: Int) {
                    isChanged = true
                    currLocation = list[position]
                    tvCurrLocation?.text = currLocation
                    list = ArrayList()
                    list.addAll(statesList)
                    list.remove(currLocation)
                    rvAdapter?.updateData(list)
                }

            }
        )
        rvLocation?.apply {
            adapter = rvAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL))
        }
        etSearch?.addTextChangedListener { s ->
            try {
                val filteredList = statesList.filter { item ->
                    item.lowercase().contains(s.toString().lowercase())
                }
                list = filteredList as ArrayList<String>
                if(currLocation.isNotEmpty()){
                    list.remove(currLocation)
                }
                if(filteredList.isEmpty()){
                    rvLocation!!.visibility = View.GONE
                    invalidTV?.visibility = View.VISIBLE
                }else{
                    rvLocation!!.visibility = View.VISIBLE
                    invalidTV?.visibility = View.GONE
                    rvAdapter?.updateData(filteredList)
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(isChanged){
            ApiUpdateUserPersonalization().updateUserState(
                tvCurrLocation?.text!!.toString(),
                object : ApiUpdateUserPersonalization.UpdatePersonalizationListener{
                    override fun onSuccess() {
                        for(listener in SpUtil.onRefreshListeners){
                            listener.value.onRefreshNeeded()
                        }
                    }

                    override fun onFailure() {
                        Constants.Toaster.show(FeedSdk.mContext!!,"Please try again")
                    }

                }
            )
            SpUtil.spUtilInstance?.putString(LOCATION_DEF, tvCurrLocation!!.text as String)
        }
    }

}
