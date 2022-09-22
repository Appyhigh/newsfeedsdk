package com.appyhigh.newsfeedsdk.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.adapter.PublisherBlocklistAdapter
import com.appyhigh.newsfeedsdk.apicalls.ApiGetPublisherPosts
import com.appyhigh.newsfeedsdk.callbacks.BlockPublisherClickListener
import com.appyhigh.newsfeedsdk.databinding.ActivityBlockPublishersBinding
import com.appyhigh.newsfeedsdk.model.PublisherBlocklistModel
import com.appyhigh.newsfeedsdk.model.PublisherDetail
import com.appyhigh.newsfeedsdk.model.feeds.Card
import com.appyhigh.newsfeedsdk.utils.SpUtil
import java.util.*
import kotlin.collections.ArrayList

class PublisherBlockActivity: AppCompatActivity() {

    private var binding: ActivityBlockPublishersBinding? = null
    private var blockList = ArrayList<PublisherDetail>()
    private var adapter: PublisherBlocklistAdapter?=null
    private var showSelector = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockPublishersBinding.inflate(layoutInflater)
        val view = binding?.root
        setContentView(view)
        Card.setFontFamily(binding?.tvTitle,  false)
        Card.setFontFamily(binding?.tvSelectAll, false)
        Card.setFontFamily(binding?.btnProceed)
        Constants.setFontFamily(binding!!.etSearch)
        try{
            val blockedPublishers = Constants.userDetails!!.blockedPublishers
            ApiGetPublisherPosts().getPublisherBlocklist(blockedPublishers, object: ApiGetPublisherPosts.PublisherBlocklistListener{
                override fun onSuccess(response: PublisherBlocklistModel) {
                    binding!!.pbLoading.visibility = View.GONE
                    blockList = response.publisherDetails as ArrayList<PublisherDetail>
                    if(blockList.isEmpty()){
                        binding!!.tvNoPublisher.visibility = View.VISIBLE
                        binding!!.rvBlockList.visibility = View.GONE
                        binding!!.tvSelectAll.visibility = View.GONE
                        binding!!.etSearch.visibility = View.GONE
                    }else{
                        val adapterBlockList = ArrayList<PublisherDetail>()
                        adapterBlockList.addAll(blockList)
                        adapter = PublisherBlocklistAdapter(adapterBlockList, object : BlockPublisherClickListener{
                            override fun onRefresh() {
                                if(blockList.isEmpty()){
                                    binding!!.tvNoPublisher.visibility = View.VISIBLE
                                    binding!!.rvBlockList.visibility = View.GONE
                                    binding!!.tvSelectAll.visibility = View.GONE
                                    binding!!.etSearch.visibility = View.GONE
                                    binding!!.headerLayout.visibility = View.VISIBLE
                                    binding!!.closeSelector.visibility = View.GONE
                                    binding!!.btnProceed.visibility = View.GONE
                                }
                            }

                            override fun onRemove(publisherDetail: PublisherDetail) {
                                blockList.remove(publisherDetail)
                            }
                        })
                        binding!!.rvBlockList.adapter = adapter
                        binding!!.btnProceed.setOnClickListener {
                            adapter?.updateBlockList()
                        }
                    }
                }
            })
        } catch (ex:Exception){ }
        binding!!.etSearch.addTextChangedListener { s ->
            try {
                val filteredList = blockList.filter {
                    it.fullname!!.lowercase(
                        Locale.getDefault()
                    ).contains(s.toString().lowercase(Locale.getDefault()))
                }
                if(filteredList.isEmpty()){
                    binding!!.tvNoPublisher.visibility = View.VISIBLE
                    binding!!.rvBlockList.visibility = View.GONE
                }else{
                    binding!!.tvNoPublisher.visibility = View.GONE
                    binding!!.rvBlockList.visibility = View.VISIBLE
                    adapter?.updateData(filteredList as ArrayList<PublisherDetail>)
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        binding!!.tvSelectAll.setOnClickListener {
            showSelector = true
            adapter?.showSelector(showSelector)
            binding!!.headerLayout.visibility = View.GONE
            binding!!.closeSelector.visibility = View.VISIBLE
            binding!!.btnProceed.visibility = View.VISIBLE
        }
        binding!!.closeSelector.setOnClickListener {
            showSelector = false
            adapter?.showSelector(showSelector)
            binding!!.headerLayout.visibility = View.VISIBLE
            binding!!.closeSelector.visibility = View.GONE
            binding!!.btnProceed.visibility = View.GONE
        }
        binding!!.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if(adapter!!.isChanged) {
                for (listener in SpUtil.onRefreshListeners) {
                    listener.value.onRefreshNeeded()
                }
            }
        } catch (ex:Exception){}
    }
}