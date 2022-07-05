package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.adapter.StickyGridAdapter
import com.appyhigh.newsfeedsdk.databinding.FragmentStickyIconsBinding
import com.appyhigh.newsfeedsdk.model.SearchStickyItemModel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class StickyBackgroundsFragment : Fragment() {

    lateinit var binding: FragmentStickyIconsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStickyIconsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backgroundTypes = arrayListOf("Solid", "Gaming", "Fashion", "Beauty", "Education", "Tech", "Glass", "Miscellaneous")
        val backgroundSizes = arrayListOf(20,5,4,5,5,4,5,6)
        val backgroundList = ArrayList<SearchStickyItemModel>()
        val backupList = ArrayList(backgroundList)
        val backgroundMap = HashMap<String,Int>()
        for(position in 0 until backgroundTypes.size) {
            val backgroundTypeInSmall = backgroundTypes[position].lowercase(Locale.getDefault())
            backgroundList.add(SearchStickyItemModel(backgroundTypes[position], 0, false, "HEADER"))
            backupList.add(SearchStickyItemModel(backgroundTypes[position], 0, false, "HEADER"))
            for (i in 0 until backgroundSizes[position]) {
                val iconName = backgroundTypes[position].lowercase(Locale.getDefault()) + "_" + i
                val itemModel = SearchStickyItemModel(iconName, 0, Constants.searchStickyModel.background == iconName,
                    if(backgroundTypeInSmall == "solid") "solid" else "color")
                backupList.add(itemModel)
            }
        }
        val adapter =  StickyGridAdapter(backupList, backgroundList, "", "BACKGROUND", backgroundMap)
        binding.rvIcons.adapter = adapter
        binding.rvIcons.layoutManager = GridLayoutManager(requireContext(), 4)
        (binding.rvIcons.layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if(backgroundList[position].type=="HEADER"){
                    4
                } else 1
            }
        }

    }

}