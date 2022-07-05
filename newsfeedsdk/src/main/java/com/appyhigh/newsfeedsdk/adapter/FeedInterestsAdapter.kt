package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.model.Interest
import com.bumptech.glide.Glide

class FeedInterestsAdapter(var interestList: ArrayList<Interest>) :
    RecyclerView.Adapter<FeedInterestsAdapter.InterestViewHolder>() {

    private var languageBgs = arrayListOf(
        R.drawable.interest_bg_one,
        R.drawable.interest_bg_two,
        R.drawable.interest_bg_three,
        R.drawable.interest_bg_four,
        R.drawable.interest_bg_five,
        R.drawable.interest_bg_six,
        R.drawable.interest_bg_seven,
        R.drawable.interest_bg_eight
    )
    private var interestColors = arrayListOf(
        R.color.interest_color_one,
        R.color.interest_color_two,
        R.color.interest_color_three,
        R.color.interest_color_four,
        R.color.interest_color_five,
        R.color.interest_color_six,
        R.color.interest_color_seven,
        R.color.interest_color_eight,
        R.color.interest_color_nine
    )

    class InterestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: AppCompatTextView = itemView.findViewById(R.id.tvTitle)
        val rlRoot: RelativeLayout = itemView.findViewById(R.id.rlRoot)
        val ivSelected: AppCompatImageView = itemView.findViewById(R.id.ivSelected)
        val vSelected: View = itemView.findViewById(R.id.vSelected)
        val ivIcon: AppCompatImageView = itemView.findViewById(R.id.ivIcon)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedInterestsAdapter.InterestViewHolder {
        return InterestViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_interest_mf, parent, false)
        )
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onBindViewHolder(holder: FeedInterestsAdapter.InterestViewHolder, position: Int) {
        val interest = interestList[holder.bindingAdapterPosition]
        holder.tvTitle.text = interest.label
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            holder.rlRoot.backgroundTintList =
                holder.itemView.context.resources.getColorStateList(
                    interestColors[holder.bindingAdapterPosition % 8],
                    holder.itemView.context.theme
                )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.rlRoot.backgroundTintList =
                    holder.itemView.context.resources.getColorStateList(
                        interestColors[holder.bindingAdapterPosition % 8]
                    )
            }
        }


        if (interest.userSelected) {
            holder.vSelected.visibility = View.VISIBLE
            holder.ivSelected.visibility = View.VISIBLE
        } else {
            holder.vSelected.visibility = View.GONE
            holder.ivSelected.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(interest.thumbnails?.xxxhdpi)
            .error(R.drawable.ic_interest_placeholder)
            .into(holder.ivIcon)

        holder.itemView.setOnClickListener {
            interestList[holder.bindingAdapterPosition].userSelected =
                !interestList[holder.bindingAdapterPosition].userSelected
            notifyItemChanged(holder.bindingAdapterPosition)
        }

    }

    override fun getItemCount(): Int {
        return interestList.size
    }

    fun getItems(): ArrayList<Interest> {
        return interestList
    }
}