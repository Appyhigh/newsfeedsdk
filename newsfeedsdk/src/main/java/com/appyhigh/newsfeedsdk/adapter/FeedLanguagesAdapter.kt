package com.appyhigh.newsfeedsdk.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.appyhigh.newsfeedsdk.Constants
import com.appyhigh.newsfeedsdk.R
import com.appyhigh.newsfeedsdk.model.Language

class FeedLanguagesAdapter(var languageList: ArrayList<Language>) :
    RecyclerView.Adapter<FeedLanguagesAdapter.LanguageViewHolder>() {

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
    private var languageColors = arrayListOf(
        R.color.language_color_one,
        R.color.language_color_two,
        R.color.language_color_three,
        R.color.language_color_four,
        R.color.language_color_five,
        R.color.language_color_six,
        R.color.language_color_seven,
        R.color.language_color_eight
    )

    class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNativeText: AppCompatTextView = itemView.findViewById(R.id.tvNativeText)
        val tvLanguage: AppCompatTextView = itemView.findViewById(R.id.tvLanguage)
        val rlRoot: RelativeLayout = itemView.findViewById(R.id.rlRoot)
        val ivSelected: AppCompatImageView = itemView.findViewById(R.id.ivSelected)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedLanguagesAdapter.LanguageViewHolder {
        return LanguageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_language_mf, parent, false)
        )
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onBindViewHolder(holder: FeedLanguagesAdapter.LanguageViewHolder, position: Int) {
        val language = languageList[holder.bindingAdapterPosition]
        holder.tvLanguage.text = language.language
        holder.tvNativeText.text = language.nativeName
        if (language.isSelected) {
            holder.tvLanguage.setTextColor(Color.parseColor("#FFFFFF"))
            holder.tvNativeText.setTextColor(Color.parseColor("#FFFFFF"))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                holder.rlRoot.backgroundTintList =
                    holder.itemView.context.resources.getColorStateList(
                        languageColors[holder.bindingAdapterPosition % 7],
                        holder.itemView.context.theme
                    )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.rlRoot.backgroundTintList =
                        holder.itemView.context.resources.getColorStateList(
                            languageColors[holder.bindingAdapterPosition % 7]
                        )
                }
            }
            holder.ivSelected.setImageResource(R.drawable.ic_tick_selected)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.rlRoot.backgroundTintList = null
            }
            holder.rlRoot.setBackgroundResource(languageBgs[holder.bindingAdapterPosition % 7])
            holder.tvLanguage.setTextColor(Color.parseColor(Constants.languageColors[holder.bindingAdapterPosition % 7]))
            holder.tvNativeText.setTextColor(Color.parseColor(Constants.languageColors[holder.bindingAdapterPosition % 7]))
            holder.ivSelected.setImageResource(R.drawable.ic_tick)
        }

        holder.itemView.setOnClickListener {
            languageList[holder.bindingAdapterPosition].isSelected =
                !languageList[holder.bindingAdapterPosition].isSelected
            notifyItemChanged(holder.bindingAdapterPosition)
        }

    }

    override fun getItemCount(): Int {
        return languageList.size
    }

    fun getItems():ArrayList<Language> {
        return languageList
    }
}