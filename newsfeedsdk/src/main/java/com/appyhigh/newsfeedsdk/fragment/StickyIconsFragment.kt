package com.appyhigh.newsfeedsdk.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.Constants.searchStickyModel
import com.appyhigh.newsfeedsdk.adapter.StickyGridAdapter
import com.appyhigh.newsfeedsdk.callbacks.StickyIconListener
import com.appyhigh.newsfeedsdk.databinding.FragmentStickyIconsBinding
import com.appyhigh.newsfeedsdk.model.SearchStickyItemModel
import com.appyhigh.newsfeedsdk.utils.SpUtil

class StickyIconsFragment : Fragment(), StickyIconListener {

    lateinit var binding: FragmentStickyIconsBinding
    var iconsList = ArrayList<SearchStickyItemModel>()
    var backupList = ArrayList<SearchStickyItemModel>()
    var adapter:StickyGridAdapter?=null

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
        SpUtil.stickyIconListener = this
        setData()
    }

    private fun setData(){
        val selectedIconMap = HashMap<String, Int>()
        val iconExpandMap = HashMap<String, Int>()
        for(icon in searchStickyModel.icons!!){
            selectedIconMap[icon] = 1
        }
        val allIcons = arrayListOf("Camera", "News", "Whatsapp", "Video", "Weather", "Messages", "Flashlight", "Alarm", "Call", "Email", "Calendar")
        val isColored = searchStickyModel.type!! == "color"
        iconsList = ArrayList()
        iconsList.add(SearchStickyItemModel("Solid", 0, true, "HEADER"))
        for(icon in allIcons){
            if(!isColored && selectedIconMap.containsKey(icon)){
                iconsList.add(SearchStickyItemModel(icon, Constants.getWidgetImage(false, icon), true, "solid"))
            } else{
                iconsList.add(SearchStickyItemModel(icon, Constants.getWidgetImage(false, icon), false, "solid"))
            }
        }
        iconsList.add(SearchStickyItemModel("Colour", 0, true, "HEADER"))
        for(icon in allIcons){
            if(isColored && selectedIconMap.containsKey(icon)){
                iconsList.add(SearchStickyItemModel(icon, Constants.getWidgetImage(true, icon), true, "color"))
            } else{
                iconsList.add(SearchStickyItemModel(icon, Constants.getWidgetImage(true, icon), false, "color"))
            }
        }
        backupList = ArrayList(iconsList)
        adapter = StickyGridAdapter(backupList, iconsList, if(isColored) "color" else "solid", "ICON", iconExpandMap)
        binding.rvIcons.adapter = adapter
        binding.rvIcons.layoutManager = GridLayoutManager(requireContext(), 4)
        (binding.rvIcons.layoutManager as GridLayoutManager).spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if(iconsList[position].type=="HEADER"){
                    4
                } else 1
            }
        }
    }

    override fun onRefresh() {
        setData()
    }

    override fun onUnSelected(iconName: String, type: String) {
        if(type == "color"){
            for (i in 13 until 24){
                if(iconsList[i].iconName == iconName){
                    iconsList[i].isSelected = false
                    adapter?.notifyItemChanged(i)
                    break
                }
            }
        } else{
            for (i in 1 until 12){
                if(iconsList[i].iconName == iconName){
                    iconsList[i].isSelected = false
                    adapter?.notifyItemChanged(i)
                    break
                }
            }
        }
    }

}